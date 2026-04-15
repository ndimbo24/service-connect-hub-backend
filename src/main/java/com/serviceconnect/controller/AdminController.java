package com.serviceconnect.controller;

import com.serviceconnect.dto.response.ActivityLogResponse;
import com.serviceconnect.dto.response.ApiResponse;
import com.serviceconnect.dto.response.ServiceRequestResponse;
import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.security.UserPrincipal;
import com.serviceconnect.service.AdminService;
import com.serviceconnect.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ServiceRequestService requestService;

    // ─────────────────────────────────────
    // TECHNICIAN MANAGEMENT
    // ─────────────────────────────────────

    /**
     * GET /admin/technicians?status=pending|approved|rejected
     */
    @GetMapping("/technicians")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getTechnicians(
            @RequestParam(required = false) String status) {
        List<UserResponse> technicians = adminService.getTechnicians(status);
        return ResponseEntity.ok(ApiResponse.success(technicians));
    }

    /**
     * GET /admin/technicians/:id
     */
    @GetMapping("/technicians/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getTechnicianById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTechnicianById(id)));
    }

    /**
     * POST /admin/technicians/:id/approve
     */
    @PostMapping("/technicians/{id}/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approveTechnician(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse updated = adminService.approveTechnician(id, principal.getId(), principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success("Technician approved successfully", updated));
    }

    /**
     * POST /admin/technicians/:id/reject
     * Body: { reason: "..." }
     */
    @PostMapping("/technicians/{id}/reject")
    public ResponseEntity<ApiResponse<UserResponse>> rejectTechnician(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = (body != null) ? body.getOrDefault("reason", "No reason provided") : "No reason provided";
        UserResponse updated = adminService.rejectTechnician(id, reason, principal.getId(), principal.getUser().getName());
        return ResponseEntity.ok(ApiResponse.success("Technician rejected", updated));
    }

    // ─────────────────────────────────────
    // SERVICE REQUESTS (admin view)
    // ─────────────────────────────────────

    /**
     * GET /admin/requests  — all requests
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.success(requestService.getAll()));
    }

    // ─────────────────────────────────────
    // ACTIVITY LOGS
    // ─────────────────────────────────────

    /**
     * GET /admin/logs
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getLogs() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getLogs()));
    }

    // ─────────────────────────────────────
    // DASHBOARD STATS
    // ─────────────────────────────────────

    /**
     * GET /admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        List<UserResponse> allTechs = adminService.getTechnicians(null);
        List<ServiceRequestResponse> allRequests = requestService.getAll();

        long pending   = allTechs.stream().filter(t -> "pending".equals(t.getStatus())).count();
        long approved  = allTechs.stream().filter(t -> "approved".equals(t.getStatus())).count();
        long rejected  = allTechs.stream().filter(t -> "rejected".equals(t.getStatus())).count();

        long activeReqs    = allRequests.stream()
                .filter(r -> !List.of("completed","cancelled").contains(r.getStatus())).count();
        long completedReqs = allRequests.stream()
                .filter(r -> "completed".equals(r.getStatus())).count();

        Map<String, Object> stats = Map.of(
            "totalTechnicians",     allTechs.size(),
            "pendingTechnicians",   pending,
            "approvedTechnicians",  approved,
            "rejectedTechnicians",  rejected,
            "totalRequests",        allRequests.size(),
            "activeRequests",       activeReqs,
            "completedRequests",    completedReqs
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
