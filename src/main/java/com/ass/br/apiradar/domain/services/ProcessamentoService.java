package com.ass.br.apiradar.domain.services;

import com.ass.br.apiradar.domain.model.AnaliseResultado;
import com.ass.br.apiradar.domain.model.Deformacao;
import com.ass.br.apiradar.domain.model.ImagemRadar;
import com.ass.br.apiradar.domain.repositories.AnaliseResultadoRepository;
import com.ass.br.apiradar.infrastructure.apis.FastAPIClientPy;
import com.ass.br.apiradar.infrastructure.component.GeoToolsProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProcessamentoService {

    private final GeoToolsProcessor geoToolsProcessor;
    private final FastAPIClientPy clientPy;
    private final ObjectMapper objectMapper;
    private final AnaliseResultadoRepository analiseResultadoRepository;
    private final ImagemRadarService imagemRadarService;
    private final ImageService imageService;

    public String processarEAnalisarImagem(ImagemRadar imagemRadar, Deformacao deformacao) {
        try {
            byte[] imagemProcessada = geoToolsProcessor.processar(imagemRadar.getDadosBrutos(), deformacao);

            // Salvar imagem processada
            ImagemRadar imagemProcessadaEntity = imagemRadarService.salvarImagem(imagemProcessada, "FASTAPI", deformacao);

            // Enviar para FastAPI
            MultipartFile multipartFile = imageService.imageToMultipartFile(imagemProcessada, imagemRadar.getId().toString());
            Response result = clientPy.analyze(multipartFile);

            String classificacao = "Instável";
            double confianca = 0.0;

            if (result.status() == 200) {
                Map<String, Object> responseBody = objectMapper.readValue(result.body().asInputStream(), Map.class);
                classificacao = (String) responseBody.get("result");
                confianca = Double.parseDouble(responseBody.get("confidence").toString());
            }

            // Salvar resultado da análise
            AnaliseResultado analiseResultado = AnaliseResultado.builder()
                    .classificacao(classificacao)
                    .confianca(confianca)
                    .imagemRadar(imagemProcessadaEntity)
                    .build();
            analiseResultadoRepository.save(analiseResultado);

            return classificacao;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar e analisar imagem", e);
        }
    }
}

