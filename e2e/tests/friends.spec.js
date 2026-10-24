import { test, expect } from "@playwright/test";
import { newUserSession } from "./helpers.js";

const userPage = login => `/user/anotherUserPage?anotherUserLogin=${login}`;

test.describe("friends", () => {
    test("a friend request is sent, accepted and the friendship removed", async ({ browser }) => {
        const anna = await newUserSession(browser, { firstName: "Anna" });
        const boris = await newUserSession(browser, { firstName: "Boris" });

        // Boris asks; the button turns into "cancel" without a reload.
        await boris.page.goto(userPage(anna.user.login));
        await boris.page.locator("#submitInvite").click();
        await expect(boris.page.locator("#submitRemoveInvite")).toHaveText("Cancel friend request");

        // Anna sees the request in her list and on Boris's page.
        await anna.page.goto(`/user/getInvitationsToFriends?id=${anna.user.login}`);
        await expect(anna.page.getByText("Boris Example")).toBeVisible();
        await anna.page.goto(userPage(boris.user.login));
        await expect(anna.page.locator("#submitReject")).toBeVisible();
        await anna.page.locator("#submitAccept").click();
        await expect(anna.page.locator("#submitRemove")).toHaveText("Remove from friends");
        await expect(anna.page.locator("#submitReject")).toHaveCount(0);

        // Both see each other as friends.
        await boris.page.goto(`/user/getUsersFriends?id=${boris.user.login}`);
        await expect(boris.page.getByText("Anna Example")).toBeVisible();
        await boris.page.goto(userPage(anna.user.login));
        await expect(boris.page.locator("#submitRemove")).toBeVisible();

        await boris.page.locator("#submitRemove").click();
        await expect(boris.page.locator("#submitInvite")).toHaveText("Add friend");
        await anna.page.goto(`/user/getUsersFriends?id=${anna.user.login}`);
        await expect(anna.page.getByText("Boris Example")).toHaveCount(0);

        await anna.close();
        await boris.close();
    });

    test("a declined request disappears for both sides", async ({ browser }) => {
        const anna = await newUserSession(browser, { firstName: "Anna" });
        const boris = await newUserSession(browser, { firstName: "Boris" });

        await boris.page.goto(userPage(anna.user.login));
        await boris.page.locator("#submitInvite").click();
        await expect(boris.page.locator("#submitRemoveInvite")).toBeVisible();

        await anna.page.goto(userPage(boris.user.login));
        await anna.page.locator("#submitReject").click();
        await expect(anna.page.locator("#submitInvite")).toHaveText("Add friend");
        await expect(anna.page.locator("#submitAccept")).toHaveCount(0);

        await boris.page.reload();
        await expect(boris.page.locator("#submitInvite")).toHaveText("Add friend");

        await anna.close();
        await boris.close();
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
