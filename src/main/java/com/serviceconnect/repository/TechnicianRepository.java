package com.serviceconnect.repository;

import com.serviceconnect.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    List<Technician> findByStatus(Technician.ApprovalStatus status);

    List<Technician> findByAvailability(Technician.Availability availability);

    @Query("SELECT t FROM Technician t WHERE :serviceType MEMBER OF t.serviceTypes AND t.status = 'approved' AND t.availability = 'available'")
    List<Technician> findAvailableByServiceType(@Param("serviceType") String serviceType);

    @Query("SELECT t FROM Technician t WHERE (LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.locationAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Technician> searchByKeyword(@Param("keyword") String keyword);

    Optional<Technician> findByPhone(String phone);
}
