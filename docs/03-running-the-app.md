# Running the App

## Prerequisites

- **Java 21+** — verify with `java -version`
- **Maven 3.9+** — verify with `mvn -version`
  (or use the included Maven Wrapper: `./mvnw -version`)
- An Auth0 tenant configured per [01-auth0-setup.md](01-auth0-setup.md)

---

## 1. Configure environment variables

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

Edit `.env`:

```
AUTH0_DOMAIN=your-tenant.us.auth0.com
AUTH0_CLIENT_ID=your-regular-web-app-client-id
AUTH0_CLIENT_SECRET=your-regular-web-app-client-secret
AUTH0_MGMT_CLIENT_ID=your-m2m-client-id
AUTH0_MGMT_CLIENT_SECRET=your-m2m-client-secret
```

Alternatively, export them as shell environment variables:

```bash
export AUTH0_DOMAIN=your-tenant.us.auth0.com
export AUTH0_CLIENT_ID=...
# etc.
```

> **Note:** Spring Boot reads environment variables directly. If you use `.env`, you need a tool
> like [`dotenv`](https://github.com/motdotla/dotenv) to load it, or use
> `export $(cat .env | xargs)` before running.

---

## 2. Start the app

```bash
./mvnw spring-boot:run
```

Or if you have Maven installed globally:

```bash
mvn spring-boot:run
```

You should see:

```
Started Application in X.XXX seconds (process running for X.XXX)
```

---

## 3. Open the app

Navigate to [http://localhost:8080](http://localhost:8080).

---

## 4. Log in

Click **Login with Auth0**. You will be redirected to Auth0's Universal Login page.

Log in as a user who is a **member of an Auth0 organization** (see step 2 in
[01-auth0-setup.md](01-auth0-setup.md)). After authentication you are returned to the home page.

---

## 5. View your profile

Click **Profile** in the nav. You should see your ID token claims, including `org_id`.
If `org_id` is missing, check the Organizations configuration in Auth0 Dashboard.

---

## 6. Generate an SSO setup link

1. Click **SSO Setup Link** in the nav.
2. Enter a **Connection Name** (e.g. `acme-saml`). Use only letters, numbers, and hyphens.
3. Optionally enter **Domain Aliases** (e.g. `acme.com, acme.org`).
4. Click **Generate Setup Link**.
5. Copy the resulting URL and share it with the enterprise IT admin.

---

## Troubleshooting

| Symptom | Likely cause |
|---------|-------------|
| Button is disabled on `/sso-setup` | `org_id` missing from token — check Organizations config |
| Error: "Management API credentials are not configured" | `AUTH0_MGMT_CLIENT_ID` / `AUTH0_MGMT_CLIENT_SECRET` not set |
| 403 from Management API | M2M app is missing required scopes — see [01-auth0-setup.md](01-auth0-setup.md) |
| Login redirects to wrong URL | Check Allowed Callback URLs in Auth0 Dashboard |
