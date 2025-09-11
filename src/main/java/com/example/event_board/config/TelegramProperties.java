package com.example.event_board.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("telegram.bot")
public class TelegramProperties {
    private String username;
    private String token;
}
