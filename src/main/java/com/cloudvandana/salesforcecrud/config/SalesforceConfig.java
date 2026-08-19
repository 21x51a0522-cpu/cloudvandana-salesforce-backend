package com.cloudvandana.salesforcecrud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SalesforceConfig {

    @Value("${salesforce.login-url}")
    private String loginUrl;

    @Value("${salesforce.client-id}")
    private String clientId;

    @Value("${salesforce.client-secret}")
    private String clientSecret;

    @Value("${salesforce.redirect-uri}")
    private String redirectUri;

    @Value("${salesforce.api-version}")
    private String apiVersion;

    public String getLoginUrl() {
        return loginUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getApiVersion() {
        return apiVersion;
    }
}