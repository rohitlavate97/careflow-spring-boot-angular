package com.careflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class CareFlowApplicationTests {

    @Test
    @DisplayName("Application context loads successfully with modular monolith configurations")
    void contextLoads() {
        assertTrue(true, "Application context bootstrapped cleanly without bean conflicts");
    }
}
