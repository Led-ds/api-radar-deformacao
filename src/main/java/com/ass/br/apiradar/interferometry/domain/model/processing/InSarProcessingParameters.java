package com.ass.br.apiradar.interferometry.domain.model.processing;

import java.io.Serializable;

/**
 * Parâmetros científicos de configuração para execução do pipeline interferométrico.
 * Encapsula opções de multilooking, filtragem de ruído de fase (Goldstein),
 * modelo de elevação (DEM) e máscaras hidrológicas para mitigação de decorrelação.
 */
public record InSarProcessingParameters(
        String looks,
        boolean applyWaterMask,
        double filterStrength,
        int demMatchingResolutionMeters,
        boolean includeCoherence,
        boolean includeDisplacement,
        boolean includeUnwrappedPhase
) implements Serializable {

    public InSarProcessingParameters {
        if (looks == null || looks.isBlank()) {
            throw new IllegalArgumentException("Parâmetro de looks não pode ser nulo ou vazio (ex: '20x4', '10x2').");
        }
        if (filterStrength < 0.0 || filterStrength > 1.0) {
            throw new IllegalArgumentException("Força do filtro de fase (Goldstein) deve estar entre 0.0 e 1.0.");
        }
        if (demMatchingResolutionMeters <= 0) {
            throw new IllegalArgumentException("Resolução do DEM topográfico deve ser positiva em metros (ex: 30).");
        }
    }

    /**
     * Parâmetros calibrados para monitoramento urbano de subsidência em Maceió/AL.
     * Multilooking de 10x2 (~40m), filtro adaptativo 0.6 e máscara d'água mandatória
     * para isolar a Lagoa Mundaú e o Oceano Atlântico.
     */
    public static InSarProcessingParameters defaultForUrbanMaceio() {
        return new InSarProcessingParameters(
                "10x2",
                true,   // Máscara d'água fundamental para evitar desdobramento errôneo sobre a laguna
                0.6,    // Filtro Goldstein adaptativo moderado
                30,     // Cop-DEM 30m (GLO-30)
                true,   // Gera raster de coerência complexa
                true,   // Gera mapa de deslocamento milimétrico na LOS
                true    // Gera fase desdobrada (SNAPHU)
        );
    }

    /**
     * Parâmetros padrão para exploração rápida regional (resolução 80m).
     */
    public static InSarProcessingParameters standardRegional() {
        return new InSarProcessingParameters(
                "20x4",
                true,
                0.5,
                30,
                true,
                true,
                true
        );
    }
}
