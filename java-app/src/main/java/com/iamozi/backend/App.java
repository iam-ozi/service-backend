package com.iamozi.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @GetMapping("/")
    public String hello() {
        return "Hello from Java Spring Boot REST API!";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    // ✅ NEW FEATURE: Personalized greeting endpoint
    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "Guest") String name) {
        return "Hello, " + name + "!";
    }
}
