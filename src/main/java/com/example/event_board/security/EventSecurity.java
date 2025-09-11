package com.example.event_board.security;

import com.example.event_board.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("eventSecurity")
@RequiredArgsConstructor
public class EventSecurity {
    private final EventRepository events;

    public boolean isOwner(Long eventId, Long uid) {
        if (eventId == null || uid == null) return false;
        return events.findById(eventId)
                .map(ev -> ev.getCreatedBy() != null && uid.equals(ev.getCreatedBy().getId()))
                .orElse(false);
    }
}
