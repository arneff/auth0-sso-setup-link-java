# Auth0 Setup

Follow these steps in the [Auth0 Dashboard](https://manage.auth0.com/) before running the app.

---

## 1. Create a Regular Web Application

1. Go to **Applications > Applications > Create Application**.
2. Choose **Regular Web Applications** (not SPA — this app needs a `client_secret`).
3. Name it (e.g. `SSO Setup Link Sample`).

### Allowed Callback URLs

```
http://localhost:8080/login/oauth2/code/okta
```

### Allowed Logout URLs

```
http://localhost:8080
```

4. Save changes and note the **Domain**, **Client ID**, and **Client Secret** — these map to
   `AUTH0_DOMAIN`, `AUTH0_CLIENT_ID`, and `AUTH0_CLIENT_SECRET` in your `.env`.

---

## 2. Enable Organizations

1. Go to **Organizations** in the left sidebar.
2. If prompted, enable the feature for your tenant.
3. Click **Create Organization**, give it a name (e.g. `acme-corp`).
4. Under the organization's **Members** tab, invite or add a test user.
5. Go to the organization's **Applications** tab and enable your Regular Web Application.

### Configure the app for Organizations

1. Open your Regular Web Application's settings.
2. Go to the **Organizations** tab.
3. Set **Login Flow** to **Business Users** (prompt for org, enables `org_id` in token).
4. Save changes.

---

## 3. Create an M2M Application for the Management API

1. Go to **Applications > Applications > Create Application**.
2. Choose **Machine to Machine Applications**.
3. Name it (e.g. `SSO Setup Link Sample (M2M)`).
4. On the next screen, select the **Auth0 Management API** as the authorized API.
5. Grant the following scopes:
   - `read:self_service_profiles`
   - `create:self_service_profiles`
   - `create:sso_access_tickets`
6. Click **Authorize**.
7. Note the **Client ID** and **Client Secret** — these map to
   `AUTH0_MGMT_CLIENT_ID` and `AUTH0_MGMT_CLIENT_SECRET` in your `.env`.

---

## Summary of environment variables

| Variable | Where to find it |
|----------|-----------------|
| `AUTH0_DOMAIN` | Applications > your app > Settings > Domain |
| `AUTH0_CLIENT_ID` | Applications > your Regular Web App > Settings > Client ID |
| `AUTH0_CLIENT_SECRET` | Applications > your Regular Web App > Settings > Client Secret |
| `AUTH0_MGMT_CLIENT_ID` | Applications > your M2M App > Settings > Client ID |
| `AUTH0_MGMT_CLIENT_SECRET` | Applications > your M2M App > Settings > Client Secret |
