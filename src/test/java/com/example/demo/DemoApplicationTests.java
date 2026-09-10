package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"ADMIN_API_KEY=mock-admin-key",
		"JWT_SECRET_KEY=SuaChaveSuperSecretaParaGerarOsTokensJWTDe32Caracteres",
		"GEMINI_API_KEY=mock-gemini-key",
		"WPPCONNECT_SECRET_KEY=mock-wpp-key",
		"WPPCONNECT_URL=http://localhost:21465",
		"gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent",
		"gemini.api.key=mock-gemini-key"
})
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
