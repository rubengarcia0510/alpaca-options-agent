package com.aah.agent.risk;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RiskGateProperties.class)
public class RiskGateConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RiskGateService riskGateService(RiskGateProperties properties) {
        return new RiskGateService(
                properties.maxAccountRisk(),
                properties.maxDailyOperations(),
                properties.minDaysToExpiration(),
                properties.maxDaysToExpiration(),
                properties.maxOptionLossRatio()
        );
    }
}
