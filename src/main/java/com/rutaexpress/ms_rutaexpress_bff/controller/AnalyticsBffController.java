package com.rutaexpress.ms_rutaexpress_bff.controller;

import com.rutaexpress.ms_rutaexpress_bff.client.AnalyticsClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsBffController {
    private final AnalyticsClient client;

    public AnalyticsBffController(AnalyticsClient client) {
        this.client = client;
    }

    @GetMapping("/summary")
    public ResponseEntity<byte[]> summary(@AuthenticationPrincipal Jwt jwt) {
        return client.summary(jwt.getTokenValue());
    }

    @GetMapping("/statuses")
    public ResponseEntity<byte[]> statuses(@AuthenticationPrincipal Jwt jwt) {
        return client.statuses(jwt.getTokenValue());
    }

    @GetMapping("/recent")
    public ResponseEntity<byte[]> recent(
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        return client.recent(limit, jwt.getTokenValue());
    }
}