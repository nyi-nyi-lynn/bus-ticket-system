package com.busticket.dto;

import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;

import java.io.Serializable;

public class CreateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long createdByUserId;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private String password;

    public CreateUserRequest() {
    }

    public CreateUserRequest(Long createdByUserId, String name, String email, String phone, Role role, UserStatus status, String password) {
        this.createdByUserId = createdByUserId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.password = password;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
