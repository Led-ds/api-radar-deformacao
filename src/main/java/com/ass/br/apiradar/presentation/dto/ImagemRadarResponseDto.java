package com.ass.br.apiradar.presentation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ImagemRadarResponseDto {
    private Long id;
    private String origem;
    private LocalDate dataColeta;
    private boolean processada;
}
