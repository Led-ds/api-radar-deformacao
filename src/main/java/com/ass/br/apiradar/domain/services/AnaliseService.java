package com.ass.br.apiradar.domain.services;

import com.ass.br.apiradar.domain.model.Deformacao;
import com.ass.br.apiradar.domain.model.ImagemRadar;
import com.ass.br.apiradar.domain.model.dto.NasaImageResponse;
import com.ass.br.apiradar.domain.repositories.DeformacaoRepository;
import com.ass.br.apiradar.infrastructure.apis.NasaOpenDataClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AnaliseService {

    private final DeformacaoRepository deformacaoRepository;
    private final ImagemRadarService imagemRadarService;
    private final ProcessamentoService processamentoService;
    private final NasaOpenDataClient nasaClient;
    private final ObjectMapper objectMapper;

    @Value("${api.key.open.data.nasa}")
    private String apiKey;

    public List<Deformacao> buscarDeformacoes() {
        return deformacaoRepository.findAll();
    }

    @Transactional
    public Deformacao salvarDeformacao(Deformacao deformacao) throws IOException {
        deformacao = deformacaoRepository.save(deformacao); // Garante que tem um ID no banco
        String risco = avaliarRisco(deformacao);
        deformacao.setRisco(risco);
        return deformacaoRepository.save(deformacao); // Atualiza o risco
    }

    private String avaliarRisco(Deformacao deformacao) throws IOException {
        Map<String, Object> resposta = nasaClient.getEarthImage(deformacao.getLongitude(), deformacao.getLatitude(), apiKey);
        NasaImageResponse nasaImage = objectMapper.convertValue(resposta, NasaImageResponse.class);

        if (Objects.isNull(nasaImage.getUrl())) {
            return "Dados insuficientes para avaliação de risco.";
        }

        byte[] imagemBruta = imagemRadarService.baixarImagem(nasaImage.getUrl());

        // Salvar imagem da NASA
        ImagemRadar imagemRadar = imagemRadarService.salvarImagem(imagemBruta, "NASA", deformacao);

        // Processar e analisar a imagem
        return processamentoService.processarEAnalisarImagem(imagemRadar, deformacao);
    }

}



