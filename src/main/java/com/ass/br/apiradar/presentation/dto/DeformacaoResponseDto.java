package com.ass.br.apiradar.presentation.dto;

import com.ass.br.apiradar.domain.model.Deformacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeformacaoResponseDto {

    private Long id;
    private double latitude;
    private double longitude;
    private double deslocamento;
    private String risco;
    private String status;

    public DeformacaoResponseDto(Deformacao deformacao) {
        this.id = deformacao.getId();
        this.latitude = deformacao.getLatitude();
        this.longitude = deformacao.getLongitude();
        this.deslocamento = deformacao.getDeslocamento();
        this.risco = deformacao.getRisco();
        this.status = "SUCCESS";
    }
}
