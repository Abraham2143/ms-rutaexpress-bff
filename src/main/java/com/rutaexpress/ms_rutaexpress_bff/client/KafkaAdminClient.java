package com.rutaexpress.ms_rutaexpress_bff.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KafkaAdminClient {
    private final RestClient http;

    public KafkaAdminClient(@Qualifier("kafkaAdminRestClient") RestClient http) {
        this.http = http;
    }

    public ResponseEntity<byte[]> cluster(String token) {
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/v1/admin/cluster").build(), null, token);
    }

    public ResponseEntity<byte[]> listTopics(String token) {
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/v1/admin/topics").build(), null, token);
    }

    public ResponseEntity<byte[]> topicDetail(String topicName, String token) {
        return DownstreamHttp.send(http, HttpMethod.GET,
                uri -> uri.path("/api/v1/admin/topics/{topicName}").build(topicName), null, token);
    }

    public ResponseEntity<byte[]> createTopic(Map<String, Object> body, String token) {
        return DownstreamHttp.send(http, HttpMethod.POST,
                uri -> uri.path("/api/v1/admin/topics").build(), body, token);
    }

    public ResponseEntity<byte[]> deleteTopic(String topicName, String token) {
        return DownstreamHttp.send(http, HttpMethod.DELETE,
                uri -> uri.path("/api/v1/admin/topics/{topicName}").build(topicName), null, token);
    }
}