package com.example.event_board.entity;

import com.example.event_board.entity.enums.AccountStatus;
import com.example.event_board.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name="users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name="telegram_id", unique = true, length = 64)
    private String telegramId;

    @ManyToOne
    @JoinColumn(name="company_id")
    private Company company;

    @Column(name="rejection_reason", length = 128)
    private String rejectionReason;
}
