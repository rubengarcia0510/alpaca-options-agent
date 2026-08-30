package com.aah.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlpacaOptionsAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlpacaOptionsAgentApplication.class, args);
    }
}
