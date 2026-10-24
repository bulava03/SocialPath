// Fills a fresh SocialPath instance with a small demo world and saves the
// README screenshots to docs/screenshots. Pictures (avatars, photos) are
// drawn on a canvas here, so the script needs nothing but the running app.
//
// Run it against an empty database, e.g. a separate compose project:
//   docker compose -p socialpath-demo up --build -d
//   cd e2e && npm run screenshots
//   docker compose -p socialpath-demo down -v
import { chromium, request as playwrightRequest } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const BASE_URL = process.env.BASE_URL ?? "http://localhost:8080";
const OUT_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../docs/screenshots");
const PASSWORD = "password123";
const VIEWPORT = { width: 1280, height: 900 };

// Deliberately generic people with no country or city: the screenshots are
// meant for anyone reading the README.
const PEOPLE = {
    alex: { first: "Alex", last: "Morgan", education: "Computer Science, BSc",
        workplace: "Frontend developer", colors: ["#f6a04d", "#d9661c"],
        birth: { day: "14", month: "5", year: "1998" } },
    sam: { first: "Sam", last: "Taylor", education: "Software Engineering, MSc",
        workplace: "Backend engineer", colors: ["#4f8fe0", "#2c5aa0"],
        birth: { day: "2", month: "11", year: "1995" } },
    jordan: { first: "Jordan", last: "Lee", education: "Fine Arts, BA",
        workplace: "Photographer", colors: ["#53b98a", "#2e7d5b"],
        birth: { day: "23", month: "8", year: "1997" } },
    riley: { first: "Riley", last: "Carter", education: "Information Systems, BSc",
        workplace: "QA engineer", colors: ["#a170d8", "#6b3fa8"],
        birth: { day: "9", month: "1", year: "2000" } },
};

// ---------- pictures ----------

/** Draws on a canvas in the browser and returns the PNG bytes. */
async function paint(art, width, height, scene, options = {}) {
    const dataUrl = await art.evaluate(({ width, height, scene, options }) => {
        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        const g = canvas.getContext("2d");
        const linear = (stops, x1 = 0, y1 = 0, x2 = 0, y2 = height) => {
            const gradient = g.createLinearGradient(x1, y1, x2, y2);
            stops.forEach((color, i) => gradient.addColorStop(i / (stops.length - 1), color));
            return gradient;
        };
        const ridge = (baseY, amplitude, color, seed) => {
            g.fillStyle = color;
            g.beginPath();
            g.moveTo(0, height);
            for (let x = 0; x <= width; x += width / 12) {
                const y = baseY - amplitude * Math.abs(Math.sin(x / width * 5 + seed)) - amplitude / 3 * Math.sin(x / 37 + seed);
                g.lineTo(x, y);
            }
            g.lineTo(width, height);
            g.fill();
        };

        if (scene === "avatar") {
            g.fillStyle = linear(options.colors, 0, 0, width, height);
            g.fillRect(0, 0, width, height);
            g.fillStyle = "rgba(255,255,255,0.95)";
            g.font = `600 ${width * 0.38}px Segoe UI, Arial, sans-serif`;
            g.textAlign = "center";
            g.textBaseline = "middle";
            g.fillText(options.initials, width / 2, height / 2 + width * 0.02);
        } else if (scene === "mountains") {
            g.fillStyle = linear(["#ffcf8a", "#f59d6b", "#8d6aa8"]);
            g.fillRect(0, 0, width, height);
            g.fillStyle = "#fff2c4";
            g.beginPath();
            g.arc(width * 0.7, height * 0.42, height * 0.09, 0, Math.PI * 2);
            g.fill();
            ridge(height * 0.62, height * 0.22, "#7a5b8f", 1);
            ridge(height * 0.75, height * 0.18, "#4f3f6e", 3);
            ridge(height * 0.9, height * 0.12, "#2c2747", 5);
        } else if (scene === "sea") {
            g.fillStyle = linear(["#7fc8f8", "#c9ecff"], 0, 0, 0, height * 0.55);
            g.fillRect(0, 0, width, height * 0.55);
            g.fillStyle = linear(["#1f7fb8", "#0d4a74"], 0, height * 0.55, 0, height);
            g.fillRect(0, height * 0.55, width, height * 0.45);
            g.fillStyle = "#fff7d6";
            g.beginPath();
            g.arc(width * 0.25, height * 0.3, height * 0.08, 0, Math.PI * 2);
            g.fill();
            g.strokeStyle = "rgba(255,255,255,0.45)";
            g.lineWidth = 3;
            for (let row = 0; row < 7; row++) {
                const y = height * 0.6 + row * height * 0.055;
                g.beginPath();
                for (let x = 0; x <= width; x += 20) {
                    g.lineTo(x, y + Math.sin(x / 40 + row) * 4);
                }
                g.stroke();
            }
        } else if (scene === "city") {
            g.fillStyle = linear(["#141b3a", "#3b2f63", "#7d4c7a"]);
            g.fillRect(0, 0, width, height);
            let x = 0;
            let i = 0;
            while (x < width) {
                const w = 60 + (i * 37) % 70;
                const h = height * (0.3 + ((i * 53) % 40) / 100);
                g.fillStyle = i % 2 ? "#1d1a33" : "#25213f";
                g.fillRect(x, height - h, w - 6, h);
                g.fillStyle = "#ffd77a";
                for (let wy = height - h + 14; wy < height - 10; wy += 22) {
                    for (let wx = x + 8; wx < x + w - 16; wx += 16) {
                        if ((wx * 7 + wy * 3 + i) % 5 < 2) {
                            g.fillRect(wx, wy, 7, 10);
                        }
                    }
                }
                x += w;
                i++;
            }
        }
        return canvas.toDataURL("image/png");
    }, { width, height, scene, options });
    return Buffer.from(dataUrl.split(",")[1], "base64");
}

