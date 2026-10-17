# Dormigo Phase 1 Integration Audit

Audit date: 2026-10-17

## Scope And Safety

This document is a read-only Phase 1 inventory. No API source, client URL, database object, runtime upload, Git state, or application behavior was changed.

Two separate implementations currently exist:

| Label | Location | Primary consumer | Authentication model |
| --- | --- | --- | --- |
| Backend API | `C:/xampp/htdocs/Dormigo_Backend/api/` | Android | Bearer tokens and role checks |
| Web API | `php_backend/api/` | Public, Student, Landlord, and Admin web portals | PHP sessions, including Admin OTP |

Android is hard-wired to the Backend API. Public root pages and Admin pages generally target the Web API. Admin property review and several upload URLs still call the Backend API. Student and Landlord pages load the Web API script but its relative base URL resolves incorrectly when served from their nested directories.

## Contract Conventions

- API responses are JSON unless an endpoint streams a protected file.
- Most responses use `success`, `message`, and/or `data` fields.
- Backend API routes generally use bearer-token authorization and proper HTTP status codes.
- Web API routes generally use PHP sessions, but several operational routes currently lack authorization. These routes must not be considered a safe canonical implementation.

## Endpoint Contract Inventory

### Shared Endpoint Names

| Endpoint | Backend contract | Web contract | Client dependency | Canonical integration decision |
| --- | --- | --- | --- | --- |
| `login.php` | `POST` JSON `email,password`; creates seven-day bearer token; excludes deactivated accounts. | `POST` JSON `email,password`; creates Student/Landlord PHP session. `GET?action=logout` destroys session. | Android, Web | Keep both auth handoffs behind one credential verifier. |
| `register.php` | `POST` JSON `full_name,email,password,user_type`, optional phone; stronger password policy. | Same core JSON payload; creates web session. | Web; legacy Android flow | Merge validation and secure password storage with an explicit web-session handoff. |
| `users.php` | `GET` by ID/search/role; bearer self-only `PATCH`; bearer password-confirmed account deactivation. | `GET` by ID/role; unrestricted `PATCH`, including password/profile. | Android, Web, Admin | Preserve Backend authorization; add separately authenticated admin operations. |
| `boarding_houses.php` | Public `GET`; bearer landlord writes; owner checks, subscription gate, validated photos/QR, soft delete. | Public/admin-oriented reads; unauthenticated caller-supplied landlord writes, direct photo URLs, hard delete. | Android, Web, Admin | Preserve Backend mutations; retain required admin read projection behind Admin authorization. |
| `rooms.php` | `GET` filters; bearer landlord writes; protects occupancy and reservation capacity. | Basic unrestricted CRUD/filtering. | Android, Web, Admin | Preserve Backend mutation rules; add authorized admin reads. |
| `amenities.php` | `GET`; `POST amenity_name` upsert. | Listing only. | Android, Web | Preserve Backend behavior; restrict management to Admin. |
| `boarding_house_amenities.php` | `GET house_id`; `POST`/`DELETE house_id,amenity_id`; no current ownership check. | Same contract. | Android, Web | Preserve one route and add landlord-owner/Admin authorization. |
| `bookings.php` | `GET` filters; bearer `POST room_id,duration_months,move_in_date` plus agreed pricing; controlled `PATCH` transitions/due dates. | ID-based `GET`/`POST`; status updates intentionally rejected. | Android, Web, Admin | Preserve Backend capacity, overlap, status, pricing snapshot, and notification logic. |
| `payments.php` | Bearer `GET`; QR/cash `POST booking_id,payment_method,transaction_ref,proof image`; authorized `PATCH` verification/rejection. | Unrestricted caller-controlled amount/status metadata. | Android, Web, Admin | Preserve Backend QR-only proof and immutable pricing behavior. |
| `reviews.php` | `GET`; bearer Student `POST house_id,rating,comment`; completed-stay and duplicate controls. | ID-based unrestricted review creation/filtering. | Android, Web, Admin | Preserve Backend eligibility and reporting rules. |
| `messages.php` | `GET`, `POST`, `DELETE`, mute actions; conversation clearing, mute state, notifications. | `GET`, `POST`, `PATCH` mark-read. | Android, Web | Merge Backend features, but add verified participant identity to every action. |
| `notifications.php` | Bearer `GET`/`PATCH`; supports type-filtered mark-all-read. | Session `GET`/`PATCH`; supports web and Admin session bridge. | Android, Web, Admin | Implement common behavior with bearer and session identity adapters. |
| `db.php` | Hard-coded local connection values and wildcard CORS. | Environment-only database configuration but origin reflection with credentials. | All | Use the Web environment model, with an explicit allowed-origin policy. |

