package com.careflow.administration.repository;

import com.careflow.administration.domain.SettingCategory;
import com.careflow.administration.domain.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for SystemSetting entities (§38).
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findByCategoryOrderBySettingKeyAsc(SettingCategory category);

    List<SystemSetting> findAllByOrderByCategoryAscSettingKeyAsc();

    boolean existsBySettingKey(String settingKey);
}
