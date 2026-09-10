package com.careflow.identity.repository;

import com.careflow.common.config.JpaConfig;
import com.careflow.identity.domain.Permission;
import com.careflow.identity.domain.Role;
import com.careflow.identity.domain.RoleType;
import com.careflow.identity.domain.User;
import com.careflow.identity.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Role doctorRole;
    private Permission readPatientPermission;

    @BeforeEach
    void setUp() {
        readPatientPermission = new Permission(UUID.randomUUID().toString(), "patients:read", "Permission to view patient records");
        entityManager.persist(readPatientPermission);

        doctorRole = new Role(UUID.randomUUID().toString(), RoleType.ROLE_DOCTOR, "Medical Doctor Role");
        doctorRole.addPermission(readPatientPermission);
        entityManager.persist(doctorRole);

        entityManager.flush();
    }

    @Test
    @DisplayName("save should persist user and retrieve by username")
    void save_shouldPersistUser_andRetrieveByUsername() {
        User user = new User(
                UUID.randomUUID().toString(),
                "dr.smith",
                "smith@hospital.org",
                "$2a$12$e8Z9K4W7T...dummyhash",
                "John",
                "Smith",
                "+1-555-0199"
        );
        user.addRole(doctorRole);

        User saved = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findByUsername("dr.smith");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getEmail()).isEqualTo("smith@hospital.org");
        assertThat(found.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(found.get().getVersion()).isEqualTo(0L);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmailWithRolesAndPermissions should fetch user and roles eagerly via fetch join (§12)")
    void findByEmailWithRolesAndPermissions_shouldFetchUserAndRolesWithoutNPlusOne() {
        User user = new User(
                UUID.randomUUID().toString(),
                "dr.watson",
                "watson@hospital.org",
                "$2a$12$e8Z9K4W7T...dummyhash",
                "John",
                "Watson",
                "+1-555-0198"
        );
        user.addRole(doctorRole);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> loaded = userRepository.findByEmailWithRolesAndPermissions("watson@hospital.org");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getRoles()).hasSize(1);
        Role loadedRole = loaded.get().getRoles().iterator().next();
        assertThat(loadedRole.getName()).isEqualTo(RoleType.ROLE_DOCTOR);
        assertThat(loadedRole.getPermissions()).hasSize(1);
    }

    @Test
    @DisplayName("save should fail with DataIntegrityViolationException when email is duplicated (§10, §92)")
    void save_shouldFail_whenDuplicateEmailPersisted() {
        User user1 = new User(
                UUID.randomUUID().toString(),
                "user.one",
                "duplicate@hospital.org",
                "hash1",
                "First",
                "User",
                null
        );
        userRepository.save(user1);
        entityManager.flush();

        User user2 = new User(
                UUID.randomUUID().toString(),
                "user.two",
                "duplicate@hospital.org",
                "hash2",
                "Second",
                "User",
                null
        );

        assertThatThrownBy(() -> {
            userRepository.save(user2);
            entityManager.flush();
        }).satisfies(throwable -> {
            assertThat(throwable).isInstanceOfAny(
                    DataIntegrityViolationException.class,
                    org.hibernate.exception.ConstraintViolationException.class,
                    jakarta.persistence.PersistenceException.class
            );
        });
    }

    @Test
    @DisplayName("optimisticLocking should increment version on update (§11)")
    void optimisticLocking_shouldIncrementVersionOnUpdate() {
        User user = new User(
                UUID.randomUUID().toString(),
                "dr.house",
                "house@hospital.org",
                "hash",
                "Gregory",
                "House",
                null
        );
        User saved = userRepository.save(user);
        entityManager.flush();
        assertThat(saved.getVersion()).isEqualTo(0L);

        saved.setPhone("+1-555-9999");
        User updated = userRepository.save(saved);
        entityManager.flush();

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("accountLocking should record failed attempts and lock account upon threshold")
    void accountLocking_shouldRecordFailedAttemptsAndLockAccount() {
        User user = new User(
                UUID.randomUUID().toString(),
                "nurse.joy",
                "joy@hospital.org",
                "hash",
                "Nurse",
                "Joy",
                null
        );
        userRepository.save(user);

        user.incrementFailedLoginAttempts();
        user.incrementFailedLoginAttempts();
        user.incrementFailedLoginAttempts();
        user.incrementFailedLoginAttempts();
        user.incrementFailedLoginAttempts();

        user.lockAccount(Duration.ofMinutes(15));
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findByUsername("nurse.joy").orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(reloaded.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(reloaded.isAccountNonLocked()).isFalse();
    }
}
