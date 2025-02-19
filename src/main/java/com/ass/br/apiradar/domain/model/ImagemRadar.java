package com.ass.br.apiradar.domain.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ImagemRadar {

    private String id;
    private String origem;
    private LocalDate dataColeta;
    private byte[] dadosBrutos; // Dados da imagem SAR
    private boolean processada;
}
