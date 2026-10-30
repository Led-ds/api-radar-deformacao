package com.ass.br.apiradar.interferometry.domain.model;

import java.io.Serializable;

/**
 * Representa uma política científica configurável e versionada para seleção e validação de pares InSAR.
 * Trata os valores iniciais como experimentais e adaptáveis por bioma, sensor e regime de deformação.
 * Impõe limites operacionais invioláveis do servidor para impedir que perfis customizados
 * contornem cotas ou provoquem descorrelação física total.
 */
public record PairSelectionPolicy(
        String profileName,
        String version,
        String description,
        int minTemporalBaselineDays,
        int maxTemporalBaselineDays,
        Double maxPerpendicularBaselineMeters,
        double minSpatialOverlapRatio,
        boolean requirePreciseOrbit,
        boolean experimental
) implements Serializable {

    // Limites operacionais absolutos do servidor (invioláveis)
    public static final int SERVER_MIN_TEMPORAL_DAYS = 6;
    public static final int SERVER_MAX_TEMPORAL_DAYS = 90;
    public static final double SERVER_MAX_PERPENDICULAR_METERS = 300.0;
    public static final double SERVER_MIN_SPATIAL_OVERLAP = 0.50; // Mínimo 50% de sobreposição

    public PairSelectionPolicy(
            String profileName,
            String version,
            String description,
            int minTemporalBaselineDays,
            int maxTemporalBaselineDays,
            Double maxPerpendicularBaselineMeters,
            double minSpatialOverlapRatio,
            boolean requirePreciseOrbit
    ) {
        this(profileName, version, description, minTemporalBaselineDays, maxTemporalBaselineDays,
                maxPerpendicularBaselineMeters, minSpatialOverlapRatio, requirePreciseOrbit, true);
    }

    public PairSelectionPolicy {
        if (profileName == null || profileName.isBlank()) {
            throw new IllegalArgumentException("Nome do perfil metodológico não pode ser vazio.");
        }
        if (minTemporalBaselineDays < 0) {
            throw new IllegalArgumentException("Intervalo temporal mínimo não pode ser negativo.");
        }
        if (maxTemporalBaselineDays < minTemporalBaselineDays) {
            throw new IllegalArgumentException("Intervalo temporal máximo não pode ser menor que o mínimo.");
        }
        if (minSpatialOverlapRatio < 0.0 || minSpatialOverlapRatio > 1.0) {
            throw new IllegalArgumentException("Razão de sobreposição espacial mínima deve estar entre 0.0 e 1.0.");
        }
    }

    /**
     * Perfil voltado para áreas urbanas e costeiras tropicais (ex.: Piloto Maceió/AL).
     * Alta vegetação e umidade demandam janelas temporais curtas (12 a 24 dias na banda C),
     * baseline perpendicular conservadora (<= 120m) e sobreposição espacial robusta (>= 85%)
     * para mitigar severa descorrelação temporal e geométrica combinada.
     */
    public static PairSelectionPolicy tropicalUrbanMaceio() {
        return new PairSelectionPolicy(
                "TROPICAL_URBAN_MACEIO",
                "1.1",
                "Perfil experimental calibrado para o Piloto Maceió (v1.1): controle de descorrelação temporal (12 a 24 dias), baseline perpendicular estrita de até 120m e sobreposição espacial mínima de 85%.",
                12,
                24,
                120.0,
                0.85,
                false,
                true
        );
    }

    /**
     * Perfil geral para Sentinel-1 IW SLC em áreas de estabilidade moderada.
     */
    public static PairSelectionPolicy generalInSar() {
        return new PairSelectionPolicy(
                "GENERAL_INSAR_S1",
                "1.0",
                "Perfil experimental padrão para Sentinel-1 IW SLC: janela temporal de até 48 dias e baseline perpendicular de até 200m.",
                12,
                48,
                200.0,
                0.70,
                false,
                true
        );
    }

    /**
     * Perfil científico estrito para séries temporais e validação geodésica com exigência de órbitas precisas.
     */
    public static PairSelectionPolicy strictScientific() {
        return new PairSelectionPolicy(
                "STRICT_SCIENTIFIC",
                "1.0",
                "Perfil científico estrito experimental: janela temporal de 6 a 36 dias, baseline de até 100m, sobreposição de 85% e obrigatoriedade de órbita precisa POEORB.",
                6,
                36,
                100.0,
                0.85,
                true,
                true
        );
    }

    /**
     * Fábrica segura para perfis customizados com aplicação de teto operacional do servidor.
     * Impede que requisições de clientes contornem as salvaguardas operacionais do sistema.
     */
    public static PairSelectionPolicy createCustom(
            Integer requestedMinDt,
            Integer requestedMaxDt,
            Double requestedMaxBPerp,
            Double requestedMinOverlap,
            Boolean requestedRequirePrecise
    ) {
        int minDt = requestedMinDt != null ? Math.max(SERVER_MIN_TEMPORAL_DAYS, requestedMinDt) : 12;
        int maxDt = requestedMaxDt != null ? Math.min(SERVER_MAX_TEMPORAL_DAYS, requestedMaxDt) : 48;
        if (minDt > maxDt) {
            minDt = maxDt;
        }

        Double maxBPerp = requestedMaxBPerp != null
                ? Math.min(SERVER_MAX_PERPENDICULAR_METERS, Math.max(20.0, requestedMaxBPerp))
                : 200.0;

        double minOverlap = requestedMinOverlap != null
                ? Math.max(SERVER_MIN_SPATIAL_OVERLAP, Math.min(1.0, requestedMinOverlap))
                : 0.80;

        boolean requirePrecise = Boolean.TRUE.equals(requestedRequirePrecise);

        return new PairSelectionPolicy(
                "CUSTOM_PROFILE",
                "1.0-custom",
                "Perfil customizado sob demanda e limitado pelas salvaguardas institucionais do servidor (máx " +
                        SERVER_MAX_TEMPORAL_DAYS + " dias, máx " + SERVER_MAX_PERPENDICULAR_METERS + "m B⊥).",
                minDt,
                maxDt,
                maxBPerp,
                minOverlap,
                requirePrecise,
                true
        );
    }
}
