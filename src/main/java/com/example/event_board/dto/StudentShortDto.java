package com.example.event_board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentShortDto {
    private Long id;
    private String username;
    private String telegramId;
}
