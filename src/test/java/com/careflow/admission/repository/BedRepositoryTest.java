package com.careflow.admission.repository;

import com.careflow.admission.domain.Bed;
import com.careflow.admission.domain.BedStatus;
import com.careflow.admission.domain.Room;
import com.careflow.admission.domain.RoomType;
import com.careflow.admission.domain.Ward;
import com.careflow.admission.domain.WardType;
import com.careflow.department.domain.Department;
import com.careflow.department.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BedRepositoryTest {

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Ward ward;
    private Room room;
    private Bed bedAvailable;
    private Bed bedOccupied;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(new Department(
                UUID.randomUUID().toString(), "SURG-" + UUID.randomUUID().toString().substring(0, 4),
                "Surgery", "Surgical Dept", "Floor 3"
        ));

        ward = wardRepository.save(new Ward(
                UUID.randomUUID().toString(), "WARD-SURG-" + UUID.randomUUID().toString().substring(0, 4),
                "Surgical Ward", dept.getId(), WardType.SURGICAL, "Floor 3", 2, true
        ));

        room = roomRepository.save(new Room(
                UUID.randomUUID().toString(), "SURG-301", ward, RoomType.SEMI_PRIVATE, true
        ));

        bedAvailable = bedRepository.save(new Bed(
                UUID.randomUUID().toString(), "BED-301-A", room, BedStatus.AVAILABLE, BigDecimal.valueOf(200.00), true
        ));

        bedOccupied = bedRepository.save(new Bed(
                UUID.randomUUID().toString(), "BED-301-B", room, BedStatus.OCCUPIED, BigDecimal.valueOf(200.00), true
        ));
    }

    @Test
    @DisplayName("Should query available beds by ward")
    void findAvailableBedsByWardId() {
        List<Bed> availableBeds = bedRepository.findAvailableBedsByWardId(ward.getId());
        assertThat(availableBeds).hasSize(1);
        assertThat(availableBeds.get(0).getBedNumber()).isEqualTo("BED-301-A");
        assertThat(availableBeds.get(0).getStatus()).isEqualTo(BedStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should retrieve bed with pessimistic lock using findByIdForUpdate")
    void findByIdForUpdate_success() {
        Optional<Bed> lockedBed = bedRepository.findByIdForUpdate(bedAvailable.getId());
        assertThat(lockedBed).isPresent();
        assertThat(lockedBed.get().getId()).isEqualTo(bedAvailable.getId());
        assertThat(lockedBed.get().getStatus()).isEqualTo(BedStatus.AVAILABLE);
    }
}
