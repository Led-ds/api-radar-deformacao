package com.ass.br.apiradar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApiRadarApplicationTests {

    @Test
    @DisplayName("Inicializa com sucesso o contexto Spring Boot no perfil de teste")
    void contextLoads() {
    }
}
