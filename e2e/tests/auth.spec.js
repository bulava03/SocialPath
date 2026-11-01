import { test, expect } from "@playwright/test";
import { PASSWORD, profileField, registerUser, signIn, smsCode, uniqueLogin, uniquePhone } from "./helpers.js";

/** Fills the registration form with valid values. */
async function fillRegistration(page, login, phone) {
    await page.goto("/registration");
    await page.locator("#login").fill(login);
    await page.locator("#password").fill(PASSWORD);
    await page.locator("#name").fill("Alex");
    await page.locator("#surname").fill("Morgan");
    await page.locator("#phone-region").selectOption(phone.region);
    await page.locator("#phone").fill(phone.typed);
    await page.locator("#email").fill(`${login}@example.com`);
}

/** Signs in with the phone number, in its international form, typed in the login field. */
async function signInByPhone(page, phone, password = PASSWORD) {
    await page.goto("/");
    await page.locator("#login").fill(phone.shown);
    await page.locator("#password").fill(password);
    await page.getByRole("button", { name: "Sign in" }).click();
}

test.describe("registration and sign-in", () => {
    test("a new user signs up, confirms the phone with the SMS code and lands on their page", async ({ page }) => {
        const login = uniqueLogin("reg");
        const phone = uniquePhone();

        await fillRegistration(page, login, phone);
        await page.getByRole("button", { name: "Sign up" }).click();

        await expect(page).toHaveURL(/\/registration\/confirm\?id=/);
        await expect(page.locator(".phone-sent-to")).toHaveText(phone.shown);
        // The demo phone shows the message the pretend operator "delivered".
        const [demoPhone] = await Promise.all([
            page.context().waitForEvent("page"),
            page.getByRole("link", { name: "Open the demo phone" }).click(),
        ]);
        await expect(demoPhone.locator(".sms-text").first()).toContainText("SocialPath code:");
        await demoPhone.close();

        await page.locator("#code").fill(await smsCode(page.request, phone.e164));
        await page.getByRole("button", { name: "Confirm" }).click();

        await expect(page).toHaveURL(/\/user\/authorisation/);
        await expect(page.locator("#left h1")).toHaveText("Alex Morgan");
        // Stored in the international form and shown grouped, as a link to call.
        await expect(profileField(page, "Phone").getByRole("link")).toHaveText(phone.shown);
        await expect(profileField(page, "Phone").getByRole("link")).toHaveAttribute("href", `tel:${phone.e164}`);
    });

    test("a wrong code is refused with the attempts left; starting over frees the login", async ({ page }) => {
        const login = uniqueLogin("code");
        const phone = uniquePhone();
        await fillRegistration(page, login, phone);
        await page.getByRole("button", { name: "Sign up" }).click();

        const right = await smsCode(page.request, phone.e164);
        await page.locator("#code").fill(right === "000000" ? "111111" : "000000");
        await page.getByRole("button", { name: "Confirm" }).click();
        await expect(page.locator("#errortext")).toHaveText("Wrong code. 4 attempts left.");

        // Until confirmed the account cannot sign in: the password leads back to the code page.
        await page.goto("/");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill(PASSWORD);
        await page.getByRole("button", { name: "Sign in" }).click();
        await expect(page).toHaveURL(/\/registration\/confirm\?id=/);

        await page.getByRole("button", { name: "Wrong number? Start over" }).click();
        await expect(page).toHaveURL("/registration");
        await fillRegistration(page, login, uniquePhone());
        await page.getByRole("button", { name: "Sign up" }).click();
        await expect(page).toHaveURL(/\/registration\/confirm\?id=/);
    });

    test("a number another account has confirmed cannot be used to sign up", async ({ page }) => {
        const { phone } = await registerUser(page.request);

        await fillRegistration(page, uniqueLogin("dup"), phone);
        await page.getByRole("button", { name: "Sign up" }).click();

        await expect(page.locator(".errortext-top")).toHaveText("This phone number is already linked to another account.");
    });

    test("a confirmed phone number in the login field signs in with the password", async ({ page }) => {
        const { phone } = await registerUser(page.request, { firstName: "Phoneuser" });

        await signInByPhone(page, phone, "wrong-password");
        await expect(page.locator("#errortext")).toHaveText("Incorrect login or password");
        await expect(page.locator("#login")).toHaveValue(phone.shown);

        await signInByPhone(page, phone);
        await expect(page).toHaveURL(/\/user\/authorisation/);
        await expect(page.locator("#left h1")).toHaveText("Phoneuser Example");
    });

    test("a changed number signs in only after it is confirmed on the Security page", async ({ page }) => {
        const { login } = await registerUser(page.request);
        await signIn(page, login);
        const newPhone = uniquePhone();
        await page.goto("/user/getUserInfo");
        await page.locator("#phone-region").selectOption(newPhone.region);
        await page.locator("#phone").fill(newPhone.typed);
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page).toHaveURL(/\/user\/authorisation/);

        await page.goto("/security/phone");
        await expect(page.locator(".phone-status")).toContainText(newPhone.shown);
        await expect(page.locator(".phone-status")).toContainText("Not confirmed");
        await page.getByRole("button", { name: "Send code" }).click();
        await expect(page.locator(".form-notice")).toHaveText("A code is on its way.");
        await page.locator("#code").fill(await smsCode(page.request, newPhone.e164));
        await page.getByRole("button", { name: "Confirm" }).click();
        await expect(page.locator(".form-notice")).toHaveText("The number is confirmed. You can sign in with it now.");
        await expect(page.locator(".phone-status")).toContainText("Confirmed");

        await page.context().clearCookies();
        await signInByPhone(page, newPhone);
        await expect(page).toHaveURL(/\/user\/authorisation/);
    });

    test("an invalid form comes back with the error and the entered values", async ({ page }) => {
        const login = uniqueLogin("bad");

        await page.goto("/registration");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill("short");
        await page.locator("#name").fill("Alex");
        const phone = uniquePhone();
        await page.locator("#phone-region").selectOption(phone.region);
        await page.locator("#phone").fill(phone.typed);
        await page.locator("#email").fill(`${login}@example.com`);
        await page.getByRole("button", { name: "Sign up" }).click();

        await expect(page.locator(".errortext").filter({ hasText: /\S/ })).toContainText("8");
        await expect(page.locator("#phone-region")).toHaveValue(phone.region);
        await expect(page.locator("#phone")).toHaveValue(phone.typed);
        await expect(page.locator("#login")).toHaveValue(login);
        await expect(page.locator("#name")).toHaveValue("Alex");
        // The password is never echoed back.
        await expect(page.locator("#password")).toHaveValue("");
    });

    test("a taken login is rejected", async ({ page }) => {
        const { login } = await registerUser(page.request);

        await page.goto("/registration");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill(PASSWORD);
        await page.locator("#name").fill("Copycat");
        await page.locator("#phone").fill("123456");
        await page.locator("#email").fill("copycat@example.com");
        await page.getByRole("button", { name: "Sign up" }).click();

        await expect(page.getByText("This login is already taken.")).toBeVisible();
    });

    test("a wrong password shows an error, keeps the login, and Enter signs in on the next try", async ({ page }) => {
        const { login } = await registerUser(page.request);

        await page.goto("/");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill("wrong-password");
        await page.getByRole("button", { name: "Sign in" }).click();

        await expect(page.locator("#errortext")).toHaveText("Incorrect login or password");
        await expect(page.locator("#login")).toHaveValue(login);
        await expect(page.locator("#password")).toHaveValue("");

        // A second, correct attempt from the re-rendered form works, sent with Enter.
        await page.locator("#password").fill(PASSWORD);
        await page.locator("#password").press("Enter");
        await expect(page).toHaveURL(/\/user\/authorisation/);
    });

    test("the user menu opens from the keyboard", async ({ page }) => {
        const { login } = await registerUser(page.request);
        await signIn(page, login);
        await page.goto("/user/authorisation");

        const menuButton = page.getByRole("button", { name: "User menu" });
        // The focus outline is drawn around the button: it needs a real box.
        await expect(menuButton).toBeVisible();
        await menuButton.focus();
        await page.keyboard.press("Enter");
        await expect(page.getByRole("link", { name: "Personal information" })).toBeVisible();

        await page.keyboard.press("Escape");
        await expect(page.getByRole("link", { name: "Personal information" })).toBeHidden();
    });

    test("pages behind the login redirect anonymous visitors to the login page", async ({ page }) => {
        await page.goto("/user/getUserInfo");

        await expect(page).toHaveURL("/");
        await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
    });
});
