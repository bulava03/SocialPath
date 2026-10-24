import { test, expect } from "@playwright/test";
import { createGroup, newUserSession, post, posts, publishMany, scrollToEnd, scrollUntil } from "./helpers.js";

const PAGE_SIZE = 20;
const postTexts = page => page.locator("#publications > .comment > .commentText").allTextContents();

const expectedTexts = count => Array.from({ length: count }, (_, i) => `post ${count - i}`);

test.describe("feed paging", () => {
    test("the feed loads in portions while scrolling and stops at the end", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await publishMany(page.request, 45);

        // The page itself carries only the first portion.
        const html = await (await page.request.get("/user/authorisation")).text();
        expect(html.match(/class="comment" id=/g)).toHaveLength(PAGE_SIZE);

        const feedRequests = [];
        page.on("request", request => {
            if (request.url().includes("/comments/view/loadCommentsUser")) {
                feedRequests.push(request.url());
            }
        });
        await page.goto("/user/authorisation");
        await scrollUntil(page, 45);

        // Newest first, every post exactly once.
        expect(await postTexts(page)).toEqual(expectedTexts(45));
        await expect(page.locator("#publications > .feed-more")).toHaveCount(0);

        // At the end the observer is gone: more scrolling asks for nothing.
        const requestsAtEnd = feedRequests.length;
        for (let i = 0; i < 3; i++) {
            await page.evaluate(() => window.scrollTo(0, 0));
            await scrollToEnd(page);
        }
        await page.waitForTimeout(500);
        expect(feedRequests).toHaveLength(requestsAtEnd);
        expect(requestsAtEnd).toBe(2);

        await close();
    });

    test("an action updates only its post and keeps the loaded portions and drafts", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await publishMany(page.request, 25);
        await page.goto("/user/authorisation");
        await scrollUntil(page, 25);

        // A draft reply in the newest post.
        const newest = post(page, "post 25");
        await newest.locator(":scope > .reply-btn").click();
        await newest.locator(":scope > .reply-section .reply-field").fill("unsent draft");

        // A comment on a post from the second portion.
        const older = post(page, "post 2");
        await older.locator(":scope > .reply-btn").click();
        await older.locator(":scope > .reply-section .reply-field").fill("a comment far below");
        await older.locator(":scope > .reply-section").getByRole("button", { name: "Publish" }).click();

        await expect(post(page, "a comment far below")).toBeVisible();
        await expect(posts(page)).toHaveCount(25);
        await expect(newest.locator(":scope > .reply-section .reply-field")).toHaveValue("unsent draft");

        // A new post goes on top without resetting the feed.
        await page.locator("#publication-text").fill("post 26");
        await page.locator("#new-publication-form").getByRole("button", { name: "Publish" }).click();
        await expect(posts(page)).toHaveCount(26);
        expect(await postTexts(page)).toEqual(expectedTexts(26));
        await expect(newest.locator(":scope > .reply-section .reply-field")).toHaveValue("unsent draft");

        // Deleting a post removes just that post.
        await post(page, "post 13").locator(":scope > .delete-btn").click();
        await expect(posts(page)).toHaveCount(25);
        await expect(post(page, "post 13")).toHaveCount(0);

        await close();
    });

    test("a group feed is paged the same way", async ({ browser }) => {
        const owner = await newUserSession(browser);
        const groupId = await createGroup(owner.page, "Paged club");
        await publishMany(owner.page.request, 25, { groupId });

        const visitor = await newUserSession(browser);
        const { page } = visitor;
        await page.goto(`/searchResult/getGroupPage?groupId=${groupId}`);
        await scrollUntil(page, 25);

        expect(await postTexts(page)).toEqual(expectedTexts(25));
        await expect(page.locator("#publications > .feed-more")).toHaveCount(0);

        await owner.close();
        await visitor.close();
    });

    test("a short feed has no marker and asks for nothing more", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        await publishMany(page.request, 3);
        let feedRequests = 0;
        page.on("request", request => {
            if (request.url().includes("/comments/view/loadCommentsUser")) {
                feedRequests++;
            }
        });

        await page.goto("/user/authorisation");
        await scrollToEnd(page);
        await page.waitForTimeout(500);

        await expect(posts(page)).toHaveCount(3);
        await expect(page.locator("#publications > .feed-more")).toHaveCount(0);
        expect(feedRequests).toBe(0);
        await close();
    });
});
