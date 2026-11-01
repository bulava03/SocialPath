import { defineConfig, devices } from "@playwright/test";

// The tests drive a running instance of the app: locally the one started
// with `docker compose up`, in CI the same compose stack. Every test creates
// its own users, so the tests are independent and may run in parallel.
export default defineConfig({
    testDir: "./tests",
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
