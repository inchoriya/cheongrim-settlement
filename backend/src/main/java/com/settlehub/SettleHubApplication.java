package com.settlehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SettleHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettleHubApplication.class, args);
    }
}
