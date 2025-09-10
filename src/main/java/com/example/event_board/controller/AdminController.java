package com.example.event_board.controller;

import com.example.event_board.dto.RejectUserRequest;
import com.example.event_board.dto.UserResponse;
import com.example.event_board.service.CompanyService;
import com.example.event_board.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('DEANERY')")
@RequiredArgsConstructor
public class AdminController {

    private final CompanyService companyService;
    private final UserService userService;

    @PostMapping("/companies")
    public ResponseEntity<?> createCompany(@RequestParam String name) {
        return ResponseEntity.ok(companyService.createCompany(name));
    }

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<UserResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(userService.approveUser(id)));
    }

    @PostMapping("/users/{id}/reject")
    public ResponseEntity<UserResponse> reject(@PathVariable Long id,
                                               @RequestBody(required = false) RejectUserRequest req) {
        String reason = req != null ? req.getReason() : null;
        return ResponseEntity.ok(UserResponse.from(userService.rejectUser(id, reason)));
    }
}
