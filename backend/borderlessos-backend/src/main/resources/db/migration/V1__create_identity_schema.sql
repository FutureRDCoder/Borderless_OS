
-- ==========================================================
-- BorderlessOS
-- V1: Create Identity Schema
-- Purpose: Establish users, roles and user-role associations
-- ==========================================================

-- 1. USERS TABLE
CREATE TABLE users (
                       id UUID PRIMARY KEY,

                       email VARCHAR(255) NOT NULL UNIQUE,

                       password_hash VARCHAR(255) NOT NULL,

                       display_name VARCHAR(120) NOT NULL,

                       enabled BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT chk_users_email_not_blank
                           CHECK (LENGTH(TRIM(email)) > 0),

                       CONSTRAINT chk_users_display_name_not_blank
                           CHECK (LENGTH(TRIM(display_name)) > 0)
);


-- 2. ROLES TABLE
CREATE TABLE roles (
                       id UUID PRIMARY KEY,

                       name VARCHAR(50) NOT NULL UNIQUE,

                       CONSTRAINT chk_roles_name_not_blank
                           CHECK (LENGTH(TRIM(name)) > 0)
);


-- 3. USER_ROLES TABLE
CREATE TABLE user_roles (
                            user_id UUID NOT NULL,

                            role_id UUID NOT NULL,

                            assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT pk_user_roles
                                PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles(id)
                                    ON DELETE RESTRICT
);


-- 4. INDEXES
CREATE INDEX idx_users_email
    ON users(email);

CREATE INDEX idx_user_roles_role_id
    ON user_roles(role_id);


-- 5. INITIAL ROLE DEFINITIONS
INSERT INTO roles (id, name) VALUES
                                 ('10000000-0000-0000-0000-000000000001', 'CUSTOMER'),
                                 ('10000000-0000-0000-0000-000000000002', 'OPERATIONS'),
                                 ('10000000-0000-0000-0000-000000000003', 'COMPLIANCE'),
                                 ('10000000-0000-0000-0000-000000000004', 'ADMIN');
