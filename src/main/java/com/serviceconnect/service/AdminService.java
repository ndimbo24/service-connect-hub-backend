package com.serviceconnect.service;

import com.serviceconnect.dto.response.ActivityLogResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.exception.BadRequestException;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.repository.TechnicianRepository;
import com.serviceconnect.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TechnicianRepository technicianRepository;
    private final ActivityLogService activityLogService;
    private final UserMapper userMapper;

    public List<UserResponse> getTechnicians(String status) {
        List<Technician> technicians;
        if (status != null && !status.isBlank()) {
            try {
                Technician.ApprovalStatus s = Technician.ApprovalStatus.valueOf(status);
                technicians = technicianRepository.findByStatus(s);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status value: " + status);
            }
        } else {
            technicians = technicianRepository.findAll();
        }
        return technicians.stream().map(userMapper::toResponse).collect(Collectors.toList());
    }

    public UserResponse getTechnicianById(Long id) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));
        return userMapper.toResponse(tech);
    }

    @Transactional
    public UserResponse approveTechnician(Long id, Long adminId, String adminName) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        if (tech.getStatus() == Technician.ApprovalStatus.approved) {
            throw new BadRequestException("Technician is already approved");
        }

        tech.setStatus(Technician.ApprovalStatus.approved);
        tech.setAvailability(Technician.Availability.available);
        Technician saved = technicianRepository.save(tech);

        activityLogService.log(adminId, adminName, "admin",
                "APPROVE_TECHNICIAN",
                "Approved technician: " + tech.getName() + " (id=" + id + ")");

        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse rejectTechnician(Long id, String reason, Long adminId, String adminName) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        tech.setStatus(Technician.ApprovalStatus.rejected);
        tech.setRejectionReason(reason);
        Technician saved = technicianRepository.save(tech);

        activityLogService.log(adminId, adminName, "admin",
                "REJECT_TECHNICIAN",
                "Rejected technician: " + tech.getName() + " (id=" + id + ") Reason: " + reason);

        return userMapper.toResponse(saved);
    }

    public List<ActivityLogResponse> getLogs() {
        return activityLogService.getAllLogs();
    }
}
