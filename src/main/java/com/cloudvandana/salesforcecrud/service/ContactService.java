package com.cloudvandana.salesforcecrud.service;

import com.cloudvandana.salesforcecrud.dto.ContactRequest;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.http.client.JdkClientHttpRequestFactory;

import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContactService {

    private final RestTemplate restTemplate;
    private final SalesforceTokenStore tokenStore;

    @Value("${salesforce.api-version}")
    private String apiVersion;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ContactService(
            SalesforceTokenStore tokenStore) {

        this.tokenStore = tokenStore;

        /*
         * Java's default HttpURLConnection does not support PATCH.
         * JdkClientHttpRequestFactory uses Java HttpClient,
         * which supports PATCH.
         */
        HttpClient httpClient =
                HttpClient.newBuilder()
                        .build();

        this.restTemplate =
                new RestTemplate(
                        new JdkClientHttpRequestFactory(
                                httpClient
                        )
                );
    }

    // =========================================================
    // COMMON HEADERS
    // =========================================================

    private HttpHeaders createHeaders() {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                tokenStore.getAccessToken()
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setAccept(
                Collections.singletonList(
                        MediaType.APPLICATION_JSON
                )
        );

        return headers;
    }

    // =========================================================
    // GET ALL CONTACTS
    // =========================================================

    public Object getAllContacts() {

        String url =
                tokenStore.getInstanceUrl()
                + "/services/data/"
                + apiVersion
                + "/query/?q=SELECT+Id,FirstName,LastName,Email,Phone+FROM+Contact";

        HttpHeaders headers =
                createHeaders();

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<Object> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        Object.class
                );

        return response.getBody();
    }

    // =========================================================
    // GET CONTACT BY ID
    // =========================================================

    public Object getContactById(String id) {

        String url =
                tokenStore.getInstanceUrl()
                + "/services/data/"
                + apiVersion
                + "/sobjects/Contact/"
                + id;

        HttpHeaders headers =
                createHeaders();

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<Object> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        Object.class
                );

        return response.getBody();
    }

    // =========================================================
    // CREATE CONTACT
    // =========================================================

    public Object createContact(
            ContactRequest request) {

        String url =
                tokenStore.getInstanceUrl()
                + "/services/data/"
                + apiVersion
                + "/sobjects/Contact";

        Map<String, Object> contact =
                new HashMap<>();

        contact.put(
                "FirstName",
                request.getFirstName()
        );

        contact.put(
                "LastName",
                request.getLastName()
        );

        contact.put(
                "Email",
                request.getEmail()
        );

        contact.put(
                "Phone",
                request.getPhone()
        );

        HttpHeaders headers =
                createHeaders();

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        contact,
                        headers
                );

        ResponseEntity<Object> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        Object.class
                );

        return response.getBody();
    }

    // =========================================================
    // UPDATE CONTACT
    // =========================================================

    public String updateContact(
            String id,
            ContactRequest request) {

        String url =
                tokenStore.getInstanceUrl()
                + "/services/data/"
                + apiVersion
                + "/sobjects/Contact/"
                + id;

        Map<String, Object> contact =
                new HashMap<>();

        contact.put(
                "FirstName",
                request.getFirstName()
        );

        contact.put(
                "LastName",
                request.getLastName()
        );

        contact.put(
                "Email",
                request.getEmail()
        );

        contact.put(
                "Phone",
                request.getPhone()
        );

        HttpHeaders headers =
                createHeaders();

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        contact,
                        headers
                );

        ResponseEntity<Void> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.PATCH,
                        entity,
                        Void.class
                );

        if (response.getStatusCode().is2xxSuccessful()) {
            return "Contact updated successfully";
        }

        return "Failed to update contact";
    }

    // =========================================================
    // DELETE CONTACT
    // =========================================================

    public String deleteContact(String id) {

        String url =
                tokenStore.getInstanceUrl()
                + "/services/data/"
                + apiVersion
                + "/sobjects/Contact/"
                + id;

        HttpHeaders headers =
                createHeaders();

        HttpEntity<Void> entity =
                new HttpEntity<>(headers);

        ResponseEntity<Void> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.DELETE,
                        entity,
                        Void.class
                );

        if (response.getStatusCode().is2xxSuccessful()) {
            return "Contact deleted successfully";
        }

        return "Failed to delete contact";
    }
}