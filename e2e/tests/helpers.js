import { expect } from "@playwright/test";

export const PASSWORD = "password123";

/**
 * A number nobody has used yet, as typed, as shown and as stored. A confirmed
 * number belongs to one account, so every user needs their own: the 555
 * exchange of a fictional area code line, the last four digits and the
 * exchange made random (8 million numbers, all valid for libphonenumber).
 */
export function uniquePhone() {
    const exchange = String(200 + Math.floor(Math.random() * 800));
    const line = String(Math.floor(Math.random() * 10_000)).padStart(4, "0");
    return {
        region: "US",
        typed: `201 ${exchange} ${line}`,
        shown: `+1 201-${exchange}-${line}`,
        e164: `+1201${exchange}${line}`,
    };
}

/**
 * The code last sent to a number, read from the demo phone (/dev/sms): the
 * app has no real SMS operator.
 */
export async function smsCode(request, e164) {
    let code = null;
    await expect.poll(async () => {
        const messages = await (await request.get("/dev/sms/messages", { params: { phone: e164 } })).json();
        code = messages[0]?.text.match(/\b\d{6}\b/)?.[0] ?? null;
        return code;
    }, { message: `an SMS code for ${e164}` }).not.toBeNull();
    return code;
}

/** A login nobody has used yet (the app allows up to 20 characters). */
export function uniqueLogin(prefix = "u") {
    const random = Math.random().toString(36).slice(2, 8);
    return `${prefix}${Date.now().toString(36)}${random}`.slice(0, 20);
}

/**
 * A first name nobody has used yet. Names allow letters only, so the time and
 * a random part are written in base 26 with the letters a-z as digits.
 */
export function uniqueName(prefix = "Zq") {
    const letters = n => {
        let s = "";
        do {
            s = String.fromCharCode(97 + n % 26) + s;
            n = Math.floor(n / 26);
        } while (n > 0);
        return s;
    };
    return `${prefix}${letters(Date.now())}${letters(Math.floor(Math.random() * 26 ** 4))}`;
}

/**
 * Registers a user through the registration endpoints and confirms the phone
 * with the code from the demo phone. The UI forms themselves are covered by
 * auth.spec.js; elsewhere this is only setup. Confirming signs the user in,
 * so the helper signs out again: callers sign in when they need to.
 * @returns {Promise<{login: string, firstName: string, phone: ReturnType<typeof uniquePhone>}>}
 */
export async function registerUser(request, { prefix, firstName = "Tester" } = {}) {
    const login = uniqueLogin(prefix);
    const phone = uniquePhone();
    const response = await request.post("/registration/registration", {
        form: {
            login,
            password: PASSWORD,
            firstName,
            lastName: "Example",
            email: `${login}@example.com`,
            phoneRegion: phone.region,
            phoneNumber: phone.typed,
        },
        maxRedirects: 0,
    });
    // Success redirects to the code page; a validation error re-renders the form.
    expect(response.status(), "registration should redirect").toBe(302);
    const confirmPage = new URL(response.headers()["location"], "http://app");
    expect(confirmPage.pathname).toBe("/registration/confirm");

    const confirmed = await request.post("/registration/confirm", {
        form: { id: confirmPage.searchParams.get("id"), code: await smsCode(request, phone.e164) },
        maxRedirects: 0,
    });
    expect(confirmed.status(), "the code should be accepted").toBe(302);
    expect(confirmed.headers()["location"]).toContain("/user/authorisation");
    await request.post("/auth/logout", { maxRedirects: 0 });
    return { login, firstName, phone };
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
