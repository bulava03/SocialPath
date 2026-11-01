import { test, expect } from "@playwright/test";
import { emailLink, newUserSession, PASSWORD, profileField, registerUser, signIn } from "./helpers.js";

/** Signs in with an email address typed in the login field. */
async function signInByEmail(page, address) {
    await page.goto("/");
    await page.locator("#login").fill(address);
    await page.locator("#password").fill(PASSWORD);
    await page.getByRole("button", { name: "Sign in" }).click();
}

test.describe("email address", () => {
    test("a new account gets a link; until it is opened the address does not sign in", async ({ page }) => {
        const { login } = await registerUser(page.request);
        const address = `${login}@example.com`;

        await signInByEmail(page, address);
        await expect(page.locator("#errortext")).toHaveText("Incorrect login or password");

        // The own page says so and leads to the Security page.
        await signIn(page, login);
        await page.goto("/user/authorisation");
        await expect(profileField(page, "Email")).toContainText("(Only me until confirmed)");
        await profileField(page, "Email").getByRole("link", { name: "Confirm it on the Security page" }).click();
        await expect(page).toHaveURL("/security/email");
        await expect(page.locator(".phone-status")).toContainText("Not confirmed");

        // The link works in any browser, signed in or not.
        await page.context().clearCookies();
        await page.goto(await emailLink(page.request, address));
        await expect(page.locator("#email-result")).toHaveText("Your email is confirmed. You can sign in with it now.");

        await signInByEmail(page, address.toUpperCase());
        await expect(page).toHaveURL(/\/user\/authorisation/);
        await expect(profileField(page, "Email")).toContainText("(Friends only)");
        await expect(page.locator(".contact-unconfirmed")).toHaveCount(0);
    });

    test("a used or made-up link is refused", async ({ page }) => {
        const { login } = await registerUser(page.request);
        const link = await emailLink(page.request, `${login}@example.com`);

        await page.goto(link);
        await expect(page.locator("#email-result")).toContainText("confirmed");
        await page.goto(link);
        await expect(page.locator("#email-result")).toContainText("This link does not work any more");
        await page.goto("/email/confirm?token=made-up");
        await expect(page.locator("#email-result")).toContainText("This link does not work any more");
    });

    test("who sees the address can be chosen once it is confirmed", async ({ page, browser }) => {
        const { login } = await registerUser(page.request);
        await signIn(page, login);
        const stranger = await newUserSession(browser);
        const ownerPage = `/user/anotherUserPage?anotherUserLogin=${login}`;

        await page.goto("/user/getUserInfo");
        await expect(page.locator("#email-visibility")).toBeDisabled();
        await expect(page.locator("#email-visibility-hint")).toContainText("Only you see it until it is confirmed.");
        // Unconfirmed, the address is the owner's only.
        await stranger.page.goto(ownerPage);
        await expect(profileField(stranger.page, "Email")).toHaveCount(0);

        await page.goto(await emailLink(page.request, `${login}@example.com`));
        await page.goto("/user/getUserInfo");
        await expect(page.locator("#email-visibility")).toBeEnabled();
        await expect(page.locator("#email-visibility-hint")).toHaveText("Only your friends see it.");
        await page.locator("#email-visibility").selectOption({ label: "Everyone" });
        // The note under the list follows the choice before saving.
        await expect(page.locator("#email-visibility-hint"))
            .toHaveText("Anyone who opens your page sees it, and can find you by it in search.");
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page).toHaveURL(/\/user\/authorisation/);
        await expect(profileField(page, "Email")).toContainText("(Everyone)");

        await stranger.page.goto(ownerPage);
        await expect(profileField(stranger.page, "Email")).toHaveText(`${login}@example.com`);
        await stranger.close();
    });

    test("a new address in the profile has to be confirmed again; the old one stops signing in", async ({ page }) => {
        const { login } = await registerUser(page.request);
        const oldAddress = `${login}@example.com`;
        await page.goto(await emailLink(page.request, oldAddress));
        await signIn(page, login);

        const newAddress = `${login}.new@example.com`;
        await page.goto("/user/getUserInfo");
        await page.locator("#email").fill(newAddress);
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page).toHaveURL("/security/email?sent");
        await expect(page.locator(".form-notice")).toContainText("A confirmation link is on its way.");
        await expect(page.locator(".phone-status")).toContainText(newAddress);
        await expect(page.locator(".phone-status")).toContainText("Not confirmed");

        // Asking again at once is held back.
        await page.getByRole("button", { name: "Send confirmation link" }).click();
        await expect(page).toHaveURL("/security/email?wait");
        await expect(page.locator(".form-notice")).toContainText("You can ask for a new link in");

        await page.context().clearCookies();
        await signInByEmail(page, oldAddress);
        await expect(page.locator("#errortext")).toHaveText("Incorrect login or password");
        await page.goto(await emailLink(page.request, newAddress));
        await signInByEmail(page, newAddress);
        await expect(page).toHaveURL(/\/user\/authorisation/);
    });
});
