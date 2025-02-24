package com.ass.br.apiradar.infrastructure.apis;

import feign.Headers;
import feign.RequestLine;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "fast-api-client", url = "http://localhost:8082")
public interface FastAPIClientPy {

    @PostMapping(value = "/analyze/", consumes = "multipart/form-data")
    @Headers("Content-Type: multipart/form-data")
    Response analyze(@RequestPart("file") MultipartFile file);
}

