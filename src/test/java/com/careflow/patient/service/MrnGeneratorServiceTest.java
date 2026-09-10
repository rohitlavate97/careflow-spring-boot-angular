package com.careflow.patient.service;

import com.careflow.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MrnGeneratorServiceTest {

    @Mock
    private PatientRepository patientRepository;

    private MrnGeneratorService mrnGeneratorService;

    @BeforeEach
    void setUp() {
        mrnGeneratorService = new MrnGeneratorService(patientRepository);
    }

    @Test
    @DisplayName("generateUniqueMrn should generate formatted MRN matching PAT-YYYYMM-XXXXX pattern (§16)")
    void generateUniqueMrn_shouldReturnFormattedMrn() {
        when(patientRepository.existsByMrn(anyString())).thenReturn(false);

        String mrn = mrnGeneratorService.generateUniqueMrn();

        String expectedPrefix = "PAT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        assertThat(mrn).isNotNull();
        assertThat(mrn).startsWith(expectedPrefix);
        assertThat(mrn).matches("^PAT-\\d{6}-\\d{5}$");
        verify(patientRepository, times(1)).existsByMrn(mrn);
    }

    @Test
    @DisplayName("generateUniqueMrn should retry upon collision until unique MRN is found (§16)")
    void generateUniqueMrn_shouldRetryOnCollision() {
        when(patientRepository.existsByMrn(anyString()))
                .thenReturn(true)  // First attempt collides
                .thenReturn(false); // Second attempt succeeds

        String mrn = mrnGeneratorService.generateUniqueMrn();

        assertThat(mrn).isNotNull();
        verify(patientRepository, times(2)).existsByMrn(anyString());
    }
}
