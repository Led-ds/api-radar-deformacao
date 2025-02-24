package com.ass.br.apiradar.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deformacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double latitude;

    private double longitude;

    private double deslocamento;

    private String risco;

    @OneToMany(mappedBy = "deformacao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImagemRadar> imagens;
}

