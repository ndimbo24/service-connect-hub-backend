package com.serviceconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private String clientName;

    private Long technicianId;
    private String technicianName;

    @Column(nullable = false)
    private String serviceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.pending;

    // Location
    @Column(nullable = false)
    private Double locationLat;

    @Column(nullable = false)
    private Double locationLng;

    @Column(nullable = false)
    private String locationAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
    private LocalDateTime estimatedArrival;

    public enum RequestStatus {
        pending, searching, matched, in_progress, completed, cancelled
    }
}
