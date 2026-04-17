package com.estorilflow.repository;

import com.estorilflow.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndActiveTrue(String username);
}
