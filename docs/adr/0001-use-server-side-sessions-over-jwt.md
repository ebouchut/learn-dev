# Use server-side sessions instead of JWT for user authentication

- Status: accepted
- Date: 2026-06-06
- Deciders: Eric Bouchut

## Context and Problem Statement

The platform was initially designed with a React single-page application (SPA)
talking to a REST API on a separate origin — a setup that naturally suggests
stateless JWT authentication. The frontend has since changed to Thymeleaf,
which renders HTML server-side and is served from the same origin as the
backend. How should browser users be authenticated under this new architecture?

## Decision Drivers

- Fit with a same-origin, server-rendered architecture
- Security: token theft (XSS), revocation, CSRF protection
- Implementation simplicity and framework support
- Avoiding unnecessary moving parts

## Considered Options

- Server-side sessions (Spring Security `formLogin` + `HttpSession`)
- JWT stored in the browser (cookie or `localStorage`)

## Decision Outcome

Chosen: **server-side sessions**, because Thymeleaf is same-origin and
server-rendered, so the browser only needs a session cookie — natively
supported by Spring Security. Storing a JWT in a cookie would effectively
re-implement a session, but with weaker security and more code.

### Consequences

- Good: native Spring Security support (`formLogin` + `HttpSession`); easy
  server-side revocation (invalidate the session); built-in CSRF token
  integrated with Thymeleaf forms; `HttpOnly` cookie not readable by JS.
- Good: removed the `refresh_tokens` table, a JWT-only concern.
- Trade-off: sessions are stateful; horizontal scaling across instances would
  need a shared session store (e.g. Spring Session + Redis). Acceptable for a
  single-instance application.
- Note: JWT/OAuth2 may be reintroduced later for a *different* boundary — a
  future mobile app or public API — without changing this decision for the web UI.

## Pros and Cons of the Options

### Server-side sessions

- 👍 Simple; revocable; CSRF built-in; `HttpOnly; Secure; SameSite` cookie
- 👎 Stateful; needs a shared store to scale horizontally

### JWT in the browser

- 👍 Stateless; fits a separate-origin SPA or mobile client
- 👎 XSS theft risk (`localStorage`); hard to revoke; manual expiry/refresh;
  reinvents a session if placed in a cookie
