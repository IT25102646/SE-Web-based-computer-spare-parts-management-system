-- =========================================
-- USER & ROLE MANAGEMENT  (SQL Server)
-- Function 4
-- =========================================

-- 1. Roles Table
IF OBJECT_ID('roles', 'U') IS NULL
CREATE TABLE roles (
                       id        BIGINT IDENTITY(1,1) PRIMARY KEY,
                       role_name VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Users Table
IF OBJECT_ID('users', 'U') IS NULL
CREATE TABLE users (
                       id            BIGINT IDENTITY(1,1) PRIMARY KEY,
                       name          VARCHAR(150) NOT NULL,
                       email         VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role_id       BIGINT NOT NULL,
                       active        BIT NOT NULL DEFAULT 1,      -- soft delete flag
                       CONSTRAINT FK_users_roles FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- If your users table already existed WITHOUT the active column:
IF COL_LENGTH('users', 'active') IS NULL
ALTER TABLE users ADD active BIT NOT NULL DEFAULT 1;

-- 3. Audit Log Table (append-only)
IF OBJECT_ID('audit_log', 'U') IS NULL
CREATE TABLE audit_log (
                           id            BIGINT IDENTITY(1,1) PRIMARY KEY,
                           performed_by  VARCHAR(150) NOT NULL,       -- who
                           action        VARCHAR(60)  NOT NULL,       -- what
                           target_record VARCHAR(255) NULL,           -- on which record
                           old_value     VARCHAR(500) NULL,           -- before
                           new_value     VARCHAR(500) NULL,           -- after
                           logged_at     DATETIME2    NOT NULL DEFAULT SYSDATETIME()   -- when
);
GO

-- 4. Make the audit log tamper-proof at DATABASE level
IF OBJECT_ID('trg_audit_log_append_only', 'TR') IS NOT NULL
DROP TRIGGER trg_audit_log_append_only;
GO
CREATE TRIGGER trg_audit_log_append_only
    ON audit_log
    INSTEAD OF UPDATE, DELETE
    AS
BEGIN
    RAISERROR('audit_log is append-only: UPDATE and DELETE are not allowed.', 16, 1);
ROLLBACK TRANSACTION;
END;
GO

-- 5. Seed the six roles (the app also does this automatically on startup)
INSERT INTO roles (role_name)
SELECT v.n FROM (VALUES
                     ('Admin'),('Operations Manager'),('Inventory Supervisor'),
                     ('Store Keeper'),('Customer Service Executive'),('Finance Officer')) AS v(n)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_name = v.n);
GO