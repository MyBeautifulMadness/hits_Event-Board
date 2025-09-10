package com.example.event_board.controller;

import com.example.event_board.dto.RegisterManagerRequest;
import com.example.event_board.dto.RegisterStudentRequest;
import com.example.event_board.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class RegController {

    private final UserService userService;

    @PostMapping("/student")
    public ResponseEntity<?> registerStudent(@Valid @RequestBody RegisterStudentRequest req) {
        userService.registerStudent(req.getUsername(), req.getPassword(), req.getTelegramId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manager")
    public ResponseEntity<?> registerManager(@Valid @RequestBody RegisterManagerRequest req) {
        userService.registerManager(req.getUsername(), req.getPassword(), req.getTelegramId(), req.getCompanyName());
        return ResponseEntity.ok().build();
    }
}
