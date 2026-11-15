// Renders the database diagram in the README, docs/db-schema.html, to
// docs/db-schema.png at twice the size, for sharp text. Run from e2e/:
//   npm run db-schema
import { chromium } from "@playwright/test";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const DOCS = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../docs");

const browser = await chromium.launch();
try {
    const page = await browser.newPage({ deviceScaleFactor: 2, viewport: { width: 1400, height: 1000 } });
    await page.goto(pathToFileURL(path.join(DOCS, "db-schema.html")).href);
    // The page lays out the tables first, then draws the relations between them.
    await page.waitForFunction(() => window.done === 1);
    await page.locator("#c").screenshot({ path: path.join(DOCS, "db-schema.png") });
    console.log("saved docs/db-schema.png");
} finally {
    await browser.close();
}
