package com.rutaexpress.ms_rutaexpress_bff.security;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import static org.assertj.core.api.Assertions.*;
class SecurityConfigTests {
    Jwt jwt(String issuer,String audience,Instant expiry) {
        return Jwt.withTokenValue("test-only").header("alg","RS256").subject("user")
            .issuer(issuer).audience(List.of(audience)).issuedAt(Instant.now().minusSeconds(600))
            .expiresAt(expiry).claim("roles",List.of("ADMIN","DISPATCHER","CLIENT","AUDITOR")).build();
    }
    @Test void roles() {
        var result=new SecurityConfig().jwtAuthenticationConverter().convert(jwt("https://issuer.test","api://bff",Instant.now().plusSeconds(300)));
        assertThat(result.getAuthorities()).extracting("authority")
            .contains("ROLE_ADMIN","ROLE_DISPATCHER","ROLE_CLIENT","ROLE_AUDITOR");
    }
    @Test void audience() {
        var validator=SecurityConfig.audienceValidator("api://bff");
        assertThat(validator.validate(jwt("https://issuer.test","api://bff",Instant.now().plusSeconds(300))).hasErrors()).isFalse();
        assertThat(validator.validate(jwt("https://issuer.test","api://other",Instant.now().plusSeconds(300))).hasErrors()).isTrue();
    }
    @Test void issuerAndExpiry() {
        var validator=JwtValidators.createDefaultWithIssuer("https://issuer.test");
        assertThat(validator.validate(jwt("https://other.test","api://bff",Instant.now().plusSeconds(300))).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("https://issuer.test","api://bff",Instant.now().minusSeconds(300))).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("https://issuer.test","api://bff",Instant.now().plusSeconds(300))).hasErrors()).isFalse();
    }
    @Test void failClosed() {
        var config=new SecurityConfig();
        assertThatThrownBy(() -> config.corsConfigurationSource("*")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.jwtDecoder("","")).isInstanceOf(IllegalArgumentException.class);
    }
}
