package com.serviceconnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "technicians")
@DiscriminatorValue("technician")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Technician extends User {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_service_types", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "service_type")
    private List<String> serviceTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability = Availability.offline;

    private Double rating = 0.0;

    private Integer totalJobs = 0;

    // Location fields
    private Double locationLat;
    private Double locationLng;
    private String locationAddress;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_documents", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "document_url")
    private List<String> documents;

    private String rejectionReason;

    public enum ApprovalStatus {
        pending, approved, rejected
    }

    public enum Availability {
        offline, available, busy
    }
}
