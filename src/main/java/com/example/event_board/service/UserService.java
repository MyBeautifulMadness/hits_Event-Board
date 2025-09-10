package com.example.event_board.service;

import com.example.event_board.entity.Company;
import com.example.event_board.entity.User;
import com.example.event_board.entity.enums.AccountStatus;
import com.example.event_board.entity.enums.UserRole;
import com.example.event_board.repository.CompanyRepository;
import com.example.event_board.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final CompanyRepository companies;

    @Transactional
    public User registerStudent(String username, String password, String telegramId) {
        User u = User.builder()
                .username(username)
                .password(password)
                .role(UserRole.STUDENT)
                .status(AccountStatus.PENDING)
                .telegramId(telegramId)
                .build();
        return users.save(u);
    }

    @Transactional
    public User registerManager(String username, String password, String telegramId, String companyName) {
        Company company = companies.findByName(companyName)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyName));
        User u = User.builder()
                .username(username)
                .password(password)
                .role(UserRole.MANAGER)
                .status(AccountStatus.PENDING)
                .telegramId(telegramId)
                .company(company)
                .build();
        return users.save(u);
    }

    @Transactional
    public User approveUser(Long userId) {
        User u = users.findById(userId).orElseThrow();
        u.setStatus(AccountStatus.APPROVED);
        return u;
    }

    public Optional<User> getByUsername(@NonNull String username) {
        return users.findByUsername(username)
                .filter(user -> username.equals(user.getUsername()));
    }
}
