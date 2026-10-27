package com.ass.br.apiradar.catalog.domain.ports.output;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta de saída (SPI) para consultas a catálogos satelitais SAR (NASA ASF / ESA CDSE).
 */
public interface SarCatalogPort {

    List<SarScene> searchScenes(BoundingBox bbox, LocalDate startDate, LocalDate endDate, String flightDirection);
}
