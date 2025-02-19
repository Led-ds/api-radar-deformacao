package com.ass.br.apiradar.domain.services;

import com.ass.br.apiradar.domain.model.Deformacao;
import com.ass.br.apiradar.presentation.dto.DeformacaoRequestDto;
import com.ass.br.apiradar.presentation.dto.DeformacaoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrarDeformacao {

    private final AnaliseService analiseService;

    public DeformacaoResponseDto registrar(DeformacaoRequestDto request) {
        Deformacao deformacao = Deformacao.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deslocamento(request.getDeslocamento())
                .risco(request.getRisco())
                .build();

        Deformacao deformacaoSalva = analiseService.salvarDeformacao(deformacao);

        return new DeformacaoResponseDto(deformacaoSalva);
    }

    /**
     * Lista todas as deformações registradas.
     *
     * @return Lista de DTOs de resposta.
     */
    public List<DeformacaoResponseDto> listar() {
        return analiseService.buscarDeformacoes().stream()
                .map(DeformacaoResponseDto::new)
                .toList();
    }
}


