# Authenticate service-to-service calls with a service token

- Status: proposed
- Date: 2026-06-06
- Deciders: Eric Bouchut

## Context and Problem Statement

Introducing microservices (for example a notification/email service) creates a
new security boundary: the web application authenticating itself when it calls
an internal service. This is distinct from browser-to-application
authentication, which is handled by server-side sessions (see ADR-0001). The
browser never calls internal services directly, so the user's login is not
involved here. How should the web application authenticate to an internal
service?

## Decision Drivers

- Small, fixed set of internal services on a trusted Docker network
- Setup and operational cost (extra infrastructure to run and maintain)
- Security of the credential in transit and at rest
- Keeping the architecture proportionate to a capstone project

## Considered Options

- Service token — shared secret (Tier 1)
- Service token — self-issued service JWT, signed by the caller (Tier 2)
- OAuth2 client-credentials grant (requires an Authorization Server)
- Mutual TLS (mTLS)

## Decision Outcome

Chosen: **a service token** that authenticates the calling *service* (not a
user). mTLS was rejected for now because it requires managing a certificate
authority and per-service certificate lifecycle — disproportionate for a few
services. The full OAuth2 client-credentials grant was rejected for now because
it requires standing up and operating an Authorization Server. The remaining
choice between Tier 1 (shared secret) and Tier 2 (self-issued JWT) is still
open; this ADR will be updated to `accepted` once that selection is made.

### Consequences

- Good: no additional infrastructure or container required; minimal setup.
- Good: the credential authenticates the service, so no user JWT is needed for
  internal calls; `refresh_tokens` remains unnecessary.
- Trade-off (Tier 1): a static shared secret must be stored securely
  (secrets manager/env), transmitted over TLS, and rotated; either side
  leaking it compromises the pair.
- Trade-off (Tier 2): introduces RSA key management (private key on the caller,
  public key on the callee) in exchange for token expiry and claims.
- Open: Tier 1 vs Tier 2 not yet decided; OAuth2 client-credentials remains a
  future option if a dedicated Authorization Server is introduced.

## Pros and Cons of the Options

### Service token — shared secret (Tier 1)

- 👍 Lowest cost; no new infrastructure; ~15 lines to implement
- 👎 Symmetric secret; both sides can forge; no built-in expiry

### Service token — self-issued JWT (Tier 2)

- 👍 Asymmetric (callee holds only the public key, cannot forge); token expiry
  and claims; validated by Spring as an OAuth2 Resource Server
- 👎 RSA key management; more moving parts than a shared secret

### OAuth2 client-credentials grant

- 👍 Standards-based; short-lived tokens; centralized client management
- 👎 Requires an Authorization Server to run and maintain (Spring Authorization
  Server or Keycloak)

### Mutual TLS (mTLS)

- 👍 Strong transport-layer identity; encryption and authentication combined
- 👎 Certificate authority and per-service certificate lifecycle to manage;
  heavy without a service mesh
