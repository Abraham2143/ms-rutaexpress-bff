package com.rutaexpress.ms_rutaexpress_bff.controller;

import com.rutaexpress.ms_rutaexpress_bff.client.KafkaAdminClient;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminBffController {
    private final KafkaAdminClient client;

    public KafkaAdminBffController(KafkaAdminClient client) {
        this.client = client;
    }

    @GetMapping("/cluster")
    public ResponseEntity<byte[]> cluster(@AuthenticationPrincipal Jwt jwt) {
        return client.cluster(jwt.getTokenValue());
    }

    @GetMapping("/topics")
    public ResponseEntity<byte[]> listTopics(@AuthenticationPrincipal Jwt jwt) {
        return client.listTopics(jwt.getTokenValue());
    }

    @GetMapping("/topics/{topicName}")
    public ResponseEntity<byte[]> topicDetail(
            @PathVariable String topicName,
            @AuthenticationPrincipal Jwt jwt) {
        return client.topicDetail(topicName, jwt.getTokenValue());
    }

    @PostMapping("/topics")
    public ResponseEntity<byte[]> createTopic(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {
        return client.createTopic(body, jwt.getTokenValue());
    }

    @DeleteMapping("/topics/{topicName}")
    public ResponseEntity<byte[]> deleteTopic(
            @PathVariable String topicName,
            @AuthenticationPrincipal Jwt jwt) {
        return client.deleteTopic(topicName, jwt.getTokenValue());
    }
}