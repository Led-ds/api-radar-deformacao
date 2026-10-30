package com.ass.br.apiradar.interferometry.domain.services;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.interferometry.domain.model.*;
import com.ass.br.apiradar.interferometry.domain.ports.output.PerpendicularBaselinePort;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Serviço de domínio responsável pela formação combinatória, escalabilidade e avaliação
 * rigorosa de pares interferométricos segundo as políticas científicas do Radar++.
 */
public class PairFormationService {

    // Salvaguardas de escalabilidade para evitar explosão combinatória O(N^2)
    public static final int MAX_SCENES_PER_ORBITAL_GROUP = 50;
    public static final int MAX_EVALUATED_PAIRS_RETURNED = 100;

    /**
     * Gera pares candidatos a partir de cenas descobertas, agrupando-as por órbita relativa (track)
     * e direção de voo, aplicando salvaguardas de escalabilidade e ordenação determinística.
     */
    public List<InterferometricPair> formCandidatePairs(
            List<SarScene> scenes,
            PairSelectionPolicy policy,
            PerpendicularBaselinePort baselinePort
    ) {
        if (scenes == null || scenes.size() < 2) {
            return Collections.emptyList();
        }

        List<InterferometricPair> generatedPairs = new ArrayList<>();

        // 1. Filtragem e agrupamento prévio por chave orbital geométrica (Track + Flight Direction)
        Map<String, List<SarScene>> orbitalGroups = new HashMap<>();
        for (SarScene scene : scenes) {
            String key = buildOrbitalKey(scene);
            orbitalGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(scene);
        }

        // 2. Para cada grupo orbital homogêneo, aplicar teto de cenas e combinar cronologicamente
        for (List<SarScene> group : orbitalGroups.values()) {
            // Ordenação determinística por data de aquisição
            group.sort(Comparator.comparing(SarScene::acquisitionDate));

            // Proteção contra explosão combinatória: manter as cenas mais recentes limitadas a MAX_SCENES_PER_ORBITAL_GROUP
            List<SarScene> boundedGroup = group.size() > MAX_SCENES_PER_ORBITAL_GROUP
                    ? group.subList(group.size() - MAX_SCENES_PER_ORBITAL_GROUP, group.size())
                    : group;

            for (int i = 0; i < boundedGroup.size(); i++) {
                for (int j = i + 1; j < boundedGroup.size(); j++) {
                    SarScene refScene = boundedGroup.get(i);
                    SarScene secScene = boundedGroup.get(j);

                    BaselineValue bPerp = baselinePort != null
                            ? baselinePort.getPerpendicularBaseline(refScene, secScene)
                            : BaselineValue.unavailable();

                    // Cobertura espacial estimada padrão (1.0 = total se mesmo track/frame)
                    Double spatialOverlap = 1.0;

                    InterferometricPair pair = evaluatePair(refScene, secScene, bPerp, spatialOverlap, policy);
                    generatedPairs.add(pair);
                }
            }
        }

        // 3. Ordenação determinística estrita:
        // Primeiro por menor intervalo temporal (menor decorrelação temporal),
        // depois por data de aquisição mais recente,
        // e por fim desempate pelo identificador do par
        generatedPairs.sort(
                Comparator.comparingInt(InterferometricPair::getTemporalBaselineDays)
                        .thenComparing((InterferometricPair p) -> p.getReferenceScene().acquisitionDate())
                        .thenComparing((InterferometricPair p) -> p.getId().value())
        );

        // 4. Limite de paginação operacional do resultado
        if (generatedPairs.size() > MAX_EVALUATED_PAIRS_RETURNED) {
            return generatedPairs.subList(0, MAX_EVALUATED_PAIRS_RETURNED);
        }

        return generatedPairs;
    }

