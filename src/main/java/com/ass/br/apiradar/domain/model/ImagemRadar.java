package com.ass.br.apiradar.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagemRadar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String origem; // "NASA" ou "FASTAPI"
    private LocalDate dataColeta;

    @Lob // Para armazenar imagens como byte[]
    @Column(columnDefinition = "oid")
    private byte[] dadosBrutos;

    private String urlImagem; // URL da imagem no armazenamento externo

    private boolean processada;

    @ManyToOne
    @JoinColumn(name = "deformacao_id")
    private Deformacao deformacao;

    @OneToOne(mappedBy = "imagemRadar", cascade = CascadeType.ALL)
    private AnaliseResultado resultado;
}
