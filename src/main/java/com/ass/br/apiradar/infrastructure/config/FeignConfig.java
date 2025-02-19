package com.ass.br.apiradar.infrastructure.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.ass.br.apiradar.infrastructure.apis")
public class FeignConfig {
}
