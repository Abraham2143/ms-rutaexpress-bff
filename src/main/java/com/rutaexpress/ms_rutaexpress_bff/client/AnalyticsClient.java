package com.rutaexpress.ms_rutaexpress_bff.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AnalyticsClient {
    private final RestClient http;

    public AnalyticsClient(@Qualifier("analyticsRestClient") RestClient http) {
        this.http = http;
    }

    public ResponseEntity<byte[]> summary(String token) {
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/v1/analytics/summary").build(), null, token);
    }

    public ResponseEntity<byte[]> statuses(String token) {
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/v1/analytics/statuses").build(), null, token);
    }

    public ResponseEntity<byte[]> recent(int limit, String token) {
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/v1/analytics/recent").queryParam("limit", limit).build(),
                null, token);
    }
}