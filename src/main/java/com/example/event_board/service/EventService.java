package com.example.event_board.service;

import com.example.event_board.entity.Event;
import com.example.event_board.entity.EventRegistration;
import com.example.event_board.entity.User;
import com.example.event_board.entity.enums.AccountStatus;
import com.example.event_board.entity.enums.UserRole;
import com.example.event_board.repository.EventRegistrationRepository;
import com.example.event_board.repository.EventRepository;
import com.example.event_board.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository events;
    private final UserRepository users;
    private final EventRegistrationRepository regs;

    public Page<Event> listFutureEvents(int page, int size) {
        return events.findByStartTime(LocalDateTime.now(),
                PageRequest.of(page, size, Sort.by("start_time").ascending()));
    }

    @Transactional
    public Event createEvent(Long managerId, String title, String desc, LocalDateTime startsAt, LocalDateTime endAt,
                             String location, LocalDateTime deadline) {
        User manager = users.findById(managerId).orElseThrow();
        if (manager.getRole() != UserRole.MANAGER || manager.getStatus() != AccountStatus.APPROVED)
            throw new IllegalStateException("Manager not approved");

        Event e = Event.builder()
                .title(title)
                .description(desc)
                .startTime(startsAt)
                .endTime(startsAt)
                .location(location)
                .signupDeadline(deadline)
                .company(manager.getCompany())
                .createdBy(manager)
                .build();
        return events.save(e);
    }

    @Transactional
    public Event getEventById(Long eventId) {
        return events.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    }

    @Transactional
    public Event updateEvent(Long managerId, Long eventId, String title, String desc,
                             LocalDateTime startsAt, LocalDateTime endAt, String location, LocalDateTime deadline) {
        Event e = events.findById(eventId).orElseThrow();
        if (!e.getCreatedBy().getId().equals(managerId))
            throw new SecurityException("Not your event");
        e.setTitle(title);
        e.setDescription(desc);
        e.setStartTime(startsAt);
        e.setEndTime(endAt);
        e.setLocation(location);
        e.setSignupDeadline(deadline);
        return e;
    }

    @Transactional
    public void deleteEvent(Long managerId, Long eventId) {
        Event e = events.findById(eventId).orElseThrow();
        if (!e.getCreatedBy().getId().equals(managerId))
            throw new SecurityException("Not your event");
        events.delete(e);
    }

    @Transactional
    public void registerStudent(Long eventId, Long studentId) {
        Event e = events.findById(eventId).orElseThrow();
        User student = users.findById(studentId).orElseThrow();

        if (student.getRole() != UserRole.STUDENT || student.getStatus() != AccountStatus.APPROVED)
            throw new IllegalStateException("Student not approved");
        if (e.getSignupDeadline() != null && LocalDateTime.now().isAfter(e.getSignupDeadline()))
            throw new IllegalStateException("Registration deadline passed");
        if (regs.existsByEventIdAndStudentId(eventId, studentId))
           throw new IllegalStateException("Already registered");

        EventRegistration r = EventRegistration.builder()
                .event(e)
                .student(student)
                .build();
        regs.save(r);

        // TODO: Google Calendar.
    }

}
