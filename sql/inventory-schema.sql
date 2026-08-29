-- ComSpare — Inventory Management schema
-- Run this in SQL Server Management Studio against your ComSpareDB database.
-- Order matters: parts must exist before the two tables that reference it.

CREATE TABLE parts (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_code    VARCHAR(30)     NOT NULL UNIQUE,
    name            VARCHAR(150)    NOT NULL,
    category        VARCHAR(100)    NOT NULL,
    brand           VARCHAR(100),
    model           VARCHAR(100),
    price           DECIMAL(10,2)   NOT NULL,
    stock_quantity  INT             NOT NULL DEFAULT 0,
    reorder_level   INT             NOT NULL DEFAULT 0,
    location        VARCHAR(100),
    specifications  VARCHAR(1000),
    compatible_with VARCHAR(500)
);

CREATE TABLE stock_adjustments (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    part_id         BIGINT          NOT NULL,
    change_amount   INT             NOT NULL,
    reason          VARCHAR(50)     NOT NULL,
    adjusted_by     VARCHAR(100),
    adjustment_date DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_stockadj_part FOREIGN KEY (part_id) REFERENCES parts(id)
);

CREATE TABLE part_history (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    part_id         BIGINT          NOT NULL,
    event_type      VARCHAR(20)     NOT NULL,  -- RECEIVED, SOLD, RETURNED, ADJUSTED, DAMAGED
    event_date      DATETIME2       NOT NULL DEFAULT GETDATE(),
    quantity        INT,
    notes           VARCHAR(500),
    CONSTRAINT FK_parthist_part FOREIGN KEY (part_id) REFERENCES parts(id)
);
