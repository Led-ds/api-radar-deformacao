package com.ass.br.apiradar.domain.services;

import com.ass.br.apiradar.domain.model.Deformacao;
import com.ass.br.apiradar.domain.model.ImagemRadar;
import com.ass.br.apiradar.domain.repositories.ImagemRadarRepository;
import com.ass.br.apiradar.presentation.dto.ImagemRadarResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImagemRadarService {

    private final ImagemRadarRepository imagemRadarRepository;
    private final ImageService imageService;
    private final StorageService storageService;

    public byte[] baixarImagem(final String url) throws IOException {
        return imageService.imageBufferize(url);
    }

    public Optional<byte[]> baixarImagem(final Long imagemId) {
        return imagemRadarRepository.findById(imagemId)
                .map(ImagemRadar::getDadosBrutos);
    }

    public ImagemRadar salvarImagem(final byte[] dados, final String origem, final Deformacao deformacao) {
        ImagemRadar imagem = ImagemRadar.builder()
                .origem(origem)
                .dataColeta(LocalDate.now())
                .dadosBrutos(dados)
                .processada(origem.equals("FASTAPI"))
                .urlImagem(gerarURLImagemS3(deformacao, dados, origem))
                .deformacao(deformacao)
                .build();

        return imagemRadarRepository.save(imagem);
    }

    public List<ImagemRadarResponseDto> buscarImagensPorDeformacao(final Long deformacaoId) {
        List<ImagemRadar> imagens = imagemRadarRepository.findByDeformacaoId(deformacaoId);

        return imagens.stream()
                .map(imagem -> ImagemRadarResponseDto.builder()
                        .id(imagem.getId())
                        .origem(imagem.getOrigem())
                        .dataColeta(imagem.getDataColeta())
                        .processada(imagem.isProcessada())
                        .build())
                .toList();
    }

    public List<String> buscarURLImagensNoS3(Long deformacaoId) {
        return imagemRadarRepository.findByDeformacaoId(deformacaoId)
                .stream()
                .map(ImagemRadar::getUrlImagem)
                .toList();
    }

    private String gerarURLImagemS3(final Deformacao deformacao, final byte[] dados, final String origem) {
        String nomeArquivo = "imagens/deformacao-" + deformacao.getId() +
                (origem.equals("NASA") ? "/nasa_original.png" : "/fastapi_processada.png");

        return storageService.uploadFile(nomeArquivo, dados); // Envia para o S3
    }
}

