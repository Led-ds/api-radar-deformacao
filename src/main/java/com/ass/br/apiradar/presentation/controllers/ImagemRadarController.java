package com.ass.br.apiradar.presentation.controllers;

import com.ass.br.apiradar.domain.services.ImagemRadarService;
import com.ass.br.apiradar.domain.services.StorageService;
import com.ass.br.apiradar.presentation.dto.ImagemRadarResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/imagens")
@RequiredArgsConstructor
@Slf4j
public class ImagemRadarController {

    private final ImagemRadarService imagemRadarService;
    private final StorageService storageService;

    @GetMapping("/deformacao/{deformacaoId}")
    public ResponseEntity<List<ImagemRadarResponseDto>> buscarImagensPorDeformacao(@PathVariable Long deformacaoId) {
        List<ImagemRadarResponseDto> response = imagemRadarService.buscarImagensPorDeformacao(deformacaoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/id/{imagemId}")
    public ResponseEntity<byte[]> baixarImagem(@PathVariable Long imagemId) {
        return imagemRadarService.baixarImagem(imagemId)
                .map(imagem -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM) // Tipo genérico para qualquer imagem
                        .body(imagem))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/s3/deformacao/{deformacaoId}")
    public ResponseEntity<List<String>> buscarUrlS3(@PathVariable Long deformacaoId) {
        List<String> urls = imagemRadarService.buscarURLImagensNoS3(deformacaoId);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/download/nome/{nomeImagem}")
    public ResponseEntity<byte[]> getImagem(@PathVariable String nomeImagem) {
        try {
            byte[] imagemBytes = storageService.downloadFile(nomeImagem);

            // Detectar tipo de mídia
            String contentType = Files.probeContentType(Paths.get(nomeImagem));
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(imagemBytes);

        } catch (Exception e) {
            log.error("Erro ao buscar imagem: {}", nomeImagem, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
