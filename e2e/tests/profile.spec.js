import { test, expect } from "@playwright/test";
import { registerUser, signIn } from "./helpers.js";

test.describe("profile editing", () => {
    test.beforeEach(async ({ page }) => {
        const { login } = await registerUser(page.request);
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
        await page.locator("#city").fill("Lviv");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page).toHaveURL(/\/user\/authorisation/);
        await expect(page.locator("#left")).toContainText("Date of birth: 05.03.1999");
        await expect(page.locator("#left")).toContainText("City: Lviv");

        // Opening the form again shows the stored date split into its fields.
        await page.goto("/user/getUserInfo");
        await expect(page.locator("#dob-day")).toHaveValue("5");
        await expect(page.locator("#dob-month")).toHaveValue("3");
        await expect(page.locator("#dob-year")).toHaveValue("1999");
    });

    test("an impossible date is rejected and the form keeps what was typed", async ({ page }) => {
        await enterDateOfBirth(page, { day: "31", month: "4", year: "2001" });
        await page.locator("#city").fill("Odesa");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errortext-top")).toHaveText("Invalid date entered");
        await expect(page.locator("#dob-day")).toHaveValue("31");
        await expect(page.locator("#dob-month")).toHaveValue("4");
        await expect(page.locator("#dob-year")).toHaveValue("2001");
        await expect(page.locator("#city")).toHaveValue("Odesa");
    });

    test("a partly filled date asks for all three fields", async ({ page }) => {
        await enterDateOfBirth(page, { day: "12" });
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errortext-top"))
            .toHaveText("Enter the full date of birth or leave all three fields empty.");
        await expect(page.locator("#dob-day")).toHaveValue("12");
    });

    test("an invalid first name is reported by the server", async ({ page }) => {
        await page.locator("#name").fill("R2-D2");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errortext-top")).toBeVisible();
        await expect(page.locator("#name")).toHaveValue("R2-D2");
    });
});
