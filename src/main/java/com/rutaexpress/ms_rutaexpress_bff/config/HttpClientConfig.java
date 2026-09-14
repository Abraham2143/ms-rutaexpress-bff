package com.rutaexpress.ms_rutaexpress_bff.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {
    @Bean
    RestClient shipmentsRestClient(
            @Value("${rutaexpress.shipments.base-url}") String url,
            @Value("${rutaexpress.http.connect-timeout}") Duration connect,
            @Value("${rutaexpress.http.read-timeout}") Duration read) {
        return create(url, connect, read);
    }

    @Bean
    RestClient catalogRestClient(
            @Value("${rutaexpress.catalog.base-url}") String url,
            @Value("${rutaexpress.http.connect-timeout}") Duration connect,
            @Value("${rutaexpress.http.read-timeout}") Duration read) {
        return create(url, connect, read);
    }

    private RestClient create(String url, Duration connect, Duration read) {
        URI uri = URI.create(url);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Downstream base URL must be an HTTP(S) URL without credentials, query or fragment");
        }
        if (connect.isZero() || connect.isNegative() || read.isZero() || read.isNegative()) {
            throw new IllegalArgumentException("HTTP timeouts must be positive");
        }
        var http = HttpClient.newBuilder().connectTimeout(connect)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(read);
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
}

