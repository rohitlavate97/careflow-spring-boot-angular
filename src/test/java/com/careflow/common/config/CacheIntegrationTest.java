package com.careflow.common.config;

import com.careflow.administration.domain.SettingCategory;
import com.careflow.administration.domain.SettingDataType;
import com.careflow.administration.domain.SystemSetting;
import com.careflow.administration.dto.SystemSettingResponse;
import com.careflow.administration.dto.UpdateSystemSettingRequest;
import com.careflow.administration.repository.SystemSettingRepository;
import com.careflow.administration.service.AdministrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private AdministrationService administrationService;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @BeforeEach
    void setUp() {
        if (systemSettingRepository.findBySettingKey("test.cache.key").isEmpty()) {
            systemSettingRepository.save(new SystemSetting(
                    "set-cache-1",
                    "test.cache.key",
                    "Initial Value",
                    SettingCategory.GENERAL,
                    SettingDataType.STRING,
                    "Cache test setting",
                    false,
                    true
            ));
        }
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_SYSTEM_SETTINGS);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("CacheManager is initialized with system-settings cache")
    void cacheManager_Initialized() {
        assertThat(cacheManager).isNotNull();
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_SYSTEM_SETTINGS);
        assertThat(cache).isNotNull();
    }

    @Test
    @DisplayName("@Cacheable caches setting value in cache manager on lookup")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void cacheable_PopulatesCacheOnRead() {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_SYSTEM_SETTINGS);
        assertThat(cache).isNotNull();
        assertThat(cache.get("test.cache.key")).isNull();

        SystemSettingResponse response = administrationService.getSettingByKey("test.cache.key");
        assertThat(response.settingValue()).isEqualTo("Initial Value");

        // Verify it is now present in the cache
        Cache.ValueWrapper cached = cache.get("test.cache.key");
        assertThat(cached).isNotNull();
        assertThat(((SystemSettingResponse) cached.get()).settingValue()).isEqualTo("Initial Value");
    }

    @Test
    @DisplayName("@CacheEvict clears cached settings when a setting is updated")
    @WithMockUser(username = "admin@careflow.local", roles = "ADMIN")
    void cacheEvict_ClearsCacheOnUpdate() {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_SYSTEM_SETTINGS);
        assertThat(cache).isNotNull();

        // 1. Warm cache
        administrationService.getSettingByKey("test.cache.key");
        assertThat(cache.get("test.cache.key")).isNotNull();

        // 2. Update setting (triggers @CacheEvict allEntries = true)
        administrationService.updateSetting("test.cache.key", new UpdateSystemSettingRequest("Updated Value"));

        // 3. Cache should be cleared
        assertThat(cache.get("test.cache.key")).isNull();

        // 4. Next read fetches fresh value and re-populates cache
        SystemSettingResponse fresh = administrationService.getSettingByKey("test.cache.key");
        assertThat(fresh.settingValue()).isEqualTo("Updated Value");
        assertThat(cache.get("test.cache.key")).isNotNull();
    }
}
