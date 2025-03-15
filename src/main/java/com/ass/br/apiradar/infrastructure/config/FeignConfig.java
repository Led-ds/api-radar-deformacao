package com.ass.br.apiradar.infrastructure.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;

@Configuration
@EnableFeignClients(basePackages = "com.ass.br.apiradar.infrastructure.apis")
public class FeignConfig {

    @Bean
    public Encoder feignFormEncoder(List<HttpMessageConverter<?>> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(() -> (org.springframework.boot.autoconfigure.http.HttpMessageConverters) messageConverters));
    }

}
