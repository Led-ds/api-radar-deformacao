package com.ass.br.apiradar.domain.services;

import com.ass.br.apiradar.domain.model.AnaliseResultado;
import com.ass.br.apiradar.domain.model.Deformacao;
import com.ass.br.apiradar.domain.repositories.DeformacaoRepository;
import com.ass.br.apiradar.infrastructure.apis.AlosPalsarClient;
import com.ass.br.apiradar.infrastructure.component.GeoToolsProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnaliseService {

    @Autowired
    private AlosPalsarClient alosPalsarClient;
    @Autowired
    private GeoToolsProcessor geoToolsProcessor;
    @Autowired
    private final DeformacaoRepository deformacaoRepository;

    public AnaliseResultado processarEAnalisarImagem(String imagemRadarId) {
        try {
            // Baixar imagem da API externa
            byte[] imagemBruta = alosPalsarClient.baixarImagem(imagemRadarId);

            // Processar a imagem usando GeoTools
            var imagemProcessada = geoToolsProcessor.processar(imagemBruta);

            // Chamar o modelo de IA para análise
            String classificacao = executarAnaliseIA(imagemProcessada);

            // Retornar o resultado da análise
            return new AnaliseResultado(imagemRadarId, classificacao, imagemProcessada);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar e analisar imagem: " + imagemRadarId, e);
        }
    }

    public Deformacao salvarDeformacao(Deformacao deformacao) {
        return deformacaoRepository.save(deformacao);
    }

    public List<Deformacao> buscarDeformacoes() {
        return deformacaoRepository.findAll();
    }

    private String executarAnaliseIA(Object imagemProcessada) {
        // Integração com o serviço REST Python para análise de IA
        // Substituir com chamada FeignClient para o modelo de IA
        return "Estável"; // Placeholder: Substituir pelo resultado real
    }
}

