package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.external;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.interferometry.domain.model.BaselineSource;
import com.ass.br.apiradar.interferometry.domain.model.BaselineValue;
import com.ass.br.apiradar.interferometry.domain.ports.output.PerpendicularBaselinePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Adaptador de saída que consulta metadados de baseline perpendicular na NASA ASF DAAC.
 * Respeita a diretriz de não utilizar valores simulados fora de testes:
 * caso a consulta externa falhe, demore ou não retorne baseline perpendicular válida,
 * o par é marcado com BaselineValue.unavailable(), permanecendo PENDENTE DE VALIDAÇÃO.
 */
@Component
public class AsfBaselineClientAdapter implements PerpendicularBaselinePort {

    private static final Logger log = LoggerFactory.getLogger(AsfBaselineClientAdapter.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final boolean mockEnabled;

    public AsfBaselineClientAdapter(
            @Value("${radar.catalog.asf-base-url:https://api.daac.asf.alaska.edu}") String baseUrl,
            @Value("${radar.catalog.mock-enabled:false}") boolean mockEnabled,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.mockEnabled = mockEnabled;
        this.objectMapper = objectMapper;
    }

    @Override
    public BaselineValue getPerpendicularBaseline(SarScene referenceScene, SarScene secondaryScene) {
        if (mockEnabled) {
            // Em testes unitários ou ambientes isolados configurados com mock, retorna valor controlado
            log.debug("[MOCK_ATIVO] Retornando baseline de teste para par {} - {}",
                    referenceScene.sceneName(), secondaryScene.sceneName());
            return BaselineValue.calculated(42.5, BaselineSource.ASF_BASELINE_SERVICE);
        }

        if (referenceScene == null || secondaryScene == null ||
                referenceScene.sceneName() == null || secondaryScene.sceneName() == null) {
            return BaselineValue.unavailable();
        }

        try {
            log.info("Consultando serviço de baseline da NASA ASF para a cena de referência: {}", referenceScene.sceneName());
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/services/search/baseline")
                            .queryParam("reference", referenceScene.sceneName())
                            .queryParam("output", "json")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(6))
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Serviço de baseline da NASA ASF retornou vazio para {}", referenceScene.sceneName());
                return BaselineValue.unavailable();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.isArray() && root.size() > 0 && root.get(0).isArray()) {
                root = root.get(0);
            }
            if (root.isArray()) {
                for (JsonNode item : root) {
                    String candidateName = item.path("granuleName").asText(item.path("sceneName").asText(""));
                    if (candidateName.equalsIgnoreCase(secondaryScene.sceneName())) {
                        JsonNode bPerpNode = item.path("perpendicularBaseline");
                        if (!bPerpNode.isMissingNode() && !bPerpNode.isNull()) {
                            double meters = bPerpNode.asDouble();
                            log.info("Baseline perpendicular localizada para o par [{}, {}]: {}m",
                                    referenceScene.sceneName(), secondaryScene.sceneName(), meters);
                            return BaselineValue.calculated(meters, BaselineSource.ASF_BASELINE_SERVICE);
                        }
                    }
                }
            }

            log.info("Baseline perpendicular não encontrada na resposta ASF para o par [{}, {}]. Mantendo indisponível.",
                    referenceScene.sceneName(), secondaryScene.sceneName());
            return BaselineValue.unavailable();

        } catch (Exception ex) {
            log.warn("Falha ou indisponibilidade ao consultar serviço de baseline da ASF para {} (motivo: {}). " +
                    "O par será registrado como pendente de validação conforme diretriz do Ciclo 5.3.",
                    referenceScene.sceneName(), ex.getMessage());
            return BaselineValue.unavailable();
        }
    }
}
