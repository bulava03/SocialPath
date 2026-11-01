import { test, expect } from "@playwright/test";
import { newUserSession, uniqueName } from "./helpers.js";

const userPage = login => `/user/anotherUserPage?anotherUserLogin=${login}`;

test.describe("friends", () => {
    test("a friend request is sent, accepted and the friendship removed", async ({ browser }) => {
        const anna = await newUserSession(browser, { firstName: "Anna" });
        const ben = await newUserSession(browser, { firstName: "Ben" });

        // Ben asks; the button turns into "cancel" without a reload.
        await ben.page.goto(userPage(anna.user.login));
        await ben.page.locator("#submitInvite").click();
        await expect(ben.page.locator("#submitRemoveInvite")).toHaveText("Cancel friend request");

        // Anna sees the request in her list and on Ben's page.
        await anna.page.goto(`/user/getInvitationsToFriends?id=${anna.user.login}`);
        await expect(anna.page.getByText("Ben Example")).toBeVisible();
        await anna.page.goto(userPage(ben.user.login));
        await expect(anna.page.locator("#submitReject")).toBeVisible();
        await anna.page.locator("#submitAccept").click();
        await expect(anna.page.locator("#submitRemove")).toHaveText("Remove from friends");
        await expect(anna.page.locator("#submitReject")).toHaveCount(0);

        // Both see each other as friends.
        await ben.page.goto(`/user/getUsersFriends?id=${ben.user.login}`);
        await expect(ben.page.getByText("Anna Example")).toBeVisible();
        await ben.page.goto(userPage(anna.user.login));
        await expect(ben.page.locator("#submitRemove")).toBeVisible();

        await ben.page.locator("#submitRemove").click();
        await expect(ben.page.locator("#submitInvite")).toHaveText("Add friend");
        await anna.page.goto(`/user/getUsersFriends?id=${anna.user.login}`);
        await expect(anna.page.getByText("Ben Example")).toHaveCount(0);

        await anna.close();
        await ben.close();
    });

    test("a declined request disappears for both sides", async ({ browser }) => {
        const anna = await newUserSession(browser, { firstName: "Anna" });
        const ben = await newUserSession(browser, { firstName: "Ben" });

        await ben.page.goto(userPage(anna.user.login));
        await ben.page.locator("#submitInvite").click();
        await expect(ben.page.locator("#submitRemoveInvite")).toBeVisible();

        await anna.page.goto(userPage(ben.user.login));
        await anna.page.locator("#submitReject").click();
        await expect(anna.page.locator("#submitInvite")).toHaveText("Add friend");
        await expect(anna.page.locator("#submitAccept")).toHaveCount(0);

        await ben.page.reload();
        await expect(ben.page.locator("#submitInvite")).toHaveText("Add friend");

        await anna.close();
        await ben.close();
    });
});

test.describe("search", () => {
    test("finds others by name, never the searcher, and opens a profile", async ({ browser }) => {
        const targetName = uniqueName("Zq");
        const searcherName = uniqueName("Zq");
        const target = await newUserSession(browser, { firstName: targetName });
        const searcher = await newUserSession(browser, { firstName: searcherName });
        const { page } = searcher;

        // Both names in one search: each word is looked up on its own.
        await page.goto("/user/authorisation");
        await page.locator("#searchInput").fill(`${targetName} ${searcherName}`);
        await page.locator("#searchButton").click();

        await expect(page.getByRole("link", { name: `${searcherName} Example` })).toHaveCount(0);
        const result = page.getByRole("link", { name: `${targetName} Example` });
        await expect(result).toBeVisible();
        await result.click();
        await expect(page).toHaveURL(new RegExp(`anotherUserLogin=${target.user.login}`));
        await expect(page.locator("#left h1")).toHaveText(`${targetName} Example`);

        await target.close();
        await searcher.close();
    });
});
