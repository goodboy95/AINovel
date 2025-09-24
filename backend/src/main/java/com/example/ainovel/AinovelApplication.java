package com.example.ainovel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class AinovelApplication {

        public static void main(String[] args) {
        SpringApplication.run(AinovelApplication.class, args);
    }

}
