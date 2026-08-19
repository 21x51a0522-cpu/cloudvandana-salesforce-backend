package com.cloudvandana.salesforcecrud.service;

import com.cloudvandana.salesforcecrud.config.SalesforceConfig;
import com.cloudvandana.salesforcecrud.dto.ContactRequest;
import com.cloudvandana.salesforcecrud.dto.SalesforceTokenResponse;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class SalesforceContactService {

    private final SalesforceConfig salesforceConfig;

    private final RestTemplate restTemplate;

    private final SalesforceTokenStore tokenStore;

    public SalesforceContactService(
            SalesforceConfig salesforceConfig,
            SalesforceTokenStore tokenStore) {

        this.salesforceConfig = salesforceConfig;
        this.restTemplate = new RestTemplate();
        this.tokenStore = tokenStore;
    }

    // ============================================================
    // GET ALL CONTACTS
    // ============================================================

    public Object getAllContacts(HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        String query =
                "SELECT Id, FirstName, LastName, Email, Phone FROM Contact";

        /*
         * IMPORTANT:
         * Build the URI as a URI object.
         * This prevents the SOQL query from being double encoded.
         */

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
                .path("/services/data/")
                .path(salesforceConfig.getApiVersion())
                .path("/query")
                .queryParam("q", query)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers =
                createHeaders(token);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<Object> response =
                restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        request,
                        Object.class
                );

        return response.getBody();
    }

    // ============================================================
    // GET CONTACT BY ID
    // ============================================================

    public Object getContactById(
            String id,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
                .path("/services/data/")
                .path(salesforceConfig.getApiVersion())
                .path("/sobjects/Contact/")
                .path(id)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers =
                createHeaders(token);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<Object> response =
                restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        request,
                        Object.class
                );

        return response.getBody();
    }

    // ============================================================
    // CREATE CONTACT
    // ============================================================

    public Object createContact(
            ContactRequest contact,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
                .path("/services/data/")
                .path(salesforceConfig.getApiVersion())
                .path("/sobjects/Contact")
                .build()
                .encode()
                .toUri();

        HttpHeaders headers =
                createHeaders(token);

        headers.set(
                "Content-Type",
                "application/json"
        );

        String body =
                createContactJson(contact);

        HttpEntity<String> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Object> response =
                restTemplate.exchange(
                        uri,
                        HttpMethod.POST,
                        request,
                        Object.class
                );

        return response.getBody();
    }

    // ============================================================
    // UPDATE CONTACT
    // ============================================================

    public String updateContact(
            String id,
            ContactRequest contact,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
                .path("/services/data/")
                .path(salesforceConfig.getApiVersion())
                .path("/sobjects/Contact/")
                .path(id)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers =
                createHeaders(token);

        headers.set(
                "Content-Type",
                "application/json"
        );

        String body =
                createContactJson(contact);

        HttpEntity<String> request =
                new HttpEntity<>(body, headers);

        restTemplate.exchange(
                uri,
                HttpMethod.PATCH,
                request,
                Void.class
        );

        return "Contact updated successfully";
    }

    // ============================================================
    // DELETE CONTACT
    // ============================================================

    public String deleteContact(
            String id,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
                .path("/services/data/")
                .path(salesforceConfig.getApiVersion())
                .path("/sobjects/Contact/")
                .path(id)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers =
                createHeaders(token);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        restTemplate.exchange(
                uri,
                HttpMethod.DELETE,
                request,
                Void.class
        );

        return "Contact deleted successfully";
    }

    // ============================================================
    // GET TOKEN
    // ============================================================

    private SalesforceTokenResponse getToken(
            HttpSession session) {

        // First check TokenStore
        SalesforceTokenResponse token =
                tokenStore.getToken();

        // If TokenStore has token, use it
        if (token != null &&
                token.getAccessToken() != null) {

            return token;
        }

        // Otherwise check HTTP session
        if (session != null) {

            Object sessionToken =
                    session.getAttribute(
                            "salesforceToken"
                    );

            if (sessionToken instanceof SalesforceTokenResponse) {

                token =
                        (SalesforceTokenResponse) sessionToken;

                // Restore TokenStore
                tokenStore.saveToken(token);

                return token;
            }
        }

        throw new RuntimeException(
                "Salesforce session not found. Please login again."
        );
    }

    // ============================================================
    // HEADERS
    // ============================================================

    private HttpHeaders createHeaders(
            SalesforceTokenResponse token) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                token.getAccessToken()
        );

        headers.set(
                "Accept",
                "application/json"
        );

        return headers;
    }

    // ============================================================
    // CONTACT JSON
    // ============================================================

    private String createContactJson(
            ContactRequest contact) {

        String firstName =
                contact.getFirstName() == null
                        ? ""
                        : contact.getFirstName();

        String lastName =
                contact.getLastName() == null
                        ? ""
                        : contact.getLastName();

        String email =
                contact.getEmail() == null
                        ? ""
                        : contact.getEmail();

        String phone =
                contact.getPhone() == null
                        ? ""
                        : contact.getPhone();

        return "{"
                + "\"FirstName\":\""
                + escapeJson(firstName)
                + "\","

                + "\"LastName\":\""
                + escapeJson(lastName)
                + "\","

                + "\"Email\":\""
                + escapeJson(email)
                + "\","

                + "\"Phone\":\""
                + escapeJson(phone)
                + "\""

                + "}";
    }

    // ============================================================
    // JSON ESCAPE
    // ============================================================

    private String escapeJson(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}