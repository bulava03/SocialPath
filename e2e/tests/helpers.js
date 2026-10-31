import { expect } from "@playwright/test";

export const PASSWORD = "password123";

/** A login nobody has used yet (the app allows up to 20 characters). */
export function uniqueLogin(prefix = "u") {
    const random = Math.random().toString(36).slice(2, 8);
    return `${prefix}${Date.now().toString(36)}${random}`.slice(0, 20);
}

/**
 * Registers a user through the registration endpoint. The UI form itself is
 * covered by auth.spec.js; elsewhere this is only setup.
 * @returns {Promise<{login: string, firstName: string}>}
 */
export async function registerUser(request, { prefix, firstName = "Tester" } = {}) {
    const login = uniqueLogin(prefix);
    const response = await request.post("/registration/registration", {
        form: {
            login,
            password: PASSWORD,
            firstName,
            lastName: "Example",
            email: `${login}@example.com`,
            phoneNumber: "555-0100",
        },
        maxRedirects: 0,
    });
    // Success redirects to the login page; a validation error re-renders the form.
    expect(response.status(), "registration should redirect").toBe(302);
    return { login, firstName };
}

/**
 * Signs in through the API. The auth cookie lands in the page's browser
 * context, so the page is logged in from the next navigation on.
 */
export async function signIn(page, login) {
    const response = await page.request.post("/auth/login", {
        form: { login, password: PASSWORD },
        maxRedirects: 0,
    });
    // A failed login re-renders the form with 200; only success redirects.
    expect(response.status(), "login should succeed").toBe(302);
    expect(response.headers()["location"]).toContain("/user/authorisation");
}

/** Opens a separate browser session (own cookies) signed in as a new user. */
export async function newUserSession(browser, options = {}) {
    const context = await browser.newContext();
    const page = await context.newPage();
    const user = await registerUser(page.request, options);
    await signIn(page, user.login);
    return { page, user, close: () => context.close() };
}

/** Creates a group through the UI and returns its id. */
export async function createGroup(page, name) {
    await page.goto("/group/getGroupCreationForm");
    await page.locator("#name").fill(name);
    await page.getByRole("button", { name: "Create" }).click();
    await expect(page).toHaveURL(/getGroupPage\?groupId=\d+/);
    return new URL(page.url()).searchParams.get("groupId");
}

/** Publishes a top-level post in the feed of the currently open page. */
export async function publish(page, text) {
    await page.locator("#publication-text").fill(text);
    await page.locator("#new-publication-form").getByRole("button", { name: "Publish" }).click();
    await expect(post(page, text)).toBeVisible();
}

/** A post or comment in the feed, found by its exact text ("post 2" is not "post 20"). */
export function post(page, text) {
    return page.locator(".comment").filter({
        has: page.locator(":scope > .commentText").getByText(text, { exact: true }),
    });
}

/**
 * Runs an action that is expected to raise an alert() and returns the
 * alert's text.
 */
export async function expectAlert(page, action) {
    // alert() blocks the page, so the click that raises it only completes once
    // the dialog is closed: the handler has to be in place before the action.
    let message = null;
    page.once("dialog", dialog => {
        message = dialog.message();
        return dialog.accept();
    });
    await action();
    await expect.poll(() => message, { message: "an alert should be shown" }).not.toBeNull();
    return message;
}

/** The top-level posts of the feed. */
export const posts = page => page.locator("#publications > .comment");

/** A value from the profile card on a user page, by its label. */
export const profileField = (page, label) => page.locator("#left .profile-info > div")
    .filter({ has: page.locator("dt", { hasText: label }) })
    .locator("dd");

/** Publishes "post 1" ... "post N" and returns their ids in that order. */
export async function publishMany(request, count, { groupId, prefix = "post" } = {}) {
    const ids = [];
    for (let i = 1; i <= count; i++) {
        const url = groupId ? "/comments/addGroupPublication" : "/comments/addUserPublication";
        const text = `${prefix} ${i}`;
        const multipart = groupId ? { groupId, text } : { text };
        const response = await request.post(url, { multipart });
        expect(response.ok()).toBeTruthy();
        ids.push((await response.json()).id);
    }
    return ids;
}

export async function scrollToEnd(page) {
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
}

/** Scrolls down until the feed holds the expected number of posts. */
export async function scrollUntil(page, count) {
    await expect.poll(async () => {
        await scrollToEnd(page);
        return posts(page).count();
    }, { timeout: 15_000 }).toBe(count);
}
