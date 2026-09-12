package com.careflow.department.controller;

import com.careflow.department.domain.DepartmentStatus;
import com.careflow.department.dto.CreateDepartmentRequest;
import com.careflow.department.dto.DepartmentResponse;
import com.careflow.department.dto.DepartmentSummaryResponse;
import com.careflow.department.dto.UpdateDepartmentRequest;
import com.careflow.department.dto.UpdateDepartmentStatusRequest;
import com.careflow.department.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST Controller exposing hospital department operational APIs (§17, §39, §89).
 */
@RestController
@RequestMapping("/api/v1/departments")
@Validated
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentResponse response = departmentService.createDepartment(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments(
            @RequestParam(required = false) DepartmentStatus status
    ) {
        List<DepartmentResponse> departments = departmentService.getAllDepartments(status);
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentSummaryResponse>> searchDepartments(
            @RequestParam String query
    ) {
        List<DepartmentSummaryResponse> results = departmentService.searchDepartments(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable String id) {
        DepartmentResponse response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepartmentResponse> getDepartmentByCode(@PathVariable String code) {
        DepartmentResponse response = departmentService.getDepartmentByCode(code);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable String id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        DepartmentResponse response = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> updateDepartmentStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateDepartmentStatusRequest request
    ) {
        DepartmentResponse response = departmentService.updateDepartmentStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateDepartment(@PathVariable String id) {
        departmentService.deactivateDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
