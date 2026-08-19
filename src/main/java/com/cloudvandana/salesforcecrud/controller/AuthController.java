package com.cloudvandana.salesforcecrud.controller;

import com.cloudvandana.salesforcecrud.dto.SalesforceTokenResponse;
import com.cloudvandana.salesforcecrud.service.SalesforceAuthService;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)
public class AuthController {

    private final SalesforceAuthService salesforceAuthService;

    public AuthController(
            SalesforceAuthService salesforceAuthService) {

        this.salesforceAuthService =
                salesforceAuthService;
    }

    @GetMapping("/login")
    public String login(HttpSession session) {

        return salesforceAuthService
                .getAuthorizationUrl(session);
    }

    @GetMapping("/callback")
    public String callback(
            @RequestParam("code") String code,
            HttpSession session) {

        salesforceAuthService.exchangeCodeForToken(
                code,
                session
        );

        return "Salesforce login successful. "
                + "You can now use the CRUD APIs.";
    }
}