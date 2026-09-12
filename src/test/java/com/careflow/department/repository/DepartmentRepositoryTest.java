package com.careflow.department.repository;

import com.careflow.common.config.JpaConfig;
import com.careflow.department.domain.Department;
import com.careflow.department.domain.DepartmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("save should persist department with unique code and audit timestamps (§11, §17)")
    void save_shouldPersistDepartment() {
        Department department = new Department(
                UUID.randomUUID().toString(),
                "DERM",
                "Dermatology",
                "Department of Dermatology",
                "Building E, Floor 1"
        );
        department.setContactPhone("+1-555-0789");
        department.setContactEmail("dermatology@careflow.local");

        Department saved = departmentRepository.save(department);
        entityManager.flush();
        entityManager.clear();

        Optional<Department> found = departmentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("DERM");
        assertThat(found.get().getName()).isEqualTo("Dermatology");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getStatus()).isEqualTo(DepartmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByCodeIgnoreCase should match code regardless of casing (§17)")
    void findByCodeIgnoreCase_shouldMatchCaseInsensitive() {
        Department department = new Department(
                UUID.randomUUID().toString(),
                "URO",
                "Urology",
                "Department of Urology",
                "Building F, Floor 2"
        );
        departmentRepository.save(department);
        entityManager.flush();
        entityManager.clear();

        Optional<Department> foundLower = departmentRepository.findByCodeIgnoreCase("uro");
        Optional<Department> foundUpper = departmentRepository.findByCodeIgnoreCase("URO");

        assertThat(foundLower).isPresent();
        assertThat(foundUpper).isPresent();
        assertThat(foundLower.get().getName()).isEqualTo("Urology");
    }

    @Test
    @DisplayName("searchDepartments should return matches matching either code or name (§17)")
    void searchDepartments_shouldMatchNameOrCode() {
        Department dept1 = new Department(UUID.randomUUID().toString(), "ENT", "Otolaryngology", "Ear Nose Throat", "Building A");
        Department dept2 = new Department(UUID.randomUUID().toString(), "OPHT", "Ophthalmology", "Eye clinic", "Building B");

        departmentRepository.saveAll(List.of(dept1, dept2));
        entityManager.flush();
        entityManager.clear();

        List<Department> codeMatches = departmentRepository.searchDepartments("ent");
        List<Department> nameMatches = departmentRepository.searchDepartments("ophthal");

        assertThat(codeMatches).isNotEmpty();
        assertThat(nameMatches).isNotEmpty();
        assertThat(nameMatches.get(0).getCode()).isEqualTo("OPHT");
    }
}
