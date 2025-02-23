package com.ass.br.apiradar.application.usecases;

import com.ass.br.apiradar.domain.services.AnaliseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProcessarImagemSAR {

    @Autowired
    private AnaliseService analiseService;

    public void executar(String imagemRadarId) {
        analiseService.processarEAnalisarImagem(imagemRadarId, null, null);
    }
}
