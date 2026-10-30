package com.ass.br.apiradar.interferometry.domain.ports.output;

import com.ass.br.apiradar.interferometry.domain.model.InterferometricPair;
import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingParameters;
import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingRequest;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessorEligibilityResult;

import java.util.List;

/**
 * Porta de saída (SPI) desacoplada para interação com processadores científicos InSAR.
 * Define o contrato para validação prévia de elegibilidade, estimativa de custos,
 * geração de pedidos em modo dry-run e submissão operacional protegida por barreira financeira.
 */
public interface InSarProcessorPort {

    /**
     * Valida se o par interferométrico e seus metadados orbitais/bursts atendem
     * aos requisitos específicos de ingestão do processador científico.
     */
    ProcessorEligibilityResult validateProcessorEligibility(InterferometricPair pair, List<String> burstIds);

    /**
     * Retorna a lista de modalidades oficialmente suportadas pelo processador.
     */
    List<ProcessingModality> getSupportedModalities();

    /**
     * Calcula a estimativa de créditos do provedor com base na modalidade e quantidade de bursts.
     */
    int estimateCredits(ProcessingModality modality, List<String> burstIds);

    /**
     * Constrói a representação auditável da requisição de processamento em modo DRY-RUN,
     * sem disparar nenhuma chamada de submissão na rede e sem consumir créditos.
     */
    InSarProcessingRequest buildDryRunRequest(
            String aoiId,
            InterferometricPair pair,
            ProcessingModality modality,
            List<String> burstIds,
            InSarProcessingParameters parameters,
            String correlationId
    );

    /**
     * Submete a requisição para execução no processador científico.
     * Deve ser bloqueado compulsoriamente por barreira de segurança financeira quando
     * a execução real estiver desabilitada por política ou ambiente.
     */
    InSarProcessingRequest submitProcessingRequest(InSarProcessingRequest request);
}
