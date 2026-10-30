import { fileURLToPath } from "node:url";
import { test, expect } from "@playwright/test";
import { createGroup, newUserSession, PASSWORD } from "./helpers.js";

// An 8x8 PNG, small enough to keep the tests fast, real enough to be processed.
const PICTURE = {
    name: "picture.png",
    mimeType: "image/png",
    buffer: Buffer.from(
        "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAABLbSncAAAAEUlEQVR4nGM4YWODFTEMLQkAZZlQAVIPr1MAAAAASUVORK5CYII=",
        "base64"),
};
const fixture = name => fileURLToPath(new URL(`../fixtures/${name}`, import.meta.url));
// Every stored avatar is re-encoded under a fresh id: {uuid}.jpg.
const STORED_AVATAR = /^\/images\/[0-9a-f-]{36}\.jpg$/;

/** Colour of the picture at the given URL, near its top and bottom edge, read in the browser. */
async function topAndBottomColours(page, url) {
    return page.evaluate(async src => {
        const image = new Image();
        image.src = src;
        await image.decode();
        const canvas = document.createElement("canvas");
        canvas.width = image.naturalWidth;
        canvas.height = image.naturalHeight;
        const context = canvas.getContext("2d");
        context.drawImage(image, 0, 0);
        const at = y => Array.from(context.getImageData(canvas.width / 2, y, 1, 1).data.slice(0, 3));
        return { top: at(5), bottom: at(canvas.height - 5) };
    }, url);
}

const isRed = ([r, g, b]) => r > 150 && g < 100 && b < 100;
const isBlue = ([r, g, b]) => b > 150 && r < 100;

/** Signs in through the endpoint without following the redirect. */
async function signInStatus(request, login, password) {
    const response = await request.post("/auth/login", { form: { login, password }, maxRedirects: 0 });
    return response.status();
}

test.describe("password", () => {
    test("a changed password replaces the old one", async ({ browser }) => {
        const { page, user, close } = await newUserSession(browser);
        await page.goto("/user/getChangePasswordForm");

        await page.locator("#oldPassword").fill(PASSWORD);
        await page.locator("#password").fill("brand-new-pass");
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page).toHaveURL(/\/user\/authorisation/);

        // Success redirects (302); a failed sign-in shows the form again (200).
        expect(await signInStatus(page.request, user.login, "brand-new-pass")).toBe(302);
        expect(await signInStatus(page.request, user.login, PASSWORD)).toBe(200);
        await close();
    });

    test("a wrong current password changes nothing", async ({ browser }) => {
        const { page, user, close } = await newUserSession(browser);
        await page.goto("/user/getChangePasswordForm");

        await page.locator("#oldPassword").fill("not-my-password");
        await page.locator("#password").fill("brand-new-pass");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errortext")).toHaveText("The old password is incorrect.");
        expect(await signInStatus(page.request, user.login, PASSWORD)).toBe(302);
        await close();
    });

    test("a too short new password is refused", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/getChangePasswordForm");

        await page.locator("#oldPassword").fill(PASSWORD);
        await page.locator("#password").fill("short");
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errortext")).toHaveText("Password must be between 8 and 20 characters long.");
        await close();
    });
});

test.describe("profile picture", () => {
    test("an uploaded avatar is shown on the page and in the header, and can be removed", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/getUserInfo");

        await page.locator("#file").setInputFiles(PICTURE);
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page).toHaveURL(/\/user\/authorisation/);

        const pageAvatar = page.locator("#left img").first();
        await expect(pageAvatar).toHaveAttribute("src", STORED_AVATAR);
        const avatarUrl = await pageAvatar.getAttribute("src");
        // The header uses the small version of the same picture.
        await expect(page.locator("#usericon")).toHaveAttribute("src", `${avatarUrl}?size=thumb`);
        const thumbnail = await page.request.get(`${avatarUrl}?size=thumb`);
        expect(thumbnail.ok()).toBe(true);
        expect(thumbnail.headers()["content-type"]).toBe("image/jpeg");

        await page.goto("/user/getUserInfo");
        await page.locator("#remove-photo").click();
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator("#left img").first()).toHaveAttribute("src", "/pictures/photo.png");
        // The removed file is gone, not just unlinked.
        expect((await page.request.get(avatarUrl)).status()).toBe(404);
        await close();
    });

    test("an iPhone photo (HEIC) becomes an upright avatar", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/getUserInfo");

        // Stored sideways with an orientation mark, like a portrait photo from an iPhone.
        await page.locator("#file").setInputFiles(fixture("rotated-photo.heic"));
        await page.getByRole("button", { name: "Save" }).click();
        await expect(page).toHaveURL(/\/user\/authorisation/);

        const avatar = page.locator("#left img").first();
        await expect(avatar).toHaveAttribute("src", STORED_AVATAR);
        // Turned upright, the red half is on top; unturned or turned the wrong way it would not be.
        const { top, bottom } = await topAndBottomColours(page, await avatar.getAttribute("src"));
        expect(isRed(top), `top ${top}`).toBe(true);
        expect(isBlue(bottom), `bottom ${bottom}`).toBe(true);
        await close();
    });

    test("a damaged HEIC is refused and the current avatar stays", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/getUserInfo");

        await page.locator("#file").setInputFiles(fixture("broken-photo.heic"));
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator(".errortext-top")).toHaveText("The image file is damaged or unreadable.");
        await expect(page.locator("#avatar-preview")).toHaveAttribute("src", "/pictures/photo.png");
        await close();
    });

    test("a group gets its own picture", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await createGroup(page, "Pictured group");
        await page.getByRole("link", { name: "Manage group" }).click();

        await page.locator("#file").setInputFiles(PICTURE);
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator("#left img").first()).toHaveAttribute("src", STORED_AVATAR);
        await close();
    });
});

test.describe("profile details", () => {
    test("a chosen country is shown on the page and found by search", async ({ browser }) => {
        const unique = `Zq${Date.now().toString(36).replace(/\d/g, "")}land`;
        const target = await newUserSession(browser, { firstName: unique });
        const { page } = target;
        await page.goto("/user/getUserInfo");

        // Whichever country comes first in the list; the test does not depend on a particular one.
        const option = page.locator("#country option:not([value=''])").first();
        const country = (await option.textContent()).trim();
        await page.locator("#country").selectOption(await option.getAttribute("value"));
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page.locator("#left")).toContainText(`Country: ${country}`);
        await page.goto("/user/getUserInfo");
        await expect(page.locator("#country option:checked")).toHaveText(country);

        const searcher = await newUserSession(browser);
        await searcher.page.goto(`/searchResult/getSearchResult?searchText=${encodeURIComponent(country)}`);
        await expect(searcher.page.getByRole("link", { name: `${unique} Example` })).toBeVisible();

        await target.close();
        await searcher.close();
    });

    test("a joined group appears among the user's groups", async ({ browser }) => {
        const owner = await newUserSession(browser);
        const groupId = await createGroup(owner.page, "Listed group");
        const visitor = await newUserSession(browser);
        const { page } = visitor;

        await page.goto(`/searchResult/getGroupPage?groupId=${groupId}`);
        await page.getByRole("button", { name: "Join group" }).click();
        await expect(page.getByRole("button", { name: "Leave group" })).toBeVisible();

        await page.goto("/user/authorisation");
        await page.locator("#left").getByRole("link", { name: "Groups" }).click();
        await page.getByRole("link", { name: "Listed group" }).click();
        await expect(page).toHaveURL(new RegExp(`groupId=${groupId}`));

        await owner.close();
        await visitor.close();
    });
});
