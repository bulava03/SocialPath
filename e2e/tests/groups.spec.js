import { test, expect } from "@playwright/test";
import { createGroup, newUserSession, post, publish } from "./helpers.js";

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
