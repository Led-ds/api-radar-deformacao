package com.ass.br.apiradar.shared.domain.model;

import com.ass.br.apiradar.shared.domain.exceptions.MaxAreaExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricAreaTest {

    @Test
    @DisplayName("Cria com sucesso área métrica positiva")
    void shouldCreatePositiveMetricArea() {
        MetricArea area = new MetricArea(35.4219);
        assertThat(area.valueKm2()).isEqualTo(35.4219);
        assertThat(area.rounded(2)).isEqualTo(35.42);
    }

    @Test
    @DisplayName("Rejeita valores de área métrica negativos ou zerados")
    void shouldRejectNonPositiveArea() {
        assertThatThrownBy(() -> new MetricArea(0.0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MetricArea(-12.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Dispara MaxAreaExceededException quando o valor ultrapassa o teto")
    void shouldThrowWhenExceedingLimit() {
        MetricArea area = new MetricArea(55.20);
        assertThatThrownBy(() -> area.validateMaxAllowed(50.0))
                .isInstanceOf(MaxAreaExceededException.class)
                .hasMessageContaining("excede o teto contratual de 50.00 km²");
    }
}