### Backend-Only HTTP Routes

| Area | Routes and contract | Consumer | Integration decision |
| --- | --- | --- | --- |
| Password recovery | `forgot_password.php` `POST email`; `verify_otp.php` `POST email,otp_code`; `reset_password.php` `POST email,otp_code,new_password`. Rate limited with generic replies. | Android | Preserve. Reuse a credential-free mail configuration provider. |
| Registration verification | `register_student.php` and `register_landlord.php` `POST` multipart account fields plus required document; create PENDING verification. | Android | Preserve Android contracts; require secure password storage and upload validation. |
| Verification submission | `submit_verification.php`, `submit_student_verification.php`, `submit_landlord_verification.php` accept verification data/files. | Android | Preserve feature contracts, but require caller identity for all student submissions. |
| Verification status | `get_student_verification_status.php`, `get_landlord_verification_status.php`, `get_landlord_account_verification_status.php`, `get_student_verifications.php`, `get_verifications.php`. | Android, Admin | Preserve data shape; require self/Admin authorization and protected document paths. |
| Verification review | `approve_verification.php`, `reject_verification.php`, `approve_student_verification.php`, `reject_student_verification.php`, `review_property_verification.php`. | Android, Admin | Preserve Backend transition logic. Replace request-supplied `admin_id` authorization with authenticated Admin identity. |
| Admin statistics | `get_admin_dashboard_stats.php` `GET`. | Android | Merge into authenticated Admin statistics service. |
| Review operations | `get_eligible_reviews.php`, `report_review.php`. | Android | Preserve bearer Student/Landlord authorization. |
| Move-in | `confirm_move_in.php` `POST booking_id`; landlord only; verifies payment, dates, and capacity. | Android | Preserve. |
| Property charges | `property_charges.php` `GET house_id`; bearer landlord write/delete actions for deposits, fees, utilities. | Android | Preserve. |
| Maintenance | `maintenance_requests.php`; `landlord_maintenance_requests.php`. | Android | Preserve Student/Landlord role transition logic. |
| Subscriptions | `landlord_subscriptions.php`; `admin_landlord_subscriptions.php`. QR submissions require transaction reference and image proof. | Android | Preserve payment-proof rules, subscription gate, audit event, and review logic. |

Backend support modules are not HTTP routes: `auth_helper.php`, `payment_model.php`, `payment_model_backup.php`, `subscription_model.php`, and `password_reset_security.php`.

### Web-Only HTTP Routes

| Route | Contract | Consumer | Integration decision |
| --- | --- | --- | --- |
| `admin_auth.php` | Admin login, email OTP verification/resend, password-change OTP, logout/check, and profile update. | Admin | Preserve as the browser Admin session boundary. |
| `stats.php` | `GET role=admin` or `role=landlord&landlord_id`. | Admin, Landlord | Replace caller-supplied role/ID with authenticated identity. |
| `verifications.php` | Listing/submission/review by `type`; multipart document input. House review already delegates to the Backend endpoint. | Web, Admin | Preserve UI data needs, but move state transitions behind authenticated canonical services. |
| `serve_verification.php` | `GET path`; streams a sanitized verification file. | Admin | Preserve protected streaming after validating Admin role from the database. |
| `subscriptions.php` | Session-Admin list/review of landlord subscriptions. | Admin | Merge with `admin_landlord_subscriptions.php`; retain session adapter. |
| `subscription_payment_config.php` | Session-Admin `GET` and multipart QR configuration `POST`. | Admin | Preserve Admin QR configuration and validated image uploads. |
| `subscription_proof.php` | Session-Admin `GET subscription_id`; streams validated proof image. | Admin | Preserve protected proof serving. |

Web support modules are not routes: `web_session.php`, `mail_helper.php`, `mail_config.php`, and the ignored `mail_config.local.php`.

## PostgreSQL Schema Inspection

### Read-Only Inspection Status

