package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking;

import com.ass.br.apiradar.interferometry.domain.model.job.InSarProductFile;
import com.ass.br.apiradar.interferometry.domain.model.job.JobStatus;
import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarJobTrackingPort;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de infraestrutura para consulta e acompanhamento de jobs InSAR na API da NASA ASF HyP3.
 * Mantém registro thread-safe com índice de idempotência determinística e trata falhas de rede e autenticação.
 */
@Component
public class Hyp3JobTrackingAdapter implements InSarJobTrackingPort {

    private static final Logger log = LoggerFactory.getLogger(Hyp3JobTrackingAdapter.class);

    private final String hyp3ApiUrl;
    private final String username;
    private final String password;
    private final String bearerToken;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    // Registro concorrente local de auditoria e resolução de idempotência
    private final Map<String, TrackedInSarJob> jobRegistry = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyIndex = new ConcurrentHashMap<>();

    public Hyp3JobTrackingAdapter(
            @Value("${radar.processing.hyp3.api-url:https://hyp3-api.asf.alaska.edu}") String hyp3ApiUrl,
            @Value("${radar.processing.hyp3.username:}") String username,
            @Value("${radar.processing.hyp3.password:}") String password,
            @Value("${radar.processing.hyp3.bearer-token:}") String bearerToken,
            ObjectMapper objectMapper
    ) {
        this.hyp3ApiUrl = hyp3ApiUrl.replaceAll("/+$", "");
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password.trim() : "";
        this.bearerToken = bearerToken != null ? bearerToken.trim() : "";
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(this.hyp3ApiUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "RadarPlusPlus-InSAR-Orchestrator/1.0")
                .build();
    }

    public boolean hasCredentialsConfigured() {
        return (!username.isBlank() && !password.isBlank()) || !bearerToken.isBlank();
    }

    @Override
    public TrackedInSarJob registerOrUpdateJob(TrackedInSarJob job) {
        if (job == null || job.jobId() == null) {
            throw new IllegalArgumentException("Job e JobId não podem ser nulos.");
        }
        jobRegistry.put(job.jobId(), job);
        if (job.idempotencyKey() != null && !job.idempotencyKey().isBlank()) {
            idempotencyIndex.put(job.idempotencyKey(), job.jobId());
        }
        return job;
    }

