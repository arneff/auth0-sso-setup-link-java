package com.auth0.example.controller;

import com.auth0.example.service.SsecService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;
import java.util.List;

@Controller
public class SsoSetupController {

    private final SsecService ssecService;

    public SsoSetupController(SsecService ssecService) {
        this.ssecService = ssecService;
    }

    @GetMapping("/sso-setup")
    public String showForm(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("orgId", user.getClaims().get("org_id"));
        model.addAttribute("form", new SsoSetupForm());
        return "sso-setup";
    }

    @PostMapping("/sso-setup/generate")
    public String generate(@AuthenticationPrincipal OidcUser user,
                           @ModelAttribute SsoSetupForm form,
                           Model model) {
        String orgId = (String) user.getClaims().get("org_id");
        try {
            List<String> aliases = parseDomainAliases(form.getDomainAliases());
            String ticket = ssecService.createSetupLink(form.getConnectionName(), aliases, orgId);
            model.addAttribute("ticket", ticket);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("orgId", orgId);
        model.addAttribute("form", form);
        return "sso-setup";
    }

    /**
     * Parses a comma-separated string of domain aliases into a list.
     * Blank entries are ignored.
     */
    private List<String> parseDomainAliases(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    /**
     * Form backing object for the SSO setup page.
     */
    public static class SsoSetupForm {

        private String connectionName = "";
        private String domainAliases = "";

        public String getConnectionName() {
            return connectionName;
        }

        public void setConnectionName(String connectionName) {
            this.connectionName = connectionName;
        }

        public String getDomainAliases() {
            return domainAliases;
        }

        public void setDomainAliases(String domainAliases) {
            this.domainAliases = domainAliases;
        }
    }
}
