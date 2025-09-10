package com.example.event_board.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateEventRequest {
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private LocalDateTime signupDeadline;
}
