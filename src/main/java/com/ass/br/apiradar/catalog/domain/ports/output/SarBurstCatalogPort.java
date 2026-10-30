package com.ass.br.apiradar.catalog.domain.ports.output;

import com.ass.br.apiradar.catalog.domain.model.SarBurst;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta de saída (SPI) para consultas a metadados de bursts elementares Sentinel-1
 * junto ao catálogo oficial da NASA ASF DAAC.
 */
public interface SarBurstCatalogPort {

    /**
     * Consulta bursts Sentinel-1 IW SLC que interceptam a AOI e obedecem aos parâmetros orbitais.
     *
     * @param aoiGeometry     Geometria exata da Área de Interesse
     * @param bbox            Bounding box delimitadora
     * @param acquisitionDate Data de aquisição (UTC)
     * @param relativeOrbit   Órbita relativa / track
     * @param flightDirection Direção orbital (ASCENDING ou DESCENDING)
     * @param polarization    Polarização requerida (ex: VV)
     * @return Lista de bursts canônicos reais com footprints
     */
    List<SarBurst> searchBursts(
            PolygonGeometry aoiGeometry,
            BoundingBox bbox,
            LocalDate acquisitionDate,
            int relativeOrbit,
            String flightDirection,
            String polarization
    );
}
