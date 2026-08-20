package com.cloudvandana.salesforcecrud.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SalesforceStandardObjectService {

    private final RestTemplate restTemplate;
    private final SalesforceTokenStore tokenStore;

    @Value("${salesforce.api-version}")
    private String apiVersion;

    private final Map<String, String> objectNames = new LinkedHashMap<>();
    private final Map<String, String> objectFields = new LinkedHashMap<>();

    public SalesforceStandardObjectService(
            SalesforceTokenStore tokenStore) {

        this.tokenStore = tokenStore;

        HttpClient httpClient =
                HttpClient.newBuilder()
                        .build();

        this.restTemplate =
                new RestTemplate(
                        new JdkClientHttpRequestFactory(httpClient)
                );

        // =====================================================
        // SALESFORCE OBJECT NAMES
        // =====================================================

        objectNames.put("accounts", "Account");
        objectNames.put("opportunities", "Opportunity");
        objectNames.put("leads", "Lead");
        objectNames.put("cases", "Case");

        // =====================================================
        // FIELDS FOR LISTING
        // 5-10 FIELDS PER OBJECT
        // =====================================================

        objectFields.put(
                "accounts",
                "Id,Name,Phone,Website,Industry,Type,BillingCity"
        );

        objectFields.put(
                "opportunities",
                "Id,Name,StageName,CloseDate,Amount,Probability,Type"
        );

        objectFields.put(
                "leads",
                "Id,FirstName,LastName,Company,Email,Phone,Status,LeadSource"
        );

        objectFields.put(
                "cases",
                "Id,CaseNumber,Subject,Status,Priority,Origin,Type,Description"
        );
    }

    // =========================================================
    // VALIDATE OBJECT
    // =========================================================

    private String getSalesforceObjectName(String object) {

        String salesforceObject =
                objectNames.get(
                        object.toLowerCase()
                );

        if (salesforceObject == null) {

            throw new IllegalArgumentException(
                    "Unsupported Salesforce object: " + object
                            + ". Supported objects: accounts, opportunities, leads, cases."
            );
        }

        return salesforceObject;
    }

    // =========================================================
    // HEADERS
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
    // GET RECORDS
    // 20 RECORDS AT A TIME
    // =========================================================

    public Object getRecords(
            String object,
            int limit,
            int offset) {

        String salesforceObject =
                getSalesforceObjectName(object);

        String fields =
                objectFields.get(
                        object.toLowerCase()
                );

        // Keep assignment requirement: max 20 records
        int safeLimit =
                Math.min(
                        Math.max(limit, 1),
                        20
                );

        int safeOffset =
                Math.max(offset, 0);

        String soql =
                "SELECT "
                        + fields
                        + " FROM "
                        + salesforceObject
                        + " LIMIT "
                        + safeLimit
                        + " OFFSET "
                        + safeOffset;

        String url =
                UriComponentsBuilder
                        .fromUriString(
                                tokenStore.getInstanceUrl()
                                        + "/services/data/"
                                        + apiVersion
                                        + "/query"
                        )
                        .queryParam("q", soql)
                        .build()
                        .encode()
                        .toUriString();

        HttpEntity<Void> entity =
                new HttpEntity<>(
                        createHeaders()
                );

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
    // GET BY ID
    // =========================================================

    public Object getById(
            String object,
            String id) {

        String salesforceObject =
                getSalesforceObjectName(object);

        String url =
                tokenStore.getInstanceUrl()
                        + "/services/data/"
                        + apiVersion
                        + "/sobjects/"
                        + salesforceObject
                        + "/"
                        + id;

        HttpEntity<Void> entity =
                new HttpEntity<>(
                        createHeaders()
                );

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
    // CREATE
    // =========================================================

    public Object create(
            String object,
            Map<String, Object> request) {

        String salesforceObject =
                getSalesforceObjectName(object);

        Map<String, Object> payload =
                cleanPayload(request);

        String url =
                tokenStore.getInstanceUrl()
                        + "/services/data/"
                        + apiVersion
                        + "/sobjects/"
                        + salesforceObject;

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        payload,
                        createHeaders()
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
    // UPDATE
    // =========================================================

    public String update(
            String object,
            String id,
            Map<String, Object> request) {

        String salesforceObject =
                getSalesforceObjectName(object);

        Map<String, Object> payload =
                cleanPayload(request);

        String url =
                tokenStore.getInstanceUrl()
                        + "/services/data/"
                        + apiVersion
                        + "/sobjects/"
                        + salesforceObject
                        + "/"
                        + id;

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        payload,
                        createHeaders()
                );

        ResponseEntity<Void> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.PATCH,
                        entity,
                        Void.class
                );

        if (response.getStatusCode().is2xxSuccessful()) {
            return "Record updated successfully";
        }

        return "Failed to update record";
    }

    // =========================================================
    // DELETE
    // =========================================================

    public String delete(
            String object,
            String id) {

        String salesforceObject =
                getSalesforceObjectName(object);

        String url =
                tokenStore.getInstanceUrl()
                        + "/services/data/"
                        + apiVersion
                        + "/sobjects/"
                        + salesforceObject
                        + "/"
                        + id;

        HttpEntity<Void> entity =
                new HttpEntity<>(
                        createHeaders()
                );

        ResponseEntity<Void> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.DELETE,
                        entity,
                        Void.class
                );

        if (response.getStatusCode().is2xxSuccessful()) {
            return "Record deleted successfully";
        }

        return "Failed to delete record";
    }

    // =========================================================
    // REMOVE NULL VALUES
    // =========================================================

    private Map<String, Object> cleanPayload(
            Map<String, Object> request) {

        Map<String, Object> cleanPayload =
                new HashMap<>();

        if (request == null) {
            return cleanPayload;
        }

        request.forEach(
                (key, value) -> {

                    if (key != null &&
                            value != null &&
                            !key.isBlank()) {

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