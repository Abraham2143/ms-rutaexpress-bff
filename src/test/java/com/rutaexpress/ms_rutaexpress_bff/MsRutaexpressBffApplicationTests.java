package com.rutaexpress.ms_rutaexpress_bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.example.test",
        "rutaexpress.security.audience=api://test"})
class MsRutaexpressBffApplicationTests {

	@Test
	void contextLoads() {
	}

}
