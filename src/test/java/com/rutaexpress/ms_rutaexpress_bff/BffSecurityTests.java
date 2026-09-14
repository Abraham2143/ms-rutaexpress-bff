package com.rutaexpress.ms_rutaexpress_bff;
import com.rutaexpress.ms_rutaexpress_bff.client.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test",
"rutaexpress.security.audience=api://test"})
@AutoConfigureMockMvc
class BffSecurityTests {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean ShipmentsClient shipments;
    @MockitoBean CatalogClient catalog;

    void token(String... roles) {
        when(decoder.decode("test-token")).thenReturn(Jwt.withTokenValue("test-token")
            .header("alg","RS256").subject("user").issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300)).claim("roles",List.of(roles)).build());
    }
    @Test void noJwt() throws Exception {
        mvc.perform(get("/api/shipments")).andExpect(status().isUnauthorized());
        verifyNoInteractions(shipments,catalog);
    }
    @Test void badJwt() throws Exception {
        when(decoder.decode("bad")).thenThrow(new BadJwtException("invalid"));
        mvc.perform(get("/api/shipments").header("Authorization","Bearer bad")).andExpect(status().isUnauthorized());
        verifyNoInteractions(shipments,catalog);
    }
    @Test void noRole() throws Exception {
        token();
        mvc.perform(post("/api/shipments").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(shipments);
    }
    @ParameterizedTest @ValueSource(strings={"Admin","Cliente"})
    void createShipment(String role) throws Exception {
        token(role);
        when(shipments.create(any(),eq("test-token"))).thenReturn(ResponseEntity.status(201).body("{}".getBytes()));
        mvc.perform(post("/api/shipments").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"extra\":{\"nested\":true}}"))
            .andExpect(status().isCreated());
        verify(shipments).create(argThat(body -> body.containsKey("extra")),eq("test-token"));
    }
    @ParameterizedTest @ValueSource(strings={"Admin","Operador","Cliente","Auditor"})
    void readShipment(String role) throws Exception {
        token(role);
        when(shipments.get("42","test-token")).thenReturn(ResponseEntity.ok("{}".getBytes()));
        when(shipments.search("S","F","T","test-token")).thenReturn(ResponseEntity.ok("[]".getBytes()));
        mvc.perform(get("/api/shipments/42").header("Authorization","Bearer test-token")).andExpect(status().isOk());
        mvc.perform(get("/api/shipments").param("status","S").param("from","F").param("to","T")
            .header("Authorization","Bearer test-token")).andExpect(status().isOk());
        verify(shipments).get("42","test-token");
        verify(shipments).search("S","F","T","test-token");
    }
    @ParameterizedTest @ValueSource(strings={"Admin","Operador"})
    void updateShipment(String role) throws Exception {
        token(role);
        when(shipments.updateStatus(eq("42"),any(),eq("test-token"))).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(put("/api/shipments/42/status").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"S\"}")).andExpect(status().isNoContent());
        verify(shipments).updateStatus(eq("42"),argThat(body -> "S".equals(body.get("status"))),eq("test-token"));
    }
    @ParameterizedTest @ValueSource(strings={"Cliente","Auditor"})
    void cannotUpdateShipment(String role) throws Exception {
        token(role);
        mvc.perform(put("/api/shipments/42/status").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(shipments);
    }
    @Test void catalogRead() throws Exception {
        token();
        when(catalog.list("test-token")).thenReturn(ResponseEntity.ok("[]".getBytes()));
        mvc.perform(get("/api/catalog/services").header("Authorization","Bearer test-token")).andExpect(status().isOk());
        verify(catalog).list("test-token");
    }
    @Test void catalogWrites() throws Exception {
        token("Admin");
        when(catalog.create(any(),eq("test-token"))).thenReturn(ResponseEntity.status(201).build());
        when(catalog.update(eq("7"),any(),eq("test-token"))).thenReturn(ResponseEntity.noContent().build());
        mvc.perform(post("/api/catalog/services").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"extra\":true}")).andExpect(status().isCreated());
        mvc.perform(put("/api/catalog/services/7").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"extra\":true}")).andExpect(status().isNoContent());
        verify(catalog).create(argThat(body -> Boolean.TRUE.equals(body.get("extra"))),eq("test-token"));
        verify(catalog).update(eq("7"),argThat(body -> Boolean.TRUE.equals(body.get("extra"))),eq("test-token"));
    }
    @ParameterizedTest @ValueSource(strings={"Cliente","Operador","Auditor"})
    void cannotWriteCatalog(String role) throws Exception {
        token(role);
        mvc.perform(post("/api/catalog/services").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/catalog/services/7").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }
    @ParameterizedTest @ValueSource(ints={400,403,404,503})
    void downstreamError(int code) throws Exception {
        token("Auditor");
        when(shipments.get("42","test-token")).thenThrow(new RestClientResponseException(
            "downstream",code,"error",null,"downstream-body".getBytes(),null));
        mvc.perform(get("/api/shipments/42").header("Authorization","Bearer test-token"))
            .andExpect(status().is(code)).andExpect(content().string("downstream-body"));
    }
    @Test void unavailable() throws Exception {
        token();
        when(catalog.list("test-token")).thenThrow(new ResourceAccessException("internal secret"));
        mvc.perform(get("/api/catalog/services").header("Authorization","Bearer test-token"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }
    @Test void malformedJson() throws Exception {
        token("Admin");
        mvc.perform(post("/api/shipments").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{")).andExpect(status().isBadRequest());
        verifyNoInteractions(shipments);
    }
    @Test void cors() throws Exception {
        mvc.perform(options("/api/shipments").header("Origin","http://localhost:4200")
            .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"));
        mvc.perform(options("/api/shipments").header("Origin","https://untrusted.test")
            .header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    }
}
