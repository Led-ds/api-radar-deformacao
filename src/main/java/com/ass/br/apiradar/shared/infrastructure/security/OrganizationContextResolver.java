package com.ass.br.apiradar.shared.infrastructure.security;

import com.ass.br.apiradar.shared.domain.exceptions.DomainException;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Componente de resolução e governança do identificador da Organização (Multi-Tenancy).
 * Isolamento de Segurança:
 * - Em ambientes locais/dev/test, permite uso da organização demonstrativa do piloto.
 * - Em ambiente de produção, veda a aceitação cega de headers arbitrários sem autenticação formal.
 */
@Component
public class OrganizationContextResolver {

    private static final Logger log = LoggerFactory.getLogger(OrganizationContextResolver.class);

    private final boolean allowDemoTenant;
    private final UUID defaultDemoTenantId;

    public OrganizationContextResolver(
            @Value("${radar.security.allow-demo-tenant:false}") boolean allowDemoTenant,
            @Value("${radar.security.demo-tenant-id:d3b07384-d113-40a2-9721-a53b2169b821}") String demoTenantId) {
        this.allowDemoTenant = allowDemoTenant;
        this.defaultDemoTenantId = UUID.fromString(demoTenantId);
    }

    /**
     * Resolve o OrganizationId para a requisição corrente.
     *
     * @param headerTenantId valor recebido no cabeçalho X-Organization-Id (opcional)
     * @return OrganizationId validado
     */
    public OrganizationId resolveOrganizationId(UUID headerTenantId) {
        if (allowDemoTenant) {
            if (headerTenantId != null) {
                log.debug("[DEV_SECURITY] Utilizando X-Organization-Id fornecido em modo demonstrativo: {}", headerTenantId);
                return OrganizationId.of(headerTenantId);
            }
            log.debug("[DEV_SECURITY] Utilizando organização demonstrativa padrão do piloto: {}", defaultDemoTenantId);
            return OrganizationId.of(defaultDemoTenantId);
        }

        // Em ambiente produtivo, rejeita a injeção arbitrária de tenant sem token JWT validado
        if (headerTenantId != null) {
            log.error("[PROD_SECURITY_VIOLATION] Tentativa de sobreposição de tenant via header X-Organization-Id em ambiente sem perfil demonstrativo.");
            throw new UnauthorizedTenantAccessException("Cabeçalho X-Organization-Id não autorizado em ambiente produtivo. Autenticação via JWT requerida.");
        }

        throw new UnauthorizedTenantAccessException("Nenhum contexto de organização autenticado disponível.");
    }

    public static class UnauthorizedTenantAccessException extends DomainException {
        public UnauthorizedTenantAccessException(String message) {
            super("ERR_UNAUTHORIZED_TENANT", message);
        }
    }
}
