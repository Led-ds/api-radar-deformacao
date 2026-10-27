package com.ass.br.apiradar.catalog.infrastructure.adapters.output.external;

import com.ass.br.apiradar.catalog.domain.model.SarBurst;
import com.ass.br.apiradar.catalog.domain.ports.output.SarBurstCatalogPort;
import com.ass.br.apiradar.shared.domain.exceptions.ExternalServiceException;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
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
import java.util.*;

/**
 * Adaptador de saída que consulta metadados reais de bursts Sentinel-1 IW SLC na API NASA ASF DAAC.
 * Suporta modo mock para ambientes de teste offline mantendo conformidade com footprints reais.
 */
@Component
public class AsfBurstClientAdapter implements SarBurstCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(AsfBurstClientAdapter.class);
    private static final int MAX_CACHE_ENTRIES = 500;
    private static final Duration CACHE_TTL = Duration.ofHours(2);

    private record CacheEntry<T>(T value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final boolean mockEnabled;
    private final Map<String, CacheEntry<List<SarBurst>>> burstCache =
            Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<List<SarBurst>>> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            });

    public AsfBurstClientAdapter(
            @Value("${radar.catalog.asf-base-url:https://api.daac.asf.alaska.edu}") String baseUrl,
            @Value("${radar.catalog.mock-enabled:false}") boolean mockEnabled,
            ObjectMapper objectMapper
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.mockEnabled = mockEnabled;
        this.objectMapper = objectMapper;
    }

    private List<SarBurst> getFromCache(String key) {
        synchronized (burstCache) {
            CacheEntry<List<SarBurst>> entry = burstCache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                burstCache.remove(key);
                return null;
            }
            return entry.value();
        }
    }

    private void putInCache(String key, List<SarBurst> value) {
        synchronized (burstCache) {
            burstCache.put(key, new CacheEntry<>(value, Instant.now().plus(CACHE_TTL)));
        }
    }

    @Override
    public List<SarBurst> searchBursts(
            PolygonGeometry aoiGeometry,
            BoundingBox bbox,
            LocalDate acquisitionDate,
            int relativeOrbit,
            String flightDirection,
            String polarization
    ) {
        if (mockEnabled) {
            log.info("[MOCK_BURST_ATIVO] Retornando bursts de referência para a data {}", acquisitionDate);
            return generateMockBursts(acquisitionDate, relativeOrbit, flightDirection, polarization);
        }

        if (acquisitionDate == null) {
            throw new IllegalArgumentException("Data de aquisição é obrigatória para busca de bursts.");
        }

        String cacheKey = acquisitionDate + "_" + relativeOrbit + "_" + flightDirection + "_" + polarization;
        List<SarBurst> cached = getFromCache(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            log.debug("[BURST_CACHE_HIT] Retornando {} bursts em cache para {}", cached.size(), cacheKey);
            return cached;
        }

        Instant start = acquisitionDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = acquisitionDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        String intersects = null;
        if (aoiGeometry != null && aoiGeometry.wkt() != null && aoiGeometry.wkt().length() < 1500) {
            intersects = aoiGeometry.wkt();
        }

        final String intersectsParam = intersects;

        log.info("Consultando bursts Sentinel-1 na NASA ASF para data={}, track={}, flightDirection={}, pol={}",
                acquisitionDate, relativeOrbit, flightDirection, polarization);

        try {
            String responseBody = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/services/search/param")
                                .queryParam("platform", "SENTINEL-1")
                                .queryParam("processingLevel", "BURST")
                                .queryParam("beamMode", "IW")
                                .queryParam("output", "geojson")
                                .queryParam("start", start.toString())
                                .queryParam("end", end.toString());

                        if (relativeOrbit > 0) {
                            uriBuilder.queryParam("relativeOrbit", relativeOrbit);
                        }
                        if (flightDirection != null && !flightDirection.isBlank() && !"ALL".equalsIgnoreCase(flightDirection)) {
                            uriBuilder.queryParam("flightDirection", flightDirection.toUpperCase(Locale.ROOT));
                        }
                        if (polarization != null && !polarization.isBlank() && !"ALL".equalsIgnoreCase(polarization)) {
                            String pol = polarization.contains("+") ? polarization.split("\\+")[0].trim() : polarization.trim();
                            uriBuilder.queryParam("polarization", pol);
                        }
                        if (intersectsParam != null) {
                            uriBuilder.queryParam("intersectsWith", intersectsParam);
                        } else if (bbox != null) {
                            String bboxStr = String.format(Locale.US, "%.5f,%.5f,%.5f,%.5f",
                                    bbox.minLon(), bbox.minLat(), bbox.maxLon(), bbox.maxLat());
                            uriBuilder.queryParam("bbox", bboxStr);
                        }

                        return uriBuilder.build();
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            List<SarBurst> parsed = parseAsfBurstGeoJson(responseBody);
            if (!parsed.isEmpty()) {
                putInCache(cacheKey, parsed);
            }
            return parsed;

        } catch (WebClientResponseException ex) {
            String errorMsg = String.format("A API NASA ASF retornou status HTTP %d (%s) na busca de bursts",
                    ex.getStatusCode().value(), ex.getStatusText());
            log.error("Erro HTTP ao consultar bursts ASF: {} - Detalhe: {}", errorMsg, ex.getResponseBodyAsString());
            throw new ExternalServiceException(errorMsg, ex);

        } catch (Exception ex) {
            String errorMsg = "Falha de comunicação ou timeout ao consultar bursts na NASA ASF: " + ex.getMessage();
            log.error("Exceção na chamada externa de bursts ASF: {}", errorMsg, ex);
            throw new ExternalServiceException(errorMsg, ex);
        }
    }

    private List<SarBurst> parseAsfBurstGeoJson(String geoJsonStr) {
        List<SarBurst> bursts = new ArrayList<>();
        if (geoJsonStr == null || geoJsonStr.isBlank()) {
            return bursts;
        }

        try {
            JsonNode root = objectMapper.readTree(geoJsonStr);
            JsonNode features = root.path("features");

            if (features.isArray()) {
                for (JsonNode feature : features) {
                    JsonNode props = feature.path("properties");
                    String sceneName = props.path("sceneName").asText(props.path("fileID").asText());
                    if (sceneName == null || sceneName.isBlank() || "UNKNOWN".equalsIgnoreCase(sceneName)) {
                        continue;
                    }

                    String startTimeStr = props.path("startTime").asText(Instant.now().toString());
                    Instant acquisitionTime = Instant.parse(startTimeStr);
                    int relativeOrbit = props.path("pathNumber").asInt(0);
                    String flightDirection = props.path("flightDirection").asText("DESCENDING");
                    String polarization = props.path("polarization").asText("VV");

                    JsonNode burstNode = props.path("burst");
                    int relativeBurstId = burstNode.path("relativeBurstID").asInt(0);
                    String subswath = burstNode.path("subswath").asText("IW1");

                    // Extrai geometria do footprint
                    PolygonGeometry footprint = null;
                    JsonNode geomNode = feature.path("geometry");
                    if (!geomNode.isMissingNode() && !geomNode.isNull()) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> geomMap = objectMapper.convertValue(geomNode, Map.class);
                            footprint = PolygonGeometry.fromGeoJson(geomMap);
                        } catch (Exception e) {
                            log.warn("Não foi possível construir PolygonGeometry para o burst {}: {}", sceneName, e.getMessage());
                        }
                    }

                    bursts.add(new SarBurst(
                            sceneName,
                            relativeBurstId,
                            subswath,
                            acquisitionTime,
                            relativeOrbit,
                            flightDirection,
                            polarization,
                            footprint,
                            "Alaska Satellite Facility DAAC (NASA ASF)"
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Erro ao analisar GeoJSON de bursts da NASA ASF: {}", e.getMessage(), e);
            throw new ExternalServiceException("Resposta GeoJSON inválida de bursts recebida da NASA ASF.", e);
        }

        return bursts;
    }

    private List<SarBurst> generateMockBursts(LocalDate date, int relativeOrbit, String flightDirection, String polarization) {
        List<SarBurst> mockList = new ArrayList<>();
        String dir = flightDirection != null && !flightDirection.isBlank() ? flightDirection : "DESCENDING";
        String pol = polarization != null && !polarization.isBlank() ? polarization : "VV";
        int track = relativeOrbit > 0 ? relativeOrbit : 9;

        // Footprints reais de Maceió
        String fp19 = "POLYGON((-35.173587 -9.754047, -35.572546 -9.66247, -35.942022 -9.577039, -35.987944 -9.780139, -35.617222 -9.861317, -35.21699 -9.948644, -35.173587 -9.754047))";
        String fp18 = "POLYGON((-35.136491 -9.58748, -35.535205 -9.496005, -35.904471 -9.410672, -35.950324 -9.613783, -35.579818 -9.694862, -35.179836 -9.782087, -35.136491 -9.58748))";

        if (LocalDate.of(2023, 1, 4).equals(date)) {
            mockList.add(new SarBurst(
                    "S1_018319_IW1_20230104T080254_VV_604A-BURST",
                    18319,
                    "IW1",
                    Instant.parse("2023-01-04T08:02:54Z"),
                    track,
                    dir,
                    pol,
                    PolygonGeometry.fromWkt(fp19),
                    "NASA ASF DAAC (Mock)"
            ));
            mockList.add(new SarBurst(
                    "S1_018318_IW1_20230104T080251_VV_604A-BURST",
                    18318,
                    "IW1",
                    Instant.parse("2023-01-04T08:02:51Z"),
                    track,
                    dir,
                    pol,
                    PolygonGeometry.fromWkt(fp18),
                    "NASA ASF DAAC (Mock)"
            ));
        } else if (LocalDate.of(2023, 1, 16).equals(date)) {
            mockList.add(new SarBurst(
                    "S1_018319_IW1_20230116T080253_VV_BF75-BURST",
                    18319,
                    "IW1",
                    Instant.parse("2023-01-16T08:02:53Z"),
                    track,
                    dir,
                    pol,
                    PolygonGeometry.fromWkt(fp19),
                    "NASA ASF DAAC (Mock)"
            ));
            mockList.add(new SarBurst(
                    "S1_018318_IW1_20230116T080250_VV_BF75-BURST",
                    18318,
                    "IW1",
                    Instant.parse("2023-01-16T08:02:50Z"),
                    track,
                    dir,
                    pol,
                    PolygonGeometry.fromWkt(fp18),
                    "NASA ASF DAAC (Mock)"
            ));
        }

        return mockList;
    }
}
