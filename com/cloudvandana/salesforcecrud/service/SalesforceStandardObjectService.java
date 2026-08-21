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

    private final Map<String, String> objectNames = new LinkedHashMap<>();
    private final Map<String, String> objectFields = new LinkedHashMap<>();

    public SalesforceStandardObjectService(SalesforceTokenStore tokenStore) {
        this.tokenStore = tokenStore;

        HttpClient httpClient = HttpClient.newBuilder().build();
        this.restTemplate = new RestTemplate(new JdkClientHttpRequestFactory(httpClient));

        objectNames.put("account", "Account");
        objectNames.put("accounts", "Account");
        objectNames.put("opportunity", "Opportunity");
        objectNames.put("opportunities", "Opportunity");
        objectNames.put("lead", "Lead");
        objectNames.put("leads", "Lead");
        objectNames.put("case", "Case");
        objectNames.put("cases", "Case");

        objectFields.put("account", "Id,Name,Phone,Website,Industry,Type,BillingCity");
        objectFields.put("accounts", "Id,Name,Phone,Website,Industry,Type,BillingCity");
        objectFields.put("opportunity", "Id,Name,StageName,CloseDate,Amount,Probability,Type");
        objectFields.put("opportunities", "Id,Name,StageName,CloseDate,Amount,Probability,Type");
        objectFields.put("lead", "Id,FirstName,LastName,Company,Email,Phone,Status,LeadSource");
        objectFields.put("leads", "Id,FirstName,LastName,Company,Email,Phone,Status,LeadSource");
        objectFields.put("case", "Id,CaseNumber,Subject,Status,Priority,Origin,Type,Description");
        objectFields.put("cases", "Id,CaseNumber,Subject,Status,Priority,Origin,Type,Description");
    }

    public Object getRecords(String object, int limit, int offset, HttpSession session) {
        SalesforceTokenResponse token = getToken(session);
        String normalizedObject = normalizeObject(object);
        String salesforceObject = getSalesforceObjectName(normalizedObject);
        String fields = objectFields.get(normalizedObject);

        if (fields == null) {
            throw new IllegalArgumentException("Unsupported Salesforce object: " + object);
        }

        int safeLimit = Math.min(Math.max(limit, 1), 20);
        int safeOffset = Math.min(Math.max(offset, 0), 2000);

        String soql = "SELECT " + fields + " FROM " + salesforceObject
                + " LIMIT " + safeLimit + " OFFSET " + safeOffset;

        URI uri = UriComponentsBuilder.fromUriString(token.getInstanceUrl())
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
            return mapQueryResponse(response.getBody(), normalizedObject);
        } catch (RestClientResponseException e) {
            throw salesforceError(salesforceObject, "query", e);
        }
    }

    public Object getById(String object, String id, HttpSession session) {
        SalesforceTokenResponse token = getToken(session);
        String normalizedObject = normalizeObject(object);
        String salesforceObject = getSalesforceObjectName(normalizedObject);

        URI uri = UriComponentsBuilder.fromUriString(token.getInstanceUrl())
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
            return mapRecord(response.getBody(), normalizedObject);
        } catch (RestClientResponseException e) {
            throw salesforceError(salesforceObject, "lookup", e);
        }
    }

    public Object create(String object, Map<String, Object> request, HttpSession session) {
        SalesforceTokenResponse token = getToken(session);
        String normalizedObject = normalizeObject(object);
        String salesforceObject = getSalesforceObjectName(normalizedObject);

        URI uri = UriComponentsBuilder.fromUriString(token.getInstanceUrl())
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
                    new HttpEntity<>(mapPayload(request, normalizedObject, false), headers),
                    Object.class
            );
            return response.getBody();
        } catch (RestClientResponseException e) {
            throw salesforceError(salesforceObject, "create", e);
        }
    }

    public String update(String object, String id, Map<String, Object> request, HttpSession session) {
        SalesforceTokenResponse token = getToken(session);
        String normalizedObject = normalizeObject(object);
        String salesforceObject = getSalesforceObjectName(normalizedObject);

        URI uri = UriComponentsBuilder.fromUriString(token.getInstanceUrl())
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
                    new HttpEntity<>(mapPayload(request, normalizedObject, true), headers),
                    Void.class
            );
            return "Record updated successfully";
        } catch (RestClientResponseException e) {
            throw salesforceError(salesforceObject, "update", e);
        }
    }

    public String delete(String object, String id, HttpSession session) {
        SalesforceTokenResponse token = getToken(session);
        String normalizedObject = normalizeObject(object);
        String salesforceObject = getSalesforceObjectName(normalizedObject);

        URI uri = UriComponentsBuilder.fromUriString(token.getInstanceUrl())
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
            throw salesforceError(salesforceObject, "delete", e);
        }
    }

    /** Convert the frontend camelCase field names to Salesforce REST field names. */
    private Map<String, Object> mapPayload(
            Map<String, Object> request,
            String object,
            boolean update) {

        Map<String, Object> payload = new LinkedHashMap<>();
        if (request == null) return payload;

        request.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) return;

            String salesforceField = salesforceField(object, key);

            // CaseNumber is read-only and generated by Salesforce.
            if ("Case".equals(getSalesforceObjectName(object))
                    && "CaseNumber".equals(salesforceField)) {
                return;
            }

            payload.put(salesforceField, value);
        });

        return payload;
    }

    private String salesforceField(String object, String key) {
        String normalized = normalizeObject(object);

        Map<String, String> fields = new LinkedHashMap<>();

        if (normalized.equals("account") || normalized.equals("accounts")) {
            fields.put("id", "Id");
            fields.put("name", "Name");
            fields.put("phone", "Phone");
            fields.put("website", "Website");
            fields.put("industry", "Industry");
            fields.put("type", "Type");
            fields.put("billingCity", "BillingCity");
        } else if (normalized.equals("opportunity") || normalized.equals("opportunities")) {
            fields.put("id", "Id");
            fields.put("name", "Name");
            fields.put("stageName", "StageName");
            fields.put("closeDate", "CloseDate");
            fields.put("amount", "Amount");
            fields.put("probability", "Probability");
            fields.put("type", "Type");
        } else if (normalized.equals("lead") || normalized.equals("leads")) {
            fields.put("id", "Id");
            fields.put("firstName", "FirstName");
            fields.put("lastName", "LastName");
            fields.put("company", "Company");
            fields.put("email", "Email");
            fields.put("phone", "Phone");
            fields.put("status", "Status");
            fields.put("leadSource", "LeadSource");
        } else if (normalized.equals("case") || normalized.equals("cases")) {
            fields.put("id", "Id");
            fields.put("caseNumber", "CaseNumber");
            fields.put("subject", "Subject");
            fields.put("status", "Status");
            fields.put("priority", "Priority");
            fields.put("origin", "Origin");
            fields.put("type", "Type");
            fields.put("description", "Description");
        }

        return fields.getOrDefault(key, key);
    }

    private Object mapQueryResponse(Object body, String object) {
        if (!(body instanceof Map<?, ?> raw)) return body;

        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> result.put(String.valueOf(key), value));

        Object records = result.get("records");
        if (records instanceof Iterable<?> iterable) {
            java.util.List<Object> mapped = new java.util.ArrayList<>();
            for (Object record : iterable) {
                mapped.add(mapRecord(record, object));
            }
            result.put("records", mapped);
        }
        return result;
    }

    private Object mapRecord(Object body, String object) {
        if (!(body instanceof Map<?, ?> raw)) return body;

        Map<String, Object> source = new LinkedHashMap<>();
        raw.forEach((key, value) -> source.put(String.valueOf(key), value));
        Map<String, Object> result = new LinkedHashMap<>();

        source.forEach((key, value) -> {
            String frontendKey = frontendField(object, key);
            result.put(frontendKey, value);
        });
        return result;
    }

    private String frontendField(String object, String key) {
        if (key.equals("Id")) return "id";
        if (key.equals("Name")) return "name";

        String normalized = normalizeObject(object);
        if (normalized.equals("account") || normalized.equals("accounts")) {
            return switch (key) {
                case "BillingCity" -> "billingCity";
                default -> key.substring(0, 1).toLowerCase() + key.substring(1);
            };
        }
        if (normalized.equals("opportunity") || normalized.equals("opportunities")) {
            return switch (key) {
                case "StageName" -> "stageName";
                case "CloseDate" -> "closeDate";
                default -> key.substring(0, 1).toLowerCase() + key.substring(1);
            };
        }
        if (normalized.equals("lead") || normalized.equals("leads")) {
            return switch (key) {
                case "FirstName" -> "firstName";
                case "LastName" -> "lastName";
                case "LeadSource" -> "leadSource";
                default -> key.substring(0, 1).toLowerCase() + key.substring(1);
            };
        }
        if (normalized.equals("case") || normalized.equals("cases")) {
            return switch (key) {
                case "CaseNumber" -> "caseNumber";
                default -> key.substring(0, 1).toLowerCase() + key.substring(1);
            };
        }
        return key;
    }

    private RuntimeException salesforceError(String object, String operation, RestClientResponseException e) {
        return new RuntimeException(
                "Salesforce " + object + " " + operation + " failed: "
                        + e.getResponseBodyAsString(), e);
    }

    private String normalizeObject(String object) {
        return object == null ? "" : object.trim().toLowerCase();
    }

    private String getSalesforceObjectName(String object) {
        String salesforceObject = objectNames.get(normalizeObject(object));
        if (salesforceObject == null) {
            throw new IllegalArgumentException("Unsupported Salesforce object: " + object);
        }
        return salesforceObject;
    }

    private SalesforceTokenResponse getToken(HttpSession session) {
        SalesforceTokenResponse token = tokenStore.getToken();
        if (token != null && token.getAccessToken() != null && !token.getAccessToken().isBlank()) {
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
        throw new RuntimeException("Salesforce session not found. Please login again.");
    }

    private HttpHeaders createHeaders(SalesforceTokenResponse token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
