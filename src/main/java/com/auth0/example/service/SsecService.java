package com.auth0.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Service for Auth0 Self-Service Enterprise Configuration (SSEC) API.
 *
 * <p>Flow:
 * <ol>
 *   <li>Obtain an M2M access token via client credentials grant (Auth0 Java SDK AuthAPI)</li>
 *   <li>GET or POST /api/v2/self-service-profiles to find/create a profile</li>
 *   <li>POST /api/v2/self-service-profiles/{id}/sso-ticket to create a one-time setup link</li>
 * </ol>
 *
 * <p>Required M2M scopes:
 * {@code read:self_service_profiles create:self_service_profiles create:sso_access_tickets}
 */
@Service
public class SsecService {

    private static final Logger log = LoggerFactory.getLogger(SsecService.class);

    @Value("${auth0.domain}")
    private String domain;

    @Value("${auth0.mgmt-client-id}")
    private String mgmtClientId;

    @Value("${auth0.mgmt-client-secret}")
    private String mgmtClientSecret;

    /**
     * Creates an SSEC setup link for the given org.
     *
     * @param connectionName  unique name for the enterprise connection (e.g. {@code acme-saml})
     * @param domainAliases   email domains that should route to this connection (may be empty)
     * @param orgId           Auth0 org_id from the authenticated user's ID token
     * @return the one-time SSEC ticket URL
     * @throws IllegalStateException if M2M credentials are not configured
     * @throws RuntimeException      on any API error
     */
    public String createSetupLink(String connectionName, List<String> domainAliases, String orgId) {
        if (mgmtClientId == null || mgmtClientId.isBlank()
                || mgmtClientSecret == null || mgmtClientSecret.isBlank()) {
            throw new IllegalStateException(
                "Management API credentials are not configured. " +
                "Set AUTH0_MGMT_CLIENT_ID and AUTH0_MGMT_CLIENT_SECRET.");
        }

        String accessToken = fetchM2MToken();
        String profileId = getOrCreateProfile(accessToken, connectionName);
        return createTicket(accessToken, profileId, connectionName, domainAliases, orgId);
    }

    // -------------------------------------------------------------------------
    // Step 1 — M2M token via client credentials grant (RestClient)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String fetchM2MToken() {
        RestClient restClient = RestClient.create();
        String audience = "https://" + domain + "/api/v2/";

        Map<String, String> body = Map.of(
            "grant_type", "client_credentials",
            "client_id", mgmtClientId,
            "client_secret", mgmtClientSecret,
            "audience", audience
        );

        Map<String, Object> response = restClient.post()
            .uri("https://" + domain + "/oauth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("No access_token in client credentials response");
        }
        return (String) response.get("access_token");
    }

    // -------------------------------------------------------------------------
    // Step 2 — Get or create a self-service profile
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String getOrCreateProfile(String accessToken, String connectionName) {
        RestClient client = buildRestClient(accessToken);
        String profilesUrl = "https://" + domain + "/api/v2/self-service-profiles";

        try {
            // Try to find an existing profile
            List<Map<String, Object>> profiles = client.get()
                .uri(profilesUrl)
                .retrieve()
                .body(List.class);

            if (profiles != null && !profiles.isEmpty()) {
                String id = (String) profiles.get(0).get("id");
                log.debug("Using existing self-service profile: {}", id);
                return id;
            }
        } catch (HttpClientErrorException e) {
            log.warn("GET self-service-profiles returned {}: {}", e.getStatusCode(), e.getMessage());
        }

        // No profile found — create one
        Map<String, Object> body = Map.of(
            "name", "SSO Setup Profile",
            "user_attributes", List.of()
        );

        Map<String, Object> created = client.post()
            .uri(profilesUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);

        String id = (String) created.get("id");
        log.debug("Created new self-service profile: {}", id);
        return id;
    }

    // -------------------------------------------------------------------------
    // Step 3 — Create the one-time SSO ticket
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String createTicket(String accessToken, String profileId,
                                 String connectionName, List<String> domainAliases,
                                 String orgId) {
        RestClient client = buildRestClient(accessToken);
        String ticketUrl = "https://" + domain
            + "/api/v2/self-service-profiles/" + profileId + "/sso-ticket";

        Map<String, Object> connectionConfig = Map.of(
            "name", connectionName,
            "options", Map.of("domain_aliases", domainAliases)
        );

        Map<String, Object> orgEntry = Map.of(
            "organization_id", orgId,
            "assign_membership_on_login", true
        );

        Map<String, Object> body = Map.of(
            "connection_config", connectionConfig,
            "enabled_organizations", List.of(orgEntry)
        );

        Map<String, Object> result = client.post()
            .uri(ticketUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);

        String ticket = (String) result.get("ticket");
        log.debug("Created SSEC ticket for org {}: {}", orgId, ticket);
        return ticket;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RestClient buildRestClient(String accessToken) {
        return RestClient.builder()
            .defaultHeader("Authorization", "Bearer " + accessToken)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
