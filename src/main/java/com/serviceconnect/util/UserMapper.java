package com.serviceconnect.util;

import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Client;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt());

        if (user instanceof Technician t) {
            builder
                .serviceTypes(t.getServiceTypes())
                .status(t.getStatus().name())
                .availability(t.getAvailability().name())
                .rating(t.getRating())
                .totalJobs(t.getTotalJobs())
                .documents(t.getDocuments());

            if (t.getLocationLat() != null) {
                builder.location(UserResponse.LocationData.builder()
                        .lat(t.getLocationLat())
                        .lng(t.getLocationLng())
                        .address(t.getLocationAddress())
                        .build());
            }
        }

        if (user instanceof Client c) {
            builder.address(c.getAddress());
            if (c.getLocationLat() != null) {
                builder.location(UserResponse.LocationData.builder()
                        .lat(c.getLocationLat())
                        .lng(c.getLocationLng())
                        .build());
            }
        }

        return builder.build();
    }
}
