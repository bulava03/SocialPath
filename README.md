# SocialPath

A social network built with Spring Boot and MongoDB: profiles, friends, groups,
publications with media and threaded comments, moderation reports, and
site-wide search.

Started as a university project, then substantially reworked: real
authentication and authorization, media streaming instead of inline Base64,
a consolidated delete cascade, English UI with i18n-ready validation messages,
and a test suite around the security-sensitive logic.

## Tech stack

- Java 21, Spring Boot 3.3 (Web MVC, Security, Validation)
- MongoDB + GridFS (Spring Data)
- Thymeleaf server-rendered pages and AJAX-loaded HTML fragments
- JWT (jjwt), BCrypt
- JUnit 5 + Mockito, Checkstyle, GitHub Actions

## Getting started

The only prerequisite is Docker:

```bash
docker compose up --build
```

Then open http://localhost:8080, register a user and explore.

To run locally instead, start a MongoDB on `localhost:27017` and run:

```bash
./mvnw spring-boot:run
```

Configuration is environment-based (see `application.properties`):

| Variable     | Default          | Purpose                          |
|--------------|------------------|----------------------------------|
| `MONGO_HOST` | `localhost`      | MongoDB host                     |
| `MONGO_PORT` | `27017`          | MongoDB port                     |
| `JWT_SECRET` | dev-only default | HMAC key for session tokens — set a long random value in any real deployment |

## Security model

- **Authentication**: login issues a JWT stored in an `HttpOnly`, `Secure`,
  `SameSite=Strict` cookie set by the server. The token never passes through
  front-end JavaScript; `SameSite=Strict` is also the CSRF defence for the
  cookie-based session, which is why the Spring CSRF filter is disabled.
- **Passwords**: BCrypt (strength 12) via a single Spring `PasswordEncoder`.
- **Authorization**: every mutating endpoint checks ownership — publications
  and comments can be deleted by their author, the page owner, or group
  moderators; group management (removing members, appointing admins) is
  restricted to the owner/admins. Violations raise `ForbiddenOperationException`,
  translated to a redirect for pages and a `403` JSON body for REST.
- **Validation**: Bean Validation on both create and update paths, with
  messages externalized to `ValidationMessages.properties`. Client-side checks
  exist for UX only; the server is the source of truth.

## Architecture notes

- **Media**: uploads live in GridFS and are streamed by `ImageController`
  (`GET /images/{id}`) with long-lived cache headers — GridFS ids are
  immutable, so browsers cache aggressively. Pages carry only links, images
  lazy-load, videos fetch metadata until played.
- **Feed**: publications and comments share one recursive document model
  (`Publication` referencing child `Publication`s), rendered server-side as
  Thymeleaf fragments and loaded over AJAX — one template is the single
  source of markup for both initial render and updates.
- **Delete cascade**: removing a publication deletes the media files of every
  node in its comment subtree and then all documents in one batch. The cascade
  has a single owner in the service layer and a regression test
  (`CommentsDeletionCascadeTest`) guarding the invariant.
- **REST contract**: JSON bodies with semantic results
  (`{"result": "INVITED"}`) and proper HTTP statuses; errors are
  `{"error": "..."}` with 400/403/404, unauthenticated requests get 401.

## Testing

```bash
./mvnw verify
```

Unit tests cover JWT issuing/validation, the authorization rules for group
management and comment deletion, the delete cascade, validation plumbing and
helpers. Checkstyle runs as part of the build.

## Roadmap

- Batch the feed queries (`$in` lookups for publications and authors) to
  remove the current N+1 pattern
- Feed pagination
- Testcontainers-based integration tests against a real MongoDB
