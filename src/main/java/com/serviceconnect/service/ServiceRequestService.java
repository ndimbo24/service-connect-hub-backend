package com.serviceconnect.service;

import com.serviceconnect.dto.request.CreateServiceRequest;
import com.serviceconnect.dto.response.JobNotificationResponse;
import com.serviceconnect.dto.response.ServiceRequestResponse;
import com.serviceconnect.dto.response.TrackingResponse;
import com.serviceconnect.entity.JobNotification;
import com.serviceconnect.entity.ServiceRequest;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.User;
import com.serviceconnect.exception.BadRequestException;
import com.serviceconnect.exception.ResourceNotFoundException;
import com.serviceconnect.exception.UnauthorizedException;
import com.serviceconnect.repository.JobNotificationRepository;
import com.serviceconnect.repository.ServiceRequestRepository;
import com.serviceconnect.repository.TechnicianRepository;
import com.serviceconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final TechnicianRepository technicianRepository;
    private final JobNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    // ──────────────────────────────────────────────
    // CREATE REQUEST
    // ──────────────────────────────────────────────
    @Transactional
    public ServiceRequestResponse createRequest(Long clientId, CreateServiceRequest dto) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ServiceRequest req = ServiceRequest.builder()
                .clientId(clientId)
                .clientName(client.getName())
                .serviceType(dto.getServiceType())
                .description(dto.getDescription())
                .locationLat(dto.getLocation().getLat())
                .locationLng(dto.getLocation().getLng())
                .locationAddress(dto.getLocation().getAddress())
                .status(ServiceRequest.RequestStatus.pending)
                .build();

        ServiceRequest saved = requestRepository.save(req);

        activityLogService.log(clientId, client.getName(), "client",
                "CREATE_REQUEST",
                "New service request for: " + dto.getServiceType() + " (id=" + saved.getId() + ")");

        // Immediately kick off matching
        triggerMatching(saved);

        return toResponse(requestRepository.findById(saved.getId()).orElse(saved));
    }

    // ──────────────────────────────────────────────
    // MATCHING LOGIC
    // ──────────────────────────────────────────────
    @Transactional
    public void triggerMatching(ServiceRequest req) {
        req.setStatus(ServiceRequest.RequestStatus.searching);
        requestRepository.save(req);

        List<Technician> candidates = technicianRepository
                .findAvailableByServiceType(req.getServiceType());

        if (candidates.isEmpty()) {
            log.warn("No available technicians for service type: {}", req.getServiceType());
            return;
        }

        // Pick nearest technician (Haversine distance)
        Technician best = candidates.stream()
                .filter(t -> t.getLocationLat() != null && t.getLocationLng() != null)
                .min((a, b) -> Double.compare(
                        haversine(req.getLocationLat(), req.getLocationLng(),
                                  a.getLocationLat(), a.getLocationLng()),
                        haversine(req.getLocationLat(), req.getLocationLng(),
                                  b.getLocationLat(), b.getLocationLng())
                ))
                .orElse(candidates.get(0)); // fallback: first available

        double distanceKm = (best.getLocationLat() != null)
                ? haversine(req.getLocationLat(), req.getLocationLng(),
                            best.getLocationLat(), best.getLocationLng())
                : 0.0;

        // Create job notification for the technician
        JobNotification notification = JobNotification.builder()
                .requestId(req.getId())
                .technicianId(best.getId())
                .serviceType(req.getServiceType())
                .clientName(req.getClientName())
                .distance(Math.round(distanceKm * 10.0) / 10.0)
                .address(req.getLocationAddress())
                .description(req.getDescription())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .status(JobNotification.NotificationStatus.pending)
                .build();
        notificationRepository.save(notification);

        // Auto-assign for now (simulating acceptance)
        assignTechnician(req.getId(), best.getId());
    }

    // ──────────────────────────────────────────────
    // ASSIGN TECHNICIAN
    // ──────────────────────────────────────────────
    @Transactional
    public ServiceRequestResponse assignTechnician(Long requestId, Long technicianId) {
        ServiceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
        Technician tech = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));

        req.setTechnicianId(technicianId);
        req.setTechnicianName(tech.getName());
        req.setStatus(ServiceRequest.RequestStatus.matched);

        // ETA: simulate ~10–30 min based on distance
        double distKm = (tech.getLocationLat() != null)
                ? haversine(req.getLocationLat(), req.getLocationLng(),
                            tech.getLocationLat(), tech.getLocationLng())
                : 5.0;
        long etaMinutes = Math.max(10, Math.min(30, (long)(distKm * 3)));
        req.setEstimatedArrival(LocalDateTime.now().plusMinutes(etaMinutes));

        tech.setAvailability(Technician.Availability.busy);
        technicianRepository.save(tech);

        // Update notification
        notificationRepository.findByRequestId(requestId).forEach(n -> {
            if (n.getTechnicianId().equals(technicianId)) {
                n.setStatus(JobNotification.NotificationStatus.accepted);
                notificationRepository.save(n);
            }
        });

        activityLogService.log(technicianId, tech.getName(), "technician",
                "ASSIGN_REQUEST",
                "Technician assigned to request id=" + requestId);

        return toResponse(requestRepository.save(req));
    }

    // ──────────────────────────────────────────────
    // GET REQUESTS
    // ──────────────────────────────────────────────
    public ServiceRequestResponse getById(Long id) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + id));
        return toResponse(req);
    }

    public List<ServiceRequestResponse> getByClientId(Long clientId) {
        return requestRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ServiceRequestResponse> getByTechnicianId(Long technicianId) {
        return requestRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ServiceRequestResponse> getAll() {
        return requestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // STATUS TRANSITIONS
    // ──────────────────────────────────────────────
    @Transactional
    public ServiceRequestResponse updateStatus(Long requestId, String newStatus, Long actorId) {
        ServiceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        ServiceRequest.RequestStatus status;
        try {
            status = ServiceRequest.RequestStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + newStatus);
        }

        req.setStatus(status);
        if (status == ServiceRequest.RequestStatus.completed) {
            req.setCompletedAt(LocalDateTime.now());
            // Free up technician
            if (req.getTechnicianId() != null) {
                technicianRepository.findById(req.getTechnicianId()).ifPresent(t -> {
                    t.setAvailability(Technician.Availability.available);
                    t.setTotalJobs(t.getTotalJobs() + 1);
                    technicianRepository.save(t);
                });
            }
        }

        return toResponse(requestRepository.save(req));
    }

    @Transactional
    public ServiceRequestResponse cancelRequest(Long requestId, Long actorId) {
        ServiceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        if (!req.getClientId().equals(actorId)) {
            throw new UnauthorizedException("Only the client who created this request can cancel it");
        }
        if (req.getStatus() == ServiceRequest.RequestStatus.completed) {
            throw new BadRequestException("Cannot cancel a completed request");
        }

        // Free up technician if already assigned
        if (req.getTechnicianId() != null) {
            technicianRepository.findById(req.getTechnicianId()).ifPresent(t -> {
                t.setAvailability(Technician.Availability.available);
                technicianRepository.save(t);
            });
        }

        req.setStatus(ServiceRequest.RequestStatus.cancelled);
        return toResponse(requestRepository.save(req));
    }

    // ──────────────────────────────────────────────
    // TRACKING
    // ──────────────────────────────────────────────
    public TrackingResponse getTracking(Long requestId) {
        ServiceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        TrackingResponse.TrackingResponseBuilder builder = TrackingResponse.builder()
                .requestId(requestId)
                .requestStatus(req.getStatus().name())
                .estimatedArrival(req.getEstimatedArrival())
                .clientLocation(TrackingResponse.ClientLocation.builder()
                        .lat(req.getLocationLat())
                        .lng(req.getLocationLng())
                        .address(req.getLocationAddress())
                        .build());

        if (req.getTechnicianId() != null) {
            technicianRepository.findById(req.getTechnicianId()).ifPresent(tech -> {
                builder.technicianLocation(TrackingResponse.TechnicianLocation.builder()
                        .technicianId(tech.getId())
                        .technicianName(tech.getName())
                        .lat(tech.getLocationLat())
                        .lng(tech.getLocationLng())
                        .address(tech.getLocationAddress())
                        .availability(tech.getAvailability().name())
                        .build());
            });
        }

        return builder.build();
    }

    // ──────────────────────────────────────────────
    // JOB NOTIFICATIONS (for technician dashboard)
    // ──────────────────────────────────────────────
    public List<JobNotificationResponse> getPendingNotifications(Long technicianId) {
        return notificationRepository
                .findByTechnicianIdAndStatus(technicianId, JobNotification.NotificationStatus.pending)
                .stream()
                .filter(n -> n.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────
    private ServiceRequestResponse toResponse(ServiceRequest req) {
        ServiceRequestResponse.ServiceRequestResponseBuilder builder = ServiceRequestResponse.builder()
                .id(req.getId())
                .clientId(req.getClientId())
                .clientName(req.getClientName())
                .technicianId(req.getTechnicianId())
                .technicianName(req.getTechnicianName())
                .serviceType(req.getServiceType())
                .description(req.getDescription())
                .status(req.getStatus().name())
                .location(ServiceRequestResponse.LocationData.builder()
                        .lat(req.getLocationLat())
                        .lng(req.getLocationLng())
                        .address(req.getLocationAddress())
                        .build())
                .createdAt(req.getCreatedAt())
                .completedAt(req.getCompletedAt())
                .estimatedArrival(req.getEstimatedArrival());

        // Enrich with technician details when matched
        if (req.getTechnicianId() != null) {
            technicianRepository.findById(req.getTechnicianId()).ifPresent(tech -> {
                ServiceRequestResponse.TechnicianInfo info = ServiceRequestResponse.TechnicianInfo.builder()
                        .id(tech.getId())
                        .name(tech.getName())
                        .phone(tech.getPhone())
                        .avatar(tech.getAvatar())
                        .rating(tech.getRating())
                        .totalJobs(tech.getTotalJobs())
                        .serviceTypes(tech.getServiceTypes())
                        .availability(tech.getAvailability().name())
                        .location(tech.getLocationLat() != null
                                ? ServiceRequestResponse.LocationData.builder()
                                    .lat(tech.getLocationLat())
                                    .lng(tech.getLocationLng())
                                    .address(tech.getLocationAddress())
                                    .build()
                                : null)
                        .build();
                builder.technician(info);
            });
        }

        return builder.build();
    }

    private JobNotificationResponse toNotificationResponse(JobNotification n) {
        return JobNotificationResponse.builder()
                .id(n.getId())
                .requestId(n.getRequestId())
                .serviceType(n.getServiceType())
                .clientName(n.getClientName())
                .distance(n.getDistance())
                .address(n.getAddress())
                .description(n.getDescription())
                .expiresAt(n.getExpiresAt())
                .status(n.getStatus().name())
                .build();
    }

    /** Haversine formula — returns distance in km */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
