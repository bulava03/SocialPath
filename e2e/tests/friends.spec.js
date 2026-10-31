import { test, expect } from "@playwright/test";
import { newUserSession } from "./helpers.js";

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

    test("nobody can befriend themselves, even with a forged request", async ({ browser }) => {
        // The page offers no such button, so send the requests directly.
        const { page, user, close } = await newUserSession(browser);

        const invite = await page.request.post("/friends/inviteToFriends", { form: { login: user.login } });
        expect(invite.status()).toBe(400);
        expect(await invite.json()).toEqual({ error: "You cannot add yourself as a friend." });

        // With no invite to accept, accepting creates nothing either.
        const accept = await page.request.post("/friends/acceptToFriends", { form: { login: user.login } });
        expect(await accept.json()).toMatchObject({ result: "NO_INVITE" });

        await page.goto(`/user/getUsersFriends?id=${user.login}`);
        await expect(page.locator(".list-card")).toHaveCount(0);
        await close();
    });
});

test.describe("search", () => {
    test("finds people and groups by name and opens a profile", async ({ browser }) => {
        const unique = `Zq${Date.now().toString(36).replace(/\d/g, "")}`;
        const target = await newUserSession(browser, { firstName: unique });
        const searcher = await newUserSession(browser);
        const { page } = searcher;

        await page.goto("/user/authorisation");
        await page.locator("#searchInput").fill(unique);
        await page.locator("#searchButton").click();

        const result = page.getByRole("link", { name: `${unique} Example` });
        await expect(result).toBeVisible();
        await result.click();
        await expect(page).toHaveURL(new RegExp(`anotherUserLogin=${target.user.login}`));
        await expect(page.locator("#left h1")).toHaveText(`${unique} Example`);

        await target.close();
        await searcher.close();
    });

    test("the user does not find themselves", async ({ browser }) => {
        const unique = `Zq${Date.now().toString(36).replace(/\d/g, "")}self`;
        const { page, close } = await newUserSession(browser, { firstName: unique });

        await page.goto(`/searchResult/getSearchResult?searchText=${unique}`);

        await expect(page.getByRole("link", { name: `${unique} Example` })).toHaveCount(0);
        await close();
    });
});
