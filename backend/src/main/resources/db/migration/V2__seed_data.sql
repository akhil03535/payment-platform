-- =====================================================
-- Seed Data for Payment Platform
-- Version: V2
-- =====================================================

-- Insert admin user (password: Admin@123456)
INSERT INTO users (id, username, email, password_hash, first_name, last_name, role, enabled)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@paymentplatform.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCgfl0jM.6.vC7Y/GsHWMBu',
    'Admin',
    'User',
    'ADMIN',
    true
) ON CONFLICT DO NOTHING;

-- Insert demo user (password: Demo@123456)
INSERT INTO users (id, username, email, password_hash, first_name, last_name, role, enabled)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'demo',
    'demo@paymentplatform.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCgfl0jM.6.vC7Y/GsHWMBu',
    'Demo',
    'User',
    'USER',
    true
) ON CONFLICT DO NOTHING;
