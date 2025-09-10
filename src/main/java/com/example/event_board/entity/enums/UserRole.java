package com.example.event_board.entity.enums;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@RequiredArgsConstructor
public enum UserRole implements GrantedAuthority {
    DEANERY("DEANERY"),
    STUDENT("STUDENT"),
    MANAGER("MANAGER");

    private final String vale;

    @Override
    public String getAuthority() {
        return vale;
    }
}