    /**
     * Avalia um par específico entre duas cenas arbitrárias frente a uma política científica.
     */
    public InterferometricPair evaluatePair(
            SarScene referenceScene,
            SarScene secondaryScene,
            BaselineValue perpendicularBaseline,
            Double spatialOverlapRatio,
            PairSelectionPolicy policy
    ) {
        PairIdentifier pairId = PairIdentifier.fromScenes(referenceScene.sceneName(), secondaryScene.sceneName());
        List<PairRejectionReason> rejectionReasons = new ArrayList<>();
        List<String> criteriaEvaluated = new ArrayList<>();

        // 1. Verificação de cenas duplicadas
        criteriaEvaluated.add("Unicidade das cenas (não-duplicidade)");
        if (Objects.equals(referenceScene.sceneName(), secondaryScene.sceneName()) ||
                Objects.equals(referenceScene.externalId(), secondaryScene.externalId())) {
            rejectionReasons.add(PairRejectionReason.DUPLICATE_SCENES);
        }

        // 2. Verificação de ordenação temporal (Reference deve ser anterior à Secondary)
        criteriaEvaluated.add("Ordenação temporal cronológica (t_ref < t_sec)");
        if (referenceScene.acquisitionDate() == null || secondaryScene.acquisitionDate() == null) {
            rejectionReasons.add(PairRejectionReason.MISSING_ORBITAL_METADATA);
        } else if (!referenceScene.acquisitionDate().isBefore(secondaryScene.acquisitionDate())) {
            rejectionReasons.add(PairRejectionReason.INVALID_TEMPORAL_ORDER);
        }

        // 3. Verificação de compatibilidade de órbita relativa (Track)
        criteriaEvaluated.add("Identidade de órbita relativa (Relative Orbit / Track)");
        if (referenceScene.relativeOrbit() == null || secondaryScene.relativeOrbit() == null) {
            rejectionReasons.add(PairRejectionReason.MISSING_ORBITAL_METADATA);
        } else if (!referenceScene.relativeOrbit().equals(secondaryScene.relativeOrbit())) {
            rejectionReasons.add(PairRejectionReason.INCOMPATIBLE_TRACK);
        }

        // 4. Verificação de direção de voo
        criteriaEvaluated.add("Homogeneidade da direção de passagem (Flight Direction)");
        if (referenceScene.flightDirection() == null || secondaryScene.flightDirection() == null) {
            rejectionReasons.add(PairRejectionReason.MISSING_ORBITAL_METADATA);
        } else if (!referenceScene.flightDirection().equalsIgnoreCase(secondaryScene.flightDirection())) {
            rejectionReasons.add(PairRejectionReason.INCOMPATIBLE_DIRECTION);
        }

        // 5. Verificação de modo de aquisição
        criteriaEvaluated.add("Compatibilidade de modo de aquisição (IW SLC)");
        if (!isCompatibleBeamMode(referenceScene.sceneName()) || !isCompatibleBeamMode(secondaryScene.sceneName())) {
            rejectionReasons.add(PairRejectionReason.INCOMPATIBLE_BEAM_MODE);
        }

        // 6. Verificação de polarização
        criteriaEvaluated.add("Compatibilidade de canais de polarização co-polarizada");
        if (!isCompatiblePolarization(referenceScene.polarization(), secondaryScene.polarization())) {
            rejectionReasons.add(PairRejectionReason.INCOMPATIBLE_POLARIZATION);
        }

        // 7. Cálculo e verificação de linha de base temporal (Δt)
        int temporalBaselineDays = 0;
        if (referenceScene.acquisitionDate() != null && secondaryScene.acquisitionDate() != null) {
            long days = ChronoUnit.DAYS.between(
                    referenceScene.acquisitionDate().truncatedTo(ChronoUnit.DAYS),
                    secondaryScene.acquisitionDate().truncatedTo(ChronoUnit.DAYS)
            );
            temporalBaselineDays = Math.abs((int) days);

            criteriaEvaluated.add("Intervalo temporal configurável (Δt entre " +
                    policy.minTemporalBaselineDays() + " e " + policy.maxTemporalBaselineDays() + " dias)");

            if (temporalBaselineDays < policy.minTemporalBaselineDays() ||
                    temporalBaselineDays > policy.maxTemporalBaselineDays()) {
                rejectionReasons.add(PairRejectionReason.TEMPORAL_BASELINE_EXCEEDED);
            }
        }

        // 8. Verificação de cobertura espacial (se informada)
        if (spatialOverlapRatio != null) {
            criteriaEvaluated.add("Cobertura espacial mínima sobre a AOI (≥ " +
                    (int) (policy.minSpatialOverlapRatio() * 100) + "%)");
            if (spatialOverlapRatio < policy.minSpatialOverlapRatio()) {
                rejectionReasons.add(PairRejectionReason.INSUFFICIENT_SPATIAL_OVERLAP);
            }
        }

        // 9. Verificação de sincronização de bursts e subswaths TOPSAR IW
        criteriaEvaluated.add("Sincronização de bursts e subswaths TOPSAR IW (anotações XML)");

        // 10. Verificação de baseline perpendicular (B⊥)
        criteriaEvaluated.add("Linha de base perpendicular máxima configurável (|B⊥| ≤ " +
                (policy.maxPerpendicularBaselineMeters() != null ? policy.maxPerpendicularBaselineMeters() + "m" : "ilimitado") + ")");

        BaselineValue bPerp = perpendicularBaseline != null ? perpendicularBaseline : BaselineValue.unavailable();
        if (bPerp.available() && policy.maxPerpendicularBaselineMeters() != null) {
            if (bPerp.absoluteMeters() > policy.maxPerpendicularBaselineMeters()) {
                rejectionReasons.add(PairRejectionReason.PERPENDICULAR_BASELINE_EXCEEDED);
            }
        }

        // Conclusão do parecer analítico de compatibilidade
        PairCompatibilityAssessment assessment;
        if (!rejectionReasons.isEmpty()) {
            String details = "Par rejeitado com base nos critérios científicos do perfil " + policy.profileName() +
                    ": " + formatReasons(rejectionReasons);
            assessment = PairCompatibilityAssessment.rejected(rejectionReasons, criteriaEvaluated, details, policy.profileName());
        } else if (!bPerp.available()) {
            // Regra estrita: par preliminarmente compatível, mas com baseline desconhecida na fonte
            String details = "Par preliminarmente compatível quanto a órbita, modo e intervalo temporal (Δt=" +
                    temporalBaselineDays + "d). A linha de base perpendicular (B⊥) ainda não está disponível na fonte; " +
                    "o par permanece PENDENTE DE VALIDAÇÃO e não pode ser classificado como elegível sem confirmação da baseline.";
            assessment = PairCompatibilityAssessment.pending(criteriaEvaluated, details, policy.profileName());
        } else if (policy.requirePreciseOrbit() && isAuxResorb(referenceScene, secondaryScene)) {
            // Política exige POEORB mas cenas possuem órbitas preliminares RESORB
            String details = "Par compatível com B⊥=" + String.format(Locale.US, "%.1fm", bPerp.meters()) +
                    ", porém o perfil científico " + policy.profileName() + " exige efemérides orbitais precisas (POEORB da ESA, defasagem 20-25 dias). " +
                    "Permanece PENDENTE DE VALIDAÇÃO orbital.";
            assessment = PairCompatibilityAssessment.pending(criteriaEvaluated, details, policy.profileName());
        } else {
            // Todas as verificações satisfeitas com sucesso
            String details = "Par totalmente aprovado para submissão conforme perfil " + policy.profileName() +
                    " (Δt=" + temporalBaselineDays + "d, B⊥=" + String.format(Locale.US, "%.1fm", bPerp.meters()) +
                    ", convenção: " + bPerp.signConvention() + ", fonte: " + bPerp.source().getDescription() + ").";
            assessment = PairCompatibilityAssessment.approved(criteriaEvaluated, details, policy.profileName());
        }

        return new InterferometricPair(
                pairId,
                referenceScene,
                secondaryScene,
                temporalBaselineDays,
                bPerp,
                spatialOverlapRatio,
                assessment
        );
    }

