# ADR-0018: Redis Caching and Resilient Infrastructure with Graceful Degradation

## Status
Accepted

## Context
CareFlow orchestrates operations across 20 distinct domain modules (§38, §103 Phase 17). As system load scales, read-heavy reference and configuration catalogs (such as hospital departments and institutional system settings) are accessed repeatedly across high-frequency workflows (e.g., patient registration, appointment scheduling, queue routing, and order entry).

Key architectural and resilience requirements (§50, §72, §76 failure scenario 10, §113):
1. **Cache Justification & Scoping (§50)**:
   - Caching must only be introduced where quantitatively justified by high read-to-write ratios and read-heavy reference catalogs.
   - Reference data to cache: `system-settings` (global configurations, facility policies) and `departments` (hospital units, medical departments).
2. **Clinical & Financial Safety Guardrails (§50, §105)**:
   - **Never Cache**: Highly dynamic, transactional, or clinically critical entities—such as patient vitals, queue positions, inpatient bed allocations, and billing invoices. Serving stale data in clinical or financial contexts could lead to catastrophic clinical decisions, double-allocation, or accounting discrepancies.
3. **Cache-Aside Pattern & Deterministic Invalidation**:
   - Services must adopt the cache-aside pattern (`@Cacheable`). Mutations (updates, additions, status changes, deactivations) must deterministically evict cached entries via `@CacheEvict` (or `@Caching(evict = ...)`), guaranteeing eventual and immediate consistency upon updates.
4. **Graceful Degradation under Infrastructure Failure (§50, §76 Failure Scenario 10, §113)**:
   - External dependencies such as Redis are volatile network infrastructure. If Redis experiences network partition, crashes, runs out of memory, or fails connection timeouts, **the application must not crash or fail incoming user requests**.
   - The platform must intercept Redis cache access exceptions, log clear diagnostic warnings, and seamlessly fall back to executing queries against the primary relational database.
5. **Serialization & Type Safety**:
   - Cached values must be serialized to JSON (`GenericJackson2JsonRedisSerializer`) with ISO-8601 timestamp support via Jackson's `JavaTimeModule` to ensure cross-service interoperability and readable cache inspection.
6. **Environment Agnostic Testing**:
   - Automated builds, CI pipelines, and unit tests must execute deterministically without mandating an active external Redis instance.

## Decision
1. **Dependencies & Framework Integration**:
   - Integrated `spring-boot-starter-cache` and `spring-boot-starter-data-redis` in `pom.xml`.
2. **Resilient Cache Error Handler (`ResilientCacheErrorHandler`)**:
   - Implemented custom `CacheErrorHandler` extending Spring's caching infrastructure.
   - Suppresses exceptions during `handleCacheGetError`, `handleCachePutError`, `handleCacheEvictError`, and `handleCacheClearError`.
   - Emits structured `WARN` logs while allowing the underlying business methods to execute transparently against MySQL.
3. **Centralized Cache Configuration (`CacheConfig`)**:
   - Annotated with `@EnableCaching`.
   - Registers `ResilientCacheErrorHandler` via `CachingConfigurer.errorHandler()`.
   - Configures `RedisCacheConfiguration` with:
     - Key serialization: `StringRedisSerializer`
     - Value serialization: `GenericJackson2JsonRedisSerializer` configured with `ObjectMapper` and `JavaTimeModule`
     - Null value caching disabled (`disableCachingNullValues()`)
     - Default TTL: 30 minutes
     - Customized TTL for `departments` (60 minutes) and `system-settings` (15 minutes)
   - Guarded Redis-specific bean configurations with `@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")`.
4. **Application Caching in Core Services**:
   - `AdministrationServiceImpl`:
     - `@Cacheable(value = "system-settings", key = "'all'")` on `getSettings(category)`
     - `@Cacheable(value = "system-settings", key = "#key")` on `getSettingByKey(key)`
     - `@CacheEvict(value = "system-settings", allEntries = true)` on `updateSetting`, `batchUpdateSettings`, and `setMaintenanceMode`
   - `DepartmentServiceImpl`:
     - `@Cacheable(value = "departments", key = "#id")` on `getDepartmentById(id)`
     - `@Cacheable(value = "departments", key = "'all'")` on `getAllDepartments()`
     - `@CacheEvict(value = "departments", allEntries = true)` on `createDepartment`, `updateDepartment`, `updateDepartmentStatus`, and `deactivateDepartment`
5. **Testing Architecture**:
   - Configured `application-test.yml` with `spring.cache.type: simple` (using Spring's in-memory `ConcurrentMapCacheManager`) ensuring fast, isolated unit and integration test runs without requiring an external Redis daemon.
   - Added `ResilientCacheErrorHandlerTest` verifying robust exception suppression across all cache operations.
   - Added `CacheIntegrationTest` verifying end-to-end cache hit, DB bypass, and cache eviction upon mutation.

## Consequences
- **Positive**:
  - Dramatic reduction in redundant SQL queries for hospital organizational hierarchy and administrative settings.
  - Zero downtime / zero user-facing errors if Redis becomes unavailable (Graceful Degradation verified).
  - Strict compliance with CareFlow Clinical Guardrails (§50) by refusing to cache dynamic patient or clinical data.
  - Full test suite passes hermetically in CI/CD without Redis containers.
- **Negative / Trade-offs**:
  - Redis cache deserialization requires JSON-compatible DTOs or entities with default constructors.
  - Distributed cache eviction requires all write paths to pass through `@CacheEvict` annotated service methods.
