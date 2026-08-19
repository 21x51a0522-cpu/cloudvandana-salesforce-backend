package com.cloudvandana.salesforcecrud.controller;

import com.cloudvandana.salesforcecrud.service.SalesforceAuthService;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "https://salesforce-phi.vercel.app"
        },
        allowCredentials = "true"
)
public class AuthController {

    private final SalesforceAuthService salesforceAuthService;

    public AuthController(
            SalesforceAuthService salesforceAuthService) {

        this.salesforceAuthService = salesforceAuthService;
    }

    // =========================================================
    // SALESFORCE LOGIN
    // =========================================================

    @GetMapping("/login")
    public RedirectView login(HttpSession session) {

        String authorizationUrl =
                salesforceAuthService.getAuthorizationUrl(session);

        return new RedirectView(authorizationUrl);
    }

    // =========================================================
    // SALESFORCE CALLBACK
    // =========================================================

    @GetMapping("/callback")
    public RedirectView callback(
            @RequestParam("code") String code,
            HttpSession session) {

        salesforceAuthService.exchangeCodeForToken(
                code,
                session
        );

        return new RedirectView(
                "https://salesforce-phi.vercel.app/"
        );
    }
}