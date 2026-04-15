package com.serviceconnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Role is required")
    private String role;

    private String email;

    // Technician-specific fields
    private List<String> serviceTypes;
    private LocationData location;

    @Data
    public static class LocationData {
        private Double lat;
        private Double lng;
        private String address;
    }
}
