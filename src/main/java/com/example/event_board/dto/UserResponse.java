package com.example.event_board.dto;

import com.example.event_board.entity.User;
import com.example.event_board.entity.enums.AccountStatus;
import com.example.event_board.entity.enums.UserRole;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private UserRole role;
    private AccountStatus status;
    private Long companyId;
    private String company;
    private String rejectionReason;

    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        if (user.getCompany() != null) {
            dto.setCompanyId(user.getCompany().getId());
            dto.setCompany(user.getCompany().getName());
        }
        dto.setRejectionReason(user.getRejectionReason());
        return dto;
    }
}
