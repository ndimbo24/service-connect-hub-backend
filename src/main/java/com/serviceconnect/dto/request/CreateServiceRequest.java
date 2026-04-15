package com.serviceconnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateServiceRequest {
    @NotBlank(message = "Service type is required")
    private String serviceType;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Location is required")
    private LocationData location;

    @Data
    public static class LocationData {
        @NotNull
        private Double lat;

        @NotNull
        private Double lng;

        @NotBlank
        private String address;
    }
}
