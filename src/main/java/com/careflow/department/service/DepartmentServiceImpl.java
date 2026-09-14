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
import com.careflow.common.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of DepartmentService managing lifecycle and organizational hierarchies (§13, §17).
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        String normalizedName = request.name().trim();
        log.info("Creating hospital department code='{}', name='{}'", normalizedCode, normalizedName);

        if (departmentRepository.existsByCodeIgnoreCase(normalizedCode)) {
            log.warn("Conflict: Department code '{}' already exists", normalizedCode);
            throw DuplicateDepartmentException.forCode(normalizedCode);
        }

        if (departmentRepository.existsByNameIgnoreCase(normalizedName)) {
            log.warn("Conflict: Department name '{}' already exists", normalizedName);
            throw DuplicateDepartmentException.forName(normalizedName);
        }

        String id = UUID.randomUUID().toString();
        Department department = departmentMapper.toEntity(id, request);
        department.setCode(normalizedCode);

        Department saved = departmentRepository.save(department);
        log.info("Successfully created department id='{}', code='{}'", saved.getId(), saved.getCode());
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_DEPARTMENTS, key = "#id")
    public DepartmentResponse getDepartmentById(String id) {
        log.debug("Fetching department with id='{}'", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentByCode(String code) {
        String normalizedCode = code.trim().toUpperCase();
        log.debug("Fetching department with code='{}'", normalizedCode);
        Department department = departmentRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> DepartmentNotFoundException.forCode(normalizedCode));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_DEPARTMENTS, key = "'status:' + (#status != null ? #status.name() : 'ALL')")
    public List<DepartmentResponse> getAllDepartments(DepartmentStatus status) {
        log.debug("Fetching departments with status filter='{}'", status);
        List<Department> departments;
        if (status != null) {
            departments = departmentRepository.findByStatusOrderByNameAsc(status);
        } else {
            departments = departmentRepository.findAllByOrderByNameAsc();
        }
        return departmentMapper.toResponseList(departments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSummaryResponse> searchDepartments(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String trimmedQuery = query.trim();
        log.debug("Searching departments with query='{}'", trimmedQuery);
        List<Department> matches = departmentRepository.searchDepartments(trimmedQuery);
        return departmentMapper.toSummaryResponseList(matches);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    public DepartmentResponse updateDepartment(String id, UpdateDepartmentRequest request) {
        String normalizedName = request.name().trim();
        log.info("Updating department id='{}', name='{}'", id, normalizedName);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            log.warn("Conflict: Department name '{}' already taken by another department", normalizedName);
            throw DuplicateDepartmentException.forName(normalizedName);
        }

        departmentMapper.updateEntity(department, request);
        Department updated = departmentRepository.save(department);
        log.info("Updated department id='{}'", updated.getId());
        return departmentMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    public DepartmentResponse updateDepartmentStatus(String id, UpdateDepartmentStatusRequest request) {
        log.info("Updating status of department id='{}' to '{}'", id, request.status());

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        department.setStatus(request.status());
        Department updated = departmentRepository.save(department);
        log.info("Department id='{}' status transitioned to '{}'", updated.getId(), updated.getStatus());
        return departmentMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    public void deactivateDepartment(String id) {
        log.info("Deactivating department id='{}'", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        department.deactivate();
        departmentRepository.save(department);
        log.info("Department id='{}' marked INACTIVE", id);
    }
}
