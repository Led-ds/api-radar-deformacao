package com.ass.br.apiradar.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeformacaoRequestDto {
    private double latitude;
    private double longitude;
    private double deslocamento;
    private String risco;
}
