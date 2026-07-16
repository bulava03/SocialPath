# SocialPath

A social network web application built with Spring Boot and MySQL. Users
create profiles, connect as friends, join groups, publish posts with photos
and video, discuss them in threaded comments, and report content for
moderation.

## Features

- **Accounts** — registration with validated input, JWT-based sessions,
  password change, profile editing (bio fields, birth date, location,
  education, workplace) and avatar upload.
- **Friends** — invitations, accepting/declining, friend lists, removal;
  friendship is always mutual.
- **Groups** — anyone can create a group and becomes its owner; owners
  appoint admins, admins moderate content and membership; users join and
  leave freely.
- **Publications** — text posts with up to five attached photos or an mp4
  video, on the user's own page or in a group.
- **Comments** — nested, unlimited depth: a comment can be replied to just
  like a post. Authors, page owners and group moderators can delete a post
  or comment together with its whole reply subtree and all attached media.
- **Reports** — users report publications, comments or groups; reports are
  stored with status tracking for a moderation workflow.
- **Search** — one search box across user profiles (name, contacts,
  location, education, workplace) and group names.

## Tech stack

Java 21 · Spring Boot 3.3 (Web MVC, Security, Validation) · MySQL 8 ·
Spring Data JPA (Hibernate) · Flyway · Thymeleaf · JWT (jjwt) · BCrypt ·
JUnit 5 + Mockito · Testcontainers · Checkstyle · GitHub Actions · Docker

## Getting started

The only prerequisite is Docker:

```bash
docker compose up --build
```

Open http://localhost:8080, register a user and explore. To run against
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

Run the test suite with `./mvnw verify` (requires a Docker daemon: the
context test boots the application against a throwaway MySQL via
Testcontainers, exercising the Flyway migration end to end).

## Architecture

Classic layered Spring MVC. Thymeleaf renders full pages; interactive parts
(feeds, comments, membership buttons) are HTML fragments re-rendered on the
server and swapped in by small vanilla-JS fetch calls — no front-end
framework. Controllers stay thin and delegate to a service layer that owns
validation, authorization checks and transactions; persistence is Spring
Data JPA repositories over a Flyway-versioned schema (`db/migration`),
with Hibernate running in `ddl-auto=none` — the database schema has exactly
one owner.

Uploaded media lives on the file system (a Docker volume) under
server-generated `{uuid}.{ext}` names. Every file id is validated against a
strict pattern and a normalized-path containment check before any disk
access, so a crafted id cannot escape the media directory.

## Data model

Ten tables around a natural primary key: `users.login` is immutable,
embedded in JWTs and URLs, and referenced by every relation.

- `friendships` stores mutual friendship as mirrored rows (one per
  direction), making membership checks a simple indexed lookup from either
  side; pending invitations live in `friend_invites`.
- `social_groups` (named so because `GROUPS` is a reserved word in MySQL 8)
  with `group_members` and `group_admins` join tables; the owner is always
  present in both.
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
- Search is a multi-field case-insensitive `LIKE` query; a `FULLTEXT` index
  is the designated next step if profiles grow large.

## Security

- **Authentication** — login issues a JWT stored in an `HttpOnly`, `Secure`,
  `SameSite=Strict` cookie set by the server; the token never passes through
  front-end JavaScript, and `SameSite=Strict` doubles as the CSRF defence
  for the cookie-based session.
- **Passwords** — BCrypt (strength 12).
- **Authorization** — every mutating endpoint checks ownership: publications
  and comments are deletable by their author, the page owner or group
  moderators; group management is restricted to the owner and admins.
  Violations raise a dedicated exception, translated to a redirect for pages
  and a `403` JSON body for REST endpoints.
- **Validation** — Bean Validation with externalized messages. Raw input is
  validated on dedicated form DTOs (e.g. `RegistrationForm` owns the
  plaintext-password rules), while entity constraints describe the stored
  data, so JPA's persist-time validation stays enabled as a second line of
  defence.

## Testing & CI

Unit tests cover the security-sensitive paths: JWT parsing and validation,
endpoint authorization rules, and the publication-deletion invariant (media
of the whole reply subtree is cleaned up alongside the rows). An integration
test boots the full context against a real MySQL via Testcontainers. Every
push runs the suite plus Checkstyle through GitHub Actions, and the
multi-stage Dockerfile builds a slim runtime image running as a non-root
user.
