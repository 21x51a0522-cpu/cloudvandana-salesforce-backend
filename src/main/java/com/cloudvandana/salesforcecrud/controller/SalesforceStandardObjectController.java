package com.cloudvandana.salesforcecrud.controller;

import com.cloudvandana.salesforcecrud.service.SalesforceStandardObjectService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "https://salesforce-phi.vercel.app"
        },
        allowCredentials = "true"
)
public class SalesforceStandardObjectController {

    private final SalesforceStandardObjectService service;

    public SalesforceStandardObjectController(
            SalesforceStandardObjectService service) {

        this.service = service;
    }

    // =========================================================
    // GET RECORDS
    // DEFAULT = 20
    // =========================================================

    @GetMapping("/{object}")
    public ResponseEntity<?> getRecords(
            @PathVariable String object,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        return ResponseEntity.ok(
                service.getRecords(
                        object,
                        limit,
                        offset
                )
        );
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @GetMapping("/{object}/{id}")
    public ResponseEntity<?> getById(
            @PathVariable String object,
            @PathVariable String id) {

        return ResponseEntity.ok(
                service.getById(
                        object,
                        id
                )
        );
    }

    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping("/{object}")
    public ResponseEntity<?> create(
            @PathVariable String object,
            @RequestBody Map<String, Object> request) {

        return ResponseEntity.ok(
                service.create(
                        object,
                        request
                )
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{object}/{id}")
    public ResponseEntity<?> update(
            @PathVariable String object,
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {

        return ResponseEntity.ok(
                service.update(
                        object,
                        id,
                        request
                )
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{object}/{id}")
    public ResponseEntity<?> delete(
            @PathVariable String object,
            @PathVariable String id) {

        return ResponseEntity.ok(
                service.delete(
                        object,
                        id
                )
        );
    }
}