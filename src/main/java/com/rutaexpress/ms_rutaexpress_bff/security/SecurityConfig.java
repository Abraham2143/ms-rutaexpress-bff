package com.rutaexpress.ms_rutaexpress_bff.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationConverter roles,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        return http
                .cors(config -> config.configurationSource(cors))
                // The API uses only Authorization Bearer, never cookie/session authentication.
                .csrf(config -> config.disable())
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalog/services").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/catalog/services").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/catalog/services/*").hasRole("Admin")
                        .requestMatchers(HttpMethod.POST, "/api/shipments").hasAnyRole("Cliente", "Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/shipments/*/status").hasAnyRole("Operador", "Admin")
                        .requestMatchers(HttpMethod.GET, "/api/shipments", "/api/shipments/*")
                            .hasAnyRole("Admin", "Operador", "Cliente", "Auditor")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(roles)))
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${rutaexpress.security.audience}") String audience) {
        if (issuer.isBlank() || audience.isBlank()) {
            throw new IllegalArgumentException("AZURE_ISSUER_URI and AZURE_AUDIENCE are required");
        }
        // Discovery is deferred until the first token; no external service is needed at startup.
        return new SupplierJwtDecoder(() -> {
            var decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(issuer), audienceValidator(audience)));
            return decoder;
        });
    }

    static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        return token -> token.getAudience() != null && token.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "Token audience does not match this API", null));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${rutaexpress.cors.allowed-origins}") String allowedOrigins) {
        var origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).toList();
        if (origins.isEmpty() || origins.stream().anyMatch(value -> value.contains("*"))) {
            throw new IllegalArgumentException("CORS requires explicit allowed origins");
        }
        var config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("ETag", "Retry-After"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
