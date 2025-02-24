package com.ass.br.apiradar.application.usecases;

import com.ass.br.apiradar.domain.services.AnaliseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessarImagemSAR {

    private final AnaliseService analiseService;

    public void executar(String imagemRadarId) {
        //analiseService.processarEAnalisarImagem(imagemRadarId, null);
    }
}
