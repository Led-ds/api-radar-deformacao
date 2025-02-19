package com.ass.br.apiradar.infrastructure.apis;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "alosPalsarClient", url = "https://api.alospalsar.jaxa.jp")
public interface AlosPalsarClient {

    @GetMapping("/images")
    byte[] baixarImagem(@RequestParam("id") String idImagem);
}
