package com.ass.br.apiradar.interferometry.domain.ports.output;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.interferometry.domain.model.BaselineValue;

/**
 * Porta de saída do domínio para obtenção ou cálculo de linha de base perpendicular (B⊥).
 * Isola o domínio da tecnologia de cálculo ou APIs de efemérides externas (ASF Baseline, CDSE, etc.).
 */
public interface PerpendicularBaselinePort {

    /**
     * Obtém ou calcula a linha de base perpendicular entre a cena de referência e a secundária.
     * Retorna BaselineValue.unavailable() caso as efemérides ainda não permitam a determinação precisa.
     */
    BaselineValue getPerpendicularBaseline(SarScene referenceScene, SarScene secondaryScene);
}
