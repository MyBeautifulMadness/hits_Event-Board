package com.example.event_board.dto;

import com.example.event_board.entity.Event;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private LocalDateTime signupDeadline;
    private Long companyId;
    private String companyName;
    private Long createdById;
    private String createdByUsername;

    public static EventResponse from(Event e) {
        EventResponse dto = new EventResponse();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription());
        dto.setStartTime(e.getStartTime());
        dto.setEndTime(e.getEndTime());
        dto.setLocation(e.getLocation());
        dto.setSignupDeadline(e.getSignupDeadline());
        if (e.getCompany() != null) {
            dto.setCompanyId(e.getCompany().getId());
            dto.setCompanyName(e.getCompany().getName());
        }
        if (e.getCreatedBy() != null) {
            dto.setCreatedById(e.getCreatedBy().getId());
            dto.setCreatedByUsername(e.getCreatedBy().getUsername());
        }
        return dto;
    }
}
