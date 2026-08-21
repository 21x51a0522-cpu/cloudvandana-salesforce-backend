package com.cloudvandana.salesforcecrud.service;

import com.cloudvandana.salesforcecrud.dto.SalesforceTokenResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
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
        this.restTemplate = new RestTemplate();

        objectNames.put("accounts", "Account");
        objectNames.put("opportunities", "Opportunity");
        objectNames.put("leads", "Lead");
        objectNames.put("cases", "Case");

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

    public Object getRecords(
            String object,
            int limit,
            int offset,
            HttpSession session) {

        SalesforceTokenResponse token = getToken(session);
        String salesforceObject = getSalesforceObjectName(object);
        String fields = objectFields.get(object.toLowerCase());

        int safeLimit = Math.min(Math.max(limit, 1), 20);
        int safeOffset = Math.min(Math.max(offset, 0), 2000);

        String soql = "SELECT " + fields
                + " FROM " + salesforceObject
                + " LIMIT " + safeLimit
                + " OFFSET " + safeOffset;

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
                .path("/services/data/")
                .path(apiVersion)
                .path("/query")
                .queryParam("q", soql)
                .build()
                .encode()
                .toUri();

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(token)),
                    Object.class
            );

            return response.getBody();
        } catch (RestClientResponseException e) {
            throw new RuntimeException(
                    "Salesforce " + salesforceObject
                            + " query failed: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    public Object getById(
            String object,
            String id,
            HttpSession session) {

        SalesforceTokenResponse token = getToken(session);
        String salesforceObject = getSalesforceObjectName(object);

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
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
            ResponseEntity<Object> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(token)),
                    Object.class
            );

            return response.getBody();
        } catch (RestClientResponseException e) {
            throw new RuntimeException(
                    "Salesforce " + salesforceObject
                            + " lookup failed: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    public Object create(
            String object,
            Map<String, Object> request,
            HttpSession session) {

        SalesforceTokenResponse token = getToken(session);
        String salesforceObject = getSalesforceObjectName(object);

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
                .path("/services/data/")
                .path(apiVersion)
                .path("/sobjects/")
                .path(salesforceObject)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = createHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(cleanPayload(request), headers),
                    Object.class
            );

            return response.getBody();
        } catch (RestClientResponseException e) {
            throw new RuntimeException(
                    "Salesforce " + salesforceObject
                            + " create failed: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    public String update(
            String object,
            String id,
            Map<String, Object> request,
            HttpSession session) {

        SalesforceTokenResponse token = getToken(session);
        String salesforceObject = getSalesforceObjectName(object);

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
                .path("/services/data/")
                .path(apiVersion)
                .path("/sobjects/")
                .path(salesforceObject)
                .path("/")
                .path(id)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = createHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(
                    uri,
                    HttpMethod.PATCH,
                    new HttpEntity<>(cleanPayload(request), headers),
                    Void.class
            );

            return "Record updated successfully";
        } catch (RestClientResponseException e) {
            throw new RuntimeException(
                    "Salesforce " + salesforceObject
                            + " update failed: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    public String delete(
            String object,
            String id,
            HttpSession session) {

        SalesforceTokenResponse token = getToken(session);
        String salesforceObject = getSalesforceObjectName(object);

        URI uri = UriComponentsBuilder
                .fromUriString(token.getInstanceUrl())
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
                    new HttpEntity<>(createHeaders(token)),
                    Void.class
            );

            return "Record deleted successfully";
        } catch (RestClientResponseException e) {
            throw new RuntimeException(
                    "Salesforce " + salesforceObject
                            + " delete failed: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private String getSalesforceObjectName(String object) {

        String salesforceObject = objectNames.get(
                object == null ? "" : object.toLowerCase()
        );

        if (salesforceObject == null) {
            throw new IllegalArgumentException(
                    "Unsupported Salesforce object: " + object
            );
        }

        return salesforceObject;
    }

    private SalesforceTokenResponse getToken(HttpSession session) {

        SalesforceTokenResponse token = tokenStore.getToken();

        if (token != null
                && token.getAccessToken() != null
                && !token.getAccessToken().isBlank()) {
            return token;
        }

        if (session != null) {
            Object sessionToken = session.getAttribute("salesforceToken");

            if (sessionToken instanceof SalesforceTokenResponse) {
                token = (SalesforceTokenResponse) sessionToken;
                tokenStore.saveToken(token);
                return token;
            }
        }

        throw new RuntimeException(
                "Salesforce session not found. Please login again."
        );
    }

    private HttpHeaders createHeaders(
            SalesforceTokenResponse token) {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(token.getAccessToken());
        headers.setAccept(
                Collections.singletonList(MediaType.APPLICATION_JSON)
        );

        return headers;
    }

    private Map<String, Object> cleanPayload(
            Map<String, Object> request) {

        Map<String, Object> cleanPayload = new LinkedHashMap<>();

        if (request == null) {
            return cleanPayload;
        }

        request.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                cleanPayload.put(key, value);
            }
        });

        return cleanPayload;
    }
}
