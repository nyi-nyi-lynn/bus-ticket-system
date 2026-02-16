package com.busticket.mapper;

import com.busticket.dto.UserDTO;
import com.busticket.model.User;

public class UserMapper {
    public static UserDTO toDTO(User user) {
        return new UserDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getStatus() == null ? null : user.getStatus().name()
        );
    }

    public static User toEntity(UserDTO dto) {
        User user = new User();
        user.setUserId(dto.getUserId());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        return user;
    }
}
