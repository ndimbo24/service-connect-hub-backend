package com.serviceconnect.controller;

import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.repository.TechnicianRepository;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/technicians")
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianRepository technicianRepository;
    private final UserMapper userMapper;

    /**
     * GET /technicians/:id/location
     * Returns the current location of a technician.
     */
    @GetMapping("/{id}/location")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTechnicianLocation(
            @PathVariable Long id) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + id));

        Map<String, Object> location = Map.of(
            "technicianId",   tech.getId(),
            "technicianName", tech.getName(),
            "lat",            tech.getLocationLat() != null ? tech.getLocationLat() : 0.0,
            "lng",            tech.getLocationLng() != null ? tech.getLocationLng() : 0.0,
            "address",        tech.getLocationAddress() != null ? tech.getLocationAddress() : "",
            "availability",   tech.getAvailability().name()
        );

        return ResponseEntity.ok(ApiResponse.success(location));
    }

    /**
     * GET /technicians/:id
     * Returns a technician's public profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getTechnicianProfile(@PathVariable Long id) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponse(tech)));
    }
}
