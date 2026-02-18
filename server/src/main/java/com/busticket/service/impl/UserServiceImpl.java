package com.busticket.service.impl;

import com.busticket.dao.impl.UserDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dao.UserDAO;
import com.busticket.dto.UserDTO;
import com.busticket.model.User;
import com.busticket.service.UserService;

import java.util.ArrayList;
import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public UserDTO login(String email, String password) {
        return null;
    }

    @Override
    public boolean register(UserDTO dto) {
        return false;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users =  userDAO.findAll();
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : users) {
            UserDTO dto = new UserDTO();
            dto.setUserId(user.getUserId());
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setPassword(user.getPassword());
            dto.setPhone(user.getPhone());
            dtos.add(dto);
        }
        return dtos;
    }
}
