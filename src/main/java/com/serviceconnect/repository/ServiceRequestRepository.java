package com.serviceconnect.repository;

import com.serviceconnect.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<ServiceRequest> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
    List<ServiceRequest> findByStatus(ServiceRequest.RequestStatus status);
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
}
