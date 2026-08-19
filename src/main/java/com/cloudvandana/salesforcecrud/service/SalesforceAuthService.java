package com.cloudvandana.salesforcecrud.service;

import com.cloudvandana.salesforcecrud.config.SalesforceConfig;
import com.cloudvandana.salesforcecrud.dto.SalesforceTokenResponse;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.security.SecureRandom;

import java.util.Base64;

@Service
public class SalesforceAuthService {

    private final SalesforceConfig salesforceConfig;
    private final SalesforceTokenStore tokenStore;
    private final RestTemplate restTemplate;

    public SalesforceAuthService(
            SalesforceConfig salesforceConfig,
            SalesforceTokenStore tokenStore) {

        this.salesforceConfig = salesforceConfig;
        this.tokenStore = tokenStore;
        this.restTemplate = new RestTemplate();
    }

    // ============================================================
    // GET AUTHORIZATION URL - PKCE
    // ============================================================

    public String getAuthorizationUrl(HttpSession session) {

        // Generate PKCE code verifier
        String codeVerifier = generateCodeVerifier();

        // Generate PKCE code challenge
        String codeChallenge =
                generateCodeChallenge(codeVerifier);

        // Store verifier in HTTP session.
        // We need the SAME verifier when exchanging the code.
        session.setAttribute(
                "pkceCodeVerifier",
                codeVerifier
        );

        String url =
                "https://login.salesforce.com/services/oauth2/authorize"
                + "?response_type=code"
                + "&client_id="
                + salesforceConfig.getClientId()
                + "&redirect_uri="
                + encodeUrl(salesforceConfig.getRedirectUri())
                + "&code_challenge="
                + encodeUrl(codeChallenge)
                + "&code_challenge_method=S256";

        return url;
    }

    // ============================================================
    // GET LOGIN URL
    // ============================================================

    public String getLoginUrl() {
        return getAuthorizationUrl(null);
    }

    // ============================================================
    // EXCHANGE CODE FOR TOKEN
    // ============================================================

    public SalesforceTokenResponse exchangeCodeForToken(
            String code,
            HttpSession session) {

        String codeVerifier =
                (String) session.getAttribute(
                        "pkceCodeVerifier"
                );

        if (codeVerifier == null ||
                codeVerifier.isBlank()) {

            throw new RuntimeException(
                    "PKCE code verifier not found. Please login again."
            );
        }

        String tokenUrl =
                "https://login.salesforce.com/services/oauth2/token";

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add(
                "grant_type",
                "authorization_code"
        );

        body.add(
                "code",
                code
        );

        body.add(
                "client_id",
                salesforceConfig.getClientId()
        );

        body.add(
                "client_secret",
                salesforceConfig.getClientSecret()
        );

        body.add(
                "redirect_uri",
                salesforceConfig.getRedirectUri()
        );

        // IMPORTANT:
        // Send the same verifier used to create
        // the code challenge.
        body.add(
                "code_verifier",
                codeVerifier
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<SalesforceTokenResponse> response =
                restTemplate.exchange(
                        tokenUrl,
                        HttpMethod.POST,
                        request,
                        SalesforceTokenResponse.class
                );

        SalesforceTokenResponse token =
                response.getBody();

        if (token == null ||
                token.getAccessToken() == null ||
                token.getAccessToken().isBlank()) {

            throw new RuntimeException(
                    "Salesforce login failed. Access token not received."
            );
        }

        // Save token globally
        tokenStore.saveToken(token);

        // Save token in HTTP session
        session.setAttribute(
                "salesforceToken",
                token
        );

        // PKCE verifier is no longer required
        session.removeAttribute(
                "pkceCodeVerifier"
        );

        return token;
    }

    // ============================================================
    // GENERATE CODE VERIFIER
    // ============================================================

    private String generateCodeVerifier() {

        SecureRandom secureRandom =
                new SecureRandom();

        byte[] bytes =
                new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // ============================================================
    // GENERATE CODE CHALLENGE
    // ============================================================

    private String generateCodeChallenge(
            String codeVerifier) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            codeVerifier.getBytes(
                                    StandardCharsets.US_ASCII
                            )
                    );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException(
                    "Unable to generate PKCE code challenge.",
                    e
            );
        }
    }

    // ============================================================
    // URL ENCODING
    // ============================================================

    private String encodeUrl(String value) {

        return java.net.URLEncoder
                .encode(
                        value,
                        StandardCharsets.UTF_8
                )
                .replace("+", "%20");
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    public void logout(HttpSession session) {

        tokenStore.clearToken();

        if (session != null) {
            session.invalidate();
        }
    }
}