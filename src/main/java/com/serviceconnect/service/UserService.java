package com.serviceconnect.service;

import com.serviceconnect.dto.request.UpdateUserRequest;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Client;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.User;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.repository.UserRepository;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.getName() != null)   user.setName(req.getName());
        if (req.getEmail() != null)  user.setEmail(req.getEmail());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());

        if (user instanceof Client client) {
            if (req.getAddress() != null) client.setAddress(req.getAddress());
            if (req.getLocation() != null) {
                client.setLocationLat(req.getLocation().getLat());
                client.setLocationLng(req.getLocation().getLng());
            }
        }

        if (user instanceof Technician tech) {
            if (req.getServiceTypes() != null) tech.setServiceTypes(req.getServiceTypes());
            if (req.getAvailability() != null) {
                tech.setAvailability(Technician.Availability.valueOf(req.getAvailability()));
            }
            if (req.getLocation() != null) {
                tech.setLocationLat(req.getLocation().getLat());
                tech.setLocationLng(req.getLocation().getLng());
                tech.setLocationAddress(req.getLocation().getAddress());
            }
        }

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }
}
