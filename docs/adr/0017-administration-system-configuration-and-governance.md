# ADR-0017: Hospital System Administration, Dynamic Configuration Management, and Governance Architecture

## Status
Accepted

## Context
CareFlow operates as a comprehensive modular monolith orchestrating all 20 hospital clinical, operational, diagnostic, and financial domains (§38, §103 Phase 17).

Key administrative and governance requirements addressed:
1. **Centralized Enterprise Administration (§38)**:
   - System administrators (`ROLE_ADMIN`) require unified management of hospital configurations, user lifecycles, and institutional facility parameters without requiring code deployments or direct database access.
2. **Dynamic System Configuration Management**:
   - Hospital operational variables (facility name, currency, timezone, consultation slot length, appointment cancellation notice cutoff, queue thresholds, value-added tax rates, maintenance state) must be dynamic, typed, categorized, and validated.
   - Protected settings (such as facility organizational code) must be immutable via user-facing APIs (`is_editable = false`).
3. **User Lifecycle Governance & Anti-Lockout Safeguards (§14, §38)**:
   - Complete administrative control over user accounts: status updates (`ACTIVE`, `LOCKED`, `SUSPENDED`, `INACTIVE`), security role modifications, and administrative password resets.
   - **Anti-Lockout Protection**: System administrators are strictly prohibited from deactivating, suspending, or revoking administrative credentials from their own accounts, preventing operational lockouts.
4. **Emergency Maintenance Controls**:
   - Hospital administrators must have the ability to toggle global maintenance mode with an administrative reason and timestamp to coordinate scheduled upgrades or emergency downtime.
5. **Auditing & Compliance Tracking (§36, §74, §105)**:
   - Administrative actions have severe institutional security and compliance consequences. Every configuration change, status update, role assignment, and password reset must be recorded in the append-only immutable audit trail (`AuditService.recordSensitiveAccess`).
6. **Strict Role-Based Access Control (RBAC, §91)**:
   - All administrative endpoints (`/api/v1/admin/**`) are restricted exclusively to authenticated users with `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`).

## Decision
1. **Package by Feature**:
   All administrative domain models, repositories, services, DTOs, controllers, and mappers reside under `com.careflow.administration`.
2. **Domain Architecture**:
   - `SystemSetting`: Entity representing dynamic system configuration with category (`SettingCategory`), data type (`SettingDataType`), description, encryption flag, and editability safeguard.
   - Enums: `SettingCategory` (`GENERAL`, `FACILITY`, `CLINICAL`, `SCHEDULING`, `BILLING`, `SECURITY`, `MAINTENANCE`) and `SettingDataType` (`STRING`, `NUMBER`, `BOOLEAN`, `JSON`).
3. **Database Schema & Indexing (Flyway V21)**:
   - Created table `system_settings` with primary key `id` and unique constraint on `setting_key`.
   - Indexed columns: `idx_sys_setting_key` and `idx_sys_setting_category`.
   - Seeded standard default hospital operational settings in Flyway `V21__create_administration_tables.sql`.
4. **Service Layer Governance (`AdministrationServiceImpl`)**:
   - Strict validation: Type-checks incoming setting values against their metadata (`BOOLEAN`, `NUMBER`, etc.).
   - Anti-lockout validation: Prevents administrators from modifying their own status or demoting their own role.
   - Password resets: Encodes new credentials using BCrypt (`PasswordEncoder`) and resets failed login counters.
   - Audit integration: Emits `CONFIGURATION_CHANGED`, `USER_STATUS_CHANGED`, `USER_ROLE_CHANGED`, and `USER_PASSWORD_RESET` audit records on every mutation.
5. **REST API Contract (`AdministrationController`)**:
   - `GET /api/v1/admin/overview`: Administrative operational overview (user counts by status/role, staff counts, department counts, setting count, maintenance status).
   - `GET /api/v1/admin/settings`: List settings with optional category filter.
   - `GET /api/v1/admin/settings/{key}`: Retrieve single setting.
   - `PUT /api/v1/admin/settings/{key}`: Update setting value with validation.
   - `POST /api/v1/admin/settings/batch`: Atomically update multiple settings.
   - `GET /api/v1/admin/users`: Paginated user list with role, status, and search filters.
   - `GET /api/v1/admin/users/{userId}`: Detailed user view with permissions.
   - `PUT /api/v1/admin/users/{userId}/status`: Update user account status.
   - `PUT /api/v1/admin/users/{userId}/roles`: Update user security roles.
   - `POST /api/v1/admin/users/{userId}/reset-password`: Administrator-initiated password reset.
   - `GET /api/v1/admin/maintenance`: Get current maintenance mode status.
   - `POST /api/v1/admin/maintenance`: Set maintenance mode with reason.

## Consequences
- **Positive**:
  - Full institutional governance: Hospital configurations can be adjusted dynamically without code redeployment.
  - Zero risk of administrative self-lockout.
  - Complete compliance: 100% of administrative mutations are audited with actor and previous/new values.
  - All 20 CareFlow core monolithic modules are fully realized, secured, and interconnected.
- **Negative / Trade-offs**:
  - Setting updates require careful data type validation to avoid runtime parsing issues in dependent services.
