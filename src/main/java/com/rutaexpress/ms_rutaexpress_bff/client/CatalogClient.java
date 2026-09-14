package com.rutaexpress.ms_rutaexpress_bff.client;

import com.rutaexpress.ms_rutaexpress_bff.dto.CatalogServiceDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CatalogClient {
    private final RestClient http;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient http) {
        this.http = http;
    }

    public ResponseEntity<byte[]> list(String token) {
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/catalog/services").build(), null, token);
    }

    public ResponseEntity<byte[]> create(CatalogServiceDto body, String token) {
        return DownstreamHttp.send(http, HttpMethod.POST,
                uri -> uri.path("/api/catalog/services").build(), body, token);
    }

    public ResponseEntity<byte[]> update(String id, CatalogServiceDto body, String token) {
        return DownstreamHttp.send(http, HttpMethod.PUT,
                uri -> uri.path("/api/catalog/services/{id}").build(id), body, token);
    }
}

