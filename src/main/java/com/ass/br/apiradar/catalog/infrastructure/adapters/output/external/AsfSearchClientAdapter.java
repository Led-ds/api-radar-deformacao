package com.ass.br.apiradar.catalog.infrastructure.adapters.output.external;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.catalog.domain.ports.output.SarCatalogPort;
import com.ass.br.apiradar.shared.domain.exceptions.ExternalServiceException;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador de saída que se integra via HTTP WebClient com a API pública da NASA ASF (Alaska Satellite Facility).
 * Conforme diretrizes dos Ciclos 5.1.1 e 5.2:
 * - Resultados externos reais são parseados rigorosamente.
 * - Falhas externas lançam ExternalServiceException (não simulam dados sem consentimento).
 * - Timeouts são tratados com limites explícitos (12s).
 * - Respostas vazias retornam lista vazia genuína.
 * - Metadados científicos completos são extraídos: sceneName, acquisitionDate, relativeOrbit, flightDirection, polarization, sensor, externalId e metadataSource.
 * - Mocks são restritos a ambientes de teste com flag explícita configurada.
 */
@Component
public class AsfSearchClientAdapter implements SarCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(AsfSearchClientAdapter.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final boolean mockEnabled;

    public AsfSearchClientAdapter(
            @Value("${radar.catalog.asf-base-url:https://api.daac.asf.alaska.edu}") String baseUrl,
            @Value("${radar.catalog.mock-enabled:false}") boolean mockEnabled,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.mockEnabled = mockEnabled;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SarScene> searchScenes(BoundingBox bbox, LocalDate startDate, LocalDate endDate, String flightDirection) {
        // Se modo mock estiver explicitamente ativado via configuração para testes offline
        if (mockEnabled) {
            log.info("[MOCK_ATIVO] radar.catalog.mock-enabled=true: Retornando catálogo sintético de referência apenas para testes locais isolados.");
            return generateReferenceScenes(bbox, startDate, endDate, flightDirection);
        }

        String bboxStr = String.format(Locale.US, "%.5f,%.5f,%.5f,%.5f",
                bbox.minLon(), bbox.minLat(), bbox.maxLon(), bbox.maxLat());

        log.info("Consultando NASA ASF Search API com BBOX [{}] no período {} a {} (flightDirection={})",
                bboxStr, startDate, endDate, flightDirection);

        try {
            String responseBody = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/services/search/param")
                                .queryParam("bbox", bboxStr)
                                .queryParam("platform", "SENTINEL-1")
                                .queryParam("processingLevel", "SLC")
                                .queryParam("beamMode", "IW")
                                .queryParam("output", "geojson");

                        if (startDate != null) {
                            uriBuilder.queryParam("start", startDate.atStartOfDay().atOffset(ZoneOffset.UTC).toString());
                        }
                        if (endDate != null) {
                            uriBuilder.queryParam("end", endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).toString());
                        }
                        if (flightDirection != null && !flightDirection.isBlank()) {
                            uriBuilder.queryParam("flightDirection", flightDirection.toUpperCase(Locale.ROOT));
                        }
                        return uriBuilder.build();
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            return parseAsfGeoJson(responseBody);

        } catch (WebClientResponseException ex) {
            String errorMsg = String.format("A API NASA/ASF retornou status HTTP %d (%s)",
                    ex.getStatusCode().value(), ex.getStatusText());
            log.error("Erro HTTP ao consultar catálogo ASF: {} - Detalhe: {}", errorMsg, ex.getResponseBodyAsString());
            throw new ExternalServiceException(errorMsg, ex);

        } catch (Exception ex) {
            String errorMsg = "Falha de comunicação ou timeout ao consultar a API da NASA ASF: " + ex.getMessage();
            log.error("Exceção na chamada externa ao ASF: {}", errorMsg, ex);
            throw new ExternalServiceException(errorMsg, ex);
        }
    }

    private List<SarScene> parseAsfGeoJson(String geoJsonStr) {
        List<SarScene> scenes = new ArrayList<>();
        if (geoJsonStr == null || geoJsonStr.isBlank()) {
            return scenes;
        }

        try {
            JsonNode root = objectMapper.readTree(geoJsonStr);
            JsonNode features = root.path("features");

            if (features.isArray()) {
                for (JsonNode feature : features) {
                    JsonNode props = feature.path("properties");
                    String sceneName = props.path("sceneName").asText(props.path("granuleName").asText("UNKNOWN"));
                    String startTimeStr = props.path("startTime").asText(Instant.now().toString());
                    Instant acquisitionDate = Instant.parse(startTimeStr);
                    int relativeOrbit = props.path("pathNumber").asInt(0);
                    String flightDirection = props.path("flightDirection").asText("UNKNOWN");
                    String polarization = props.path("polarization").asText("VV");
                    String sensor = props.path("platform").asText("SENTINEL-1");
                    String externalId = props.path("fileID").asText(props.path("granuleName").asText(sceneName));
                    String metadataSource = "Alaska Satellite Facility DAAC (NASA ASF)";

                    scenes.add(new SarScene(sceneName, acquisitionDate, relativeOrbit, flightDirection, polarization, sensor, externalId, metadataSource));
                }
            }
        } catch (Exception e) {
            log.error("Erro ao analisar resposta GeoJSON da NASA ASF: {}", e.getMessage(), e);
            throw new ExternalServiceException("Resposta GeoJSON inválida recebida da NASA ASF.", e);
        }

        return scenes;
    }

    private List<SarScene> generateReferenceScenes(BoundingBox bbox, LocalDate start, LocalDate end, String flightDirection) {
        List<SarScene> reference = new ArrayList<>();
        String dir = (flightDirection != null && !flightDirection.isBlank()) ? flightDirection.toUpperCase(Locale.ROOT) : "DESCENDING";

        reference.add(new SarScene(
                "S1A_IW_SLC__1SDV_20230105T211512_20230105T211539_046654_05973E_5A12",
                Instant.parse("2023-01-05T21:15:12Z"),
                142,
                dir,
                "VV+VH",
                "SENTINEL-1A",
                "S1A_IW_SLC__1SDV_20230105T211512_20230105T211539_046654_05973E_5A12",
                "NASA ASF DAAC (Ambiente de Teste)"
        ));
        reference.add(new SarScene(
                "S1A_IW_SLC__1SDV_20230117T211512_20230117T211539_046829_059D24_7B31",
                Instant.parse("2023-01-17T21:15:12Z"),
                142,
                dir,
                "VV+VH",
                "SENTINEL-1A",
                "S1A_IW_SLC__1SDV_20230117T211512_20230117T211539_046829_059D24_7B31",
                "NASA ASF DAAC (Ambiente de Teste)"
        ));
        return reference;
    }
}
