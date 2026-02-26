package com.busticket.service;

import com.busticket.dto.PassengerProfileDTO;
import com.busticket.dto.PassengerProfileUpdateDTO;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;

public interface PassengerProfileService {
    PassengerProfileDTO getProfile(Long userId) throws ValidationException, UnauthorizedException;

    void updateProfile(PassengerProfileUpdateDTO dto) throws ValidationException, UnauthorizedException;
}
