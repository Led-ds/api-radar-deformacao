package com.ass.br.apiradar.shared.infrastructure.web;

import com.ass.br.apiradar.shared.domain.exceptions.DomainException;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.ass.br.apiradar.shared.domain.exceptions.ExternalServiceException;
import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import com.ass.br.apiradar.shared.domain.exceptions.MaxAreaExceededException;
import com.ass.br.apiradar.shared.infrastructure.security.OrganizationContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tratamento global de erros em conformidade estrita com a RFC 7807 (Problem Details for HTTP APIs).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_BASE_URI = "https://api.radarplusplus.com/errors/";

    @ExceptionHandler(MaxAreaExceededException.class)
    public ProblemDetail handleMaxAreaExceeded(MaxAreaExceededException ex, HttpServletRequest request) {
        log.warn("Limite de área excedido: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(ERROR_BASE_URI + "area-exceeded"));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now().toString());

        List<Map<String, String>> invalidParams = new ArrayList<>();
        Map<String, String> param = new HashMap<>();
        param.put("name", "geometryWkt");
        param.put("reason", String.format("Área calculada %.2f km2 excede limite maxAllowedAreaKm2 %.2f km2",
                ex.getActualAreaKm2(), ex.getMaxAllowedAreaKm2()));
        invalidParams.add(param);
        problem.setProperty("invalidParams", invalidParams);

        return problem;
    }

    @ExceptionHandler(InvalidGeometryException.class)
    public ProblemDetail handleInvalidGeometry(InvalidGeometryException ex, HttpServletRequest request) {
        log.warn("Geometria inválida: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create(ERROR_BASE_URI + "invalid-geometry"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento ou parâmetro inválido: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create(ERROR_BASE_URI + "bad-argument"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "ERR_INVALID_ARGUMENT");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.info("Entidade não encontrada: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(ERROR_BASE_URI + "not-found"));
        problem.setTitle("Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handleExternalService(ExternalServiceException ex, HttpServletRequest request) {
        log.error("Erro em serviço externo: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setType(URI.create(ERROR_BASE_URI + "bad-gateway"));
        problem.setTitle("Bad Gateway");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Falha de validação sintática em requisição: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parâmetros de requisição inválidos.");
        problem.setType(URI.create(ERROR_BASE_URI + "validation-error"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "ERR_VALIDATION");
        problem.setProperty("timestamp", Instant.now().toString());

        List<Map<String, String>> invalidParams = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> item = new HashMap<>();
            item.put("name", fieldError.getField());
            item.put("reason", fieldError.getDefaultMessage());
            invalidParams.add(item);
        }
        problem.setProperty("invalidParams", invalidParams);

        return problem;
    }

    @ExceptionHandler(OrganizationContextResolver.UnauthorizedTenantAccessException.class)
    public ProblemDetail handleUnauthorizedTenant(OrganizationContextResolver.UnauthorizedTenantAccessException ex, HttpServletRequest request) {
        log.warn("Acesso de tenant não autorizado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create(ERROR_BASE_URI + "unauthorized-tenant"));
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleGenericDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("Exceção de domínio não mapeada especificamente: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(ERROR_BASE_URI + "domain-error"));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Erro interno não tratado: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado.");
        problem.setType(URI.create(ERROR_BASE_URI + "internal-server-error"));
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "ERR_INTERNAL_ERROR");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
