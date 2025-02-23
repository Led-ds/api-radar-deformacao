package com.ass.br.apiradar.domain.services;

import com.ass.br.apiradar.domain.model.AnaliseResultado;
import com.ass.br.apiradar.domain.model.Deformacao;
import com.ass.br.apiradar.domain.model.dto.NasaImageResponse;
import com.ass.br.apiradar.domain.repositories.DeformacaoRepository;
import com.ass.br.apiradar.infrastructure.apis.AlosPalsarClient;
import com.ass.br.apiradar.infrastructure.apis.NasaOpenDataClient;
import com.ass.br.apiradar.infrastructure.component.GeoToolsProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AnaliseService {

    @Autowired
    private AlosPalsarClient alosPalsarClient;
    @Autowired
    private GeoToolsProcessor geoToolsProcessor;
    @Autowired
    private final DeformacaoRepository deformacaoRepository;
    @Autowired
    private final NasaOpenDataClient nasaClient;
    @Autowired
    private ObjectMapper objectMapper;

    private final ImageService imageService;

    @Value("${api.key.open.data.nasa}")
    private String apiKey;


    public List<Deformacao> buscarDeformacoes() {
        return deformacaoRepository.findAll();
    }

    public Deformacao salvarDeformacao(final Deformacao deformacao) throws IOException {
        String risco = avaliarRisco(deformacao);

        deformacao.setRisco(risco);

        deformacaoRepository.save(deformacao);
        return deformacao;
    }

    public String avaliarRisco(final Deformacao deformacao) throws IOException {
        Map<String, Object> resposta = nasaClient.getEarthImage(deformacao.getLongitude(), deformacao.getLatitude(), apiKey);
        NasaImageResponse nasaImage = objectMapper.convertValue(resposta, NasaImageResponse.class);

        if (Objects.isNull(nasaImage.getUrl())) {
            return "Dados insuficientes para avaliação de risco.";
        }

        byte[] imagemBruta = imageService.imageBufferize(nasaImage.getUrl());

        this.processarEAnalisarImagem(nasaImage.getId(), imagemBruta, deformacao);

        if (deformacao.getDeslocamento() > 5.0) {
            return "ALTO RISCO - Grande deslocamento detectado";
        } else if (deformacao.getDeslocamento() > 2.0) {
            return "RISCO MODERADO - Pequeno deslocamento, precisa de monitoramento";
        } else {
            return "BAIXO RISCO - Nenhuma anomalia detectada";
        }
    }

    //TRATAR A IMAGE NO SERVICE É JÁ PASSAR PARA O SERVICE GEOTOOLSPROCESSAR
    public AnaliseResultado processarEAnalisarImagem(String imagemRadarId, byte[] imagemBruta, Deformacao deformacao) {
        try {

            // Processar a imagem usando GeoTools
            var imagemProcessada = geoToolsProcessor.processar(imagemBruta, deformacao);

            // Chamar o modelo de IA para análise
            //String classificacao = executarAnaliseIA(imagemProcessada);

            // Retornar o resultado da análise
            return new AnaliseResultado(imagemRadarId, "classificacao", imagemProcessada);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar e analisar imagem: " + imagemRadarId, e);
        }
    }

    private String executarAnaliseIA(Object imagemProcessada) {
        // Integração com o serviço REST Python para análise de IA
        // Substituir com chamada FeignClient para o modelo de IA
        return "Estável"; // Placeholder: Substituir pelo resultado real
    }
}

