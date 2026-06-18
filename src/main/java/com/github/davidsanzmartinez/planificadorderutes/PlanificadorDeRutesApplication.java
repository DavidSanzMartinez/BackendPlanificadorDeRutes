package com.github.davidsanzmartinez.planificadorderutes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlanificadorDeRutesApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanificadorDeRutesApplication.class, args);
    }

}
