package com.rutaexpress.ms_rutaexpress_bff.client;

import java.net.URI;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/** Shared transport only. Tokens are passed as request-local arguments. */
final class DownstreamHttp {
    private DownstreamHttp() {}

    static ResponseEntity<byte[]> send(RestClient client, HttpMethod method,
            Function<UriBuilder, URI> uri, Object body, String token) {
        var request = client.method(method).uri(uri)
                .headers(headers -> headers.setBearerAuth(token));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        var response = request.retrieve()
                .onStatus(status -> status.is3xxRedirection(), (req, res) -> {
                    throw new RestClientException("Unexpected downstream redirect");
                }).toEntity(byte[].class);
        return ResponseEntity.status(response.getStatusCode())
                .headers(safeHeaders(response.getHeaders())).body(response.getBody());
    }

    static HttpHeaders safeHeaders(HttpHeaders source) {
        var headers = new HttpHeaders();
        for (String name : new String[]{HttpHeaders.CONTENT_TYPE, HttpHeaders.ETAG,
                HttpHeaders.LAST_MODIFIED, HttpHeaders.RETRY_AFTER}) {
            if (source.containsHeader(name)) {
                headers.put(name, source.get(name));
            }
        }
        return headers;
    }
}
