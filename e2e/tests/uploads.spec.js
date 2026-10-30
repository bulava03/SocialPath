import { writeFileSync } from "node:fs";
import { test, expect } from "@playwright/test";
import { createGroup, newUserSession, post, publish } from "./helpers.js";

// Must match UploadLimitExceptionHandler.MESSAGE.
const TOO_LARGE = "The file is too large. The limit is 100 MB per file.";
// Just over spring.servlet.multipart.max-request-size (105 MB).
const OVERSIZED = Buffer.alloc(106 * 1024 * 1024);

/** Playwright only takes files over 50 MB from disk, so write one per test. */
function oversizedFile(testInfo) {
    const path = testInfo.outputPath("huge.jpg");
    writeFileSync(path, OVERSIZED);
    return path;
}

test.describe("upload limits", () => {
    // The oversized uploads really send 106 MB to the app.
    test.slow();

    test("the post and reply forms tell the upload limits", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/authorisation");

        await expect(page.locator("#new-publication .media-limits"))
            .toHaveText("Up to 5 photos (10 MB each) or one mp4 video (up to 100 MB).");

        await publish(page, "A post to reply to");
        const first = post(page, "A post to reply to");
        await first.locator(":scope > .reply-btn").click();
        await expect(first.locator(":scope > .reply-section .media-limits")).toBeVisible();
        await close();
    });

    test("an avatar over the limit is reported on the profile form", async ({ browser }, testInfo) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/getUserInfo");

        await page.locator("#file").setInputFiles(oversizedFile(testInfo));
        await page.getByRole("button", { name: "Save" }).click();

        await expect(page).toHaveURL(/\/user\/getUserInfo/);
        await expect(page.locator(".errortext-top")).toHaveText(TOO_LARGE);
        // Nothing was stored: the default picture is still shown.
        await expect(page.locator("#avatar-preview")).toHaveAttribute("src", "/pictures/photo.png");
        await close();
    });

    test("a group avatar over the limit is reported on the group form", async ({ browser }, testInfo) => {
        const { page, close } = await newUserSession(browser);
        const groupId = await createGroup(page, "Big pictures");
        await page.getByRole("link", { name: "Manage group" }).click();

        await page.locator("#file").setInputFiles(oversizedFile(testInfo));
        await page.getByRole("button", { name: "Save" }).click();

        // Back on the same group's form, not on a generic error page.
        await expect(page).toHaveURL(new RegExp(`getGroupUpdatingForm\\?id=${groupId}`));
        await expect(page.locator(".errorText-top")).toHaveText(TOO_LARGE);
        await close();
    });

    test("a page disguised as a photo is refused by the server", async ({ browser }) => {
        // The feed's script checks files too, but a forged request skips it.
        const { page, close } = await newUserSession(browser);

        const response = await page.request.post("/comments/addUserPublication", {
            multipart: {
                text: "Look at this",
                media: { name: "photo.jpg", mimeType: "image/jpeg", buffer: Buffer.from("<script>alert(1)</script>") },
            },
        });

        expect(response.status()).toBe(400);
        expect(await response.json()).toEqual({ error: "Only photos and mp4 videos are allowed." });
        await page.goto("/user/authorisation");
        await expect(page.getByText("No posts yet.")).toBeVisible();
        await close();
    });

    test("an oversized post upload gets a JSON error the feed can show", async ({ browser }) => {
        // The feed form refuses such a file before sending it, so go to the endpoint directly.
        const { page, close } = await newUserSession(browser);

        const response = await page.request.post("/comments/addUserPublication", {
            multipart: {
                text: "A very large video",
                media: { name: "huge.mp4", mimeType: "video/mp4", buffer: OVERSIZED },
            },
        });

        expect(response.status()).toBe(413);
        expect(await response.json()).toEqual({ error: TOO_LARGE });
        await close();
    });
});