PostgreSQL 18 client tooling is installed. A passwordless `psql` connection to the local `dormigo` database was rejected, and no `DORMIGO_DB_PASSWORD` value is available to the shell. No password was read, displayed, copied, or placed in a command. Therefore, the live schema has **not** been inspected yet.

Static API analysis identifies candidate requirements only; it is not proof that these database objects are absent from the live database.

### Required Read-Only Metadata Queries

Run these with a read-only PostgreSQL role or with an approved local credential supplied outside this repository:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

SELECT table_name, column_name, data_type, udt_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
ORDER BY table_name, ordinal_position;

SELECT t.typname AS enum_name, e.enumlabel AS enum_value
FROM pg_type t
JOIN pg_enum e ON t.oid = e.enumtypid
JOIN pg_namespace n ON n.oid = t.typnamespace
WHERE n.nspname = 'public'
ORDER BY t.typname, e.enumsortorder;

SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
ORDER BY table_name::text, conname;

SELECT indexrelid::regclass AS index_name, indrelid::regclass AS table_name,
       pg_get_indexdef(indexrelid) AS definition
FROM pg_index
WHERE indrelid::regclass::text LIKE 'public.%'
ORDER BY table_name::text, index_name::text;
```

### Candidate Objects To Verify

| Category | Tables, columns, or enum values to verify against the live schema |
| --- | --- |
| Authentication | `auth_tokens`, `password_reset_tokens`, `password_reset_rate_limits`, `admin_otps`, and `users.deactivated_at`. |
| Verification | `student_verifications`, `landlord_verifications`, `boarding_house_verification`, reviewer/rejection/timestamp columns. |
| Messaging/reviews | `muted_conversations`, `deleted_conversations`, `review_reports`, `reviews.is_hidden`, `notifications.reference_id`. |
| Property pricing | `property_deposits`, `property_additional_fees`, `property_utilities`, `room_payment_terms`, boarding-house and room charge fields. |
| Booking/payment | `booking_pricing_snapshots`, `payment_legacy_duplicate_exceptions`, booking due/payment flags, payment proof/type/review fields. |
| Subscriptions | `subscription_plans`, `subscription_payment_config`, `landlord_subscription_preferences`, `landlord_subscriptions`, `landlord_subscription_review_events`. |
| Enums | `ADMIN` user role; booking/payment/notification states required by Android and Admin operations; payment type enum. |

No migration should be authored until the above live-schema results are captured and compared with the checked-in development script.

## Publication Security Findings

### Blockers Before Public Publication

| Priority | Finding | Evidence | Required resolution |
| --- | --- | --- | --- |
| Critical | Web verification upload accepts unauthenticated files under the web root, preserving client extensions. | `php_backend/api/verifications.php` | Require authorization, validate content and extension, store private documents outside the web root, and disable execution in any public upload directory. |
| Critical | Web API permits unauthenticated reads and mutations of users, properties, and verification status. | `php_backend/api/users.php`, `boarding_houses.php`, `verifications.php` | Route all mutations through session/bearer role and ownership checks. |
| Critical | Student verification approval/rejection trusts request-supplied Admin IDs. | `Dormigo_Backend/api/approve_student_verification.php`, `reject_student_verification.php` | Derive the reviewer only from authenticated bearer/session identity. |
| Critical | Password compatibility permits plaintext storage/verification, and the development SQL includes demo identities and plaintext passwords. | Both `register.php` routes, login routes, `dormigo_database.sql` | Hash new passwords, migrate legacy hashes safely, remove/sanitize public seed data. |
| High | Sibling Backend has hard-coded database settings in publicly reachable PHP. | `Dormigo_Backend/api/db.php`, `Dormigo_Backend/db.php` | Apply environment-only configuration during approved consolidation. |
| High | CORS allows arbitrary origins, including credentialed origin reflection in the Web API. | Both `db.php` files | Use an environment-configured allow-list and disable credentials for untrusted origins. |
| High | Verification/user information and document paths are exposed by unauthenticated routes. | Backend verification/status/statistics routes | Require self/Admin authorization and remove direct file paths from public responses. |
| High | Live uploads contain identity documents, QR images, and payment proofs. | `Dormigo_Backend/uploads/` | Keep all user uploads out of Git, retain controlled access only, and review existing Git history before publication. |
| High | Local SMTP credentials and OTP logs exist in the workspace. | Ignored `mail_config.local.php`; `php_backend/logs/otp_debug.log` | Rotate SMTP credential; delete/rotate OTP logs only with explicit approval; keep both ignored. |

### Additional Risks

- Android uses HTTP and a private LAN address with cleartext traffic enabled.
- Several PHP routes reveal exception details to clients.
- The Backend upload `.htaccess` blocks only selected verification filename patterns; payment proofs and other sensitive file names are not equivalently protected.
- `Dormigo_Backend` has no ignore rules and includes dependencies, runtime uploads, and historical test/preflight artifacts.
- The Backend tracks Composer `vendor/`; retain `composer.json` and `composer.lock`, then decide whether to rebuild dependencies during the approved consolidation.

## Sensitive And Runtime File Policy

| Content | Git policy |
| --- | --- |
| Database, SMTP, signing, and local environment credentials | Never commit. |
| OTP/debug logs | Never commit. |
| Identity documents, payment proofs, property photos, live QR images | Never commit. Preserve operationally outside Git. |
| Upload access rules and empty-directory markers | Track only reviewed `.htaccess` and `.gitkeep` files. |
| Composer manifest and lockfile | Track. |
| Composer `vendor/` | Rebuild from lockfile for the consolidated repository unless a deployment constraint requires another approved policy. |
| Android build products, IDE state, APKs, local SDK settings | Ignore. |

## Target Repository Structure

This is a target structure only. No file move is approved or performed.

```text
Dormigo/
|- android/
|  |- app/
|  |- gradle/
|  |- build.gradle
|  `- settings.gradle
|- backend/
|  |- api/
|  |- config/
|  |- database/
|  |  |- migrations/
|  |  `- seeds/
|  |- storage/
|  |  `- uploads/                 ignored, external/private in production
|  |- composer.json
|  `- composer.lock
|- web/
|  |- public/
|  |- admin/
|  |- landlord/
|  |- student/
|  `- shared-assets/
|- docs/
|- .env.example
|- .gitignore
|- README.md
|- LICENSE
`- SECURITY.md
```

The current structure must remain in place until all client paths, PHP include paths, upload paths, and deployment settings have passed compatibility tests.

## Prioritized Integration And Testing Checklist

### P0: Block Publication Or Consolidation

- [ ] Obtain a read-only database connection and record the actual schema metadata.
- [ ] Rotate the local SMTP credential and confirm it is not tracked by any repository history.
- [ ] Remove/sanitize development identities and plaintext password seeds before public release.
- [ ] Define the canonical authentication model and protect every mutation endpoint.
- [ ] Protect verification and payment-proof storage from public direct access and script execution.
- [ ] Confirm no live uploads, logs, IDE files, APKs, or local configuration would be staged.

### P1: Preserve Functional Contracts

- [ ] Create request/response fixtures for all Android-used endpoints.
- [ ] Preserve QR payment proof, transaction-reference, verification, and review contracts.
- [ ] Preserve landlord subscription activation, expiry, review, and audit behavior.
- [ ] Preserve Admin OTP login, password-change verification, session timeout, and protected document serving.
- [ ] Preserve booking capacity, overlap, move-in, pricing snapshot, and status-transition rules.
- [ ] Test each route as Student, Landlord, Admin, unauthenticated, and unauthorized users.

### P2: Consolidate Safely

- [ ] Add versioned, additive migrations only after live-schema comparison.
- [ ] Introduce server environment configuration for database, CORS, mail, upload root, and upload URL.
- [ ] Add Android build configuration and web runtime configuration for API base URLs.
- [ ] Port Backend-only endpoints into the canonical backend without changing endpoint contracts.
- [ ] Port Web-only session/Admin endpoints as adapters over shared domain services.
- [ ] Correct nested Student/Landlord API URL resolution and remove Admin sibling-backend fallbacks.

### P3: Release Validation

- [ ] Run PHP linting, Composer integrity checks, and Android build/tests.
- [ ] Run endpoint contract, authorization, upload, booking, payment, subscription, and verification tests against a staging database copy.
- [ ] Verify migrations preserve row counts, records, document paths, and payment/subscription state.
- [ ] Switch clients only after staging parity and rollback procedures are approved.
