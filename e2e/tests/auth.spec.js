import { test, expect } from "@playwright/test";
import { PASSWORD, registerUser, signIn, uniqueLogin } from "./helpers.js";

test.describe("registration and sign-in", () => {
    test("a new user signs up through the form and lands on their page after signing in", async ({ page }) => {
        const login = uniqueLogin("reg");

        await page.goto("/registration");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill(PASSWORD);
        await page.locator("#name").fill("Olena");
        await page.locator("#surname").fill("Kovalenko");
        await page.locator("#phone").fill("+380 50 123 4567");
        await page.locator("#email").fill(`${login}@example.com`);
        await page.getByRole("button", { name: "Sign up" }).click();

        await expect(page).toHaveURL("/");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill(PASSWORD);
        await page.getByRole("button", { name: "Sign in" }).click();

        await expect(page).toHaveURL(/\/user\/authorisation/);
        await expect(page.locator("#left h1")).toHaveText("Olena Kovalenko");
    });

    test("an invalid form comes back with the error and the entered values", async ({ page }) => {
        const login = uniqueLogin("bad");

        await page.goto("/registration");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill("short");
        await page.locator("#name").fill("Olena");
        await page.locator("#phone").fill("+380 50 123 4567");
        await page.locator("#email").fill(`${login}@example.com`);
        await page.getByRole("button", { name: "Sign up" }).click();

        await expect(page.locator(".errortext").filter({ hasText: /\S/ })).toContainText("8");
        await expect(page.locator("#login")).toHaveValue(login);
        await expect(page.locator("#name")).toHaveValue("Olena");
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

    test("a wrong password shows an error and keeps the typed login", async ({ page }) => {
        const { login } = await registerUser(page.request);

        await page.goto("/");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill("wrong-password");
        await page.getByRole("button", { name: "Sign in" }).click();

        await expect(page.locator("#errortext")).toHaveText("Incorrect login or password");
        await expect(page.locator("#login")).toHaveValue(login);
        await expect(page.locator("#password")).toHaveValue("");

        // A second, correct attempt from the re-rendered form works.
        await page.locator("#password").fill(PASSWORD);
        await page.getByRole("button", { name: "Sign in" }).click();
        await expect(page).toHaveURL(/\/user\/authorisation/);
    });

    test("pressing Enter in the password field signs in", async ({ page }) => {
        const { login } = await registerUser(page.request);

        await page.goto("/");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill(PASSWORD);
        await page.locator("#password").press("Enter");

        await expect(page).toHaveURL(/\/user\/authorisation/);
    });

    test("logging out ends the session", async ({ page }) => {
        const { login } = await registerUser(page.request);
        await page.goto("/");
        await page.locator("#login").fill(login);
        await page.locator("#password").fill(PASSWORD);
        await page.getByRole("button", { name: "Sign in" }).click();
        await expect(page).toHaveURL(/\/user\/authorisation/);

        await page.getByRole("button", { name: "User menu" }).click();
        await page.getByRole("button", { name: "Log out" }).click();
        await expect(page).toHaveURL("/");

        await page.goto("/user/authorisation");
        await expect(page).toHaveURL("/");
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
