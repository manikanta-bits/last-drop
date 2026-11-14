package com.lastdrop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lastdrop.entity.Role;
import com.lastdrop.entity.User;

/** Database access for users. Spring writes the SQL from the method names. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findAllByOrderByCreatedAtDesc();
}
