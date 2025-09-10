package com.example.event_board.service;

import com.example.event_board.entity.Company;
import com.example.event_board.repository.CompanyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companies;

    @Transactional
    public Company createCompany(String name) {
        companies.findByName(name).ifPresent(c -> { throw new IllegalArgumentException("Company already exists"); });
        Company c = Company.builder().name(name).build();
        return companies.save(c);
    }
}
