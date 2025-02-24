package com.ass.br.apiradar.presentation.controllers;

import com.ass.br.apiradar.application.usecases.ProcessarImagemSAR;
import com.ass.br.apiradar.domain.services.DeformacaoService;
import com.ass.br.apiradar.presentation.dto.DeformacaoRequestDto;
import com.ass.br.apiradar.presentation.dto.DeformacaoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/analise")
@RequiredArgsConstructor
public class DeformacaoController {

    private final DeformacaoService deformacaoService;
    private final ProcessarImagemSAR processarImagemSAR;

    @PostMapping("/deformacao")
    public ResponseEntity<DeformacaoResponseDto> criar(@RequestBody DeformacaoRequestDto request) throws IOException {
        DeformacaoResponseDto response = deformacaoService.registrarDeformacao(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/deformacoes")
    public ResponseEntity<List<DeformacaoResponseDto>> listar() {
        List<DeformacaoResponseDto> respostas = deformacaoService.listarDeformacoes();
        return ResponseEntity.ok(respostas);
    }

    @PostMapping("/processar-imagem/{imagemRadarId}")
    public ResponseEntity<Void> processarImagem(@PathVariable String imagemRadarId) {
        processarImagemSAR.executar(imagemRadarId);
        return ResponseEntity.ok().build();
    }
}
