package com.amateurleague;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LeagueApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeagueApplication.class, args);
    }
}
