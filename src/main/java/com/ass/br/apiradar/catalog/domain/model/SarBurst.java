package com.ass.br.apiradar.catalog.domain.model;

import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Representa um burst elementar Sentinel-1 IW SLC proveniente do catálogo oficial da NASA ASF.
 * Contém metadados canônicos, identificador relativo, subswath, footprint geométrico WGS84 e polarização.
 */
public record SarBurst(
        String canonicalId,
        int relativeBurstId,
        String subswath,
        Instant acquisitionTime,
        int relativeOrbit,
        String flightDirection,
        String polarization,
        PolygonGeometry footprint,
        String metadataSource
) implements Serializable, Comparable<SarBurst> {

    public SarBurst {
        Objects.requireNonNull(canonicalId, "canonicalId cannot be null");
        Objects.requireNonNull(subswath, "subswath cannot be null");
        Objects.requireNonNull(acquisitionTime, "acquisitionTime cannot be null");
        Objects.requireNonNull(flightDirection, "flightDirection cannot be null");
        Objects.requireNonNull(polarization, "polarization cannot be null");
    }

    @Override
    public int compareTo(SarBurst o) {
        if (o == null) return 1;
        int cmp = Integer.compare(this.relativeBurstId, o.relativeBurstId);
        if (cmp != 0) return cmp;
        return this.acquisitionTime.compareTo(o.acquisitionTime);
    }
}
