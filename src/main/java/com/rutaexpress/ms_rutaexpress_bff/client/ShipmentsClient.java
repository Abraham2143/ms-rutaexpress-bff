package com.rutaexpress.ms_rutaexpress_bff.client;

import com.rutaexpress.ms_rutaexpress_bff.dto.ShipmentDto;
import java.util.Optional;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ShipmentsClient {
    private final RestClient http;

    public ShipmentsClient(@Qualifier("shipmentsRestClient") RestClient http) {
        this.http = http;
    }

    public ResponseEntity<byte[]> create(ShipmentDto body, String token) {
        return DownstreamHttp.send(http, HttpMethod.POST,
                uri -> uri.path("/api/shipments").build(), body, token);
    }

    public ResponseEntity<byte[]> get(String id, String token) {
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/shipments/{id}").build(id), null, token);
    }

    public ResponseEntity<byte[]> updateStatus(String id, ShipmentDto body, String token) {
        return DownstreamHttp.send(http, HttpMethod.PUT,
                uri -> uri.path("/api/shipments/{id}/status").build(id), body, token);
    }

    public ResponseEntity<byte[]> search(String status, String from, String to, String token) {
        var filters = new HashMap<String, Object>();
        filters.put("status", status);
        filters.put("from", from);
        filters.put("to", to);
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/shipments")
                        .queryParamIfPresent("status", Optional.ofNullable(status).map(value -> "{status}"))
                        .queryParamIfPresent("from", Optional.ofNullable(from).map(value -> "{from}"))
                        .queryParamIfPresent("to", Optional.ofNullable(to).map(value -> "{to}"))
                        .build(filters), null, token);
    }
}
