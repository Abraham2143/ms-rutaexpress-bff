package com.rutaexpress.ms_rutaexpress_bff.client;
import com.rutaexpress.ms_rutaexpress_bff.dto.*;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ClientsTests {
    MockRestServiceServer server;
    ShipmentsClient shipments;
    CatalogClient catalog;
    @BeforeEach void setup() {
        var builder=RestClient.builder().baseUrl("http://downstream.test");
        server=MockRestServiceServer.bindTo(builder).build();
        var http=builder.build();
        shipments=new ShipmentsClient(http);
        catalog=new CatalogClient(http);
    }
    @Test void shipmentCreate() {
        var body=new ShipmentDto(); body.put("extra","unchanged");
        server.expect(requestTo("http://downstream.test/api/shipments")).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer one")).andExpect(content().json("{\"extra\":\"unchanged\"}"))
            .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"id\":1}"));
        var response=shipments.create(body,"one");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).asString().isEqualTo("{\"id\":1}");
        server.verify();
    }
    @Test void shipmentGetAndUpdateUseSeparateTokens() {
        server.expect(requestTo("http://downstream.test/api/shipments/42")).andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer one")).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://downstream.test/api/shipments/42/status")).andExpect(method(HttpMethod.PUT))
            .andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer two")).andExpect(content().json("{\"status\":\"S\"}"))
            .andRespond(withNoContent());
        shipments.get("42","one");
        var body=new ShipmentDto(); body.put("status","S");
        assertThat(shipments.updateStatus("42",body,"two").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        server.verify();
    }
    @Test void searchEncodesFilters() {
        server.expect(requestTo("http://downstream.test/api/shipments?status=IN%20TRANSIT&from=2026-09-01&to=2026-09-13"))
            .andExpect(method(HttpMethod.GET)).andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer one"))
            .andRespond(withSuccess("{\"content\":[]}",MediaType.APPLICATION_JSON));
        assertThat(shipments.search("IN TRANSIT","2026-09-01","2026-09-13","one").getBody()).asString()
            .isEqualTo("{\"content\":[]}");
        server.verify();
    }
    @Test void filtersCannotInjectExtraQueryParameters() {
        server.expect(requestTo("http://downstream.test/api/shipments?status=A%26to%3DB%2BC"))
            .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        shipments.search("A&to=B+C",null,null,"one");
        server.verify();
    }
    @Test void redirectsAreRejected() {
        server.expect(requestTo("http://downstream.test/api/catalog/services"))
            .andRespond(withStatus(HttpStatus.FOUND).location(java.net.URI.create("https://other.test")));
        assertThatThrownBy(() -> catalog.list("one")).isInstanceOf(RestClientException.class)
            .hasMessage("Unexpected downstream redirect");
        server.verify();
    }
    @Test void searchOmitsNullFilters() {
        server.expect(requestTo("http://downstream.test/api/shipments")).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        shipments.search(null,null,null,"one");
        server.verify();
    }
    @Test void catalogOperations() {
        server.expect(requestTo("http://downstream.test/api/catalog/services")).andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer one")).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://downstream.test/api/catalog/services")).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer two")).andExpect(content().json("{\"extra\":true}"))
            .andRespond(withStatus(HttpStatus.CREATED));
        server.expect(requestTo("http://downstream.test/api/catalog/services/7")).andExpect(method(HttpMethod.PUT))
            .andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer three")).andExpect(content().json("{\"extra\":true}"))
            .andRespond(withNoContent());
        catalog.list("one");
        var body=new CatalogServiceDto(); body.put("extra",true);
        assertThat(catalog.create(body,"two").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(catalog.update("7",body,"three").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        server.verify();
    }
    @ParameterizedTest @ValueSource(ints={400,403,404,503})
    void errorsReachAdvice(int code) {
        server.expect(requestTo("http://downstream.test/api/shipments/42"))
            .andRespond(withStatus(HttpStatusCode.valueOf(code)).body("error"));
        assertThatThrownBy(() -> shipments.get("42","one")).isInstanceOfSatisfying(RestClientResponseException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(code));
        server.verify();
    }
    @Test void connectionFailure() {
        server.expect(requestTo("http://downstream.test/api/catalog/services")).andRespond(withException(new IOException("refused")));
        assertThatThrownBy(() -> catalog.list("one")).isInstanceOf(ResourceAccessException.class);
        server.verify();
    }
}
