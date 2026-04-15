package com.serviceconnect.controller;

import com.serviceconnect.dto.request.CreateServiceRequest;
import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.JobNotificationResponse;
import com.serviceconnect.dto.response.ServiceRequestResponse;
import com.serviceconnect.dto.response.TrackingResponse;
import com.serviceconnect.security.UserPrincipal;
import com.serviceconnect.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService requestService;

    /**
     * POST /requests
     * Create a new service request (client only).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> createRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateServiceRequest dto) {
        ServiceRequestResponse response = requestService.createRequest(principal.getId(), dto);
        return ResponseEntity.status(201).body(ApiResponse.success("Request created and matching started", response));
    }

    /**
     * GET /requests/:id
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getById(id)));
    }

    /**
     * GET /requests?clientId=...  OR  ?technicianId=...
     * If no param, returns all (admin use).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getRequests(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long technicianId) {

        List<ServiceRequestResponse> list;
        if (clientId != null) {
            list = requestService.getByClientId(clientId);
        } else if (technicianId != null) {
            list = requestService.getByTechnicianId(technicianId);
        } else {
            list = requestService.getAll();
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * GET /requests/:id/status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStatus(@PathVariable Long id) {
        ServiceRequestResponse req = requestService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "requestId", String.valueOf(id),
            "status",    req.getStatus()
        )));
    }

    /**
     * POST /requests/:id/assign
     * Body: { technicianId: ... }
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> assignTechnician(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long technicianId = body.get("technicianId");
        ServiceRequestResponse response = requestService.assignTechnician(id, technicianId);
        return ResponseEntity.ok(ApiResponse.success("Technician assigned", response));
    }

    /**
     * POST /requests/:id/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceRequestResponse response = requestService.cancelRequest(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Request cancelled", response));
    }

    /**
     * POST /requests/:id/status
     * Body: { status: "in_progress" | "completed" | ... }
     */
    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String newStatus = body.get("status");
        ServiceRequestResponse response = requestService.updateStatus(id, newStatus, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Status updated", response));
    }

    /**
     * GET /requests/:id/tracking
     * Returns technician live location + ETA for a request.
     */
    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<TrackingResponse>> getTracking(@PathVariable Long id) {
        TrackingResponse tracking = requestService.getTracking(id);
        return ResponseEntity.ok(ApiResponse.success(tracking));
    }

    /**
     * GET /requests/notifications/pending
     * Pending job notifications for the authenticated technician.
     */
    @GetMapping("/notifications/pending")
    public ResponseEntity<ApiResponse<List<JobNotificationResponse>>> getPendingNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<JobNotificationResponse> notifications =
                requestService.getPendingNotifications(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }
}
