package com.example.event_board.controller;

import com.example.event_board.repository.CompanyRepository;
import com.example.event_board.security.JwtAuthentication;
import com.example.event_board.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companies;

    @GetMapping
    public List<Map<String,Object>> list() {
        return companies.findAll().stream()
                .map(c -> {
                    Map<String,Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    return m;
                })
                .toList();
    }

}

