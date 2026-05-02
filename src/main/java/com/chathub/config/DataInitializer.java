package com.chathub.config;

import com.chathub.model.Channel;
import com.chathub.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ChannelRepository channelRepository;

    @Override
    public void run(String... args) {
        // Ensure "general" channel exists
        if (channelRepository.findByName("general").isEmpty()) {
            Channel general = Channel.builder()
                    .name("general")
                    .description("General discussion for everyone")
                    .isDm(false)
                    .isPrivate(false)
                    .members(new ArrayList<>())
                    .createdBy("system")
                    .build();
            channelRepository.save(general);
            System.out.println("Default 'general' channel created.");
        }
    }
}
