package com.rutaexpress.ms_rutaexpress_bff.controller;

import com.rutaexpress.ms_rutaexpress_bff.client.CatalogClient;
import com.rutaexpress.ms_rutaexpress_bff.dto.CatalogServiceDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/services")
public class CatalogBffController {
    private final CatalogClient client;

    public CatalogBffController(CatalogClient client) {
        this.client = client;
    }

    @GetMapping
    public ResponseEntity<byte[]> list(@AuthenticationPrincipal Jwt jwt) {
        return client.list(jwt.getTokenValue());
    }

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestBody CatalogServiceDto body, @AuthenticationPrincipal Jwt jwt) {
        return client.create(body, jwt.getTokenValue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<byte[]> update(@PathVariable String id,
            @RequestBody CatalogServiceDto body, @AuthenticationPrincipal Jwt jwt) {
        return client.update(id, body, jwt.getTokenValue());
    }
}

