package com.careflow.admission.repository;

import com.careflow.admission.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ward rooms (§28).
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    List<Room> findByWardIdAndActiveTrue(String wardId);

    Optional<Room> findByWardIdAndRoomNumber(String wardId, String roomNumber);
}
