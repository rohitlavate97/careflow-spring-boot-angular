# ADR-0014: Multi-Channel Notification Center and Event Abstraction Architecture

## Status
Accepted

## Context
CareFlow requires a reliable, multi-channel notification subsystem to communicate time-sensitive clinical, administrative, and financial updates to patients, healthcare providers, and administrative staff (§35, §103 Phase 14).

Key architectural drivers:
1. **Diverse Healthcare Notification Types (§35)**:
   - Clinical and operational lifecycles trigger automated communications:
     - `APPOINTMENT_CONFIRMATION`: Sent immediately upon appointment booking.
     - `APPOINTMENT_CANCELLATION`: Emitted when an appointment is cancelled by a patient, doctor, or receptionist.
     - `APPOINTMENT_REMINDER`: Dispatched in advance of scheduled clinical consultations to reduce patient no-show rates.
     - `LAB_RESULT_AVAILABLE`: Notifies patients and physicians when diagnostic test orders transition to `REPORTED`.
     - `PRESCRIPTION_READY`: Alerts patients when inpatient/outpatient medications have been dispensed by the hospital pharmacy.
     - `INVOICE_GENERATED`: Informs patients when outpatient or inpatient billing statements are issued.
     - `INSURANCE_CLAIM_UPDATE`: Notifies patients when payer claim adjudication decisions (`APPROVED`, `PARTIALLY_APPROVED`, `REJECTED`) are recorded.
2. **Channel Abstraction (In-App, Email, SMS) (§35)**:
   - Communication channels differ in delivery mechanics, costs, and recipient availability.
   - The platform requires a unified notification abstraction (`NotificationChannelSender`) supporting `IN_APP` (persisted notification feed with read/unread tracking), `EMAIL` (electronic mail delivery abstraction), and `SMS` (short messaging service abstraction).
   - The abstraction must gracefully handle delivery failures without breaking the originating clinical transaction.
3. **In-App Notification Center & State Transitions**:
   - In-app alerts require full lifecycle tracking: `PENDING` $\rightarrow$ `SENT` $\rightarrow$ `READ` / `ARCHIVED`.
   - Users need rapid unread counters, bulk read acknowledgments, and paginated inbox views.
4. **User Channel Preferences**:
   - Patients and staff must be able to configure channel opt-ins (`emailEnabled`, `smsEnabled`, `inAppEnabled`).
5. **Security & Privacy (§91)**:
   - Notifications contain Protected Health Information (PHI) and clinical context.
   - In-app notification feeds must strictly isolate recipient records (users can only access their own notifications).
   - Outbound dispatch triggers must be protected by RBAC (`ADMIN`, `RECEPTIONIST`, `DOCTOR`).

## Decision
1. **Package by Feature**:
   All notification models, channels, repositories, services, DTOs, controllers, and mappers reside under `com.careflow.notification`.
2. **Domain Architecture**:
   - `Notification`: Aggregate root managing recipient identifiers (`recipientUserId`, `recipientEmail`, `recipientPhone`, `patientId`), `notificationType`, `channel`, `title`, `message`, `referenceType`, `referenceId`, lifecycle `status` (`PENDING`, `SENT`, `DELIVERED`, `READ`, `FAILED`), failure diagnostics, and audit timestamps.
   - `NotificationPreference`: Manages recipient opt-in flags for `EMAIL`, `SMS`, and `IN_APP`.
3. **Channel Provider Abstraction**:
   - `NotificationChannelSender` interface with methods `supports(channel)` and `send(notification)`.
   - Implementations:
     - `InAppNotificationSender`: Manages persistent in-app delivery and unread status.
     - `EmailNotificationSender`: Pluggable electronic mail sender abstraction.
     - `SmsNotificationSender`: Pluggable SMS gateway sender abstraction.
   - `NotificationDispatcherService`: Coordinates preference validation, recipient contact resolution, and multi-channel fanout.
4. **Appointment Reminder Engine**:
   - Dedicated service method `sendAppointmentReminder(appointmentId)` resolving patient demographics and dispatching reminders.
5. **Access Control & RBAC**:
   - Authenticated users access their own in-app inbox and mark messages as read.
   - Administrative and clinical staff have authorization to manually dispatch operational notices or trigger appointment reminders.

## Consequences
- Clean separation between core domain workflows and external communication mechanisms.
- Reliable in-app notification center accessible to both hospital staff and patient portal users.
- Ready for future distributed event stream (Apache Kafka) without altering notification API contracts.
