package com.lastdrop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lastdrop.entity.User;

/**
 * Database access for users.
 *
 * Spring Data writes the SQL for us: by extending JpaRepository we get
 * save(), findById(), findAll(), delete() for free, and methods named like
 * "findByEmail" are turned into "SELECT ... WHERE email = ?" automatically.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
