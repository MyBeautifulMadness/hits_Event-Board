package com.example.event_board.repository;

import com.example.event_board.entity.User;
import com.example.event_board.entity.enums.AccountStatus;
import com.example.event_board.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRoleAndStatus(UserRole role, AccountStatus status);
    List<User> findByRole(UserRole role);
    List<User> findByStatus(AccountStatus status);
}
