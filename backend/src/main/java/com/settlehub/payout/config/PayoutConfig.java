package com.settlehub.payout.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PayoutProperties.class)
public class PayoutConfig {
}
