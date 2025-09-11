package com.example.event_board.controller;

import com.example.event_board.dto.UpdateTelegramRequest;
import com.example.event_board.dto.UserResponse;
import com.example.event_board.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository users;
    private final ObjectMapper om = new ObjectMapper();

    private Long extractUid(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No bearer token");
        }
        String[] parts = auth.substring(7).split("\\.");
        if (parts.length < 2) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad JWT");
        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String,Object> claims = om.readValue(json, new TypeReference<>() {});
            Object uid = claims.get("uid");
            if (uid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No uid in token");
            return Long.valueOf(uid.toString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot parse JWT");
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me(HttpServletRequest req) {
        Long uid = extractUid(req);
        return users.findById(uid)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me/telegram")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateTelegram(HttpServletRequest req,
                                               @RequestBody UpdateTelegramRequest body) {
        Long uid = extractUid(req);
        var u = users.findById(uid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        u.setTelegramId(body.getTelegramId());
        users.save(u);
        return ResponseEntity.noContent().build();
    }
}
