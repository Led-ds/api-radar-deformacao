package com.ass.br.apiradar.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnaliseResultado {
    private String imagemRadarId;
    private String classificacao;
    private Object imagemProcessada;
}
