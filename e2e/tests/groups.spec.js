import { test, expect } from "@playwright/test";
import { createGroup, newUserSession, post, profileField, publish, uniqueName } from "./helpers.js";

test.describe("groups", () => {
    test("the creator owns the group: can manage and post, cannot leave", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);

        const groupId = await createGroup(page, "Hiking club");

        await expect(page.locator("#left h1")).toHaveText("Hiking club");
        await expect(page.getByRole("link", { name: "Manage group" })).toBeVisible();
        await expect(page.locator("#leaveButton")).toHaveCount(0);
        await expect(page.locator("#joinButton")).toHaveCount(0);

        await publish(page, "First trip on Saturday");
        await page.goto(`/searchResult/getGroupPage?groupId=${groupId}`);
        await expect(post(page, "First trip on Saturday")).toBeVisible();
        await close();
    });

    test("an empty group name is rejected", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/group/getGroupCreationForm");

        await page.getByRole("button", { name: "Create" }).click();

        await expect(page.locator(".errorText").filter({ hasText: /\S/ })).toBeVisible();
        await expect(page).toHaveURL(/createGroup/);
        await close();
    });

    test("a visitor joins to post and leaves again", async ({ browser }) => {
        const owner = await newUserSession(browser);
        const groupId = await createGroup(owner.page, "Book club");
        const visitor = await newUserSession(browser);
        const { page } = visitor;
        const groupPage = `/searchResult/getGroupPage?groupId=${groupId}`;

        await page.goto(groupPage);
        await expect(page.getByRole("link", { name: "Manage group" })).toHaveCount(0);
        await expect(page.locator("#new-publication-form")).toHaveCount(0);

        await page.locator("#joinButton").click();
        await expect(page.locator("#leaveButton")).toHaveText("Leave group");

        await page.reload();
        await publish(page, "Glad to be here");

        await page.locator("#leaveButton").click();
        await expect(page.locator("#joinButton")).toHaveText("Join group");
        await page.reload();
        await expect(page.locator("#new-publication-form")).toHaveCount(0);
        // A former member's post stays, but they can no longer reply in the group.
        await expect(post(page, "Glad to be here").locator(":scope > .reply-btn")).toHaveCount(0);

        await owner.close();
        await visitor.close();
    });

    test("only the owner may open the group settings", async ({ browser }) => {
        const owner = await newUserSession(browser);
        const groupId = await createGroup(owner.page, "Chess club");
        const stranger = await newUserSession(browser);

        await stranger.page.goto(`/group/getGroupUpdatingForm?id=${groupId}`);

        await expect(stranger.page.locator("h1")).toHaveText("Access denied");
        await owner.close();
        await stranger.close();
    });

    test("the group owner renames the group", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        const groupId = await createGroup(page, "Old name");

        await page.getByRole("link", { name: "Manage group" }).click();
        await page.locator("#name").fill("New name");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page).toHaveURL(new RegExp(`groupId=${groupId}`));
        await expect(page.locator("#left h1")).toHaveText("New name");
        await close();
    });

    test("a missing group shows the not found page", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);

        const response = await page.goto("/searchResult/getGroupPage?groupId=999999999");

        expect(response.status()).toBe(404);
        await expect(page.locator("h1")).toHaveText("Page not found");
        await page.getByRole("link", { name: "To my page" }).click();
        await expect(page).toHaveURL(/\/user\/authorisation/);
        await close();
    });
});

test.describe("group details", () => {
    /** Fills the details on the group creation or edit form. */
    async function fillDetails(page, { topic, description, website }) {
        await page.locator("#category").selectOption({ label: topic });
        await page.locator("#description").fill(description);
        await page.locator("#website").fill(website);
    }

    test("a group's details are shown on its page and in search, the member count grows", async ({ browser }) => {
        const owner = await newUserSession(browser);
        const name = uniqueName("Club");
        await owner.page.goto("/group/getGroupCreationForm");
        await owner.page.locator("#name").fill(name);
        await fillDetails(owner.page, {
            topic: "Games",
            description: "Weekly games.\nBeginners welcome.",
            website: "https://example.com/club",
        });
        await owner.page.getByRole("button", { name: "Create" }).click();

        await expect(owner.page.locator(".group-stats")).toHaveText(/Games\s+1 member/);
        await expect(owner.page.locator(".group-description")).toHaveText("Weekly games.\nBeginners welcome.");
        await expect(profileField(owner.page, "Created by")).toHaveText("Tester Example");
        await expect(profileField(owner.page, /^Created$/)).toHaveText(/^\d{2}\.\d{2}\.\d{4}$/);
        const link = profileField(owner.page, "Website").getByRole("link");
        await expect(link).toHaveAttribute("href", "https://example.com/club");
        await expect(link).toHaveAttribute("target", "_blank");
        await expect(link).toHaveAttribute("rel", /noopener/);

        const visitor = await newUserSession(browser);
        await visitor.page.goto(`/searchResult/getSearchResult?searchText=${name}`);
        const card = visitor.page.locator(".list-card").filter({ hasText: name });
        await expect(card).toContainText("Games · 1 member");
        await card.getByRole("link", { name }).click();
        await visitor.page.getByRole("button", { name: "Join group" }).click();
        // The button turns into "Leave group" once the server has taken the join.
        await expect(visitor.page.getByRole("button", { name: "Leave group" })).toBeVisible();
        await visitor.page.reload();
        await expect(visitor.page.locator(".group-stats")).toContainText("2 members");
        await owner.close();
        await visitor.close();
    });

    // The rules themselves are checked in GroupFormTest; here the form keeps what was typed and the page follows.
    test("the owner edits the details: \"Other\" needs its own topic, empty fields disappear", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await createGroup(page, "Garden club");
        await page.getByRole("link", { name: "Manage group" }).click();
        const ownTopic = page.locator("#customCategory");
        await expect(ownTopic).toBeHidden();

        await fillDetails(page, { topic: "Other", description: "Weekend planting.", website: "" });
        await expect(ownTopic).toBeVisible();
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page.locator(".errorText-top")).toHaveText("Name the topic, or choose one from the list.");
        await expect(page.locator("#category")).toHaveValue("OTHER");
        await expect(page.locator("#description")).toHaveValue("Weekend planting.");

        await ownTopic.fill("Urban gardening");
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page.locator(".group-topic")).toHaveText("Urban gardening");
        await expect(page.locator(".group-description")).toHaveText("Weekend planting.");

        await page.getByRole("link", { name: "Manage group" }).click();
        await fillDetails(page, { topic: "No topic", description: "", website: "" });
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page.locator(".group-topic")).toHaveCount(0);
        await expect(page.locator(".group-description")).toHaveCount(0);
        await expect(profileField(page, "Website")).toHaveCount(0);
        await close();
    });
});
