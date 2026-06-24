package com.nexa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nexa backend entry point.
 * Reklámmentes, előfizetéses közösségi platform API-ja.
 */
@SpringBootApplication
public class NexaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexaApplication.class, args);
    }
}
