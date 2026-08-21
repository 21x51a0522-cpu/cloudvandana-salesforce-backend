package com.cloudvandana.salesforcecrud.service;

import com.cloudvandana.salesforcecrud.dto.SalesforceTokenResponse;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.http.client.JdkClientHttpRequestFactory;

import org.springframework.stereotype.Service;

import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SalesforceStandardObjectService {

    private final RestTemplate restTemplate;
    private final SalesforceTokenStore tokenStore;

    @Value("${salesforce.api-version}")
    private String apiVersion;

    private final Map<String, String> objectNames =
            new LinkedHashMap<>();

    private final Map<String, String> objectFields =
            new LinkedHashMap<>();

    public SalesforceStandardObjectService(
            SalesforceTokenStore tokenStore) {

        this.tokenStore = tokenStore;

        HttpClient httpClient =
                HttpClient.newBuilder()
                        .build();

        this.restTemplate =
                new RestTemplate(
                        new JdkClientHttpRequestFactory(
                                httpClient
                        )
                );

        // =====================================================
        // SALESFORCE STANDARD OBJECTS
        // Accept BOTH singular and plural API names
        // =====================================================

        objectNames.put("account", "Account");
        objectNames.put("accounts", "Account");

        objectNames.put("opportunity", "Opportunity");
        objectNames.put("opportunities", "Opportunity");

        objectNames.put("lead", "Lead");
        objectNames.put("leads", "Lead");

        objectNames.put("case", "Case");
        objectNames.put("cases", "Case");

        // =====================================================
        // SALESFORCE FIELDS
        // =====================================================

        objectFields.put(
                "account",
                "Id,Name,Phone,Website,Industry,Type,BillingCity"
        );

        objectFields.put(
                "accounts",
                "Id,Name,Phone,Website,Industry,Type,BillingCity"
        );

        objectFields.put(
                "opportunity",
                "Id,Name,StageName,CloseDate,Amount,Probability,Type"
        );

        objectFields.put(
                "opportunities",
                "Id,Name,StageName,CloseDate,Amount,Probability,Type"
        );

        objectFields.put(
                "lead",
                "Id,FirstName,LastName,Company,Email,Phone,Status,LeadSource"
        );

        objectFields.put(
                "leads",
                "Id,FirstName,LastName,Company,Email,Phone,Status,LeadSource"
        );

        objectFields.put(
                "case",
                "Id,CaseNumber,Subject,Status,Priority,Origin,Type,Description"
        );

        objectFields.put(
                "cases",
                "Id,CaseNumber,Subject,Status,Priority,Origin,Type,Description"
        );
    }

    // =========================================================
    // GET ALL RECORDS
    // =========================================================

    public Object getRecords(
            String object,
            int limit,
            int offset,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        String normalizedObject =
                normalizeObject(object);

        String salesforceObject =
                getSalesforceObjectName(normalizedObject);

        String fields =
                objectFields.get(normalizedObject);

        if (fields == null) {
            throw new IllegalArgumentException(
                    "Unsupported Salesforce object: " + object
            );
        }

        int safeLimit =
                Math.min(
                        Math.max(limit, 1),
                        20
                );

        int safeOffset =
                Math.min(
                        Math.max(offset, 0),
                        2000
                );

        String soql =
                "SELECT "
                        + fields
                        + " FROM "
                        + salesforceObject
                        + " LIMIT "
                        + safeLimit
                        + " OFFSET "
                        + safeOffset;

        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                token.getInstanceUrl()
                        )
                        .path("/services/data/")
                        .path(apiVersion)
                        .path("/query")
                        .queryParam("q", soql)
                        .build()
                        .encode()
                        .toUri();

        try {

            ResponseEntity<Object> response =
                    restTemplate.exchange(
                            uri,
                            HttpMethod.GET,
                            new HttpEntity<>(
                                    createHeaders(token)
                            ),
                            Object.class
                    );

            return response.getBody();

        } catch (RestClientResponseException e) {

            throw new RuntimeException(
                    "Salesforce "
                            + salesforceObject
                            + " query failed: "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    // =========================================================
    // GET RECORD BY ID
    // =========================================================

    public Object getById(
            String object,
            String id,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        String salesforceObject =
                getSalesforceObjectName(object);

        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                token.getInstanceUrl()
                        )
                        .path("/services/data/")
                        .path(apiVersion)
                        .path("/sobjects/")
                        .path(salesforceObject)
                        .path("/")
                        .path(id)
                        .build()
                        .encode()
                        .toUri();

        try {

            ResponseEntity<Object> response =
                    restTemplate.exchange(
                            uri,
                            HttpMethod.GET,
                            new HttpEntity<>(
                                    createHeaders(token)
                            ),
                            Object.class
                    );

            return response.getBody();

        } catch (RestClientResponseException e) {

            throw new RuntimeException(
                    "Salesforce "
                            + salesforceObject
                            + " lookup failed: "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    // =========================================================
    // CREATE
    // =========================================================

    public Object create(
            String object,
            Map<String, Object> request,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        String salesforceObject =
                getSalesforceObjectName(object);

        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                token.getInstanceUrl()
                        )
                        .path("/services/data/")
                        .path(apiVersion)
                        .path("/sobjects/")
                        .path(salesforceObject)
                        .build()
                        .encode()
                        .toUri();

        HttpHeaders headers =
                createHeaders(token);

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        try {

            ResponseEntity<Object> response =
                    restTemplate.exchange(
                            uri,
                            HttpMethod.POST,
                            new HttpEntity<>(
                                    cleanPayload(request),
                                    headers
                            ),
                            Object.class
                    );

            return response.getBody();

        } catch (RestClientResponseException e) {

            throw new RuntimeException(
                    "Salesforce "
                            + salesforceObject
                            + " create failed: "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public String update(
            String object,
            String id,
            Map<String, Object> request,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        String salesforceObject =
                getSalesforceObjectName(object);

        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                token.getInstanceUrl()
                        )
                        .path("/services/data/")
                        .path(apiVersion)
                        .path("/sobjects/")
                        .path(salesforceObject)
                        .path("/")
                        .path(id)
                        .build()
                        .encode()
                        .toUri();

        HttpHeaders headers =
                createHeaders(token);

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        try {

            restTemplate.exchange(
                    uri,
                    HttpMethod.PATCH,
                    new HttpEntity<>(
                            cleanPayload(request),
                            headers
                    ),
                    Void.class
            );

            return "Record updated successfully";

        } catch (RestClientResponseException e) {

            throw new RuntimeException(
                    "Salesforce "
                            + salesforceObject
                            + " update failed: "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    public String delete(
            String object,
            String id,
            HttpSession session) {

        SalesforceTokenResponse token =
                getToken(session);

        String salesforceObject =
                getSalesforceObjectName(object);

        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                token.getInstanceUrl()
                        )
                        .path("/services/data/")
                        .path(apiVersion)
                        .path("/sobjects/")
                        .path(salesforceObject)
                        .path("/")
                        .path(id)
                        .build()
                        .encode()
                        .toUri();

        try {

            restTemplate.exchange(
                    uri,
                    HttpMethod.DELETE,
                    new HttpEntity<>(
                            createHeaders(token)
                    ),
                    Void.class
            );

            return "Record deleted successfully";

        } catch (RestClientResponseException e) {

            throw new RuntimeException(
                    "Salesforce "
                            + salesforceObject
                            + " delete failed: "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    // =========================================================
    // NORMALIZE OBJECT
    // =========================================================

    private String normalizeObject(String object) {

        if (object == null) {
            return "";
        }

        return object
                .trim()
                .toLowerCase();
    }

    // =========================================================
    // GET SALESFORCE OBJECT NAME
    // =========================================================

    private String getSalesforceObjectName(
            String object) {

        String normalized =
                normalizeObject(object);

        String salesforceObject =
                objectNames.get(normalized);

        if (salesforceObject == null) {

            throw new IllegalArgumentException(
                    "Unsupported Salesforce object: "
                            + object
            );
        }

        return salesforceObject;
    }

    // =========================================================
    // GET TOKEN
    // =========================================================

    private SalesforceTokenResponse getToken(
            HttpSession session) {

        SalesforceTokenResponse token =
                tokenStore.getToken();

        if (token != null
                && token.getAccessToken() != null
                && !token.getAccessToken().isBlank()) {

            return token;
        }

        if (session != null) {

            Object sessionToken =
                    session.getAttribute(
                            "salesforceToken"
                    );

            if (sessionToken
                    instanceof SalesforceTokenResponse) {

                token =
                        (SalesforceTokenResponse)
                                sessionToken;

                tokenStore.saveToken(token);

                return token;
            }
        }

        throw new RuntimeException(
                "Salesforce session not found. Please login again."
        );
    }

    // =========================================================
    // HEADERS
    // =========================================================

    private HttpHeaders createHeaders(
            SalesforceTokenResponse token) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                token.getAccessToken()
        );

        headers.setAccept(
                Collections.singletonList(
                        MediaType.APPLICATION_JSON
                )
        );

        return headers;
    }

    // =========================================================
    // CLEAN PAYLOAD
    // =========================================================

    private Map<String, Object> cleanPayload(
            Map<String, Object> request) {

        Map<String, Object> cleanPayload =
                new LinkedHashMap<>();

        if (request == null) {
            return cleanPayload;
        }

        request.forEach(
                (key, value) -> {

                    if (key != null
                            && !key.isBlank()
                            && value != null) {

                        cleanPayload.put(
                                key,
                                value
                        );
                    }
                }
        );

        return cleanPayload;
    }
}