const png = (name, buffer) => new Blob([buffer], { type: "image/png" });

// ---------- seeding through the app's own endpoints ----------

async function signedInAs(login) {
    const person = PEOPLE[login];
    const api = await playwrightRequest.newContext({ baseURL: BASE_URL });
    const registered = await api.post("/registration/registration", {
        form: {
            login, password: PASSWORD, firstName: person.first, lastName: person.last,
            email: `${login}@example.com`, phoneNumber: "555-0100",
        },
        maxRedirects: 0,
    });
    check(registered.status() === 302, `register ${login}`);
    const loggedIn = await api.post("/auth/login", { form: { login, password: PASSWORD }, maxRedirects: 0 });
    check(loggedIn.status() === 302, `log in ${login}`);
    return api;
}

function check(condition, what) {
    if (!condition) {
        throw new Error(`Seeding failed: ${what}`);
    }
}

async function updateProfile(api, login, avatar) {
    const person = PEOPLE[login];
    const form = new FormData();
    const fields = {
        firstName: person.first, lastName: person.last, email: `${login}@example.com`,
        phoneNumber: "555-0100", ...person.birth, countryCode: "",
        education: person.education, workplace: person.workplace, removeImage: "false",
    };
    Object.entries(fields).forEach(([name, value]) => form.append(name, value));
    form.append("file", png("avatar.png", avatar), "avatar.png");
    const response = await api.post("/user/changeUserInfo", { multipart: form, maxRedirects: 0 });
    check(response.status() === 302, `profile of ${login}`);
}

async function publish(api, text, { groupId, photos = [] } = {}) {
    const form = new FormData();
    form.append("text", text);
    if (groupId) {
        form.append("groupId", groupId);
    }
    photos.forEach((photo, i) => form.append("media", png(`photo${i}.png`, photo), `photo${i}.png`));
    const url = groupId ? "/comments/addGroupPublication" : "/comments/addUserPublication";
    const response = await api.post(url, { multipart: form });
    check(response.ok(), `publish "${text}"`);
    return (await response.json()).id;
}

async function comment(api, parentId, text, { groupId } = {}) {
    const form = new FormData();
    form.append("idPublication", String(parentId));
    form.append("text", text);
    if (groupId) {
        form.append("groupId", groupId);
    }
    const response = await api.post(groupId ? "/comments/addCommentGroup" : "/comments/addCommentUser", { multipart: form });
    check(response.ok(), `comment "${text}"`);
    return findCommentId(api, parentId, text);
}

