package com.careflow.staff.repository;

import com.careflow.staff.domain.DoctorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DoctorProfile entity (§11, §17).
 */
@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, String> {

    Optional<DoctorProfile> findByMedicalLicenseNumberIgnoreCase(String medicalLicenseNumber);

    boolean existsByMedicalLicenseNumberIgnoreCase(String medicalLicenseNumber);

    boolean existsByMedicalLicenseNumberIgnoreCaseAndIdNot(String medicalLicenseNumber, String id);

    Optional<DoctorProfile> findByStaffMemberId(String staffId);

    @Query("SELECT d FROM DoctorProfile d JOIN FETCH d.staffMember s WHERE s.status = 'ACTIVE' " +
           "AND (:specialization IS NULL OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :specialization, '%'))) " +
           "AND (:departmentId IS NULL OR s.departmentId = :departmentId)")
    List<DoctorProfile> findActiveDoctors(
            @Param("specialization") String specialization,
            @Param("departmentId") String departmentId
    );

    @Query(value = "SELECT d FROM DoctorProfile d JOIN FETCH d.staffMember s WHERE s.status = 'ACTIVE' " +
                   "AND (:specialization IS NULL OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :specialization, '%'))) " +
                   "AND (:departmentId IS NULL OR s.departmentId = :departmentId)",
           countQuery = "SELECT COUNT(d) FROM DoctorProfile d JOIN d.staffMember s WHERE s.status = 'ACTIVE' " +
                        "AND (:specialization IS NULL OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :specialization, '%'))) " +
                        "AND (:departmentId IS NULL OR s.departmentId = :departmentId)")
    Page<DoctorProfile> findActiveDoctorsPaged(
            @Param("specialization") String specialization,
            @Param("departmentId") String departmentId,
            Pageable pageable
    );
}
