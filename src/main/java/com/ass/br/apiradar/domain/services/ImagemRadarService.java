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

    public byte[] baixarImagem(String url) throws IOException {
        return imageService.imageBufferize(url);
    }

    public Optional<byte[]> baixarImagem(Long imagemId) {
        return imagemRadarRepository.findById(imagemId)
                .map(ImagemRadar::getDadosBrutos);
    }

    public ImagemRadar salvarImagem(byte[] dados, String origem, Deformacao deformacao) {
        ImagemRadar imagem = ImagemRadar.builder()
                .origem(origem)
                .dataColeta(LocalDate.now())
                .dadosBrutos(dados)
                .processada(origem.equals("FASTAPI"))
                .deformacao(deformacao)
                .build();
        return imagemRadarRepository.save(imagem);
    }

    public List<ImagemRadarResponseDto> buscarImagensPorDeformacao(Long deformacaoId) {
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
}

