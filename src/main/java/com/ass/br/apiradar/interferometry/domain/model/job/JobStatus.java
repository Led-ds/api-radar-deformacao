package com.ass.br.apiradar.interferometry.domain.model.job;

/**
 * Estados do ciclo de vida de processamento de um job InSAR no cluster HyP3 / ISCE2.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    UNKNOWN;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }

    public boolean isSuccess() {
        return this == SUCCEEDED;
    }

    public boolean isFailure() {
        return this == FAILED;
    }
}
