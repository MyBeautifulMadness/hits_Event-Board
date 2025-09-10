package com.example.event_board.dto;

import lombok.Data;

@Data
public class RegisterManagerRequest {
    private String username;
    private String password;
    private String telegramId;
    private String companyName;
}
