package com.ass.br.apiradar.shared.domain.exceptions;

/**
 * Lançada quando um serviço externo (como a API ASF da NASA) falha ou fica indisponível.
 */
public class ExternalServiceException extends DomainException {

    public static final String ERROR_CODE = "ERR_EXTERNAL_SERVICE";

    public ExternalServiceException(String message) {
        super(ERROR_CODE, message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
