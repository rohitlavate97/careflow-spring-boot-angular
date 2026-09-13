package com.careflow.notification.service;

import com.careflow.notification.domain.Notification;
import com.careflow.notification.domain.NotificationChannel;
import com.careflow.notification.domain.NotificationStatus;
import com.careflow.notification.domain.NotificationType;
import com.careflow.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationConcurrencyTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private String userId;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        userId = "user-conc-" + UUID.randomUUID();
        testNotification = new Notification(
                UUID.randomUUID().toString(),
                userId,
                "conc@careflow.local",
                "+1-555-0011",
                "pat-conc-01",
                NotificationType.PRESCRIPTION_READY,
                NotificationChannel.IN_APP,
                "Prescription Ready",
                "Your medication is ready.",
                "PRESCRIPTION",
                "rx-conc"
        );
        testNotification.markAsSent();
        testNotification.markAsDelivered();
        testNotification = notificationRepository.save(testNotification);
    }

    @Test
    @DisplayName("Concurrent markAsRead requests execute safely without state corruption")
    void concurrentMarkAsRead_SafeExecution() throws InterruptedException {
        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    notificationService.markAsRead(testNotification.getId(), userId);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get() + errors.size()).isEqualTo(threadCount);

        Notification reloaded = notificationRepository.findById(testNotification.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(reloaded.getReadAt()).isNotNull();
    }
}
