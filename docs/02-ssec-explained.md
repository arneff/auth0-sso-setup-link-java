# How SSEC Works

Auth0's **Self-Service Enterprise Configuration (SSEC)** API lets you delegate enterprise IdP
setup to your customers' IT admins — without any manual work on your end.

---

## The old way

1. Enterprise IT admin emails you their SAML metadata XML (or OIDC discovery URL).
2. You log into the Auth0 Dashboard, create an enterprise connection, paste in the metadata.
3. You configure the connection for the right organization, set domain aliases for HRD.
4. You email back the Auth0 SP metadata (entity ID, ACS URL) so the IT admin can finish
   configuring their IdP.
5. Back and forth, error-prone, takes days.

## The new way (SSEC)

1. Your app calls the Auth0 Management API to create a one-time **SSEC ticket URL**.
2. You send that URL to the enterprise IT admin.
3. The IT admin visits the URL and sees Auth0's **hosted setup wizard**.
4. They pick their IdP type (Okta, Azure AD, generic SAML, OIDC, etc.).
5. They paste in their metadata or enter their OIDC details.
6. Auth0 shows them the SP metadata they need to configure on their IdP side.
7. They save. Done. The enterprise connection is live.

No manual Dashboard work required on your part.

---

## The three API calls this app makes

### 1. Client Credentials Token

```
POST https://{domain}/oauth/token
Content-Type: application/json

{
  "grant_type": "client_credentials",
  "client_id": "{m2m_client_id}",
  "client_secret": "{m2m_client_secret}",
  "audience": "https://{domain}/api/v2/"
}
```

Returns an access token with the Management API scopes you requested.

### 2. Get or Create a Self-Service Profile

A **self-service profile** is a reusable template that defines how setup links behave
(which user attributes to collect, branding, etc.).

```
GET https://{domain}/api/v2/self-service-profiles
Authorization: Bearer {mgmt_token}
```

If no profile exists:

```
POST https://{domain}/api/v2/self-service-profiles
Authorization: Bearer {mgmt_token}
Content-Type: application/json

{
  "name": "SSO Setup Profile",
  "user_attributes": []
}
```

### 3. Create the SSO Ticket

```
POST https://{domain}/api/v2/self-service-profiles/{profile_id}/sso-ticket
Authorization: Bearer {mgmt_token}
Content-Type: application/json

{
  "connection_config": {
    "name": "acme-saml",
    "options": {
      "domain_aliases": ["acme.com"]
    }
  },
  "enabled_organizations": [
    {
      "organization_id": "org_abc123",
      "assign_membership_on_login": true
    }
  ]
}
```

Response:

```json
{
  "ticket": "https://your-tenant.us.auth0.com/self-service/..."
}
```

The `ticket` URL is **valid for 5 days** and is **single-use**.

---

## What the enterprise IT admin sees

When the IT admin opens the ticket URL they see Auth0's hosted SSEC portal:

1. **Pick your IdP type** — Okta, Microsoft Azure AD, Google Workspace, generic SAML 2.0,
   generic OIDC, etc.
2. **Enter your IdP details** — paste SAML metadata XML, or enter OIDC issuer/client credentials.
3. **Copy SP metadata** — Auth0 provides the Entity ID and ACS URL the IT admin needs to register
   in their IdP.
4. **Save** — the enterprise connection is created and enabled for the organization.

---

## After setup: Home Realm Discovery (HRD)

Once the connection is live, any user who signs in with an email address matching one of the
configured `domain_aliases` (e.g. `@acme.com`) is automatically routed to the enterprise IdP.
No manual step required from you or the user.
