package com.cloudvandana.salesforcecrud.controller;

import com.cloudvandana.salesforcecrud.dto.ContactRequest;
import com.cloudvandana.salesforcecrud.service.ContactService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "https://salesforce-phi.vercel.app"
        },
        allowCredentials = "true"
)
public class ContactController {

    private final ContactService contactService;

    public ContactController(
            ContactService contactService) {

        this.contactService = contactService;
    }

    // =========================================================
    // GET ALL CONTACTS
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getAllContacts() {

        return ResponseEntity.ok(
                contactService.getAllContacts()
        );
    }

    // =========================================================
    // GET CONTACT BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getContactById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                contactService.getContactById(id)
        );
    }

    // =========================================================
    // CREATE CONTACT
    // =========================================================

    @PostMapping
    public ResponseEntity<?> createContact(
            @RequestBody ContactRequest request) {

        return ResponseEntity.ok(
                contactService.createContact(request)
        );
    }

    // =========================================================
    // UPDATE CONTACT
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateContact(
            @PathVariable String id,
            @RequestBody ContactRequest request) {

        return ResponseEntity.ok(
                contactService.updateContact(
                        id,
                        request
                )
        );
    }

    // =========================================================
    // DELETE CONTACT
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContact(
            @PathVariable String id) {

        return ResponseEntity.ok(
                contactService.deleteContact(id)
        );
    }
}