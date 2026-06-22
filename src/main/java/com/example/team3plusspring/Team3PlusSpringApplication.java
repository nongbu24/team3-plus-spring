package com.example.team3plusspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class Team3PlusSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(Team3PlusSpringApplication.class, args);
    }

}
