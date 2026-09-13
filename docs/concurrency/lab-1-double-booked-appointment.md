# Concurrency Lab 1: Double-Booked Appointments

## Objective
Demonstrate how CareFlow prevents the "double-booking" race condition where two concurrent users attempt to book the exact same doctor time slot.

## The Scenario
Two patients, Alice and Bob, simultaneously see that Dr. Fleming has an open 10:00 AM slot. They both click "Book" at the exact same millisecond. 

## The Naive Approach (And why it fails)
1. **Thread 1 (Alice)** checks if the slot is free: `SELECT count(*) FROM appointments WHERE doctor_id = 'D1' AND time = '10:00' AND status NOT IN ('CANCELLED')`. Result: 0 (Free).
2. **Thread 2 (Bob)** checks if the slot is free: Result: 0 (Free).
3. **Thread 1 (Alice)** inserts appointment for 10:00 AM.
4. **Thread 2 (Bob)** inserts appointment for 10:00 AM.
*Result*: Both Alice and Bob are booked for 10:00 AM.

## The CareFlow Solution
We enforce a hard constraint at the database layer.

1. **Schema Design**:
   We added a column `active_slot_flag` to the `appointments` table.
   - For active appointments (`REQUESTED`, `CONFIRMED`, `CHECKED_IN`, `IN_PROGRESS`, `COMPLETED`), `active_slot_flag = 1`.
   - For inactive appointments (`CANCELLED`, `NO_SHOW`), `active_slot_flag = NULL`.

2. **Database Constraint**:
   `UNIQUE KEY uk_active_appointment_slot (doctor_id, appointment_date_time, active_slot_flag)`
   - MySQL ignores `NULL` values in unique indices. This allows multiple `CANCELLED` appointments for the same slot.
   - However, MySQL enforces the unique constraint for `1`. If two threads try to insert `1` for the same doctor and time, one will fail with a `DataIntegrityViolationException`.

3. **Service Layer**:
   The `AppointmentService` catches `DataIntegrityViolationException` and translates it to a business-friendly `DoubleBookingException`.

## How to run the Lab
Execute the `AppointmentConcurrencyIT` integration test.
This test uses:
- `ExecutorService` with 10 threads to simulate 10 simultaneous bookings for the same slot.
- `CountDownLatch(1)` as a starting gate to ensure true concurrency.
- It asserts that exactly 1 booking succeeds and 9 throw `DoubleBookingException`.
