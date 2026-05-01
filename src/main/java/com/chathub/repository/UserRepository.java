package com.chathub.repository;

import com.chathub.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    /** Find by the `email` field */
    Optional<User> findByEmail(String email);

    /** Check if email is already taken */
    boolean existsByEmail(String email);

    /** Check if username is already taken (case-sensitive index) */
    boolean existsByUsernameIgnoreCase(String username);

    /** Find by the custom UUID `id` field (not Mongo _id) */
    @Query("{ 'id': ?0 }")
    Optional<User> findByCustomId(String id);
}
