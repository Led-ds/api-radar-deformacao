package com.ass.br.apiradar.catalog.infrastructure.adapters.output.external;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.shared.domain.exceptions.ExternalServiceException;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsfSearchClientAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BoundingBox bbox = new BoundingBox(-35.795, -9.680, -35.710, -9.600);

    @Test
    @DisplayName("Retorna cenas simuladas apenas quando mockEnabled=true explicitamente configurado")
    void shouldReturnMockScenesOnlyWhenMockEnabled() {
        AsfSearchClientAdapter adapter = new AsfSearchClientAdapter(
                "https://fake-url.invalid",
                true, // mockEnabled = true
                objectMapper
        );

        List<SarScene> scenes = adapter.searchScenes(bbox, LocalDate.now().minusDays(30), LocalDate.now(), "DESCENDING");

        assertThat(scenes).isNotEmpty();
        assertThat(scenes.get(0).relativeOrbit()).isEqualTo(142);
        assertThat(scenes.get(0).flightDirection()).isEqualTo("DESCENDING");
        assertThat(scenes.get(0).metadataSource()).contains("NASA ASF DAAC");
    }

    @Test
    @DisplayName("Lança ExternalServiceException quando mockEnabled=false e endpoint é inalcançável")
    void shouldThrowExternalServiceExceptionWhenOfflineAndMockDisabled() {
        AsfSearchClientAdapter adapter = new AsfSearchClientAdapter(
                "http://localhost:59999", // porta inalcançável
                false, // mockEnabled = false
                objectMapper
        );

        assertThatThrownBy(() -> adapter.searchScenes(
                bbox,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 3, 31),
                "DESCENDING"
        )).isInstanceOf(ExternalServiceException.class)
          .hasMessageContaining("Falha de comunicação ou timeout");
    }
}
