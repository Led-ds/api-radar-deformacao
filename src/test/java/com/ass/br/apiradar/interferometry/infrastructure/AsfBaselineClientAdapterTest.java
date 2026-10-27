package com.ass.br.apiradar.interferometry.infrastructure;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.interferometry.domain.model.BaselineSource;
import com.ass.br.apiradar.interferometry.domain.model.BaselineValue;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.external.AsfBaselineClientAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Infraestrutura: AsfBaselineClientAdapter")
class AsfBaselineClientAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SarScene createScene(String name) {
        return new SarScene(
                name,
                Instant.now(),
                96,
                "DESCENDING",
                "VV+VH",
                "SENTINEL-1A",
                name,
                "NASA ASF DAAC"
        );
    }

    @Test
    @DisplayName("Retorna baseline de teste apenas quando mockEnabled=true explicitamente configurado")
    void shouldReturnMockBaselineOnlyWhenMockEnabled() {
        AsfBaselineClientAdapter adapter = new AsfBaselineClientAdapter(
                "https://fake-url.invalid",
                true, // mockEnabled = true
                objectMapper
        );

        SarScene master = createScene("S1A_IW_SLC__1SDV_20230101T210000");
        SarScene slave = createScene("S1A_IW_SLC__1SDV_20230113T210000");

        BaselineValue baseline = adapter.getPerpendicularBaseline(master, slave);

        assertNotNull(baseline);
        assertTrue(baseline.available());
        assertEquals(42.5, baseline.meters());
        assertEquals(BaselineSource.ASF_BASELINE_SERVICE, baseline.source());
        assertEquals("meters", baseline.unit());
    }

    @Test
    @DisplayName("Retorna UNAVAILABLE de forma resiliente quando mockEnabled=false e endpoint é inalcançável")
    void shouldReturnUnavailableGracefullyWhenOfflineAndMockDisabled() {
        AsfBaselineClientAdapter adapter = new AsfBaselineClientAdapter(
                "http://localhost:59999", // porta inalcançável
                false, // mockEnabled = false
                objectMapper
        );

        SarScene master = createScene("S1A_IW_SLC__1SDV_20230101T210000");
        SarScene slave = createScene("S1A_IW_SLC__1SDV_20230113T210000");

        // Invariante mandatório do Ciclo 5.3: falha na rede não deve lançar exceção fatal,
        // mas sim retornar UNAVAILABLE mantendo o par pendente de validação
        BaselineValue baseline = adapter.getPerpendicularBaseline(master, slave);

        assertNotNull(baseline);
        assertFalse(baseline.available());
        assertNull(baseline.meters());
        assertEquals(BaselineSource.UNAVAILABLE, baseline.source());
    }

    @Test
    @DisplayName("Retorna UNAVAILABLE quando as cenas de entrada são nulas")
    void shouldReturnUnavailableWhenScenesAreNull() {
        AsfBaselineClientAdapter adapter = new AsfBaselineClientAdapter(
                "http://localhost:59999",
                false,
                objectMapper
        );

        BaselineValue baseline = adapter.getPerpendicularBaseline(null, null);

        assertNotNull(baseline);
        assertFalse(baseline.available());
        assertEquals(BaselineSource.UNAVAILABLE, baseline.source());
    }

    @Test
    @DisplayName("Processa com sucesso payload de baseline superior ao buffer default de 256KB sem lançar DataBufferLimitException")
    void shouldHandlePayloadsExceedingDefaultBufferLimit() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        try {
            String masterName = "S1A_IW_SLC__1SDV_20230104T080249_20230104T080316_046631_0596C5_604A";
            String slaveName = "S1A_IW_SLC__1SDV_20230116T080248_20230116T080315_046806_059CA6_BF75";

            // Gera payload JSON com mais de 300KB (excedendo 256KB default do WebClient)
            StringBuilder sb = new StringBuilder("[[");
            for (int i = 0; i < 300; i++) {
                sb.append("{\"granuleName\":\"DUMMY_SCENE_").append(i)
                        .append("\",\"perpendicularBaseline\":10.0,\"filler\":\"")
                        .append("x".repeat(1024)) // 1KB cada item
                        .append("\"},");
            }
            sb.append("{\"granuleName\":\"").append(slaveName).append("\",\"perpendicularBaseline\":-79.0}");
            sb.append("]]");
            byte[] responseBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(responseBytes.length > 262144, "Payload de teste deve ser > 256KB");

            server.createContext("/services/search/baseline", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            });
            server.start();

            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AsfBaselineClientAdapter adapter = new AsfBaselineClientAdapter(baseUrl, false, objectMapper);

            SarScene master = createScene(masterName);
            SarScene slave = createScene(slaveName);

            BaselineValue baseline = adapter.getPerpendicularBaseline(master, slave);

            assertNotNull(baseline);
            assertTrue(baseline.available(), "Baseline deve estar disponível quando payload é parseado com sucesso");
            assertEquals(-79.0, baseline.meters());
            assertEquals(BaselineSource.ASF_BASELINE_SERVICE, baseline.source());
        } finally {
            server.stop(0);
        }
    }
}
