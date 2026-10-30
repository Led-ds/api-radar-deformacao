package com.ass.br.apiradar.interferometry.domain.exceptions;

/**
 * Exceção de segurança financeira e governança.
 * Disparada compulsoriamente caso qualquer componente tente submeter processamento real
 * ou consumir créditos externos enquanto a barreira de execução estiver ativa.
 */
public class InSarExecutionBlockedException extends RuntimeException {

    public InSarExecutionBlockedException(String message) {
        super(message);
    }

    public InSarExecutionBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
