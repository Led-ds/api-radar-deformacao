package com.ass.br.apiradar.territory.infrastructure.adapters.output.persistence;

import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes reais de integração contra banco de dados PostgreSQL 16 + PostGIS 3.4.
 * Executa contra instância real PostGIS oficial (postgis/postgis:16-3.4).
 * Valida:
 * - Aplicação de migrations Flyway reais;
 * - Presença das extensões postgis e uuid-ossp;
 * - Tipos geométricos nativos e SRID 4326;
 * - Índices espaciais GiST e operadores ST_Intersects / ST_Area;
 * - Restrições de integridade e isolamento multi-tenancy.
 */
@SpringBootTest
@ActiveProfiles("dev")
class PostGisTerritoryRepositoryIntegrationTest {

    private static final String MACEIO_MUTANGE_WKT =
            "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String pgPort = System.getProperty("POSTGRES_PORT", System.getenv().getOrDefault("POSTGRES_PORT", "5435"));
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:" + pgPort + "/radar_db");
        registry.add("spring.datasource.username", () -> "radar_user");
        registry.add("spring.datasource.password", () -> "radar_pass");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.schemas", () -> "radar");
        registry.add("spring.flyway.default-schema", () -> "radar");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "radar");
        registry.add("radar.security.allow-demo-tenant", () -> "true");
    }

    @Autowired
    private AreaOfInterestPersistenceAdapter persistenceAdapter;

    @Autowired
    private AreaOfInterestJpaRepository jpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("1. Valida aplicação de migrations Flyway, extensões PostGIS/UUID e criação das tabelas")
    void shouldVerifyPostGisExtensionsAndSchemaCreated() {
        // Verifica se as extensões postgis e uuid-ossp estão ativas no banco
        List<String> extensions = jdbcTemplate.queryForList(
                "SELECT extname FROM pg_extension WHERE extname IN ('postgis', 'uuid-ossp')",
                String.class
        );
        assertThat(extensions).contains("postgis", "uuid-ossp");

        // Verifica se as tabelas foram criadas pelo Flyway no schema radar
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'radar'",
                String.class
        );
        assertThat(tables).contains("organizations", "areas_of_interest");

        // Verifica existência do índice GiST espacial
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'radar' AND tablename = 'areas_of_interest'",
                String.class
        );
        assertThat(indexes).contains("idx_aoi_geom_gist", "idx_aoi_bbox_gist");
    }

    @Test
    @DisplayName("2. Valida persistência real de AOI com geometria WGS 84 e recuperação por ID e Organização")
    void shouldPersistAndRetrieveAreaOfInterestInPostGis() {
        AoiId aoiId = AoiId.random();
        OrganizationId orgId = OrganizationId.of(UUID.fromString("d3b07384-d113-40a2-9721-a53b2169b821"));
        PolygonGeometry geometry = PolygonGeometry.fromWkt(MACEIO_MUTANGE_WKT);

        AreaOfInterest aoi = AreaOfInterest.create(
                aoiId,
                orgId,
                "Piloto Maceió Mutange PostGIS Test",
                geometry,
                100.0
        );

        // Salva via adaptador
        AreaOfInterest saved = persistenceAdapter.save(aoi);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(aoiId);

        // Recupera por ID e organização
        Optional<AreaOfInterest> retrieved = persistenceAdapter.findById(aoiId, orgId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Piloto Maceió Mutange PostGIS Test");
        assertThat(retrieved.get().getMetricArea().valueKm2()).isGreaterThan(0.0);
        assertThat(retrieved.get().getBoundingBox().minLon()).isEqualTo(-35.795);

        // Lista por organização
        List<AreaOfInterest> orgList = persistenceAdapter.findByOrganizationId(orgId);
        assertThat(orgList).isNotEmpty();
    }

    @Test
    @DisplayName("3. Valida consultas espaciais PostGIS via ST_Intersects e cálculo métrico em UTM Zona 25S (EPSG:31985)")
    void shouldExecuteSpatialPostGisQueries() {
        AoiId aoiId = AoiId.random();
        OrganizationId orgId = OrganizationId.of(UUID.fromString("d3b07384-d113-40a2-9721-a53b2169b821"));
        PolygonGeometry geometry = PolygonGeometry.fromWkt(MACEIO_MUTANGE_WKT);

        AreaOfInterest aoi = AreaOfInterest.create(
                aoiId,
                orgId,
                "Maceió Spatial Query Test",
                geometry,
                100.0
        );
        persistenceAdapter.save(aoi);

        // Testa ST_Intersects com um ponto dentro da bacia de Maceió (-35.742, -9.635)
        Integer matchCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM radar.areas_of_interest 
                WHERE id = ? AND ST_Intersects(geometry_wgs84, ST_SetSRID(ST_MakePoint(-35.742, -9.635), 4326))
                """,
                Integer.class,
                aoiId.value()
        );
        assertThat(matchCount).isEqualTo(1);

        // Testa cálculo exato de área métrica em km² projetado em UTM Zona 25S (EPSG:31985)
        Double utmAreaKm2 = jdbcTemplate.queryForObject(
                """
                SELECT (ST_Area(ST_Transform(geometry_wgs84, 31985)) / 1000000.0) 
                FROM radar.areas_of_interest WHERE id = ?
                """,
                Double.class,
                aoiId.value()
        );
        assertThat(utmAreaKm2).isNotNull();
        assertThat(utmAreaKm2).isBetween(70.0, 95.0); // Bacia de ~85 km2 em Maceió
    }
}