/** Comment endpoints answer "OK" only; the id is read back from the rendered thread. */
async function findCommentId(api, postId, text) {
    const html = await (await api.get(`/comments/view/thread?id=${postId}`)).text();
    const decode = body => body.replace(/&#39;/g, "'").replace(/&quot;/g, "\"").replace(/&amp;/g, "&");
    const ids = [...html.matchAll(/class="comment" id="(\d+)"[\s\S]*?class="commentText">([^<]*)</g)];
    const match = ids.find(([, , body]) => decode(body) === text);
    check(match, `find comment "${text}"`);
    return Number(match[1]);
}

async function seed(art) {
    const api = {};
    for (const login of Object.keys(PEOPLE)) {
        api[login] = await signedInAs(login);
        const { first, last, colors } = PEOPLE[login];
        await updateProfile(api[login], login, await paint(art, 256, 256, "avatar", { colors, initials: first[0] + last[0] }));
    }

    // Friends: Alex and Sam are friends, Jordan has asked Alex.
    await api.sam.post("/friends/inviteToFriends", { form: { login: "alex" } });
    await api.alex.post("/friends/acceptToFriends", { form: { login: "sam" } });
    await api.jordan.post("/friends/inviteToFriends", { form: { login: "alex" } });

    // Alex's page.
    await publish(api.alex, "Finally moved into the new flat. Boxes everywhere, but the view is worth it.");
    await publish(api.alex, "Sunset from the rooftops tonight.", { photos: [await paint(art, 1200, 700, "city")] });
    const trip = await publish(api.alex, "Back from three days in the mountains. Sunrise from the summit was unreal!",
        { photos: [await paint(art, 1200, 700, "mountains")] });
    const samComment = await comment(api.sam, trip, "Wow, the colours! Which trail did you take?");
    await comment(api.alex, samComment, "The ridge trail, about four hours up. Highly recommend going in September.");
    await comment(api.riley, trip, "Adding this to my list for next summer.");

    // Sam's and Jordan's pages.
    await publish(api.sam, "Shipped the new search today: one query instead of nine. Coffee well earned.");
    const nightCity = await publish(api.jordan, "The city at blue hour. Working on a new series about streets at night.",
        { photos: [await paint(art, 1200, 700, "city")] });
    await comment(api.riley, nightCity, "Can't wait to see the whole series!");

    // A group.
    const created = await api.jordan.post("/group/createGroup", { form: { Name: "Seaside Photographers" }, maxRedirects: 0 });
    const groupId = new URL(created.headers()["location"], BASE_URL).searchParams.get("groupId");
    check(groupId, "create group");
    const groupForm = new FormData();
    groupForm.append("groupId", groupId);
    groupForm.append("Name", "Seaside Photographers");
    groupForm.append("removeImage", "false");
    groupForm.append("file", png("group.png", await paint(art, 256, 256, "avatar",
        { colors: ["#2aa6c9", "#14627f"], initials: "SP" })), "group.png");
    check((await api.jordan.post("/group/updateGroup", { multipart: groupForm, maxRedirects: 0 })).status() === 302,
        "group avatar");
    for (const login of ["alex", "sam", "riley"]) {
        await api[login].post("/groupMembership/joinGroup", { form: { groupId } });
    }
    await publish(api.sam, "Weekly theme: long exposure. Share your best shot below!", { groupId });
    const sea = await publish(api.jordan, "Morning at the beach, 6 a.m., not a soul around.",
        { groupId, photos: [await paint(art, 1200, 700, "sea")] });
    const alexOnSea = await comment(api.alex, sea, "This light is gorgeous. What lens?", { groupId });
    await comment(api.jordan, alexOnSea, "35mm, f/8, tripod and a lot of patience.", { groupId });

    await Promise.all(Object.values(api).filter(Boolean).map(context => context.dispose()));
    return { groupId };
}

// ---------- screenshots ----------

async function shoot(browser, login, url, file, { scrollTo } = {}) {
    const context = await browser.newContext({ baseURL: BASE_URL, viewport: VIEWPORT });
    const page = await context.newPage();
    const response = await page.request.post("/auth/login", { form: { login, password: PASSWORD }, maxRedirects: 0 });
    check(response.status() === 302, `screenshot login ${login}`);
    await page.goto(url);
    await page.waitForLoadState("networkidle");
    if (scrollTo) {
        await page.locator(scrollTo).first().evaluate(element => window.scrollTo(0, element.getBoundingClientRect().top + window.scrollY - 20));
    }
    await page.screenshot({ path: path.join(OUT_DIR, file) });
    await context.close();
    console.log(`saved docs/screenshots/${file}`);
}

const browser = await chromium.launch();
try {
    const art = await browser.newPage();
    const { groupId } = await seed(art);
    await mkdir(OUT_DIR, { recursive: true });

    await shoot(browser, "alex", "/user/authorisation", "user-page.png");
    await shoot(browser, "alex", "/user/authorisation", "feed.png", { scrollTo: "#publications" });
    await shoot(browser, "alex", `/searchResult/getGroupPage?groupId=${groupId}`, "group.png");
    await shoot(browser, "alex", "/user/anotherUserPage?anotherUserLogin=jordan", "friend-request.png");
} finally {
    await browser.close();
}
