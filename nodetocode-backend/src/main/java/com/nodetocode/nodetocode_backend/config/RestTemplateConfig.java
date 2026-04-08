package com.nodetocode.nodetocode_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);  // 30 seconds to establish connection
        factory.setReadTimeout(120_000);    // 120 seconds to read response (AI can be slow)
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}