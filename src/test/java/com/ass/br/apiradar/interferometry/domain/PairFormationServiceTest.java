package com.ass.br.apiradar.interferometry.domain;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.interferometry.domain.model.*;
import com.ass.br.apiradar.interferometry.domain.services.PairFormationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Domínio: PairFormationService e Regras Científicas de Pares InSAR")
class PairFormationServiceTest {

    private PairFormationService service;
    private PairSelectionPolicy maceioPolicy;
    private PairSelectionPolicy generalPolicy;
    private PairSelectionPolicy strictPolicy;

    @BeforeEach
    void setUp() {
        service = new PairFormationService();
        maceioPolicy = PairSelectionPolicy.tropicalUrbanMaceio();
        generalPolicy = PairSelectionPolicy.generalInSar();
        strictPolicy = PairSelectionPolicy.strictScientific();
    }

    private SarScene createScene(String name, String dateIso, int track, String dir, String pol) {
        return new SarScene(
                name,
                Instant.parse(dateIso),
                track,
                dir,
                pol,
                "SENTINEL-1A",
                name,
                "NASA ASF DAAC"
        );
    }

    @Test
    @DisplayName("Cenário 1: Rejeitar par com órbitas relativas (tracks) incompatíveis")
    void shouldRejectPairWithIncompatibleTracks() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 142, "DESCENDING", "VV+VH");

        InterferometricPair pair = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(45.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.REJECTED, pair.getStatus());
        assertTrue(pair.getAssessment().rejectionReasons().contains(PairRejectionReason.INCOMPATIBLE_TRACK));
        assertTrue(pair.getAssessment().details().contains("Órbitas relativas (tracks) divergentes"));
    }

    @Test
    @DisplayName("Cenário 2: Rejeitar par com direções de passagem divergentes (ASCENDING vs DESCENDING)")
    void shouldRejectPairWithIncompatibleFlightDirections() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "ASCENDING", "VV");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV");

        InterferometricPair pair = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(45.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.REJECTED, pair.getStatus());
        assertTrue(pair.getAssessment().rejectionReasons().contains(PairRejectionReason.INCOMPATIBLE_DIRECTION));
    }

    @Test
    @DisplayName("Cenário 3: Rejeitar par com modos de aquisição incompatíveis (ex: Stripmap SM vs IW SLC)")
    void shouldRejectPairWithIncompatibleBeamModes() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV");
        SarScene s2 = createScene("S1A_S3_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV");

        InterferometricPair pair = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(45.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.REJECTED, pair.getStatus());
        assertTrue(pair.getAssessment().rejectionReasons().contains(PairRejectionReason.INCOMPATIBLE_BEAM_MODE));
    }

    @Test
    @DisplayName("Cenário 4: Rejeitar par com polarizações incompatíveis (ex: HH com VV)")
    void shouldRejectPairWithIncompatiblePolarizations() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "HH");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV");

        InterferometricPair pair = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(45.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.REJECTED, pair.getStatus());
        assertTrue(pair.getAssessment().rejectionReasons().contains(PairRejectionReason.INCOMPATIBLE_POLARIZATION));
    }

    @Test
    @DisplayName("Cenário 5: Rejeitar cenas idênticas (duplicatas)")
    void shouldRejectDuplicateScenes() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV+VH");

        InterferometricPair pair = service.evaluatePair(
                s1, s1,
                BaselineValue.calculated(0.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.REJECTED, pair.getStatus());
        assertTrue(pair.getAssessment().rejectionReasons().contains(PairRejectionReason.DUPLICATE_SCENES));
    }

    @Test
    @DisplayName("Cenário 6: Rejeitar par com ordenação temporal invertida ou intervalo excedido")
    void shouldRejectInvalidTemporalBaseline() {
        // Data invertida (secundária antes da referência)
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230125T210000_1", "2023-01-25T21:00:00Z", 96, "DESCENDING", "VV");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230101T210000_2", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV");

        InterferometricPair invertedPair = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(30.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );
        assertEquals(PairEligibilityStatus.REJECTED, invertedPair.getStatus());
        assertTrue(invertedPair.getAssessment().rejectionReasons().contains(PairRejectionReason.INVALID_TEMPORAL_ORDER));

        // Intervalo temporal que excede o limite da política Maceió (12 a 24 dias, aqui = 36 dias)
        SarScene s3 = createScene("S1A_IW_SLC__1SDV_20230206T210000_3", "2023-02-06T21:00:00Z", 96, "DESCENDING", "VV");
        InterferometricPair exceededPair = service.evaluatePair(
                s2, s3,
                BaselineValue.calculated(30.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );
        assertEquals(PairEligibilityStatus.REJECTED, exceededPair.getStatus());
        assertTrue(exceededPair.getAssessment().rejectionReasons().contains(PairRejectionReason.TEMPORAL_BASELINE_EXCEEDED));
    }

    @Test
    @DisplayName("Cenário 7: Rejeitar quando metadados orbitais essenciais estiverem ausentes")
    void shouldRejectWhenOrbitalMetadataMissing() {
        SarScene sceneNoTrack = new SarScene(
                "S1A_IW_SLC__1SDV_20230101T210000_1",
                Instant.parse("2023-01-01T21:00:00Z"),
                null, // Track ausente
                "DESCENDING",
                "VV",
                "SENTINEL-1A",
                "S1A_1",
                "NASA ASF"
        );
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV");

        InterferometricPair pair = service.evaluatePair(
                sceneNoTrack, s2,
                BaselineValue.calculated(30.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.REJECTED, pair.getStatus());
        assertTrue(pair.getAssessment().rejectionReasons().contains(PairRejectionReason.MISSING_ORBITAL_METADATA));
    }

    @Test
    @DisplayName("Cenário 8: Baseline desconhecida DEVE transitar para PENDING_VALIDATION (nunca elegível)")
    void shouldKeepPendingValidationWhenBaselineUnavailable() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV+VH");

        // Baseline explicitamente indisponível na fonte
        BaselineValue unavailableBaseline = BaselineValue.unavailable();

        InterferometricPair pair = service.evaluatePair(
                s1, s2,
                unavailableBaseline,
                1.0,
                maceioPolicy
        );

        // Invariante de ouro: não pode ser classificado como ELIGIBLE_FOR_SUBMISSION!
        assertNotEquals(PairEligibilityStatus.ELIGIBLE_FOR_SUBMISSION, pair.getStatus());
        assertEquals(PairEligibilityStatus.PENDING_VALIDATION, pair.getStatus());
        assertTrue(pair.getAssessment().details().contains("PENDENTE DE VALIDAÇÃO"));
        assertFalse(pair.getPerpendicularBaseline().available());
        assertEquals(BaselineSource.UNAVAILABLE, pair.getPerpendicularBaseline().source());
    }

    @Test
    @DisplayName("Cenário 9: Rejeitar par com cobertura espacial insuficiente (< 80%)")
    void shouldRejectPairWithInsufficientSpatialCoverage() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV+VH");

        InterferometricPair pair = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(45.0, BaselineSource.ASF_BASELINE_SERVICE),
                0.65, // 65% de cobertura da AOI (< 80% exigido pela política Maceió)
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.REJECTED, pair.getStatus());
        assertTrue(pair.getAssessment().rejectionReasons().contains(PairRejectionReason.INSUFFICIENT_SPATIAL_OVERLAP));
    }

    @Test
    @DisplayName("Cenário 10: Comparação de políticas metodológicas (Maceió vs Geral)")
    void shouldApplyDifferentMethodologicalPoliciesCorrectly() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230206T210000_2", "2023-02-06T21:00:00Z", 96, "DESCENDING", "VV+VH");
        // Δt = 36 dias, B_perp = 180m

        // Na política restrita Maceió (máx 24 dias, máx 150m): rejeitado
        InterferometricPair pairMaceio = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(180.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                maceioPolicy
        );
        assertEquals(PairEligibilityStatus.REJECTED, pairMaceio.getStatus());

        // Na política geral (máx 48 dias, máx 200m): aprovado!
        InterferometricPair pairGeneral = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(180.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                generalPolicy
        );
        assertEquals(PairEligibilityStatus.ELIGIBLE_FOR_SUBMISSION, pairGeneral.getStatus());
    }

    @Test
    @DisplayName("Cenário 11: Rastreabilidade completa dos critérios executados e justificativas")
    void shouldProvideFullTraceabilityOfCriteriaAndJustifications() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV+VH");

        InterferometricPair pair = service.evaluatePair(
                s1, s2,
                BaselineValue.calculated(35.2, BaselineSource.ASF_BASELINE_SERVICE),
                0.95,
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.ELIGIBLE_FOR_SUBMISSION, pair.getStatus());
        assertNotNull(pair.getAssessment().evaluatedAt());
        assertEquals("TROPICAL_URBAN_MACEIO", pair.getAssessment().policyProfileUsed());
        assertFalse(pair.getAssessment().criteriaEvaluated().isEmpty());
        assertTrue(pair.getAssessment().details().contains("Par totalmente aprovado para submissão"));
        assertEquals("Metadados fornecidos pela API de baseline da NASA ASF", pair.getPerpendicularBaseline().source().getDescription());
    }

    @Test
    @DisplayName("Cenário 12: Formação de pares a partir de coleção de cenas descoberta")
    void shouldFormCandidatePairsFromSceneCollection() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene s3 = createScene("S1A_IW_SLC__1SDV_20230125T210000_3", "2023-01-25T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene sDiffTrack = createScene("S1A_IW_SLC__1SDV_20230105T210000_4", "2023-01-05T21:00:00Z", 142, "DESCENDING", "VV+VH");

        List<SarScene> scenes = List.of(s1, s2, s3, sDiffTrack);

        List<InterferometricPair> pairs = service.formCandidatePairs(
                scenes,
                maceioPolicy,
                (ref, sec) -> BaselineValue.calculated(50.0, BaselineSource.ASF_BASELINE_SERVICE)
        );

        // 3 cenas no track 96 geram C(3,2) = 3 combinações. A cena no track 142 não tem par.
        assertEquals(3, pairs.size());
        assertEquals(12, pairs.get(0).getTemporalBaselineDays());
        assertEquals(12, pairs.get(1).getTemporalBaselineDays());
        assertEquals(24, pairs.get(2).getTemporalBaselineDays());
    }

    @Test
    @DisplayName("Cenário 13: Aplicar teto operacional estrito do servidor em políticas customizadas")
    void shouldEnforceServerOperationalLimitsOnCustomPolicy() {
        // Usuário tenta enviar valores exorbitantes: 500 dias, 2000m B_perp, 1% de overlap
        PairSelectionPolicy customPolicy = PairSelectionPolicy.createCustom(
                2,
                500,
                2000.0,
                0.01,
                false
        );

        // Verifica que o servidor limitou rigidamente
        assertEquals(PairSelectionPolicy.SERVER_MIN_TEMPORAL_DAYS, customPolicy.minTemporalBaselineDays()); // 6 dias
        assertEquals(PairSelectionPolicy.SERVER_MAX_TEMPORAL_DAYS, customPolicy.maxTemporalBaselineDays()); // 90 dias
        assertEquals(PairSelectionPolicy.SERVER_MAX_PERPENDICULAR_METERS, customPolicy.maxPerpendicularBaselineMeters()); // 300m
        assertEquals(PairSelectionPolicy.SERVER_MIN_SPATIAL_OVERLAP, customPolicy.minSpatialOverlapRatio()); // 50%
    }

    @Test
    @DisplayName("Cenário 14: Convenção de sinal da baseline perpendicular (sinal negativo com magnitude aceitável)")
    void shouldEvaluateNegativeSignPerpendicularBaselineCorrectly() {
        SarScene s1 = createScene("S1A_IW_SLC__1SDV_20230101T210000_1", "2023-01-01T21:00:00Z", 96, "DESCENDING", "VV+VH");
        SarScene s2 = createScene("S1A_IW_SLC__1SDV_20230113T210000_2", "2023-01-13T21:00:00Z", 96, "DESCENDING", "VV+VH");

        // B_perp negativo (-95.5m), com magnitude 95.5m <= 150m da política Maceió
        BaselineValue negBaseline = BaselineValue.calculated(-95.5, BaselineSource.ASF_BASELINE_SERVICE);

        InterferometricPair pair = service.evaluatePair(
                s1, s2,
                negBaseline,
                1.0,
                maceioPolicy
        );

        assertEquals(PairEligibilityStatus.ELIGIBLE_FOR_SUBMISSION, pair.getStatus());
        assertEquals(-95.5, pair.getPerpendicularBaseline().meters());
        assertEquals(95.5, pair.getPerpendicularBaseline().absoluteMeters());
    }

    @Test
    @DisplayName("Cenário 15: Escalabilidade e limitação combinatória para grande volume de cenas")
    void shouldLimitCombinatorialExplosionWhenSceneVolumeIsLarge() {
        // Cria 65 cenas sintéticas em ordem cronológica
        java.util.List<SarScene> largeCollection = new java.util.ArrayList<>();
        Instant baseTime = Instant.parse("2023-01-01T21:00:00Z");
        for (int i = 0; i < 65; i++) {
            largeCollection.add(createScene(
                    "S1A_IW_SLC__1SDV_20230101T210000_" + i,
                    baseTime.plus(java.time.Duration.ofDays(i * 12)).toString(),
                    96,
                    "DESCENDING",
                    "VV+VH"
            ));
        }

        // Sem salvaguarda, C(65, 2) = 2.080 pares.
        // Com o limite operacional MAX_EVALUATED_PAIRS_RETURNED = 100, deve retornar exatamente 100 pares.
        List<InterferometricPair> pairs = service.formCandidatePairs(
                largeCollection,
                generalPolicy,
                (ref, sec) -> BaselineValue.calculated(50.0, BaselineSource.ASF_BASELINE_SERVICE)
        );

        assertNotNull(pairs);
        assertTrue(pairs.size() <= PairFormationService.MAX_EVALUATED_PAIRS_RETURNED);
        assertEquals(PairFormationService.MAX_EVALUATED_PAIRS_RETURNED, pairs.size());

        // Ordenação determinística estrita: os primeiros pares devem ter o menor intervalo temporal (12 dias)
        assertEquals(12, pairs.get(0).getTemporalBaselineDays());
    }
}
