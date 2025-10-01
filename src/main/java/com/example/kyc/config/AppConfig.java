package com.example.kyc.config;

import com.example.kyc.service.PointDeserializer;
import com.example.kyc.service.PointSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
    
    @Bean
    public Module jtsJacksonModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(org.locationtech.jts.geom.Point.class, new PointSerializer());
        module.addDeserializer(org.locationtech.jts.geom.Point.class, new PointDeserializer());
        return module;
    }
}