package com.auth0.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Value("${auth0.domain}")
    private String domain;

    @Value("${auth0.client-id}")
    private String clientId;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/error").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(ae -> ae
                    .authorizationRequestResolver(orgAwareRequestResolver())))
            .logout(logout -> logout
                .addLogoutHandler(logoutHandler())
                .logoutSuccessUrl("/"));
        return http.build();
    }

    /**
     * Custom resolver that reads an optional {@code organization} query parameter
     * from the login URL and forwards it to Auth0's authorization endpoint.
     *
     * <p>Usage: /oauth2/authorization/okta?organization=org_xxx
     *
     * <p>When {@code organization} is present, Auth0 will bypass the username/password
     * prompt and route the user directly through the org's configured enterprise
     * connection (e.g. SAML/OIDC SSO).
     */
    private OAuth2AuthorizationRequestResolver orgAwareRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver delegate =
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(delegate.resolve(request), request);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return customize(delegate.resolve(request, clientRegistrationId), request);
            }

            private OAuth2AuthorizationRequest customize(
                    OAuth2AuthorizationRequest req, HttpServletRequest servletReq) {
                if (req == null) return null;
                String org = servletReq.getParameter("organization");
                if (org == null || org.isBlank()) return req;
                return OAuth2AuthorizationRequest.from(req)
                    .additionalParameters(Map.of("organization", org))
                    .build();
            }
        };
    }

    /**
     * Redirects to Auth0's /v2/logout endpoint to end the Auth0 session,
     * then returns the user to the app's home page.
     */
    private LogoutHandler logoutHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();

            String baseUrl;
            if (("http".equals(scheme) && serverPort == 80)
                    || ("https".equals(scheme) && serverPort == 443)) {
                baseUrl = scheme + "://" + serverName;
            } else {
                baseUrl = scheme + "://" + serverName + ":" + serverPort;
            }

            String logoutUrl = UriComponentsBuilder
                .fromUriString("https://" + domain + "/v2/logout")
                .queryParam("client_id", clientId)
                .queryParam("returnTo", baseUrl)
                .build()
                .toUriString();

            try {
                response.sendRedirect(logoutUrl);
            } catch (IOException e) {
                throw new RuntimeException("Logout redirect failed", e);
            }
        };
    }
}
