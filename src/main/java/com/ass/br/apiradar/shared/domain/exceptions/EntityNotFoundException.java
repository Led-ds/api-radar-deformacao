package com.ass.br.apiradar.shared.domain.exceptions;

/**
 * Lançada quando uma entidade de domínio requerida não é encontrada.
 */
public class EntityNotFoundException extends DomainException {

    public static final String ERROR_CODE = "ERR_ENTITY_NOT_FOUND";

    public EntityNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
