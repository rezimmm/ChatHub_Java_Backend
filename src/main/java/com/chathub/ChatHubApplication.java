package com.chathub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChatHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatHubApplication.class, args);
    }
}
