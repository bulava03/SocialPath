import { test, expect } from "@playwright/test";
import { newUserSession, PASSWORD, signIn } from "./helpers.js";

/** Signs the same user in from another browser (own cookies): a second device. */
async function anotherDevice(browser, login) {
    const context = await browser.newContext();
    const page = await context.newPage();
    await signIn(page, login);
    return { page, close: () => context.close() };
}

/** Whether a page is still signed in: pages behind the login send others to the start page. */
async function expectSignedOut(page) {
    await page.goto("/user/authorisation");
    await expect(page).toHaveURL("/");
}

const sessions = page => page.locator(".session");

/** Fills in and sends the password form. */
async function changePassword(page, oldPassword, password, confirmPassword) {
    await page.goto("/security/password");
    await page.locator("#oldPassword").fill(oldPassword);
    await page.locator("#password").fill(password);
    await page.locator("#confirmPassword").fill(confirmPassword);
    await page.getByRole("button", { name: "Save" }).click();
}

/** Signs in through the endpoint without following the redirect: 302 on success, 200 with the form on failure. */
async function signInStatus(request, login, password) {
    const response = await request.post("/auth/login", { form: { login, password }, maxRedirects: 0 });
    return response.status();
}

test.describe("security page", () => {
    test("the user menu opens the password tab, the other tab lists this device", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/authorisation");

        await page.getByRole("button", { name: "User menu" }).click();
        await page.getByRole("link", { name: "Security" }).click();
        await expect(page).toHaveURL("/security/password");
        await expect(page.getByRole("heading", { name: "Change password" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Active sessions" })).toHaveCount(0);

        const tabs = page.getByRole("navigation", { name: "Security" });
        await expect(tabs.getByRole("link", { name: "Password" })).toHaveAttribute("aria-current", "page");
        await tabs.getByRole("link", { name: /Sessions/ }).click();
        await expect(page).toHaveURL("/security/sessions");
        await expect(page.getByRole("heading", { name: "Active sessions" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Change password" })).toHaveCount(0);
        await expect(tabs.getByRole("link", { name: /Sessions/ })).toHaveAttribute("aria-current", "page");
        await expect(sessions(page)).toHaveCount(1);
        const current = sessions(page).first();
        await expect(current).toContainText("This device");
        await expect(current).toContainText("Active now");
        await expect(current).toContainText("Chrome");
        // The tests reach the app from the same machine or the Docker network.
        await expect(current).toContainText("Local network");
        // The current device signs out through the menu, not from this list.
        await expect(current.getByRole("button", { name: "End" })).toHaveCount(0);
        await expect(page.getByRole("button", { name: "End all other sessions" })).toHaveCount(0);
        await close();
    });

    test("other devices are signed out one by one or all at once", async ({ browser }) => {
        const first = await newUserSession(browser);
        const second = await anotherDevice(browser, first.user.login);
        const third = await anotherDevice(browser, first.user.login);
        await first.page.goto("/security/sessions");
        await expect(sessions(first.page)).toHaveCount(3);

        await sessions(first.page).filter({ hasNotText: "This device" }).first()
            .getByRole("button", { name: "End" }).click();
        await expect(sessions(first.page)).toHaveCount(2);

        await first.page.getByRole("button", { name: "End all other sessions" }).click();
        await expect(sessions(first.page)).toHaveCount(1);
        await expect(sessions(first.page).first()).toContainText("This device");
        await expectSignedOut(second.page);
        await expectSignedOut(third.page);
        // The device that ended them stays signed in.
        await first.page.goto("/user/authorisation");
        await expect(first.page).toHaveURL(/\/user\/authorisation/);
        await first.close();
        await second.close();
        await third.close();
    });

    test("a new password replaces the old one and signs every other device out", async ({ browser }) => {
        const first = await newUserSession(browser);
        const second = await anotherDevice(browser, first.user.login);

        await changePassword(first.page, PASSWORD, "brand-new-pass", "brand-new-pass");

        await expect(first.page).toHaveURL(/\/security\/password/);
        await expect(first.page.getByText("The password has been changed. Other sessions were ended.")).toBeVisible();
        // The tab switcher counts the sessions left: only this one.
        await expect(first.page.locator(".security-tab-count")).toHaveText("1");
        await expectSignedOut(second.page);
        expect(await signInStatus(first.page.request, first.user.login, "brand-new-pass")).toBe(302);
        expect(await signInStatus(first.page.request, first.user.login, PASSWORD)).toBe(200);
        await first.close();
        await second.close();
    });

    // Every reason to refuse is checked in SecurityControllerTest; here only that the page shows it.
    test("a refused password change shows why and keeps the old password", async ({ browser }) => {
        const { page, user, close } = await newUserSession(browser);

        await changePassword(page, PASSWORD, "brand-new-pass", "brand-new-pas");

        await expect(page.locator(".errortext")).toHaveText("The new passwords do not match.");
        expect(await signInStatus(page.request, user.login, PASSWORD)).toBe(302);
        await close();
    });

    test("after logging out a copy of the old token no longer works", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/authorisation");
        const [token] = (await page.context().cookies()).filter(cookie => cookie.name === "token");

        await page.getByRole("button", { name: "User menu" }).click();
        await page.getByRole("button", { name: "Log out" }).click();
        await expect(page).toHaveURL("/");

        // Someone who copied the cookie before the logout tries it in their own browser.
        const context = await browser.newContext();
        await context.addCookies([token]);
        const thief = await context.newPage();
        await expectSignedOut(thief);
        await context.close();
        await close();
    });
});
