package com.ass.br.apiradar.shared.domain.exceptions;

/**
 * Lançada quando a geometria informada é sintática ou topologicamente inválida.
 */
public class InvalidGeometryException extends DomainException {

    public static final String ERROR_CODE = "ERR_INVALID_GEOMETRY";

    public InvalidGeometryException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidGeometryException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
