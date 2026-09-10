-- CareFlow Enterprise Hospital Operations Platform
-- Flyway Migration V3: Seed Demo User Accounts (§101)
-- Populates synthetic demo accounts for all hospital operational roles

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, status, failed_login_attempts, locked_until, version)
VALUES
('user-admin-001', 'admin@careflow.local', 'admin@careflow.local', '$2a$12$KD7dYofBXspncDV2NSi2Y.oGsLGZY754mpoMhsZLuqLflZgeXkByG', 'Admin', 'System', '+1-555-0101', 'ACTIVE', 0, NULL, 0),
('user-doctor-001', 'doctor@careflow.local', 'doctor@careflow.local', '$2a$12$HwiBVy.vQhsZtBT2VAh6Eeeq2uL8qi.5balrr1pkgL5nrFNE4YJ/m', 'Alexander', 'Fleming', '+1-555-0102', 'ACTIVE', 0, NULL, 0),
('user-nurse-001', 'nurse@careflow.local', 'nurse@careflow.local', '$2a$12$diHRsW8lkvl8EUWDPS4JBO6PESWAilewFPhGOFDULNGFDYXZNssci', 'Florence', 'Nightingale', '+1-555-0103', 'ACTIVE', 0, NULL, 0),
('user-receptionist-001', 'receptionist@careflow.local', 'receptionist@careflow.local', '$2a$12$daopFPgx4G/NSXocOr.cFOSj9Tf/rlVS36ijr1W1FrXHlOZEPpxx.', 'Clara', 'Barton', '+1-555-0104', 'ACTIVE', 0, NULL, 0),
('user-pharmacist-001', 'pharmacist@careflow.local', 'pharmacist@careflow.local', '$2a$12$SZHpn9mzPnERhaKIFaDZmuj5/A2mwqAEZppqkPll.HBW8mMvy4DAm', 'John', 'Pemberton', '+1-555-0105', 'ACTIVE', 0, NULL, 0),
('user-lab-001', 'lab@careflow.local', 'lab@careflow.local', '$2a$12$dCHSIKoSjWc6qgsZtlrkhe5eTDUQyv3Ly4wX4pIYvcE0Q1/lM61uK', 'Marie', 'Curie', '+1-555-0106', 'ACTIVE', 0, NULL, 0),
('user-billing-001', 'billing@careflow.local', 'billing@careflow.local', '$2a$12$1m1M0pqM7oTnbhCUmxF62u0hdACLrUlTeOAeFjLLqoh0/kIGb072m', 'Ada', 'Lovelace', '+1-555-0107', 'ACTIVE', 0, NULL, 0)
ON DUPLICATE KEY UPDATE first_name = VALUES(first_name);

INSERT INTO users_roles (user_id, role_id)
VALUES
('user-admin-001', 'role-admin'),
('user-doctor-001', 'role-doctor'),
('user-nurse-001', 'role-nurse'),
('user-receptionist-001', 'role-receptionist'),
('user-pharmacist-001', 'role-pharmacist'),
('user-lab-001', 'role-lab-technician'),
('user-billing-001', 'role-billing-officer')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
