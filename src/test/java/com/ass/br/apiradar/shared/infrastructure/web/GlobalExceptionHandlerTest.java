package com.ass.br.apiradar.shared.infrastructure.web;

import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import com.ass.br.apiradar.shared.domain.exceptions.MaxAreaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/areas-of-interest");
    }

    @Test
    @DisplayName("Manipula MaxAreaExceededException retornando HTTP 422 com código ERR_AOI_AREA_EXCEEDED")
    void handleMaxAreaExceeded() {
        MaxAreaExceededException ex = new MaxAreaExceededException(68.40, 50.00);
        ProblemDetail problem = exceptionHandler.handleMaxAreaExceeded(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getTitle()).isEqualTo("Unprocessable Entity");
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties().get("code")).isEqualTo("ERR_AOI_AREA_EXCEEDED");
        assertThat(problem.getDetail()).contains("68.40 km²").contains("50.00 km²");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> invalidParams = (List<Map<String, String>>) problem.getProperties().get("invalidParams");
        assertThat(invalidParams).isNotEmpty();
        assertThat(invalidParams.get(0).get("name")).isEqualTo("geometryWkt");
    }

    @Test
    @DisplayName("Manipula InvalidGeometryException retornando HTTP 400 com código ERR_INVALID_GEOMETRY")
    void handleInvalidGeometry() {
        InvalidGeometryException ex = new InvalidGeometryException("Polígono com auto-interseção");
        ProblemDetail problem = exceptionHandler.handleInvalidGeometry(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getProperties().get("code")).isEqualTo("ERR_INVALID_GEOMETRY");
    }

    @Test
    @DisplayName("Manipula EntityNotFoundException retornando HTTP 404 com código ERR_ENTITY_NOT_FOUND")
    void handleEntityNotFound() {
        EntityNotFoundException ex = new EntityNotFoundException("AOI não encontrada");
        ProblemDetail problem = exceptionHandler.handleEntityNotFound(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Not Found");
        assertThat(problem.getProperties().get("code")).isEqualTo("ERR_ENTITY_NOT_FOUND");
    }
}
