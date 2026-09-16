-- ==========================================
-- USER & ROLE MANAGEMENT
-- Function 4
-- ==========================================

-- 1. Roles Table
CREATE TABLE roles (
                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                       role_name VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Users Table
CREATE TABLE users (
                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                       name VARCHAR(150) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role_id BIGINT NOT NULL,

                       CONSTRAINT FK_users_roles
                           FOREIGN KEY (role_id)
                               REFERENCES roles(id)
);

-- 3. Audit Log Table
CREATE TABLE audit_logs (
                            id BIGINT IDENTITY(1,1) PRIMARY KEY,

                            user_id BIGINT NULL,

                            action VARCHAR(50) NOT NULL,

                            table_name VARCHAR(100),

                            record_id BIGINT,

                            old_value VARCHAR(MAX),

    new_value VARCHAR(MAX),

    timestamp DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

-- ==========================================
-- DEFAULT SYSTEM ROLES
-- ==========================================

INSERT INTO roles (role_name)
VALUES
    ('ADMIN'),
    ('INVENTORY_SUPERVISOR'),
    ('STORE_KEEPER'),
    ('CUSTOMER_SERVICE_EXECUTIVE'),
    ('FINANCE_OFFICER'),
    ('OPERATIONS_MANAGER');

-- ==========================================
-- SAMPLE ADMIN USER
-- Password should be replaced with BCrypt hash
-- ==========================================

INSERT INTO users (name, email, password_hash, role_id)
VALUES
    (
        'System Admin',
        'admin@comspare.com',
        '$2a$10$exampleBCryptHashReplaceThis',
        1
    );