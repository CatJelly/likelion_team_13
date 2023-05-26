package com.placehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PlacehubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlacehubApplication.class, args);
    }

}
