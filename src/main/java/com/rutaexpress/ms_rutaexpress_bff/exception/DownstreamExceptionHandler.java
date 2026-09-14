package com.rutaexpress.ms_rutaexpress_bff.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class DownstreamExceptionHandler {
    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<byte[]> downstreamResponse(RestClientResponseException exception) {
        var headers = new HttpHeaders();
        var downstream = exception.getResponseHeaders();
        if (downstream != null) {
            for (String name : new String[]{HttpHeaders.CONTENT_TYPE, HttpHeaders.RETRY_AFTER}) {
                if (downstream.containsHeader(name)) {
                    headers.put(name, downstream.get(name));
                }
            }
        }
        return ResponseEntity.status(exception.getStatusCode()).headers(headers)
                .body(exception.getResponseBodyAsByteArray());
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<ProblemDetail> unavailable(ResourceAccessException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                        "El servicio downstream no está disponible o excedió el tiempo de espera."));
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ProblemDetail> invalidResponse(RestClientException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                        "No fue posible procesar la respuesta del servicio downstream."));
    }
}

