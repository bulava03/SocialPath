import { test, expect } from "@playwright/test";
import { newUserSession, profileField, registerUser, signIn } from "./helpers.js";

test.describe("profile editing", () => {
    let login;
    /** The user's own number: a confirmed number belongs to one account. */
    let PHONE;

    test.beforeEach(async ({ page }) => {
        ({ login, phone: PHONE } = await registerUser(page.request));
        await signIn(page, login);
        await page.goto("/user/getUserInfo");
    });

    async function enterDateOfBirth(page, { day, month, year }) {
        await page.locator("#dob-day").fill(day);
        if (month) {
            await page.locator("#dob-month").selectOption(month);
        }
        if (year) {
            await page.locator("#dob-year").selectOption(year);
        }
    }

    test("a valid date of birth is saved and shown on the user page", async ({ page }) => {
        await enterDateOfBirth(page, { day: "5", month: "3", year: "1999" });
        await page.locator("#city").fill("Riverside");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page).toHaveURL(/\/user\/authorisation/);
        await expect(profileField(page, "Date of birth")).toHaveText("05.03.1999");
        await expect(profileField(page, "City")).toHaveText("Riverside");

        // Opening the form again shows the stored date split into its fields.
        await page.goto("/user/getUserInfo");
        await expect(page.locator("#dob-day")).toHaveValue("5");
        await expect(page.locator("#dob-month")).toHaveValue("3");
        await expect(page.locator("#dob-year")).toHaveValue("1999");
    });

    // Every rule for the date is in DateOfBirthResolverTest; here the form shows one and keeps the input.
    test("an impossible date is rejected and the form keeps what was typed", async ({ page }) => {
        await enterDateOfBirth(page, { day: "31", month: "4", year: "2001" });
        await page.locator("#city").fill("Lakeside");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errortext-top")).toHaveText("Invalid date entered");
        await expect(page.locator("#dob-day")).toHaveValue("31");
        await expect(page.locator("#dob-month")).toHaveValue("4");
        await expect(page.locator("#dob-year")).toHaveValue("2001");
        await expect(page.locator("#city")).toHaveValue("Lakeside");
    });

    test("an invalid first name is reported by the server", async ({ page }) => {
        await page.locator("#name").fill("R2-D2");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errortext-top")).toBeVisible();
        await expect(page.locator("#name")).toHaveValue("R2-D2");
    });

    test("the phone is shown only to whom the owner allows, and found by search only when public", async ({ page, browser }) => {
        const friend = await newUserSession(browser);
        const stranger = await newUserSession(browser);
        await friend.page.request.post("/friends/inviteToFriends", { form: { login } });
        await page.request.post("/friends/acceptToFriends", { form: { login: friend.user.login } });
        const ownerPage = `/user/anotherUserPage?anotherUserLogin=${login}`;
        const ownerInSearch = searcher => searcher.locator(`a[href*="anotherUserLogin=${login}"]`);

        // Friends only, the default.
        await page.goto("/user/authorisation");
        await expect(profileField(page, "Phone")).toContainText(`${PHONE.shown} (Friends only)`);
        await friend.page.goto(ownerPage);
        await expect(profileField(friend.page, "Phone")).toContainText(PHONE.shown);
        await stranger.page.goto(ownerPage);
        await expect(profileField(stranger.page, "Phone")).toHaveCount(0);
        await stranger.page.goto(`/searchResult/getSearchResult?searchText=${PHONE.e164}`);
        await expect(ownerInSearch(stranger.page)).toHaveCount(0);

        // Only me: not even friends.
        await page.goto("/user/getUserInfo");
        await page.locator("#phone-visibility").selectOption({ label: "Only me" });
        await page.getByRole("button", { name: "Save" }).click();
        await friend.page.goto(ownerPage);
        await expect(profileField(friend.page, "Phone")).toHaveCount(0);

        // Everyone: strangers see it and can find the owner by it.
        await page.goto("/user/getUserInfo");
        await page.locator("#phone-visibility").selectOption({ label: "Everyone" });
        await page.getByRole("button", { name: "Save" }).click();
        await stranger.page.goto(ownerPage);
        await expect(profileField(stranger.page, "Phone")).toContainText(PHONE.shown);
        await stranger.page.goto(`/searchResult/getSearchResult?searchText=${PHONE.e164}`);
        await expect(ownerInSearch(stranger.page)).toHaveCount(1);

        await friend.close();
        await stranger.close();
    });
});
