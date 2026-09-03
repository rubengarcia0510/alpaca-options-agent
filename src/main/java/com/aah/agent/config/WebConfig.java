package com.aah.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:4200",
                        "https://alpaca-options-agent-nine.vercel.app",
                        "https://alpaca-options-agent-eo2wyoxnq-rubengarcia0510s-projects.vercel.app"
                )
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
