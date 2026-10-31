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

    test("a new group shows who created it, when, and that it has one member", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);

        await createGroup(page, "Book club");

        await expect(page.locator(".group-stats")).toHaveText("1 member");
        await expect(profileField(page, "Created by")).toHaveText("Tester Example");
        await expect(profileField(page, /^Created$/)).toHaveText(/^\d{2}\.\d{2}\.\d{4}$/);
        // Nothing filled in, nothing shown.
        await expect(page.locator(".group-description")).toHaveCount(0);
        await expect(page.locator(".group-topic")).toHaveCount(0);
        await close();
    });

    test("topic, description and link are shown to visitors, the topic also in search", async ({ browser }) => {
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
        await expect(owner.page).toHaveURL(/getGroupPage\?groupId=\d+/);

        const visitor = await newUserSession(browser);
        await visitor.page.goto(`/searchResult/getSearchResult?searchText=${name}`);
        const card = visitor.page.locator(".list-card").filter({ hasText: name });
        await expect(card).toContainText("Games · 1 member");
        await card.getByRole("link", { name }).click();

        await expect(visitor.page.locator(".group-topic")).toHaveText("Games");
        await expect(visitor.page.locator(".group-description")).toHaveText("Weekly games.\nBeginners welcome.");
        const link = profileField(visitor.page, "Website").getByRole("link");
        await expect(link).toHaveAttribute("href", "https://example.com/club");
        await expect(link).toHaveAttribute("target", "_blank");
        await expect(link).toHaveAttribute("rel", /noopener/);

        await visitor.page.getByRole("button", { name: "Join group" }).click();
        // The button turns into "Leave group" once the server has taken the join.
        await expect(visitor.page.getByRole("button", { name: "Leave group" })).toBeVisible();
        await visitor.page.reload();
        await expect(visitor.page.locator(".group-stats")).toContainText("2 members");
        await owner.close();
        await visitor.close();
    });

    test("a link that is not a web address is refused and the typed details stay", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        const groupId = await createGroup(page, "Film club");

        await page.getByRole("link", { name: "Manage group" }).click();
        await fillDetails(page, { topic: "Arts & culture", description: "Old films.", website: "javascript:alert(1)" });
        // The browser's own check of type="url" would stop the form before the server sees it.
        await page.locator("#regForm").evaluate(form => form.setAttribute("novalidate", ""));
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errorText-top")).toHaveText(
            "The link must be a web address starting with http:// or https://.");
        await expect(page.locator("#description")).toHaveValue("Old films.");
        await expect(page.locator("#category")).toHaveValue("ARTS");
        await expect(page.locator("#name")).toHaveValue("Film club");

        await page.goto(`/searchResult/getGroupPage?groupId=${groupId}`);
        await expect(page.locator(".group-description")).toHaveCount(0);
        await close();
    });

    test("the owner edits the details and can clear them again", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await createGroup(page, "Running club");

        await page.getByRole("link", { name: "Manage group" }).click();
        await fillDetails(page, { topic: "Sports", description: "Morning runs.", website: "https://example.org" });
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page.locator(".group-topic")).toHaveText("Sports");
        await expect(page.locator(".group-description")).toHaveText("Morning runs.");

        await page.getByRole("link", { name: "Manage group" }).click();
        await expect(page.locator("#description")).toHaveValue("Morning runs.");
        await fillDetails(page, { topic: "No topic", description: "", website: "" });
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".group-topic")).toHaveCount(0);
        await expect(page.locator(".group-description")).toHaveCount(0);
        await expect(profileField(page, "Website")).toHaveCount(0);
        await close();
    });

    test("with \"Other\" the owner names the topic, and that name is shown", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        const name = uniqueName("Club");
        await page.goto("/group/getGroupCreationForm");
        await page.locator("#name").fill(name);
        const ownTopic = page.locator("#customCategory");
        await expect(ownTopic).toBeHidden();

        await page.locator("#category").selectOption({ label: "Other" });
        await expect(ownTopic).toBeVisible();
        await page.getByRole("button", { name: "Create" }).click();
        // "Other" alone is not enough.
        await expect(page.getByText("Name the topic, or choose one from the list.")).toBeVisible();
        await expect(page.locator("#category")).toHaveValue("OTHER");

        await ownTopic.fill("Urban gardening");
        await page.getByRole("button", { name: "Create" }).click();
        await expect(page.locator(".group-topic")).toHaveText("Urban gardening");

        await page.goto(`/searchResult/getSearchResult?searchText=${name}`);
        await expect(page.locator(".list-card").filter({ hasText: name })).toContainText("Urban gardening · 1 member");
        await close();
    });
});
