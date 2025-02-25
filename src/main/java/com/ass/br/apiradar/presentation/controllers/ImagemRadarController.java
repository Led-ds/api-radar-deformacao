package com.ass.br.apiradar.presentation.controllers;

import com.ass.br.apiradar.domain.model.ImagemRadar;
import com.ass.br.apiradar.domain.services.ImagemRadarService;
import com.ass.br.apiradar.presentation.dto.ImagemRadarResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/imagens")
@RequiredArgsConstructor
public class ImagemRadarController {

    private final ImagemRadarService imagemRadarService;

    @GetMapping("/{deformacaoId}")
    public ResponseEntity<List<ImagemRadarResponseDto>> buscarImagensPorDeformacao(@PathVariable Long deformacaoId) {
        List<ImagemRadarResponseDto> response = imagemRadarService.buscarImagensPorDeformacao(deformacaoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{imagemId}")
    public ResponseEntity<byte[]> baixarImagem(@PathVariable Long imagemId) {
        return imagemRadarService.baixarImagem(imagemId)
                .map(imagem -> ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imagem))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/s3/{deformacaoId}")
    public ResponseEntity<List<String>> buscarUrlS3(@PathVariable Long deformacaoId) {
        List<String> urls = imagemRadarService.buscarURLImagensNoS3(deformacaoId);
        return ResponseEntity.ok(urls);
    }

}

