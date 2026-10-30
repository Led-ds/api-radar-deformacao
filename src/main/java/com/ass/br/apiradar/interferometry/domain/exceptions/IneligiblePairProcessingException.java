package com.ass.br.apiradar.interferometry.domain.exceptions;

/**
 * Exceção lançada ao tentar construir ou submeter requisição de processamento
 * para um par interferométrico que não possui o estado ELIGIBLE_FOR_SUBMISSION.
 */
public class IneligiblePairProcessingException extends RuntimeException {

    public IneligiblePairProcessingException(String message) {
        super(message);
    }

    public IneligiblePairProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
