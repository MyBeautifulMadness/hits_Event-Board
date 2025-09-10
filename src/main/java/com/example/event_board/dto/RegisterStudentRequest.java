package com.example.event_board.dto;

import lombok.Data;

@Data
public class RegisterStudentRequest {
    private String username;
    private String password;
    private String telegramId;
}
