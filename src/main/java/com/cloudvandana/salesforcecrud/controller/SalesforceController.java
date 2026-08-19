package com.cloudvandana.salesforcecrud.controller;

import com.cloudvandana.salesforcecrud.service.SalesforceApiService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/salesforce")
@CrossOrigin(origins = "http://localhost:5173")
public class SalesforceController {

    private final SalesforceApiService salesforceApiService;

    public SalesforceController(
            SalesforceApiService salesforceApiService) {

        this.salesforceApiService = salesforceApiService;
    }

    @GetMapping("/{objectName}")
    public ResponseEntity<String> getRecords(
            @PathVariable String objectName,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset) {

        return ResponseEntity.ok(
                salesforceApiService.getRecords(
                        objectName,
                        limit,
                        offset
                )
        );
    }

    @GetMapping("/{objectName}/{recordId}")
    public ResponseEntity<String> getRecord(
            @PathVariable String objectName,
            @PathVariable String recordId) {

        return ResponseEntity.ok(
                salesforceApiService.getRecord(
                        objectName,
                        recordId
                )
        );
    }

    @PostMapping("/{objectName}")
    public ResponseEntity<String> createRecord(
            @PathVariable String objectName,
            @RequestBody Map<String, Object> data) {

        return ResponseEntity.ok(
                salesforceApiService.createRecord(
                        objectName,
                        data
                )
        );
    }

    @PutMapping("/{objectName}/{recordId}")
    public ResponseEntity<String> updateRecord(
            @PathVariable String objectName,
            @PathVariable String recordId,
            @RequestBody Map<String, Object> data) {

        return ResponseEntity.ok(
                salesforceApiService.updateRecord(
                        objectName,
                        recordId,
                        data
                )
        );
    }

    @DeleteMapping("/{objectName}/{recordId}")
    public ResponseEntity<String> deleteRecord(
            @PathVariable String objectName,
            @PathVariable String recordId) {

        return ResponseEntity.ok(
                salesforceApiService.deleteRecord(
                        objectName,
                        recordId
                )
        );
    }
}