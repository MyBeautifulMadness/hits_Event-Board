package com.example.event_board.controller;

import com.example.event_board.dto.CreateEventRequest;
import com.example.event_board.dto.EventResponse;
import com.example.event_board.repository.UserRepository;
import com.example.event_board.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final UserRepository users;
    @GetMapping
    public ResponseEntity<Page<EventResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        var p = eventService.listFutureEvents(page, size).map(EventResponse::from);
        return ResponseEntity.ok(p);
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest req, Principal principal) {
        Long managerId = users.findByUsername(principal.getName()).orElseThrow().getId();
        var e = eventService.createEvent(managerId, req.getTitle(), req.getDescription(),
                req.getStartTime(), req.getEndTime(), req.getLocation(), req.getSignupDeadline());
        return ResponseEntity.ok(EventResponse.from(e));
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long id) {
        var event = eventService.getEventById(id);
        return ResponseEntity.ok(EventResponse.from(event));
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody CreateEventRequest req,
                                                Principal principal) {
        Long managerId = users.findByUsername(principal.getName()).orElseThrow().getId();
        var e = eventService.updateEvent(managerId, id, req.getTitle(), req.getDescription(),
                req.getStartTime(), req.getEndTime(), req.getLocation(), req.getSignupDeadline());
        return ResponseEntity.ok(EventResponse.from(e));
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal) {
        Long managerId = users.findByUsername(principal.getName()).orElseThrow().getId();
        eventService.deleteEvent(managerId, id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('STUDENT')")
    @PostMapping("/{id}/register")
    public ResponseEntity<?> register(@PathVariable Long id, Principal principal) {
        Long studentId = users.findByUsername(principal.getName()).orElseThrow().getId();
        eventService.registerStudent(id, studentId);
        return ResponseEntity.ok().build();
    }
}
