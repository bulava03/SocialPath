import { test, expect } from "@playwright/test";
import { createGroup, newUserSession, post, posts, publishMany, scrollToEnd, scrollUntil } from "./helpers.js";

// The open feed asks the server for new activity every 30 seconds. The page
// clock is installed before the page opens, so tests move time forward
// instead of waiting.
const CHECK_INTERVAL = 31_000;
const banner = page => page.locator(".feed-news");

async function join(session, groupId) {
    const response = await session.page.request.post("/groupMembership/joinGroup", { form: { groupId } });
    expect(response.ok()).toBeTruthy();
}

/** An owner's group with a second member and the viewer, who is a member too. */
async function groupWithMembers(browser) {
    const owner = await newUserSession(browser);
    const groupId = await createGroup(owner.page, "Busy feed");
    const other = await newUserSession(browser);
    const viewer = await newUserSession(browser);
    await join(other, groupId);
    await join(viewer, groupId);
    const closeAll = async () => {
        await owner.close();
        await other.close();
        await viewer.close();
    };
    return { owner, other, viewer, groupId, closeAll };
}

async function openFeed(page, groupId) {
    await page.clock.install();
    await page.goto(`/searchResult/getGroupPage?groupId=${groupId}`);
}

test.describe("new activity in an open feed", () => {
    test("at the top of the feed a new post by someone else simply appears", async ({ browser }) => {
        const { other, viewer, groupId, closeAll } = await groupWithMembers(browser);
        const { page } = viewer;
        await openFeed(page, groupId);

        await publishMany(other.page.request, 1, { groupId, prefix: "fresh news" });
        await page.clock.runFor(CHECK_INTERVAL);

        await expect(post(page, "fresh news 1")).toBeVisible();
        await expect(banner(page)).toHaveCount(0);
        await closeAll();
    });

    test("further down the feed a banner offers the update instead of moving the content", async ({ browser }) => {
        const { owner, other, viewer, groupId, closeAll } = await groupWithMembers(browser);
        await publishMany(owner.page.request, 25, { groupId });
        const { page } = viewer;
        await openFeed(page, groupId);
        await scrollUntil(page, 25);

        await publishMany(other.page.request, 1, { groupId, prefix: "breaking" });
        await page.clock.runFor(CHECK_INTERVAL);

        await expect(banner(page)).toBeVisible();
        await expect(posts(page)).toHaveCount(25);
        await expect(post(page, "breaking 1")).toHaveCount(0);

        await banner(page).getByRole("button").click();
        await expect(post(page, "breaking 1")).toBeVisible();
        await expect(posts(page)).toHaveCount(20);
        await expect(banner(page)).toHaveCount(0);
        await closeAll();
    });

    test("a new comment under a post that is not even loaded counts as activity", async ({ browser }) => {
        const { owner, other, viewer, groupId, closeAll } = await groupWithMembers(browser);
        const ids = await publishMany(owner.page.request, 25, { groupId });
        const { page } = viewer;
        await openFeed(page, groupId);
        await scrollToEnd(page);
        await expect(posts(page)).not.toHaveCount(0);

        // "post 1" is the oldest one, far below the first portion.
        const response = await other.page.request.post("/comments/addCommentGroup", {
            multipart: { groupId, idPublication: String(ids[0]), text: "late comment" },
        });
        expect(response.ok()).toBeTruthy();
        await page.clock.runFor(CHECK_INTERVAL);

        await expect(banner(page)).toBeVisible();
        await closeAll();
    });

    test("the viewer's own posts are not news", async ({ browser }) => {
        const { viewer, groupId, closeAll } = await groupWithMembers(browser);
        const { page } = viewer;
        await openFeed(page, groupId);
        let feedReloads = 0;
        page.on("request", request => {
            const url = request.url();
            if (url.includes("/comments/view/loadCommentsGroup") && !url.includes("before=")) {
                feedReloads++;
            }
        });

        await page.locator("#publication-text").fill("my own post");
        await page.locator("#new-publication-form").getByRole("button", { name: "Publish" }).click();
        await expect(post(page, "my own post")).toBeVisible();
        await page.clock.runFor(CHECK_INTERVAL);
        await page.clock.runFor(CHECK_INTERVAL);

        await expect(banner(page)).toHaveCount(0);
        expect(feedReloads).toBe(0);
        await closeAll();
    });

    test("an unsent draft at the top is never wiped by an update", async ({ browser }) => {
        const { other, viewer, groupId, closeAll } = await groupWithMembers(browser);
        const { page } = viewer;
        await openFeed(page, groupId);
        await page.locator("#publication-text").fill("half-written thought");

        await publishMany(other.page.request, 1, { groupId, prefix: "meanwhile" });
        await page.clock.runFor(CHECK_INTERVAL);

        await expect(banner(page)).toBeVisible();
        await expect(page.locator("#publication-text")).toHaveValue("half-written thought");
        await expect(post(page, "meanwhile 1")).toHaveCount(0);
        await closeAll();
    });
});
