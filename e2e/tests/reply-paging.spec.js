import { test, expect } from "@playwright/test";
import { newUserSession, post, publishMany } from "./helpers.js";

// Must match app.feed.replies-page-size in application.properties.
const REPLIES_PAGE_SIZE = 10;

/** Comments through the API and returns the new comment's id. */
async function reply(request, parentId, text) {
    const response = await request.post("/comments/addCommentUser", {
        multipart: { idPublication: String(parentId), text },
    });
    expect(response.ok()).toBeTruthy();
    return (await response.json()).id;
}

/** The texts of a post's or comment's direct replies, top to bottom. */
const replyTexts = comment => comment.locator(":scope > .replies > .comment > .commentText").allTextContents();
const moreButton = comment => comment.locator(":scope > .replies > .replies-more");

async function writeReply(page, parentText, text) {
    const parent = post(page, parentText);
    await parent.locator(":scope > .reply-btn").click();
    await parent.locator(":scope > .reply-section .reply-field").fill(text);
    await parent.locator(":scope > .reply-section").getByRole("button", { name: "Publish" }).click();
}

test.describe("replies", () => {
    test("come in portions, and deeper levels open one at a time", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        const [postId] = await publishMany(page.request, 1, { prefix: "thread" });
        const ids = [];
        for (let i = 1; i <= REPLIES_PAGE_SIZE + 2; i++) {
            ids.push(await reply(page.request, postId, `reply ${i}`));
        }
        const nested = await reply(page.request, ids.at(-1), "nested");
        await reply(page.request, nested, "deep");

        await page.goto("/user/authorisation");
        const thread = post(page, "thread 1");

        // The newest portion, newest first; the two oldest wait behind a button.
        await expect(post(page, "reply 12")).toBeVisible();
        expect(await replyTexts(thread)).toEqual(
            Array.from({ length: REPLIES_PAGE_SIZE }, (_, i) => `reply ${12 - i}`));
        await expect(moreButton(thread)).toHaveText("Show the last 2 replies");
        await moreButton(thread).click();
        await expect(post(page, "reply 1")).toBeVisible();
        expect((await replyTexts(thread)).slice(-2)).toEqual(["reply 2", "reply 1"]);
        await expect(moreButton(thread)).toHaveCount(0);

        // Replies to replies: one level per click.
        await expect(post(page, "nested")).toHaveCount(0);
        await moreButton(post(page, "reply 12")).click();
        await expect(post(page, "nested")).toBeVisible();
        await expect(post(page, "deep")).toHaveCount(0);
        await expect(moreButton(post(page, "nested"))).toHaveText("Show 1 reply");
        await moreButton(post(page, "nested")).click();
        await expect(post(page, "deep")).toBeVisible();
        await close();
    });

    test("the button tells how many the click brings out of how many are left", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        const [postId] = await publishMany(page.request, 1, { prefix: "thread" });
        const comment = await reply(page.request, postId, "busy comment");
        for (let i = 1; i <= 2 * REPLIES_PAGE_SIZE + 3; i++) {
            await reply(page.request, comment, `answer ${i}`);
        }
        await page.goto("/user/authorisation");
        const busy = post(page, "busy comment");

        await expect(moreButton(busy)).toHaveText("Show 10 of 23 replies");
        await moreButton(busy).click();
        await expect(moreButton(busy)).toHaveText("Show next 10 of 13 remaining replies");
        await moreButton(busy).click();
        await expect(moreButton(busy)).toHaveText("Show the last 3 replies");
        await moreButton(busy).click();
        await expect(post(page, "answer 1")).toBeVisible();
        await expect(moreButton(busy)).toHaveCount(0);
        expect(await replyTexts(busy)).toHaveLength(23);
        await close();
    });

    test("a new reply opens its branch or goes on top of it, and only it is added", async ({ browser }) => {
        const { page, close } = await newUserSession(browser);
        const [postId] = await publishMany(page.request, 1, { prefix: "thread" });
        const first = await reply(page.request, postId, "first comment");
        await reply(page.request, postId, "second comment");
        await reply(page.request, first, "older answer");

        await page.goto("/user/authorisation");
        await expect(moreButton(post(page, "first comment"))).toHaveText("Show 1 reply");

        // A branch with nothing to show yet simply gets the reply.
        await writeReply(page, "second comment", "answer to the second");
        await expect(post(page, "answer to the second")).toBeVisible();

        // A collapsed branch opens, the new reply on top of the older one.
        await post(page, "thread 1").locator(":scope > .reply-btn").click();
        await post(page, "thread 1").locator(":scope > .reply-section .reply-field").fill("a draft in the post");
        await writeReply(page, "first comment", "new answer");
        await expect(post(page, "new answer")).toBeVisible();
        expect(await replyTexts(post(page, "first comment"))).toEqual(["new answer", "older answer"]);
        await expect(moreButton(post(page, "first comment"))).toHaveCount(0);
        // The rest of the page stayed as it was.
        await expect(post(page, "answer to the second")).toBeVisible();
        await expect(post(page, "thread 1").locator(":scope > .reply-section .reply-field"))
            .toHaveValue("a draft in the post");

        // An open branch gets the reply on top.
        await writeReply(page, "first comment", "newest answer");
        await expect(post(page, "newest answer")).toBeVisible();
        expect(await replyTexts(post(page, "first comment")))
            .toEqual(["newest answer", "new answer", "older answer"]);

        // Deleting takes out only that reply.
        await post(page, "new answer").locator(":scope > .delete-btn").click();
        await expect(post(page, "new answer")).toHaveCount(0);
        expect(await replyTexts(post(page, "first comment"))).toEqual(["newest answer", "older answer"]);
        await close();
    });
});
