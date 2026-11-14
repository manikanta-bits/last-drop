package com.lastdrop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the LASTDROP backend.
 *
 * Running this class starts an embedded web server (Tomcat) on port 8080.
 * The @SpringBootApplication annotation tells Spring to scan the "com.lastdrop"
 * package and all sub-packages (controller, service, config, ...) for components.
 */
@SpringBootApplication
public class LastdropApplication {

    public static void main(String[] args) {
        SpringApplication.run(LastdropApplication.class, args);
    }
}
