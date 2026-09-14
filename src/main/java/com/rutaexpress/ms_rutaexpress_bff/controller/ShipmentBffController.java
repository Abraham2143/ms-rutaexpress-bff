package com.rutaexpress.ms_rutaexpress_bff.controller;

import com.rutaexpress.ms_rutaexpress_bff.client.ShipmentsClient;
import com.rutaexpress.ms_rutaexpress_bff.dto.ShipmentDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentBffController {
    private final ShipmentsClient client;

    public ShipmentBffController(ShipmentsClient client) {
        this.client = client;
    }

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestBody ShipmentDto body, @AuthenticationPrincipal Jwt jwt) {
        return client.create(body, jwt.getTokenValue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return client.get(id, jwt.getTokenValue());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<byte[]> updateStatus(@PathVariable String id,
            @RequestBody ShipmentDto body, @AuthenticationPrincipal Jwt jwt) {
        return client.updateStatus(id, body, jwt.getTokenValue());
    }

    @GetMapping
    public ResponseEntity<byte[]> search(@RequestParam(required = false) String status,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @AuthenticationPrincipal Jwt jwt) {
        return client.search(status, from, to, jwt.getTokenValue());
    }
}

