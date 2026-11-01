import { defineConfig, devices } from "@playwright/test";
import path from "node:path";

// The tests drive a running instance of the app: locally the one started
// with `docker compose up`, in CI the same compose stack. Every test creates
// its own users, so the tests are independent and may run in parallel.
export default defineConfig({
    testDir: "./tests",
    // Screenshots, traces and the 106 MB files of the upload tests.
    // PLAYWRIGHT_OUTPUT_DIR moves them off the project's disk (e.g. out of a
    // synced folder) into a folder several projects can share: each gets a
    // subfolder of its own, as Playwright empties its output folder on start.
    outputDir: process.env.PLAYWRIGHT_OUTPUT_DIR
        ? path.join(process.env.PLAYWRIGHT_OUTPUT_DIR, "socialpath")
        : "test-results",
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
    use: {
        // The tests' own stack (npm run stack:up), never the instance on 8080.
        baseURL: process.env.BASE_URL ?? "http://localhost:8081",
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
    },
    projects: [
        { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    ],
});