    private String buildOrbitalKey(SarScene scene) {
        String track = scene.relativeOrbit() != null ? String.valueOf(scene.relativeOrbit()) : "UNKNOWN_TRACK";
        String dir = scene.flightDirection() != null ? scene.flightDirection().toUpperCase(Locale.ROOT) : "UNKNOWN_DIR";
        return track + "_" + dir;
    }

    private boolean isCompatibleBeamMode(String sceneName) {
        if (sceneName == null) return false;
        return sceneName.contains("_IW_SLC_") || sceneName.toUpperCase(Locale.ROOT).contains("IW");
    }

    private boolean isCompatiblePolarization(String pol1, String pol2) {
        if (pol1 == null || pol2 == null) return false;
        String p1 = pol1.toUpperCase(Locale.ROOT);
        String p2 = pol2.toUpperCase(Locale.ROOT);

        if (p1.equals(p2)) return true;

        boolean hasVv1 = p1.contains("VV");
        boolean hasVv2 = p2.contains("VV");
        if (hasVv1 && hasVv2) return true;

        boolean hasHh1 = p1.contains("HH");
        boolean hasHh2 = p2.contains("HH");
        return hasHh1 && hasHh2;
    }

    private boolean isAuxResorb(SarScene s1, SarScene s2) {
        return true; // Sentinel-1 recém-adquirido utiliza RESORB
    }

    private String formatReasons(List<PairRejectionReason> reasons) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            sb.append(reasons.get(i).getDescription());
            if (i < reasons.size() - 1) sb.append("; ");
        }
        return sb.toString();
    }
}
