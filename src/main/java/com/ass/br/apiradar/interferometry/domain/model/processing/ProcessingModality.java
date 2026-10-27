package com.ass.br.apiradar.interferometry.domain.model.processing;

import java.util.List;

/**
 * Modalidades oficialmente suportadas para processamento interferométrico Sentinel-1 InSAR
 * pelo provedor científico NASA ASF HyP3 (OpenAPI specification e hyp3-sdk).
 * <p>
 * Fonte oficial de custos em créditos e parâmetros:
 * https://hyp3-docs.asf.alaska.edu/using/credits/
 * https://hyp3-docs.asf.alaska.edu/using/api/
 * https://hyp3-docs.asf.alaska.edu/hyp3-docs/guides/burst_insar_product_guide/
 */
public enum ProcessingModality {

    /**
     * Processamento de cena completa Sentinel-1 IW SLC (~3 subswaths IW1-IW3, ~27 bursts por cena)
     * utilizando o processador científico GAMMA.
     * <p>
     * Job Type oficial HyP3: INSAR_GAMMA
     * Looks suportados: '20x4' (80m) ou '10x2' (40m)
     * Créditos oficiais:
     * - 20x4 (80m): 10 créditos
     * - 10x2 (40m): 15 créditos
     */
    FULL_SCENE(
            "INSAR_GAMMA",
            "Cena Completa (Full-Scene IW SLC - GAMMA)",
            "hyp3-gamma:v2.0",
            10,
            List.of("20x4", "10x2"),
            "Processa todos os 3 subswaths da cena SAR com software GAMMA. Gera coerência, deslocamento LOS e fase desdobrada (MCF)."
    ),

    /**
     * Processamento Single-Burst InSAR focado no burst elementar que intercepta a AOI
     * utilizando o processador científico ISCE2.
     * <p>
     * Job Type oficial HyP3: INSAR_ISCE_BURST
     * Looks suportados: '20x4' (80m), '10x2' (40m) ou '5x1' (20m)
     * Créditos oficiais: 1 crédito (qualquer opção de looks para 1 par de bursts)
     */
    BURST_INSAR(
            "INSAR_ISCE_BURST",
            "Single-Burst InSAR (ISCE2)",
            "hyp3-isce:v1.0",
            1,
            List.of("20x4", "10x2", "5x1"),
            "Processa um único par de bursts Sentinel-1 canônicos (~85x20 km) cobrindo diretamente a AOI. Custo: 1 crédito."
    ),

    /**
     * Processamento Multi-Burst InSAR combinando sequência contígua de 2 a 15 bursts
     * utilizando o processador científico ISCE2 com fusão (merge) automática.
     * <p>
     * Job Type oficial HyP3: INSAR_ISCE_MULTI_BURST
     * Looks suportados: '20x4' (80m), '10x2' (40m) ou '5x1' (20m)
     * Créditos oficiais (tabela ASF HyP3):
     * - 20x4: 1 a 4 pares = 1 crédito; 5 a 12 pares = 5 créditos; 13 a 15 pares = 10 créditos
     * - 10x2: 1 a 3 pares = 1 crédito; 4 a 9 pares = 5 créditos; 10 a 15 pares = 10 créditos
     * - 5x1: 1 par = 1 crédito; 2 pares = 5 créditos; 3 pares = 10 créditos; ... (+5 por par)
     */
    MULTI_BURST(
            "INSAR_ISCE_MULTI_BURST",
            "Multi-Burst InSAR (ISCE2)",
            "hyp3-isce:v1.0",
            1,
            List.of("20x4", "10x2", "5x1"),
            "Funde e processa múltiplos bursts adjacentes contíguos com ISCE2 para cobertura metropolitana integral (100% da AOI)."
    );

    private final String officialJobType;
    private final String displayName;
    private final String defaultPipeline;
    private final int baseCredits;
    private final List<String> supportedLooks;
    private final String description;

    ProcessingModality(
            String officialJobType,
            String displayName,
            String defaultPipeline,
            int baseCredits,
            List<String> supportedLooks,
            String description
    ) {
        this.officialJobType = officialJobType;
        this.displayName = displayName;
        this.defaultPipeline = defaultPipeline;
        this.baseCredits = baseCredits;
        this.supportedLooks = supportedLooks;
        this.description = description;
    }

    public String getOfficialJobType() {
        return officialJobType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultPipeline() {
        return defaultPipeline;
    }

    public int getBaseCredits() {
        return baseCredits;
    }

    public List<String> getSupportedLooks() {
        return supportedLooks;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Calcula o custo exato em créditos com base na tabela oficial da NASA ASF HyP3.
     * Fonte: https://hyp3-docs.asf.alaska.edu/using/credits/#credit-cost-table
     *
     * @param burstPairCount Quantidade de pares de burst (para single-burst = 1; para multi-burst = número de bursts)
     * @param looks Resolução/Looks selecionados ('20x4', '10x2' ou '5x1')
     * @return Custo exato em créditos segundo o contrato oficial do HyP3
     */
    public int calculateOfficialCredits(int burstPairCount, String looks) {
        String effectiveLooks = (looks != null && !looks.isBlank()) ? looks : "20x4";

        switch (this) {
            case FULL_SCENE:
                if ("10x2".equalsIgnoreCase(effectiveLooks)) {
                    return 15;
                }
                return 10;

            case BURST_INSAR:
                // Single-burst custa exatamente 1 crédito independentemente do multilooking
                return 1;

            case MULTI_BURST:
                int pairs = Math.max(1, burstPairCount);
                if ("5x1".equalsIgnoreCase(effectiveLooks)) {
                    // Tabela HyP3 para 5x1: 1 par = 1; 2 pares = 5; 3 = 10; 4 = 15; ...
                    if (pairs == 1) return 1;
                    return (pairs - 1) * 5;
                } else if ("10x2".equalsIgnoreCase(effectiveLooks)) {
                    // Tabela HyP3 para 10x2: 1-3 pares = 1; 4-9 pares = 5; 10-15 = 10
                    if (pairs <= 3) return 1;
                    if (pairs <= 9) return 5;
                    return 10;
                } else {
                    // Tabela HyP3 para 20x4: 1-4 pares = 1; 5-12 pares = 5; 13-15 = 10
                    if (pairs <= 4) return 1;
                    if (pairs <= 12) return 5;
                    return 10;
                }

            default:
                return baseCredits;
        }
    }
}
