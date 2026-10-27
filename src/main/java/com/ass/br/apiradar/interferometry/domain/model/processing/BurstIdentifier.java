package com.ass.br.apiradar.interferometry.domain.model.processing;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value Object imutável que representa um identificador canônico de burst Sentinel-1 SLC IW
 * em conformidade estrita com o padrão ESA/ASF exigido pela API NASA HyP3 (OpenAPI schema).
 * <p>
 * Padrão oficial HyP3: ^S1_\d{6}_IW[1-3]_\d{8}T\d{6}_[VH]{2}_[0-9A-F]{4}-BURST$
 * Exemplo real de Maceió: S1_018319_IW1_20230104T080254_VV_604A-BURST (43 caracteres).
 * <p>
 * Rejeita categoricamente identificadores fictícios ou sintéticos (ex.: t009_018590_iw2)
 * ou nomes de cenas SLC completas mascaradas de burst.
 */
public final class BurstIdentifier implements Serializable, Comparable<BurstIdentifier> {

    private static final String CANONICAL_REGEX = "^S1_(\\d{6})_(IW[1-3])_(\\d{8}T\\d{6})_([VH]{2})_([0-9A-F]{4})-BURST$";
    private static final Pattern CANONICAL_PATTERN = Pattern.compile(CANONICAL_REGEX);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final String value;
    private final int relativeBurstId;
    private final String subswath;
    private final Instant acquisitionTime;
    private final String polarization;
    private final String productCode;

    private BurstIdentifier(
            String value,
            int relativeBurstId,
            String subswath,
            Instant acquisitionTime,
            String polarization,
            String productCode
    ) {
        this.value = value;
        this.relativeBurstId = relativeBurstId;
        this.subswath = subswath;
        this.acquisitionTime = acquisitionTime;
        this.polarization = polarization;
        this.productCode = productCode;
    }

    /**
     * Cria e valida um BurstIdentifier a partir da string canônica oficial.
     *
     * @param burstString Identificador a ser validado
     * @return BurstIdentifier validado
     * @throws IllegalArgumentException se o formato não obedecer estritamente à especificação oficial do HyP3
     */
    public static BurstIdentifier of(String burstString) {
        if (burstString == null || burstString.isBlank()) {
            throw new IllegalArgumentException("Identificador de burst não pode ser nulo ou vazio.");
        }

        String trimmed = burstString.trim();
        Matcher matcher = CANONICAL_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format(
                    "Identificador de burst '%s' não segue o contrato canônico oficial da NASA ASF HyP3. " +
                            "Formato exigido: S1_RRRRRR_IW[1-3]_YYYYMMDDTHHMMSS_PP_CCCC-BURST (ex: S1_018319_IW1_20230104T080254_VV_604A-BURST). " +
                            "Identificadores simplificados ou fictícios (como 't009_018590_iw2') violam a validação OpenAPI do provedor.",
                    burstString
            ));
        }

        int relId = Integer.parseInt(matcher.group(1));
        String subswath = matcher.group(2);
        String rawTime = matcher.group(3);
        String pol = matcher.group(4);
        String code = matcher.group(5);

        Instant acqInstant;
        try {
            LocalDateTime ldt = LocalDateTime.parse(rawTime, TIME_FORMATTER);
            acqInstant = ldt.toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao analisar timestamp do burst: " + rawTime, e);
        }

        return new BurstIdentifier(trimmed, relId, subswath, acqInstant, pol, code);
    }

    /**
     * Retorna se a string informada é um identificador canônico válido.
     */
    public static boolean isValidCanonical(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        return CANONICAL_PATTERN.matcher(candidate.trim()).matches();
    }

    /**
     * Valida se este burst é geometricamente compatível com outro para formar um par interferométrico
     * (mesmo relativeBurstId, mesmo subswath e mesma polarização).
     */
    public boolean isGeometricallyCompatibleWith(BurstIdentifier other) {
        if (other == null) {
            return false;
        }
        return this.relativeBurstId == other.relativeBurstId
                && Objects.equals(this.subswath, other.subswath)
                && Objects.equals(this.polarization, other.polarization);
    }

    public String value() {
        return value;
    }

    public int relativeBurstId() {
        return relativeBurstId;
    }

    public String subswath() {
        return subswath;
    }

    public Instant acquisitionTime() {
        return acquisitionTime;
    }

    public String polarization() {
        return polarization;
    }

    public String productCode() {
        return productCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BurstIdentifier that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(BurstIdentifier o) {
        return this.value.compareTo(o.value);
    }
}
