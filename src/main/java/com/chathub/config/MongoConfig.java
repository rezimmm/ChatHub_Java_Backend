package com.chathub.config;

import com.chathub.repository.ChannelRepository;
import com.chathub.repository.InviteLinkRepository;
import com.chathub.repository.MessageRepository;
import com.chathub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            // Create uploads directory
            Path uploadsDir = Paths.get("uploads");
            if (!Files.exists(uploadsDir)) {
                Files.createDirectories(uploadsDir);
                log.info("Created uploads directory");
            }

            // Create MongoDB indexes
            mongoTemplate.indexOps("users")
                    .ensureIndex(new Index().on("id", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps("users")
                    .ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps("channels")
                    .ensureIndex(new Index().on("id", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps("messages")
                    .ensureIndex(new Index().on("id", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps("messages")
                    .ensureIndex(new Index().on("channelId", Sort.Direction.ASC));
            mongoTemplate.indexOps("invite_links")
                    .ensureIndex(new Index().on("id", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps("invite_links")
                    .ensureIndex(new Index().on("token", Sort.Direction.ASC));

            log.info("Database indexes created successfully");
        };
    }
}
