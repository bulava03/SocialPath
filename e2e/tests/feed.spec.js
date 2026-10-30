import { test, expect } from "@playwright/test";
import { expectAlert, newUserSession, post, publish } from "./helpers.js";

test.describe("user page feed", () => {
    test("the owner publishes, replies and deletes without reloading the page", async ({ browser }) => {
        const owner = await newUserSession(browser);
        const { page } = owner;
        await page.goto("/user/authorisation");
        const pageUrl = page.url();

        await publish(page, "Hello from the e2e suite");
        await expect(page.getByText("No posts yet.")).toBeHidden();

        const first = post(page, "Hello from the e2e suite");
        await first.locator(":scope > .reply-btn").click();
        const reply = first.locator(":scope > .reply-section");
        await expect(reply).toBeVisible();
        await reply.locator(".reply-field").fill("A reply under the post");
        await reply.getByRole("button", { name: "Publish" }).click();
        await expect(post(page, "A reply under the post")).toBeVisible();

        await post(page, "A reply under the post").locator(":scope > .delete-btn").click();
        await expect(post(page, "A reply under the post")).toHaveCount(0);
        await expect(first).toBeVisible();

        await first.locator(":scope > .delete-btn").click();
        await expect(first).toHaveCount(0);
        await expect(page.getByText("No posts yet.")).toBeVisible();
        // Everything above happened through fetch, on the same page.
        expect(page.url()).toBe(pageUrl);

        await owner.close();
    });

    test("an empty post is not sent", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/authorisation");

        const message = await expectAlert(page, () =>
            page.locator("#new-publication-form").getByRole("button", { name: "Publish" }).click());

        expect(message).toBe("Write something or attach media!");
        await close();
    });

    test("unsupported files are refused as soon as they are picked", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/authorisation");

        const message = await expectAlert(page, () =>
            page.locator("#publication-media").setInputFiles({
                name: "notes.txt",
                mimeType: "text/plain",
                buffer: Buffer.from("not a picture"),
            }));

        expect(message).toBe("Only photos and mp4 or WebM videos are allowed.");
        await expect(page.locator("#publication-media")).toHaveValue("");
        await expect(page.locator("#publication-media-preview > *")).toHaveCount(0);
        await close();
    });

    test("a video cannot be combined with photos", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/authorisation");

        const message = await expectAlert(page, () =>
            page.locator("#publication-media").setInputFiles([
                { name: "clip.mp4", mimeType: "video/mp4", buffer: Buffer.from("video") },
                { name: "photo.png", mimeType: "image/png", buffer: Buffer.from("photo") },
            ]));

        expect(message).toBe("You can attach only a single video without photos.");
        await close();
    });

    test("a photo is uploaded with the post and served back", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/authorisation");

        await page.locator("#publication-text").fill("Post with a photo");
        await page.locator("#publication-media").setInputFiles({
            name: "dot.png",
            mimeType: "image/png",
            // 1x1 PNG
            buffer: Buffer.from(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
                "base64"),
        });
        await expect(page.locator("#publication-media-preview img")).toHaveCount(1);
        await page.locator("#new-publication-form").getByRole("button", { name: "Publish" }).click();

        const image = post(page, "Post with a photo").locator(".commentMedia img");
        await expect(image).toBeVisible();
        const response = await page.request.get(await image.getAttribute("src"));
        expect(response.ok()).toBeTruthy();
        await close();
    });

    test("visitors may comment but delete only their own comments", async ({ browser }) => {
        const owner = await newUserSession(browser, { firstName: "Owner" });
        await owner.page.goto("/user/authorisation");
        await publish(owner.page, "Owner's post");

        const visitor = await newUserSession(browser, { firstName: "Visitor" });
        const { page } = visitor;
        await page.goto(`/user/anotherUserPage?anotherUserLogin=${owner.user.login}`);

        // No publishing form on someone else's page, and no delete on their post.
        await expect(page.locator("#new-publication-form")).toHaveCount(0);
        const ownersPost = post(page, "Owner's post");
        await expect(ownersPost.locator(":scope > .delete-btn")).toHaveCount(0);

        await ownersPost.locator(":scope > .reply-btn").click();
        await ownersPost.locator(":scope > .reply-section .reply-field").fill("Visitor's comment");
        await ownersPost.locator(":scope > .reply-section").getByRole("button", { name: "Publish" }).click();
        const comment = post(page, "Visitor's comment");
        await expect(comment).toBeVisible();
        await expect(comment.locator(":scope > .delete-btn")).toBeVisible();

        // The page owner can remove the visitor's comment from their own page.
        await owner.page.reload();
        await post(owner.page, "Visitor's comment").locator(":scope > .delete-btn").click();
        await expect(post(owner.page, "Visitor's comment")).toHaveCount(0);

        await owner.close();
        await visitor.close();
    });

    test("an expired session sends the user to the login page instead of breaking the feed", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await page.goto("/user/authorisation");

        await page.context().clearCookies();
        await page.locator("#publication-text").fill("Too late");
        await page.locator("#new-publication-form").getByRole("button", { name: "Publish" }).click();

        await expect(page).toHaveURL("/");
        await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
        await close();
    });
});
