package com.ass.br.apiradar.infrastructure.apis;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "usgsClient", url = "https://earthquake.usgs.gov")
public interface UsgsEarthquakeClient {

    @GetMapping("/fdsnws/event/1/query")
    Map<String, Object> getEarthquakeData(
            @RequestParam("format") String format,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam("maxradiuskm") double maxRadius
    );
}
