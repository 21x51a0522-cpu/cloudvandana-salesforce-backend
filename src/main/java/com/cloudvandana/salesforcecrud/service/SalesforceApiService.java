package com.cloudvandana.salesforcecrud.service;

import com.cloudvandana.salesforcecrud.config.SalesforceConfig;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class SalesforceApiService {

    private final SalesforceConfig salesforceConfig;
    private final SalesforceTokenStore tokenStore;
    private final RestTemplate restTemplate;

    public SalesforceApiService(
            SalesforceConfig salesforceConfig,
            SalesforceTokenStore tokenStore) {

        this.salesforceConfig = salesforceConfig;
        this.tokenStore = tokenStore;
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders createHeaders() {

        if (!tokenStore.isAuthenticated()) {
            throw new RuntimeException(
                    "Salesforce authentication required. Please login first."
            );
        }

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                tokenStore.getAccessToken()
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        return headers;
    }

    private String getBaseUrl() {

        return tokenStore.getInstanceUrl()
                + "/services/data/"
                + salesforceConfig.getApiVersion();
    }

    // GET all records
    public String getRecords(
            String objectName,
            Integer limit,
            Integer offset) {

        String soql =
                "SELECT FIELDS(ALL) FROM "
                + objectName
                + " LIMIT "
                + limit
                + " OFFSET "
                + offset;

        String url =
                getBaseUrl()
                + "/query?q="
                + soql.replace(" ", "+");

        HttpEntity<Void> request =
                new HttpEntity<>(createHeaders());

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        request,
                        String.class
                );

        return response.getBody();
    }

    // GET one record
    public String getRecord(
            String objectName,
            String recordId) {

        String url =
                getBaseUrl()
                + "/sobjects/"
                + objectName
                + "/"
                + recordId;

        HttpEntity<Void> request =
                new HttpEntity<>(createHeaders());

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        request,
                        String.class
                );

        return response.getBody();
    }

    // CREATE
    public String createRecord(
            String objectName,
            Map<String, Object> data) {

        String url =
                getBaseUrl()
                + "/sobjects/"
                + objectName;

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        data,
                        createHeaders()
                );

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        String.class
                );

        return response.getBody();
    }

    // UPDATE
    public String updateRecord(
            String objectName,
            String recordId,
            Map<String, Object> data) {

        String url =
                getBaseUrl()
                + "/sobjects/"
                + objectName
                + "/"
                + recordId;

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        data,
                        createHeaders()
                );

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.PATCH,
                        request,
                        String.class
                );

        return response.getBody();
    }

    // DELETE
    public String deleteRecord(
            String objectName,
            String recordId) {

        String url =
                getBaseUrl()
                + "/sobjects/"
                + objectName
                + "/"
                + recordId;

        HttpEntity<Void> request =
                new HttpEntity<>(createHeaders());

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.DELETE,
                        request,
                        String.class
                );

        if (response.getStatusCode().is2xxSuccessful()) {
            return "Record deleted successfully";
        }

        return response.getBody();
    }
}