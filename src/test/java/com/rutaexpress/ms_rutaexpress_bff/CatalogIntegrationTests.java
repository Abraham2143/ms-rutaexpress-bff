package com.rutaexpress.ms_rutaexpress_bff;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP transport to a local stub; no Catalog or Entra deployment is required. */
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test",
        "rutaexpress.security.audience=api://test"})
@AutoConfigureMockMvc
class CatalogIntegrationTests {
    private record Request(String method, String path, String authorization, String body) {}
    private static final LinkedBlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    private static final HttpServer catalog = startCatalog();

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;

    private static HttpServer startCatalog() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/catalog/services", exchange -> {
                requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                byte[] response = "{\"externalField\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders("POST".equals(exchange.getRequestMethod()) ? 201 : 200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start local Catalog test server", exception);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Exercise the actual application.properties placeholder, not a manually built client.
        registry.add("CATALOG_BASE_URL", () -> "http://127.0.0.1:" + catalog.getAddress().getPort());
    }

    @AfterAll static void stopCatalog() {
        catalog.stop(0);
    }

    @Test void configuredCatalogReceivesAllOperationsAndOriginalTokenPerRequest() throws Exception {
        for (String token : List.of("first-test-token", "second-test-token", "third-test-token")) {
            when(decoder.decode(token)).thenReturn(Jwt.withTokenValue(token).header("alg", "RS256")
                    .subject("user").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                    .claim("roles", List.of("Admin")).build());
        }
        mvc.perform(get("/api/catalog/services").header("Authorization", "Bearer first-test-token"))
                .andExpect(status().isOk()).andExpect(content().json("{\"externalField\":true}"));
        mvc.perform(post("/api/catalog/services").header("Authorization", "Bearer second-test-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"externalField\":true}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/catalog/services/7").header("Authorization", "Bearer third-test-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"externalField\":true}"))
                .andExpect(status().isOk());

        assertThat(requests.poll(1, TimeUnit.SECONDS)).isEqualTo(
                new Request("GET", "/api/catalog/services", "Bearer first-test-token", ""));
        assertThat(requests.poll(1, TimeUnit.SECONDS)).isEqualTo(
                new Request("POST", "/api/catalog/services", "Bearer second-test-token", "{\"externalField\":true}"));
        assertThat(requests.poll(1, TimeUnit.SECONDS)).isEqualTo(
                new Request("PUT", "/api/catalog/services/7", "Bearer third-test-token", "{\"externalField\":true}"));
        assertThat(requests).isEmpty();
    }
}
