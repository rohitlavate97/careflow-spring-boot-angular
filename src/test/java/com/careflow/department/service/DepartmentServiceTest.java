package com.careflow.department.service;

import com.careflow.department.domain.Department;
import com.careflow.department.domain.DepartmentStatus;
import com.careflow.department.dto.CreateDepartmentRequest;
import com.careflow.department.dto.DepartmentResponse;
import com.careflow.department.dto.DepartmentSummaryResponse;
import com.careflow.department.dto.UpdateDepartmentRequest;
import com.careflow.department.dto.UpdateDepartmentStatusRequest;
import com.careflow.department.exception.DepartmentNotFoundException;
import com.careflow.department.exception.DuplicateDepartmentException;
import com.careflow.department.mapper.DepartmentMapper;
import com.careflow.department.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private DepartmentMapper departmentMapper;
    private DepartmentServiceImpl departmentService;

    @BeforeEach
    void setUp() {
        departmentMapper = new DepartmentMapper();
        departmentService = new DepartmentServiceImpl(departmentRepository, departmentMapper);
    }

    @Test
    @DisplayName("createDepartment should save and return department response when code and name are unique (§17)")
    void createDepartment_shouldSucceed_whenValid() {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "ONCO",
                "Oncology",
                "Department of Oncology and Cancer Research",
                "+1-555-0300",
                "oncology@careflow.local",
                "Building E, Floor 2",
                null
        );

        when(departmentRepository.existsByCodeIgnoreCase("ONCO")).thenReturn(false);
        when(departmentRepository.existsByNameIgnoreCase("Oncology")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse response = departmentService.createDepartment(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("ONCO");
        assertThat(response.name()).isEqualTo("Oncology");
        assertThat(response.status()).isEqualTo(DepartmentStatus.ACTIVE);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("createDepartment should throw DuplicateDepartmentException when code already exists (§17, §92)")
    void createDepartment_shouldThrowDuplicate_whenCodeExists() {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "CARD",
                "Cardiology duplicate",
                null,
                null,
                null,
                null,
                null
        );

        when(departmentRepository.existsByCodeIgnoreCase("CARD")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DuplicateDepartmentException.class)
                .hasMessageContaining("code 'CARD'");
    }

    @Test
    @DisplayName("createDepartment should throw DuplicateDepartmentException when name already exists (§17, §92)")
    void createDepartment_shouldThrowDuplicate_whenNameExists() {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "CAR2",
                "Cardiology",
                null,
                null,
                null,
                null,
                null
        );

        when(departmentRepository.existsByCodeIgnoreCase("CAR2")).thenReturn(false);
        when(departmentRepository.existsByNameIgnoreCase("Cardiology")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DuplicateDepartmentException.class)
                .hasMessageContaining("name 'Cardiology'");
    }

    @Test
    @DisplayName("getDepartmentByCode should return department when found (§17)")
    void getDepartmentByCode_shouldReturnDepartment_whenFound() {
        Department dept = new Department("d-1", "CARD", "Cardiology", "Cardio desc", "Building A");
        when(departmentRepository.findByCodeIgnoreCase("CARD")).thenReturn(Optional.of(dept));

        DepartmentResponse response = departmentService.getDepartmentByCode("card");

        assertThat(response.code()).isEqualTo("CARD");
        assertThat(response.name()).isEqualTo("Cardiology");
    }

    @Test
    @DisplayName("getDepartmentByCode should throw DepartmentNotFoundException when not found (§17)")
    void getDepartmentByCode_shouldThrowNotFound_whenNotFound() {
        when(departmentRepository.findByCodeIgnoreCase("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartmentByCode("XYZ"))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    @DisplayName("updateDepartment should modify details when name is not taken (§17)")
    void updateDepartment_shouldModifyDetails() {
        Department existing = new Department("d-1", "ORTH", "Orthopedics", "Old desc", "Building B");
        UpdateDepartmentRequest updateReq = new UpdateDepartmentRequest(
                "Orthopedic Surgery",
                "Updated specialized surgery desc",
                "+1-555-0450",
                "ortho@careflow.local",
                "Building B, Floor 2",
                "staff-001"
        );

        when(departmentRepository.findById("d-1")).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Orthopedic Surgery", "d-1")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse response = departmentService.updateDepartment("d-1", updateReq);

        assertThat(response.name()).isEqualTo("Orthopedic Surgery");
        assertThat(response.description()).isEqualTo("Updated specialized surgery desc");
        assertThat(response.contactPhone()).isEqualTo("+1-555-0450");
        assertThat(response.headOfDepartmentId()).isEqualTo("staff-001");
    }

    @Test
    @DisplayName("updateDepartmentStatus should transition status to SUSPENDED (§17, §69)")
    void updateDepartmentStatus_shouldTransitionStatus() {
        Department existing = new Department("d-1", "RADI", "Radiology", null, null);
        UpdateDepartmentStatusRequest statusReq = new UpdateDepartmentStatusRequest(DepartmentStatus.SUSPENDED);

        when(departmentRepository.findById("d-1")).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse response = departmentService.updateDepartmentStatus("d-1", statusReq);

        assertThat(response.status()).isEqualTo(DepartmentStatus.SUSPENDED);
    }

    @Test
    @DisplayName("searchDepartments should return matching summaries (§17)")
    void searchDepartments_shouldReturnMatchingSummaries() {
        Department dept1 = new Department("d-1", "CARD", "Cardiology", null, "Building A");
        Department dept2 = new Department("d-2", "PEDI", "Pediatrics", null, "Building B");

        when(departmentRepository.searchDepartments("di")).thenReturn(List.of(dept1, dept2));

        List<DepartmentSummaryResponse> results = departmentService.searchDepartments("di");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).code()).isEqualTo("CARD");
        assertThat(results.get(1).code()).isEqualTo("PEDI");
    }
}
