package com.ass.br.apiradar.domain.model;

import lombok.Data;

@Data
public class AnaliseSolo {

    private String id;
    private String imagemRadarId;
    private double deslocamentoDetectado; // Em centímetros
    private String status; // "Estável", "Risco Médio", "Risco Alto"
}
