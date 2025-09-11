package com.example.event_board.controller;

import com.example.event_board.dto.EventRegistrationDto;
import com.example.event_board.dto.StudentShortDto;
import com.example.event_board.entity.Event;
import com.example.event_board.entity.EventRegistration;
import com.example.event_board.entity.User;
import com.example.event_board.repository.EventRegistrationRepository;
import com.example.event_board.repository.EventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationRepository registrations;
    private final EventRepository events;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long extractUidFromHeader(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No bearer token");
        }
        String token = auth.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad JWT");
        }
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = objectMapper.readValue(payloadJson, new TypeReference<>() {});
            Object uid = claims.get("uid");
            if (uid == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No uid in token");
            }
            return Long.valueOf(uid.toString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot parse JWT payload");
        }
    }

    @GetMapping("/registrations/mine")
    @PreAuthorize("isAuthenticated()")
    public List<EventRegistrationDto> myRegistrations(HttpServletRequest request) {
        Long uid = extractUidFromHeader(request);

        return registrations.findByStudentId(uid).stream()
                .map(r -> new EventRegistrationDto(
                        r.getId(),
                        r.getEvent().getId(),
                        r.getStudent().getId()
                ))
                .toList();
    }

    @GetMapping("/events/{eventId}/registrations")
    @PreAuthorize("hasAnyAuthority('DEAN','MANAGER')")
    public List<StudentShortDto> registrationsByEvent(@PathVariable Long eventId,
                                                      HttpServletRequest request) {
        Event ev = events.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        Long uid = extractUidFromHeader(request);

        boolean owner = ev.getCreatedBy() != null && ev.getCreatedBy().getId().equals(uid);

        if (!owner) {
            // TODO
        }

        List<EventRegistration> regs = registrations.findByEventId(eventId);
        return regs.stream()
                .map(r -> {
                    User s = r.getStudent();
                    return new StudentShortDto(
                            s.getId(),
                            s.getUsername(),
                            s.getTelegramId()
                    );
                })
                .toList();
    }
}