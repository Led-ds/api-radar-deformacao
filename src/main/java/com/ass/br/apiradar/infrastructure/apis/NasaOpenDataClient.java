package com.ass.br.apiradar.infrastructure.apis;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

@FeignClient(name = "nasaOpenDataClient", url = "https://api.nasa.gov")
public interface NasaOpenDataClient {

    @GetMapping("/planetary/earth/assets")
    Map<String, Object> getEarthImage(
            @RequestParam("lon") double longitude,
            @RequestParam("lat") double latitude,
            @RequestParam("api_key") String apiKey
    );
}
