package com.cloudvandana.salesforcecrud.service;

import com.cloudvandana.salesforcecrud.dto.SalesforceTokenResponse;
import org.springframework.stereotype.Component;

@Component
public class SalesforceTokenStore {

    private SalesforceTokenResponse token;

    public synchronized void saveToken(SalesforceTokenResponse token) {
        this.token = token;
    }

    public synchronized SalesforceTokenResponse getToken() {
        return token;
    }

    public synchronized boolean isAuthenticated() {
        return token != null
                && token.getAccessToken() != null
                && !token.getAccessToken().isBlank();
    }

    public synchronized String getAccessToken() {
        if (!isAuthenticated()) {
            throw new RuntimeException(
                    "Salesforce authentication required. Please login first."
            );
        }

        return token.getAccessToken();
    }

    public synchronized String getInstanceUrl() {
        if (!isAuthenticated()) {
            throw new RuntimeException(
                    "Salesforce authentication required. Please login first."
            );
        }

        return token.getInstanceUrl();
    }

    public synchronized void clearToken() {
        token = null;
    }
}