    @Override
    public Optional<TrackedInSarJob> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String jobId = idempotencyIndex.get(idempotencyKey);
        if (jobId != null) {
            return Optional.ofNullable(jobRegistry.get(jobId));
        }
        return Optional.empty();
    }

    @Override
    public TrackedInSarJob getJobStatus(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("JobId não pode ser nulo ou vazio.");
        }

        TrackedInSarJob localJob = jobRegistry.get(jobId);

        // Se já alcançou status terminal com sucesso localmente, evita consulta externa desnecessária
        if (localJob != null && localJob.status().isSuccess() && localJob.downloaded()) {
            return localJob;
        }

        try {
            WebClient.RequestHeadersSpec<?> requestSpec = webClient.get()
                    .uri("/jobs/{jobId}", jobId);

            // Injeta autenticação segura sem expor segredos em logs
            if (!bearerToken.isBlank()) {
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            } else if (!username.isBlank() && !password.isBlank()) {
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            }

            String responseBody = requestSpec.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        if (clientResponse.statusCode().value() == 401 || clientResponse.statusCode().value() == 403) {
                            log.warn("[HYP3_AUTH_FAIL] Autenticação NASA Earthdata rejeitada para o job {}. Verifique se a aplicação ASF HyP3 está autorizada na conta Earthdata.", jobId);
                        } else if (clientResponse.statusCode().value() == 404) {
                            log.info("[HYP3_JOB_NOT_FOUND] Job {} não localizado no cluster HyP3.", jobId);
                        }
                        return clientResponse.createException();
                    })
                    .bodyToMono(String.class)
                    .block();

            if (responseBody != null && !responseBody.isBlank()) {
                TrackedInSarJob updated = parseHyp3JobResponse(responseBody, localJob);
                return registerOrUpdateJob(updated);
            }
        } catch (Exception ex) {
            log.warn("[HYP3_POLLING_WARN] Falha ao consultar status remoto do job {}: {}. Mantendo registro local.",
                    jobId, ex.getMessage());
        }

        if (localJob != null) {
            return localJob;
        }

        throw new EntityNotFoundException("Job InSAR com ID '" + jobId + "' não encontrado no registro local ou no cluster HyP3.");
    }

    public TrackedInSarJob parseHyp3JobResponse(String jsonString, TrackedInSarJob existingLocal) {
        try {
            JsonNode root = objectMapper.readTree(jsonString);

            // A resposta pode ser o objeto do job diretamente ou um array "jobs"
            JsonNode jobNode = root.has("jobs") && root.get("jobs").isArray() && !root.get("jobs").isEmpty()
                    ? root.get("jobs").get(0)
                    : root;

            String id = jobNode.hasNonNull("job_id") ? jobNode.get("job_id").asText() : (existingLocal != null ? existingLocal.jobId() : UUID.randomUUID().toString());
            String jobType = jobNode.hasNonNull("job_type") ? jobNode.get("job_type").asText() : (existingLocal != null ? existingLocal.jobType() : "INSAR_ISCE_BURST");
            String statusCode = jobNode.hasNonNull("status_code") ? jobNode.get("status_code").asText().toUpperCase(Locale.ROOT) : "UNKNOWN";

            JobStatus status = switch (statusCode) {
                case "PENDING", "SUBMITTED", "QUEUED" -> JobStatus.PENDING;
                case "RUNNING" -> JobStatus.RUNNING;
                case "SUCCEEDED", "SUCCESS" -> JobStatus.SUCCEEDED;
                case "FAILED", "ERROR" -> JobStatus.FAILED;
                default -> JobStatus.UNKNOWN;
            };

            Instant reqTime = jobNode.hasNonNull("request_time")
                    ? Instant.parse(jobNode.get("request_time").asText())
                    : (existingLocal != null ? existingLocal.requestTime() : Instant.now());

            Instant expTime = jobNode.hasNonNull("expiration_time")
                    ? Instant.parse(jobNode.get("expiration_time").asText())
                    : (existingLocal != null ? existingLocal.expirationTime() : null);

            List<InSarProductFile> files = new ArrayList<>();
            if (jobNode.has("files") && jobNode.get("files").isArray()) {
                for (JsonNode f : jobNode.get("files")) {
                    String filename = f.hasNonNull("filename") ? f.get("filename").asText() : "";
                    String url = f.hasNonNull("url") ? f.get("url").asText() : "";
                    Long size = f.hasNonNull("size") ? f.get("size").asLong() : null;
                    String sha256 = f.hasNonNull("sha256") ? f.get("sha256").asText() : null;
                    String type = InSarProductFile.inferFileType(filename);
                    files.add(new InSarProductFile(filename, url, size, sha256, type));
                }
            } else if (existingLocal != null) {
                files = existingLocal.files();
            }

            String failureReason = null;
            if (status == JobStatus.FAILED) {
                if (jobNode.hasNonNull("failure_reason")) {
                    failureReason = jobNode.get("failure_reason").asText();
                } else if (jobNode.hasNonNull("error_message")) {
                    failureReason = jobNode.get("error_message").asText();
                } else {
                    failureReason = "Falha reportada pelo cluster HyP3 durante a execução do job.";
                }
            }

            String logs = null;
            if (jobNode.hasNonNull("logs")) {
                logs = jobNode.get("logs").asText();
            }

            String pairId = existingLocal != null ? existingLocal.pairId() : "UNKNOWN_PAIR";
            String aoiId = existingLocal != null ? existingLocal.aoiId() : "UNKNOWN_AOI";
            String idempotencyKey = existingLocal != null ? existingLocal.idempotencyKey() : "";
            boolean downloaded = existingLocal != null && existingLocal.downloaded();
            String localPath = existingLocal != null ? existingLocal.localProductPath() : null;

            return new TrackedInSarJob(
                    id, idempotencyKey, pairId, aoiId, jobType, status,
                    reqTime, expTime, files, failureReason, logs,
                    downloaded, localPath
            );
        } catch (Exception e) {
            log.error("[HYP3_PARSE_ERROR] Erro ao deserializar resposta do job HyP3: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Resposta de job HyP3 inválida: " + e.getMessage(), e);
        }
    }
}
