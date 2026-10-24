import { test, expect } from "@playwright/test";
import { createGroup, newUserSession } from "./helpers.js";

/** A person's card on the members or admins page. */
const card = (page, name) => page.locator(".table-wrapper").filter({ hasText: name });

async function join(session, groupId) {
    const response = await session.page.request.post("/groupMembership/joinGroup", {
        form: { groupId },
    });
    expect(response.ok()).toBeTruthy();
}

test.describe("group roles", () => {
    test("the owner promotes a member to admin and demotes them again", async ({ browser }) => {
        const owner = await newUserSession(browser, { firstName: "Olha" });
        const groupId = await createGroup(owner.page, "Roles club");
        const member = await newUserSession(browser, { firstName: "Marko" });
        await join(member, groupId);

        const { page } = owner;
        await page.goto(`/group/getGroupsMembers?id=${groupId}`);
        await card(page, "Marko Example").getByRole("button", { name: "Make admin" }).click();

        await expect(card(page, "Marko Example").getByRole("button", { name: "Remove from admins" })).toBeVisible();
        await page.goto(`/group/getGroupsAdmins?id=${groupId}`);
        await expect(card(page, "Marko Example")).toBeVisible();

        await card(page, "Marko Example").getByRole("button", { name: "Remove from admins" }).click();
        await expect(card(page, "Marko Example")).toHaveCount(0);
        await expect(card(page, "Olha Example")).toBeVisible();

        await owner.close();
        await member.close();
    });

    test("a removed member loses the group, an admin removes plain members only", async ({ browser }) => {
        const owner = await newUserSession(browser, { firstName: "Olha" });
        const groupId = await createGroup(owner.page, "Strict club");
        const admin = await newUserSession(browser, { firstName: "Adam" });
        const member = await newUserSession(browser, { firstName: "Marko" });
        await join(admin, groupId);
        await join(member, groupId);
        await owner.page.goto(`/group/getGroupsMembers?id=${groupId}`);
        await card(owner.page, "Adam Example").getByRole("button", { name: "Make admin" }).click();

        const { page } = admin;
        await page.goto(`/group/getGroupsMembers?id=${groupId}`);
        await expect(card(page, "Olha Example").getByRole("button", { name: "Remove from group" })).toHaveCount(0);
        await card(page, "Marko Example").getByRole("button", { name: "Remove from group" }).click();
        await expect(card(page, "Marko Example")).toHaveCount(0);

        // The removed member is back to a visitor: can join, cannot post.
        await member.page.goto(`/searchResult/getGroupPage?groupId=${groupId}`);
        await expect(member.page.locator("#joinButton")).toBeVisible();
        await expect(member.page.locator("#new-publication-form")).toHaveCount(0);

        await owner.close();
        await admin.close();
        await member.close();
    });

    test("people joining at the same moment are all kept", async ({ browser }) => {
        const owner = await newUserSession(browser);
        const groupId = await createGroup(owner.page, "Busy club");
        const joiners = await Promise.all(
            ["Ira", "Ivan", "Ilya", "Inna", "Ihor"].map(firstName => newUserSession(browser, { firstName })));

        await Promise.all(joiners.map(session => join(session, groupId)));
        // Joining twice is harmless.
        await join(joiners[0], groupId);

        await owner.page.goto(`/group/getGroupsMembers?id=${groupId}`);
        await expect(owner.page.locator(".table-wrapper")).toHaveCount(6);

        await owner.close();
        await Promise.all(joiners.map(session => session.close()));
    });
});
