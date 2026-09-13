package com.careflow.staff.repository;

import com.careflow.common.config.JpaConfig;
import com.careflow.staff.domain.DoctorProfile;
import com.careflow.staff.domain.StaffMember;
import com.careflow.staff.domain.StaffStatus;
import com.careflow.staff.domain.StaffType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class StaffMemberRepositoryTest {

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("save should persist staff and cascade persist doctor profile (§11, §17)")
    void save_shouldPersistStaffAndDoctorProfile() {
        String staffId = UUID.randomUUID().toString();
        StaffMember staff = new StaffMember(
                staffId,
                "DOC-TEST-001",
                "dept-card-001",
                "Leonard",
                "McCoy",
                "mccoy@careflow.local",
                "+1-555-0777",
                StaffType.DOCTOR,
                LocalDate.of(2023, 1, 10)
        );

        DoctorProfile docProfile = new DoctorProfile(
                UUID.randomUUID().toString(),
                "Cardiology",
                "MD, PhD",
                "LIC-TEST-001",
                BigDecimal.valueOf(175.00),
                "Room 303",
                "Chief Medical Officer"
        );
        staff.setDoctorProfile(docProfile);

        StaffMember saved = staffMemberRepository.save(staff);
        entityManager.flush();
        entityManager.clear();

        Optional<StaffMember> found = staffMemberRepository.findByIdWithDoctorProfile(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStaffCode()).isEqualTo("DOC-TEST-001");
        assertThat(found.get().getDoctorProfile()).isNotNull();
        assertThat(found.get().getDoctorProfile().getSpecialization()).isEqualTo("Cardiology");
        assertThat(found.get().getDoctorProfile().getMedicalLicenseNumber()).isEqualTo("LIC-TEST-001");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("findByStaffCodeIgnoreCase should match code regardless of casing (§17)")
    void findByStaffCodeIgnoreCase_shouldMatchCaseInsensitive() {
        String staffId = UUID.randomUUID().toString();
        StaffMember staff = new StaffMember(
                staffId,
                "NUR-TEST-001",
                "dept-emer-001",
                "Beverly",
                "Crusher",
                "crusher@careflow.local",
                "+1-555-0888",
                StaffType.NURSE,
                LocalDate.of(2023, 2, 1)
        );
        staffMemberRepository.save(staff);
        entityManager.flush();
        entityManager.clear();

        Optional<StaffMember> foundLower = staffMemberRepository.findByStaffCodeIgnoreCase("nur-test-001");
        Optional<StaffMember> foundUpper = staffMemberRepository.findByStaffCodeIgnoreCase("NUR-TEST-001");

        assertThat(foundLower).isPresent();
        assertThat(foundUpper).isPresent();
        assertThat(foundLower.get().getLastName()).isEqualTo("Crusher");
    }

    @Test
    @DisplayName("existsByEmailIgnoreCaseAndIdNot should return true if another staff has email (§17, §92)")
    void existsByEmailIgnoreCaseAndIdNot_shouldDetectEmailClash() {
        String staffId1 = UUID.randomUUID().toString();
        StaffMember staff1 = new StaffMember(
                staffId1, "STF-001", "dept-card-001", "John", "Watson",
                "watson@careflow.local", "+1-555-0991", StaffType.DOCTOR, LocalDate.of(2023, 1, 1)
        );
        String staffId2 = UUID.randomUUID().toString();
        StaffMember staff2 = new StaffMember(
                staffId2, "STF-002", "dept-card-001", "Mary", "Watson",
                "mary@careflow.local", "+1-555-0992", StaffType.NURSE, LocalDate.of(2023, 1, 2)
        );
        staffMemberRepository.save(staff1);
        staffMemberRepository.save(staff2);
        entityManager.flush();
        entityManager.clear();

        boolean clashWithOther = staffMemberRepository.existsByEmailIgnoreCaseAndIdNot("watson@careflow.local", staffId2);
        boolean clashWithSelf = staffMemberRepository.existsByEmailIgnoreCaseAndIdNot("watson@careflow.local", staffId1);

        assertThat(clashWithOther).isTrue();
        assertThat(clashWithSelf).isFalse();
    }

    @Test
    @DisplayName("Specification withFilters should filter staff by department and staffType (§17, §72)")
    void withFilters_shouldFilterByDepartmentAndType() {
        String id1 = UUID.randomUUID().toString();
        StaffMember staff1 = new StaffMember(
                id1, "STF-PEDI-01", "dept-pedi-001", "Donna", "Noble",
                "donna@careflow.local", "+1-555-0993", StaffType.NURSE, LocalDate.of(2023, 1, 1)
        );
        String id2 = UUID.randomUUID().toString();
        StaffMember staff2 = new StaffMember(
                id2, "STF-CARD-02", "dept-card-001", "Martha", "Jones",
                "martha@careflow.local", "+1-555-0994", StaffType.DOCTOR, LocalDate.of(2023, 1, 2)
        );
        staffMemberRepository.save(staff1);
        staffMemberRepository.save(staff2);
        entityManager.flush();
        entityManager.clear();

        Specification<StaffMember> spec = StaffMemberSpecification.withFilters(null, "dept-pedi-001", StaffType.NURSE, StaffStatus.ACTIVE);
        Page<StaffMember> page = staffMemberRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStaffCode()).isEqualTo("STF-PEDI-01");
    }
}
