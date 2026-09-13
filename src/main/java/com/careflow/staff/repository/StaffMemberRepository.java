package com.careflow.staff.repository;

import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for StaffMember entity (§11, §17).
 */
@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, String>, JpaSpecificationExecutor<StaffMember> {

    Optional<StaffMember> findByStaffCodeIgnoreCase(String staffCode);

    Optional<StaffMember> findByUserId(String userId);

    Optional<StaffMember> findByEmailIgnoreCase(String email);

    boolean existsByStaffCodeIgnoreCase(String staffCode);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUserId(String userId);

    boolean existsByStaffCodeIgnoreCaseAndIdNot(String staffCode, String id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, String id);

    boolean existsByUserIdAndIdNot(String userId, String id);

    List<StaffMember> findByDepartmentId(String departmentId);

    List<StaffMember> findByStaffType(StaffType staffType);

    List<StaffMember> findByStatus(StaffStatus status);

    @Query("SELECT s FROM StaffMember s LEFT JOIN FETCH s.doctorProfile WHERE s.id = :id")
    Optional<StaffMember> findByIdWithDoctorProfile(@Param("id") String id);

    @Query("SELECT s FROM StaffMember s LEFT JOIN FETCH s.doctorProfile WHERE UPPER(s.staffCode) = UPPER(:staffCode)")
    Optional<StaffMember> findByStaffCodeWithDoctorProfile(@Param("staffCode") String staffCode);

    @Query("SELECT s FROM StaffMember s LEFT JOIN FETCH s.doctorProfile WHERE s.userId = :userId")
    Optional<StaffMember> findByUserIdWithDoctorProfile(@Param("userId") String userId);
}
