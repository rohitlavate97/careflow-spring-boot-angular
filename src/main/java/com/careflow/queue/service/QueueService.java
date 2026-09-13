package com.careflow.queue.service;

import com.careflow.queue.dto.DepartmentQueueLiveStatusResponse;
import com.careflow.queue.dto.EnqueuePatientRequest;
import com.careflow.queue.dto.QueueEntryResponse;

/**
 * Service contract for Patient Queue operations (§21).
 */
public interface QueueService {

    /**
     * Enqueue a patient for outpatient consultation (walk-in or pre-booked appointment) (§21).
     */
    QueueEntryResponse enqueuePatient(EnqueuePatientRequest request);

    /**
     * Concurrency-safe selection and calling of the highest-priority next patient in the queue (§21, §92).
     */
    QueueEntryResponse callNextPatient(String departmentId, String doctorId);

    /**
     * Advance patient from CALLED to IN_CONSULTATION when entering the consultation room (§21).
     */
    QueueEntryResponse startConsultation(String queueEntryId);

    /**
     * Advance patient from IN_CONSULTATION to COMPLETED when consultation ends (§21).
     */
    QueueEntryResponse completeConsultation(String queueEntryId);

    /**
     * Mark a called patient as SKIPPED when they do not respond (§21).
     */
    QueueEntryResponse skipPatient(String queueEntryId);

    /**
     * Return a skipped patient back into the active WAITING pool (§21).
     */
    QueueEntryResponse requeuePatient(String queueEntryId);

    /**
     * Cancel a patient queue entry (§21).
     */
    QueueEntryResponse cancelQueueEntry(String queueEntryId, String reason);

    /**
     * Retrieve queue entry details including dynamic count of patients ahead (§21).
     */
    QueueEntryResponse getQueueEntry(String id);

    /**
     * Get live queue dashboard metrics and active display lists for a department (§21).
     */
    DepartmentQueueLiveStatusResponse getDepartmentLiveStatus(String departmentId);
}
