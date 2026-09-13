package com.careflow.admission.repository;

import com.careflow.admission.domain.Bed;
import com.careflow.admission.domain.BedStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for physical beds with pessimistic write locking (§28, §57 Lab 4, §92).
 */
@Repository
public interface BedRepository extends JpaRepository<Bed, String> {

    List<Bed> findByRoomIdAndActiveTrue(String roomId);

    List<Bed> findByStatusAndActiveTrue(BedStatus status);

    @Query("SELECT b FROM Bed b WHERE b.room.ward.id = :wardId AND b.active = true")
    List<Bed> findByWardId(@Param("wardId") String wardId);

    @Query("SELECT b FROM Bed b WHERE b.room.ward.id = :wardId AND b.status = 'AVAILABLE' AND b.active = true")
    List<Bed> findAvailableBedsByWardId(@Param("wardId") String wardId);

    /**
     * Acquires an exclusive pessimistic write lock on the bed row (SELECT ... FOR UPDATE).
     * Prevents race conditions and double-occupancy during concurrent patient admissions (§57 Lab 4).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bed b WHERE b.id = :id")
    Optional<Bed> findByIdForUpdate(@Param("id") String id);
}
