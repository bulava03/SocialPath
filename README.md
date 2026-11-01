# SocialPath

A social network web application built with Spring Boot and MySQL. Users
create profiles, connect as friends, join groups, publish posts with photos
and video, and discuss them in threaded comments.

| User page with a comment thread | Group feed |
|---|---|
| ![Feed with nested comments](docs/screenshots/feed.png) | ![Group page](docs/screenshots/group.png) |
| **Own page** | **Incoming friend request** |
| ![User page](docs/screenshots/user-page.png) | ![Another user's page](docs/screenshots/friend-request.png) |

## Features

- **Accounts** — registration with server-side validation, profile editing
  (contacts, birth date, country, city, education, workplace).
- **Phone numbers** — chosen country code plus the number, checked with
  Google's libphonenumber, stored in E.164 and shown grouped as a `tel:`
  link. The owner decides who sees the number and the email address
  (everyone, friends, only them), with a note on the page and in the form
  saying who does now; a contact is found by search only when it is
  confirmed and public.
- **Phone confirmation and phone sign-in** — a registration is finished by
  entering the 6-digit code sent to the number (10 minutes, 5 attempts, a
  new code after 60 s), and a number is never stored unconfirmed: a new one
  from the profile replaces the old only once its code is entered. The
  number then signs in instead of the login: it goes in the same field, in
  the international format (+ and the country code), with the same
  password. A number can be confirmed by one account only. There is no real
  SMS operator: messages go to the log and to a demo phone page, `/dev/sms`,
  on when `SMS_DEMO_INBOX=true` (the default in `docker-compose.yml`, off
  otherwise). Unconfirmed registrations are removed after a day.
- **Email confirmation and email sign-in** — a new account, or a new
  address in the profile, gets a letter with a link that confirms the
  address (24 hours, one link at a time). Until then the address does not
  sign in, only its owner sees it, and the owner's page says so with a way
  to the Security page; once confirmed it signs in like the login, case
  aside, and the owner chooses who sees it. An address can be confirmed by
  one account only. Letters go over SMTP: to a local
  [Mailpit](https://mailpit.axllent.org/) test inbox by default
  (http://localhost:8025 with `docker compose`), or to a real mailbox such
  as Gmail with an app password when the `MAIL_*` variables are set (see
  `application.properties`).
- **Security page** — password change, confirming the phone number and
  the email address, and
  the list of active sessions with browser, system, IP and approximate
  location; any other device can be signed out, or all of them at once.
  Sessions last 7 days without use and are extended while used.
- **Avatars** — any common format including HEIC and AVIF (decoded with a
  bundled ffmpeg); every upload is center-cropped to a square and stored as
  a 1024 px image plus a 256 px thumbnail for lists, comments and the header.
- **Friends** — requests, accepting/declining, friend lists, removal;
  friendship is always mutual.
- **Groups** — anyone can create a group and becomes its owner; owners
  appoint admins from the members, admins moderate posts and membership.
  A group has a topic, a description and a link to its site; its page
  shows the member count, the owner and the creation date, and search
  results show the topic.
- **Feed** — posts with up to five photos or an mp4 video, on a user page or
  in a group, with nested comments of unlimited depth.
  - Loads in portions of 20 with infinite scroll.
  - A post, comment or deletion updates only the post it touches, so
    drafts and loaded portions elsewhere stay intact.
  - New posts and comments by others show up on their own at the top of
    the feed; further down a banner offers them instead of moving the page.
- **Search** — one search box across user profiles and group names.

## Tech stack

Java 21 · Spring Boot 4.1 (Web MVC, Security, Validation) · MySQL 8.4 ·
Spring Data JPA (Hibernate) · Flyway · Thymeleaf · vanilla JavaScript ·
JWT (jjwt) · BCrypt · libphonenumber · JUnit 5 + Mockito · Testcontainers · Playwright ·
Checkstyle · GitHub Actions · Docker

## Getting started

The only prerequisite is Docker:

```bash
docker compose up --build
```

Open http://localhost:8080, register a user and explore: the SMS codes
are on http://localhost:8080/dev/sms and the letters in the test inbox on
http://localhost:8025. To run against
your own MySQL instead, start MySQL 8 on `localhost:3306` and run
`./mvnw spring-boot:run`. Flyway creates and versions the schema on first
start.

Configuration is environment-based:

| Variable      | Default          | Purpose                                  |
|---------------|------------------|------------------------------------------|
| `DB_HOST`     | `localhost`      | MySQL host                                |
| `DB_PORT`     | `3306`           | MySQL port                                |
| `DB_NAME`     | `socialpath`     | Database name                             |
| `DB_USER`     | `root`           | Database user                             |
| `DB_PASSWORD` | `root`           | Database password                         |
| `MEDIA_DIR`   | `./media`        | Directory for uploaded media (a named Docker volume in compose) |
| `JWT_SECRET`  | dev-only default | HMAC key for session tokens — set a long random value in any real deployment |
| `SESSION_TIMEOUT` | `7d`         | How long a session lasts without being used |
| `GEOIP_DATABASE` | `./geoip/dbip-city-lite.mmdb` | Offline IP location database; the Docker image downloads it, without it locations show as unknown |
| `SPRING_PROFILES_ACTIVE` | — | `dev` adds exception messages and stack traces to error responses, for local debugging only |

## Architecture

Classic layered Spring MVC. Thymeleaf renders full pages and the HTML
fragments that small vanilla-JS modules fetch and swap in — no front-end
framework, and forms that need no interactivity (login, profile, group
settings) are plain HTML forms. Controllers stay thin: they receive the
signed-in user as a parameter (`@CurrentUser`, the entity the JWT filter
already loaded) and delegate to services that own validation,
authorization and transactions. Persistence is Spring Data JPA over a
Flyway-versioned schema with Hibernate in `ddl-auto=none`, so the schema
has exactly one owner.

The feed is built in a fixed number of queries per portion: the posts,
the whole comment tree under them in one recursive CTE, the authors as a
projection, and the media in batches; the tree is assembled in memory.
Portions use keyset pagination (`id < last shown`), so posts published
while someone scrolls never shift or duplicate what they already have.
Open feeds poll a single "newest activity by others" id every 30 seconds
while the tab is visible.

Uploaded media lives on the file system (a Docker volume) under
server-generated `{uuid}.{ext}` names. Every file id is validated against a
strict pattern and a normalized-path containment check before any disk
access, so a crafted id cannot escape the media directory.

## Performance

Measured with `show-sql` on a feed of 20 posts with 3 comments each:

| | SQL statements |
|---|---|
| Feed, before (one query chain per post and comment) | 327 |
| Feed, now (any portion size) | 5 |
| Any page, per request for authentication | 4 → 2, + 1 session lookup |

The last line comes from a classic Spring Boot pitfall: the JWT filter was
a `@Component`, so it was registered both as a servlet filter and inside
the security chain and ran twice per request. Its servlet registration is
now disabled. The session lookup (by primary key) came later with
revocable sessions; writing a used session back happens at most once per
five minutes, not on every request.

## Data model

Thirteen tables around a natural primary key: `users.login` is immutable,
embedded in JWTs and URLs, and referenced by every relation.

- `friendships` stores mutual friendship as mirrored rows (one per
  direction), making membership checks a simple indexed lookup from either
  side; pending invitations live in `friend_invites`.
- `social_groups` (named so because `GROUPS` is a reserved word in MySQL 8)
  with `group_members` and `group_admins` join tables; the owner is always
  present in both. Groups created before `created_at` existed got an
  estimate in the migration: the earliest first post among the group and
  all groups after it (ids follow creation order), the latest time that
  agrees with both facts. Joining and leaving are single `INSERT`/`DELETE`
  statements, idempotent thanks to the composite primary keys.
- `publications` holds posts **and** comments in one table with a
  self-referencing `parent_id` (adjacency list): a comment is simply a
  publication with a parent. A row's placement is expressed by its columns —
  `author_login` for user pages, `group_id` for group feeds, `parent_id`
  for replies. Attached files are ordered rows in `publication_media`.
- Deleting a post is one root `DELETE`: `ON DELETE CASCADE` atomically
  removes the reply subtree and its media rows. The application handles the
  part the database cannot — files on disk: their ids are collected up
  front with a single recursive CTE over the subtree, and the files are
  removed only after the transaction commits, so a rollback never leaves
  rows pointing at deleted files.
- `countries` is an ISO 3166 reference table for profile countries.
- Phone numbers moved from free text to E.164 in a Java Flyway migration
  (parsing takes libphonenumber): numbers without a country code were read
  as local numbers of the profile's country, and what still could not be
  read is kept as typed in `phone_legacy` until the user corrects it.
- A number is confirmed per account (`users.phone_verified`), and a stored
  generated column `verified_phone` - the number while it is confirmed,
  `NULL` otherwise - carries a unique key: any number may be typed by
  several accounts but confirmed by one, the one it signs in.
  `phone_codes` keeps the last code sent to each user as a BCrypt hash.
  Email addresses work the same way (`email_verified`, a unique
  `verified_email` that ignores case through the column's collation), and
  `email_links` keeps one link per user as a SHA-256 hash of its token,
  overwritten in place: replacing it by delete-then-insert made concurrent
  sign-ups deadlock on InnoDB gap locks.
- `user_sessions` has one row per signed-in browser; the JWT carries its id,
  so deleting the row signs that browser out. `video_jobs` is the queue of
  uploaded videos waiting to be converted to MP4.

## Security

- **Authentication** — login issues a JWT stored in an `HttpOnly`, `Secure`,
  `SameSite=Strict` cookie set by the server; the token never passes through
  front-end JavaScript, and `SameSite=Strict` doubles as the CSRF defence
  for the cookie-based session.
- **Revocable sessions** — the token names a server-side session and is
  accepted only while that session exists, so logging out, ending a session
  from the Security page or changing the password (which ends every other
  session) stops even a copied token at once. IP locations come from the
  offline [DB-IP](https://db-ip.com) Lite database (CC BY 4.0): addresses
  are never sent to a third party.
- **Passwords** — BCrypt (strength 12).
- **Phone codes** — random 6 digits from `SecureRandom`, stored only as a
  BCrypt hash, valid for 10 minutes and 5 attempts, and only for the number
  they were sent to.
- **Email links** — 256 random bits from `SecureRandom`, stored only as a
  SHA-256 hash, valid for 24 hours and once, and only for the address they
  were sent to; a newer link replaces the older one. A failed sign-in says
  "incorrect login or password" whatever was wrong, so it does not reveal
  which logins, numbers or addresses have accounts.
- **Authorization from stored data only** — permissions never trust ids the
  client sends along. Where a comment lives is read from the top-level post
  of its thread, so a comment cannot be posted to, or deleted from, a feed
  the user has no rights in. Only members post in a group, only the owner
  manages it and appoints admins (from its members), and the owner cannot
  leave their own group.
- **Errors** — denied and missing resources render 403/404 pages for page
  requests and JSON errors for REST calls; stack traces and exception
  messages never reach the client outside the `dev` profile.
- **Validation** — Bean Validation with externalized messages on dedicated
  form DTOs (e.g. `RegistrationForm` owns the plaintext-password rules;
  the date of birth is assembled and checked on the server), while entity
  constraints describe the stored data, so JPA's persist-time validation
  stays enabled as a second line of defence.

## Testing & CI

- **Unit tests** (JUnit 5 + Mockito) cover the security-sensitive and
  tricky paths: JWT and session handling, every authorization rule of the comment and
  group endpoints, feed assembly and paging, the deletion invariant (media
  of the whole subtree is cleaned up), avatar processing including HEIC and
  AVIF, and date-of-birth validation.
- **Integration test** — boots the full context against a real MySQL via
  Testcontainers, exercising the Flyway migrations end to end.
- **Migration test** — migrates an empty database to the previous version,
  writes rows the old schema allows, then runs the new migration and checks
  how it filled in the data it had to derive.
- **Browser tests** (Playwright, `e2e/`) drive the running app the way a
  user does: sign-up and login, profile editing, the feed with infinite
  scroll and live updates, comments, groups and roles, friends and search,
  active sessions and signing other devices out,
  including the permission rules as seen from the UI.

GitHub Actions runs two jobs on every push: `build` (unit and integration
tests plus Checkstyle) and `e2e` (the app started with `docker compose`,
then Playwright; on failure the report, screenshots and traces are kept as
an artifact). The multi-stage Dockerfile builds a slim runtime image
running as a non-root user.

Running the tests locally:

```bash
./mvnw verify              # unit + integration tests (needs a Docker daemon)

cd e2e                     # browser tests
npm ci
npx playwright install chromium
npm run stack:up           # their own app + MySQL on port 8081
npx playwright test
npm run stack:down         # removes it with everything the tests wrote
```

The browser tests never touch the instance on port 8080: they run against
a separate Compose project (`docker-compose.e2e.yml`) whose data is thrown
away with its containers; its letters go to a test inbox of its own on
port 8026, which the tests read through Mailpit's API, never to a real
mailbox. `BASE_URL` and `MAILPIT_URL` point them at another instance, and
`PLAYWRIGHT_OUTPUT_DIR` puts what they write (screenshots, traces, the
106 MB files of the upload tests; `e2e/test-results` by default) into its
`socialpath` subfolder. The
screenshots above are produced by `npm run screenshots` in `e2e/`, which
fills that empty instance with demo data and captures the pages.
