package com.ass.br.apiradar.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnaliseResultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String classificacao; // "Estável" ou "Instável"
    private double confianca; // Probabilidade da classificação

    @OneToOne
    @JoinColumn(name = "imagem_radar_id")
    private ImagemRadar imagemRadar;
}
