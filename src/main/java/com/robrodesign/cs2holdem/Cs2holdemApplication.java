package com.robrodesign.cs2holdem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // hittar alla @ConfigurationProperties-klasser, t.ex. SteamProperties
public class Cs2holdemApplication {

    public static void main(String[] args) {
        SpringApplication.run(Cs2holdemApplication.class, args);
    }

}
