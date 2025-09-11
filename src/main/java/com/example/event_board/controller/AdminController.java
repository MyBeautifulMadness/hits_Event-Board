package com.example.event_board.controller;

import com.example.event_board.dto.AttachManagerRequest;
import com.example.event_board.dto.RejectUserRequest;
import com.example.event_board.dto.UserResponse;
import com.example.event_board.entity.Company;
import com.example.event_board.entity.User;
import com.example.event_board.entity.enums.AccountStatus;
import com.example.event_board.entity.enums.UserRole;
import com.example.event_board.repository.CompanyRepository;
import com.example.event_board.repository.UserRepository;
import com.example.event_board.service.CompanyService;
import com.example.event_board.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('DEANERY')")
@RequiredArgsConstructor
public class AdminController {

    private final CompanyService companyService;
    private final UserService userService;

    private final UserRepository users;
    private final CompanyRepository companies;

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

    @GetMapping("/users/pending")
    public List<UserResponse> pendingUsers() {
        List<UserResponse> out = new ArrayList<>();
        users.findByRoleAndStatus(UserRole.STUDENT, AccountStatus.PENDING).forEach(u -> out.add(UserResponse.from(u)));
        users.findByRoleAndStatus(UserRole.MANAGER, AccountStatus.PENDING).forEach(u -> out.add(UserResponse.from(u)));
        return out;
    }

    @GetMapping("/managers")
    public List<UserResponse> managers(@RequestParam(name = "status", required = false) String status) {
        AccountStatus st = status == null ? AccountStatus.APPROVED : AccountStatus.valueOf(status);
        return users.findByRoleAndStatus(UserRole.MANAGER, st).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping("/companies/{companyId}/managers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachManager(@PathVariable Long companyId, @RequestBody AttachManagerRequest req) {
        if (req == null || req.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId required");
        }
        Company company = companies.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        User manager = users.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (manager.getRole() != UserRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not MANAGER");
        }
        if (manager.getStatus() != AccountStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manager is not APPROVED");
        }

        manager.setCompany(company);
        users.save(manager);
    }

    @GetMapping("/users")
    public List<UserResponse> users(
            @RequestParam("role") UserRole role,
            @RequestParam(value = "status", required = false) AccountStatus status
    ) {
        if (status != null) {
            return users.findByRoleAndStatus(role, status)
                    .stream()
                    .map(UserResponse::from)
                    .toList();
        } else {
            return users.findByRole(role)
                    .stream()
                    .map(UserResponse::from)
                    .toList();
        }
    }
}
