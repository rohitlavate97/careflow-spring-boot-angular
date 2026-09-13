package com.careflow.department.repository;

import com.careflow.department.domain.Department;
import com.careflow.department.domain.DepartmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Department entities (§11, §17).
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Department d WHERE d.id = :id")
    Optional<Department> findByIdForUpdate(@Param("id") String id);

    Optional<Department> findByCodeIgnoreCase(String code);

    Optional<Department> findByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    List<Department> findByStatusOrderByNameAsc(DepartmentStatus status);

    List<Department> findAllByOrderByNameAsc();

    @Query("SELECT d FROM Department d WHERE " +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(d.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY d.name ASC")
    List<Department> searchDepartments(@Param("query") String query);
}
