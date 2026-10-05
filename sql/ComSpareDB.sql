CREATE DATABASE ComSpareDB;
USE ComSpareDB

/* ============================================================
   ============================================================
   COMSPARE DATABASE
   COMPLETE JOINT DATABASE + SEED DATA
   Microsoft SQL Server

   TABLE CREATION ORDER:

   1.  roles
   2.  users
   3.  audit_logs
   4.  parts
   5.  stock_adjustments
   6.  part_history
   7.  suppliers
   8.  purchase_orders
   9.  purchase_order_items
   10. return_requests
   11. orders
   12. order_items
   13. invoices
   14. payments

   SEED DATA:
   - Roles
   - Admin user
   - Parts
   - Suppliers
   - Orders
   - Order items

   ============================================================
*/


/* ============================================================
   1. ROLES
   ============================================================ */

CREATE TABLE roles
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    role_name VARCHAR(100) NOT NULL,

    CONSTRAINT PK_roles
        PRIMARY KEY (id),

    CONSTRAINT UQ_roles_role_name
        UNIQUE (role_name)
);
GO


/* ============================================================
   SEED ROLES
   ============================================================ */

INSERT INTO roles (role_name)
VALUES
    ('ADMIN'),
    ('INVENTORY_SUPERVISOR'),
    ('STORE_KEEPER'),
    ('CUSTOMER_SERVICE_EXECUTIVE'),
    ('FINANCE_OFFICER'),
    ('OPERATIONS_MANAGER');
GO


/* ============================================================
   2. USERS
   ============================================================ */

CREATE TABLE users
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    name VARCHAR(150) NOT NULL,

    email VARCHAR(150) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    role_id BIGINT NOT NULL,

    CONSTRAINT PK_users
        PRIMARY KEY (id),

    CONSTRAINT UQ_users_email
        UNIQUE (email),

    CONSTRAINT FK_users_roles
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
);
GO


/* ============================================================
   SEED ADMIN USER
   ============================================================ */

INSERT INTO users
(
    name,
    email,
    password_hash,
    role_id
)
VALUES
(
    'System Admin',
    'admin@comspare.com',
    '$2a$10$exampleBCryptHashReplaceThis',
    1
);
GO


/* ============================================================
   3. AUDIT LOGS
   ============================================================ */

CREATE TABLE audit_logs
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    user_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    table_name VARCHAR(100),
    record_id BIGINT,

    old_value VARCHAR(MAX),

    new_value VARCHAR(MAX),

    timestamp DATETIME2 NOT NULL
        CONSTRAINT DF_audit_logs_timestamp
        DEFAULT GETDATE(),

    CONSTRAINT PK_audit_logs
        PRIMARY KEY (id),

    CONSTRAINT FK_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);
GO


/* ============================================================
   4. PARTS
   ============================================================ */

CREATE TABLE parts
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    product_code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    brand VARCHAR(100),
    model VARCHAR(100),
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL
        CONSTRAINT DF_parts_stock_quantity
        DEFAULT 0,
    reorder_level INT NOT NULL
        CONSTRAINT DF_parts_reorder_level
        DEFAULT 0,
    location VARCHAR(100),
    specifications VARCHAR(1000),
    compatible_with VARCHAR(500),
    CONSTRAINT PK_parts
        PRIMARY KEY (id),
    CONSTRAINT UQ_parts_product_code
        UNIQUE (product_code),
    CONSTRAINT CK_parts_price
        CHECK (price >= 0),
    CONSTRAINT CK_parts_stock_quantity
        CHECK (stock_quantity >= 0),
    CONSTRAINT CK_parts_reorder_level
        CHECK (reorder_level >= 0)
);
GO


/* ============================================================
   SEED PARTS
   These are required for order_items
   ============================================================ */

INSERT INTO parts
(
    product_code,
    name,
    category,
    brand,
    model,
    price,
    stock_quantity,
    reorder_level,
    location,
    specifications,
    compatible_with
)
VALUES
(
    'BRK-001',
    'Brake Pad Set',
    'Braking System',
    'Bosch',
    'BP-100',
    12500.00,
    25,
    5,
    'A-01',
    'Front brake pad set',
    'Toyota Corolla, Toyota Axio'
),
(
    'OIL-001',
    'Engine Oil Filter',
    'Filters',
    'Mann',
    'W-712',
    3500.00,
    40,
    10,
    'A-02',
    'High efficiency oil filter',
    'Toyota, Honda, Nissan'
),
(
    'BAT-001',
    'Car Battery',
    'Electrical',
    'Amaron',
    'NS40Z',
    28500.00,
    15,
    3,
    'B-01',
    '12V automotive battery',
    'Toyota, Suzuki, Honda'
),
(
    'SPK-001',
    'Spark Plug Set',
    'Engine',
    'NGK',
    'BKR6E',
    6500.00,
    30,
    8,
    'B-02',
    'Set of 4 spark plugs',
    'Toyota Corolla, Honda Civic'
),
(
    'AIR-001',
    'Air Filter',
    'Filters',
    'Denso',
    'AF-200',
    4500.00,
    35,
    10,
    'A-03',
    'Engine air filter',
    'Toyota, Nissan'
);
GO


/* ============================================================
   5. STOCK ADJUSTMENTS
   ============================================================ */

CREATE TABLE stock_adjustments
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    part_id BIGINT NOT NULL,

    change_amount INT NOT NULL,

    reason VARCHAR(50) NOT NULL,

    adjusted_by VARCHAR(100),

    adjustment_date DATETIME2 NOT NULL
        CONSTRAINT DF_stock_adjustments_date
        DEFAULT GETDATE(),

    CONSTRAINT PK_stock_adjustments
        PRIMARY KEY (id),

    CONSTRAINT FK_stockadj_part
        FOREIGN KEY (part_id)
        REFERENCES parts(id)
);
GO


/* ============================================================
   6. PART HISTORY
   ============================================================ */

CREATE TABLE part_history
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    part_id BIGINT NOT NULL,

    event_type VARCHAR(20) NOT NULL,

    event_date DATETIME2 NOT NULL
        CONSTRAINT DF_part_history_date
        DEFAULT GETDATE(),

    quantity INT,

    notes VARCHAR(500),

    CONSTRAINT PK_part_history
        PRIMARY KEY (id),

    CONSTRAINT FK_parthist_part
        FOREIGN KEY (part_id)
        REFERENCES parts(id),

    CONSTRAINT CK_parthist_event_type
        CHECK
        (
            event_type IN
            (
                'RECEIVED',
                'SOLD',
                'RETURNED',
                'ADJUSTED',
                'DAMAGED'
            )
        )
);
GO


/* ============================================================
   7. SUPPLIERS
   ============================================================ */

CREATE TABLE suppliers
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    supplier_name VARCHAR(255) NOT NULL,

    contact_person VARCHAR(255),

    phone VARCHAR(50),

    email VARCHAR(255),

    address VARCHAR(500),

    status VARCHAR(50),

    CONSTRAINT PK_suppliers
        PRIMARY KEY (id)
);
GO


/* ============================================================
   SEED SUPPLIERS
   ============================================================ */

INSERT INTO suppliers
(
    supplier_name,
    contact_person,
    phone,
    email,
    address,
    status
)
VALUES
(
    'AutoParts Lanka',
    'Kasun Perera',
    '0712345678',
    'sales@autopartslanka.com',
    'Colombo, Sri Lanka',
    'ACTIVE'
),
(
    'Lanka Motor Supplies',
    'Nimal Silva',
    '0771234567',
    'info@lankamotorsupplies.com',
    'Kandy, Sri Lanka',
    'ACTIVE'
),
(
    'Global Auto Components',
    'Tharindu Fernando',
    '0759876543',
    'sales@globalauto.com',
    'Gampaha, Sri Lanka',
    'ACTIVE'
);
GO


/* ============================================================
   8. PURCHASE ORDERS
   ============================================================ */

CREATE TABLE purchase_orders
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    order_number VARCHAR(255) NOT NULL,

    supplier_id BIGINT NOT NULL,

    order_date DATE,

    expected_delivery_date DATE,

    delivery_date DATE,

    status VARCHAR(50),

    notes VARCHAR(1000),

    CONSTRAINT PK_purchase_orders
        PRIMARY KEY (id),

    CONSTRAINT UQ_purchase_orders_order_number
        UNIQUE (order_number),

    CONSTRAINT FK_purchase_order_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id)
);
GO


/* ============================================================
   9. PURCHASE ORDER ITEMS
   ============================================================ */

CREATE TABLE purchase_order_items
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    purchase_order_id BIGINT NOT NULL,

    part_id BIGINT NOT NULL,

    ordered_quantity INT NOT NULL,

    received_quantity INT
        CONSTRAINT DF_purchase_order_items_received
        DEFAULT 0,

    unit_cost DECIMAL(10,2),

    CONSTRAINT PK_purchase_order_items
        PRIMARY KEY (id),

    CONSTRAINT FK_purchase_order_item_order
        FOREIGN KEY (purchase_order_id)
        REFERENCES purchase_orders(id),

    CONSTRAINT FK_purchase_order_item_part
        FOREIGN KEY (part_id)
        REFERENCES parts(id),

    CONSTRAINT CK_purchase_order_item_ordered_quantity
        CHECK (ordered_quantity > 0),

    CONSTRAINT CK_purchase_order_item_received_quantity
        CHECK (received_quantity >= 0),

    CONSTRAINT CK_purchase_order_item_unit_cost
        CHECK (unit_cost >= 0)
);
GO


/* ============================================================
   10. RETURN REQUESTS
   ============================================================ */

CREATE TABLE return_requests
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    customer_name VARCHAR(150) NOT NULL,

    customer_contact VARCHAR(100) NOT NULL,

    part_id BIGINT NOT NULL,

    quantity INT NOT NULL,

    return_reason VARCHAR(500) NOT NULL,

    claim_type VARCHAR(20) NOT NULL,

    status VARCHAR(20) NOT NULL
        CONSTRAINT DF_return_requests_status
        DEFAULT 'PENDING',

    resolution VARCHAR(20),

    decision_notes VARCHAR(500),

    inventory_processed BIT NOT NULL
        CONSTRAINT DF_return_requests_inventory_processed
        DEFAULT 0,

    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_return_requests_created_at
        DEFAULT GETDATE(),

    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_return_requests_updated_at
        DEFAULT GETDATE(),

    CONSTRAINT PK_return_requests
        PRIMARY KEY (id),

    CONSTRAINT FK_return_requests_part
        FOREIGN KEY (part_id)
        REFERENCES parts(id),

    CONSTRAINT CK_return_claim_type
        CHECK
        (
            claim_type IN
            (
                'RETURN',
                'WARRANTY'
            )
        ),

    CONSTRAINT CK_return_status
        CHECK
        (
            status IN
            (
                'PENDING',
                'APPROVED',
                'REJECTED',
                'PROCESSING',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    CONSTRAINT CK_return_quantity
        CHECK (quantity > 0)
);
GO


/* ============================================================
   11. ORDERS
   Order & Sales Management
   ============================================================ */

CREATE TABLE orders
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    order_number VARCHAR(30) NOT NULL,

    customer_name VARCHAR(100) NOT NULL,

    customer_email VARCHAR(150) NOT NULL,

    order_date DATETIME2 NOT NULL
        CONSTRAINT DF_orders_order_date
        DEFAULT GETDATE(),

    total_amount DECIMAL(10,2) NOT NULL
        CONSTRAINT DF_orders_total_amount
        DEFAULT 0,

    status VARCHAR(30) NOT NULL
        CONSTRAINT DF_orders_status
        DEFAULT 'PENDING',

    CONSTRAINT PK_orders
        PRIMARY KEY (id),

    CONSTRAINT UQ_orders_order_number
        UNIQUE (order_number),

    CONSTRAINT CK_orders_status
        CHECK
        (
            status IN
            (
                'PENDING',
                'CONFIRMED',
                'SHIPPED',
                'DELIVERED',
                'CANCELLED'
            )
        ),

    CONSTRAINT CK_orders_total_amount
        CHECK (total_amount >= 0)
);
GO


/* ============================================================
   SEED ORDERS
   These allow CRUD operations to be tested
   ============================================================ */

INSERT INTO orders
(
    order_number,
    customer_name,
    customer_email,
    order_date,
    total_amount,
    status
)
VALUES
(
    'ORD001',
    'Kasun Perera',
    'kasun@example.com',
    DATEADD(DAY, -5, GETDATE()),
    25000.00,
    'PENDING'
),
(
    'ORD002',
    'Nimal Silva',
    'nimal@example.com',
    DATEADD(DAY, -4, GETDATE()),
    35000.00,
    'CONFIRMED'
),
(
    'ORD003',
    'Tharindu Fernando',
    'tharindu@example.com',
    DATEADD(DAY, -3, GETDATE()),
    28500.00,
    'SHIPPED'
),
(
    'ORD004',
    'Amal Perera',
    'amal@example.com',
    DATEADD(DAY, -2, GETDATE()),
    11000.00,
    'DELIVERED'
),
(
    'ORD005',
    'Sahan Jayasuriya',
    'sahan@example.com',
    DATEADD(DAY, -1, GETDATE()),
    4500.00,
    'CANCELLED'
);
GO


/* ============================================================
   12. ORDER ITEMS
   ============================================================ */

CREATE TABLE order_items
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    order_id BIGINT NOT NULL,

    part_id BIGINT NOT NULL,

    quantity INT NOT NULL,

    unit_price DECIMAL(10,2) NOT NULL,

    subtotal DECIMAL(10,2) NOT NULL,

    CONSTRAINT PK_order_items
        PRIMARY KEY (id),

    CONSTRAINT FK_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT FK_order_items_part
        FOREIGN KEY (part_id)
        REFERENCES parts(id),

    CONSTRAINT CK_order_items_quantity
        CHECK (quantity >= 1),

    CONSTRAINT CK_order_items_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT CK_order_items_subtotal
        CHECK (subtotal >= 0)
);
GO


/* ============================================================
   SEED ORDER ITEMS
   ============================================================ */

INSERT INTO order_items
(
    order_id,
    part_id,
    quantity,
    unit_price,
    subtotal
)
VALUES
/* ORD001 - 2 Brake Pad Sets */
(
    1,
    1,
    2,
    12500.00,
    25000.00
),

/* ORD002 - Brake Pads + Oil Filters */
(
    2,
    1,
    2,
    12500.00,
    25000.00
),
(
    2,
    2,
    1,
    10000.00,
    10000.00
),

/* ORD003 - 1 Battery */
(
    3,
    3,
    1,
    28500.00,
    28500.00
),

/* ORD004 - 1 Spark Plug Set + 1 Air Filter */
(
    4,
    4,
    1,
    6500.00,
    6500.00
),
(
    4,
    5,
    1,
    4500.00,
    4500.00
),

/* ORD005 - 1 Air Filter */
(
    5,
    5,
    1,
    4500.00,
    4500.00
);
GO


/* ============================================================
   13. INVOICES
   ============================================================ */

CREATE TABLE invoices
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    invoice_number VARCHAR(30) NOT NULL,

    order_id BIGINT NOT NULL,

    customer_name VARCHAR(100),

    total_amount NUMERIC(12,2) NOT NULL
        CONSTRAINT DF_invoices_total_amount
        DEFAULT 0,

    amount_paid NUMERIC(12,2) NOT NULL
        CONSTRAINT DF_invoices_amount_paid
        DEFAULT 0,

    payment_status VARCHAR(20) NOT NULL
        CONSTRAINT DF_invoices_payment_status
        DEFAULT 'PENDING',

    invoice_date DATETIME2(6) NOT NULL
        CONSTRAINT DF_invoices_invoice_date
        DEFAULT SYSDATETIME(),

    CONSTRAINT PK_invoices
        PRIMARY KEY (id),

    CONSTRAINT UQ_invoices_invoice_number
        UNIQUE (invoice_number),

    CONSTRAINT FK_invoices_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT CK_invoices_payment_status
        CHECK
        (
            payment_status IN
            (
                'PENDING',
                'PARTIAL',
                'PAID'
            )
        ),

    CONSTRAINT CK_invoices_total_amount
        CHECK (total_amount >= 0),

    CONSTRAINT CK_invoices_amount_paid
        CHECK (amount_paid >= 0),

    CONSTRAINT CK_invoices_paid_not_greater
        CHECK (amount_paid <= total_amount)
);
GO


/* ============================================================
   14. PAYMENTS
   ============================================================ */

CREATE TABLE payments
(
    id BIGINT IDENTITY(1,1) NOT NULL,

    invoice_id BIGINT NOT NULL,

    amount NUMERIC(12,2) NOT NULL,

    payment_method VARCHAR(20) NOT NULL,

    payment_date DATETIME2(6) NOT NULL
        CONSTRAINT DF_payments_payment_date
        DEFAULT SYSDATETIME(),

    reference_no VARCHAR(50),

    CONSTRAINT PK_payments
        PRIMARY KEY (id),

    CONSTRAINT FK_payments_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id),

    CONSTRAINT CK_payments_payment_method
        CHECK
        (
            payment_method IN
            (
                'CASH',
                'CARD',
                'ONLINE'
            )
        ),

    CONSTRAINT CK_payments_amount
        CHECK (amount > 0)
);
GO


/* ============================================================
   INDEXES
   ============================================================ */

CREATE INDEX IX_audit_logs_user_id
    ON audit_logs(user_id);
GO

CREATE INDEX IX_stock_adjustments_part_id
    ON stock_adjustments(part_id);
GO

CREATE INDEX IX_part_history_part_id
    ON part_history(part_id);
GO

CREATE INDEX IX_purchase_orders_supplier_id
    ON purchase_orders(supplier_id);
GO

CREATE INDEX IX_purchase_order_items_order_id
    ON purchase_order_items(purchase_order_id);
GO

CREATE INDEX IX_purchase_order_items_part_id
    ON purchase_order_items(part_id);
GO

CREATE INDEX IX_return_requests_part_id
    ON return_requests(part_id);
GO

CREATE INDEX IX_return_requests_status
    ON return_requests(status);
GO

CREATE INDEX IX_orders_customer_email
    ON orders(customer_email);
GO

CREATE INDEX IX_orders_status
    ON orders(status);
GO

CREATE INDEX IX_order_items_order_id
    ON order_items(order_id);
GO

CREATE INDEX IX_order_items_part_id
    ON order_items(part_id);
GO

CREATE INDEX IX_invoices_order_id
    ON invoices(order_id);
GO

CREATE INDEX IX_invoices_payment_status
    ON invoices(payment_status);
GO

CREATE INDEX IX_payments_invoice_id
    ON payments(invoice_id);
GO


/* ============================================================
   VERIFY ALL TABLES
   ============================================================ */

SELECT
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
GO


/* ============================================================
   VERIFY FOREIGN KEYS
   ============================================================ */

SELECT
    fk.name AS ForeignKeyName,
    OBJECT_NAME(fk.parent_object_id) AS ChildTable,
    COL_NAME(
        fkc.parent_object_id,
        fkc.parent_column_id
    ) AS ChildColumn,
    OBJECT_NAME(fk.referenced_object_id) AS ParentTable,
    COL_NAME(
        fkc.referenced_object_id,
        fkc.referenced_column_id
    ) AS ParentColumn
FROM sys.foreign_keys fk
INNER JOIN sys.foreign_key_columns fkc
    ON fk.object_id = fkc.constraint_object_id
ORDER BY ChildTable, ForeignKeyName;
GO


/* ============================================================
   VERIFY SEED ORDERS
   ============================================================ */

SELECT
    o.id,
    o.order_number,
    o.customer_name,
    o.customer_email,
    o.order_date,
    o.total_amount,
    o.status
FROM orders o
ORDER BY o.id;
GO


/* ============================================================
   VERIFY ORDERS WITH ITEMS
   ============================================================ */

SELECT
    o.order_number,
    o.customer_name,
    p.product_code,
    p.name AS part_name,
    oi.quantity,
    oi.unit_price,
    oi.subtotal,
    o.total_amount,
    o.status
FROM orders o
INNER JOIN order_items oi
    ON o.id = oi.order_id
INNER JOIN parts p
    ON oi.part_id = p.id
ORDER BY o.id, oi.id;
GO

USE ComSpareDB;
GO

/* ================================================================
   PART C (COMPLETION) — SEED DATA FOR TABLES THAT HAD < 5 ROWS
   Run this AFTER your original script (roles/users/parts/suppliers
   /orders/order_items already seeded).
   ================================================================ */

/* ---- Top up USERS to 5 rows (only 1 existed) ---- */
INSERT INTO users (name, email, password_hash, role_id)
VALUES
('Nadeesha Perera',  'nadeesha@comspare.com', '$2a$10$exampleBCryptHash01', 2),
('Kasun Jayasinghe', 'kasun.j@comspare.com',  '$2a$10$exampleBCryptHash02', 3),
('Chamari Silva',    'chamari@comspare.com',  '$2a$10$exampleBCryptHash03', 4),
('Ruwan Fernando',   'ruwan@comspare.com',    '$2a$10$exampleBCryptHash04', 5),
('Dilani Wickrama',  'dilani@comspare.com',   '$2a$10$exampleBCryptHash05', 6);
GO

/* ---- Top up SUPPLIERS to 5 rows (only 3 existed) ---- */
INSERT INTO suppliers (supplier_name, contact_person, phone, email, address, status)
VALUES
('Precision Auto Imports', 'Sanduni Rathnayake', '0776543210', 'sales@precisionauto.lk', 'Negombo, Sri Lanka', 'ACTIVE'),
('Ceylon Auto Traders',    'Harsha Bandara',      '0701122334', 'info@ceylonauto.lk',     'Galle, Sri Lanka',   'ACTIVE');
GO

/* ---- AUDIT_LOGS (0 rows before) ---- */
INSERT INTO audit_logs (user_id, action, table_name, record_id, old_value, new_value)
VALUES
(1, 'UPDATE', 'parts',   1, 'stock_quantity=28', 'stock_quantity=25'),
(2, 'INSERT', 'orders',  5, NULL,                 'status=PENDING'),
(3, 'UPDATE', 'orders',  5, 'status=PENDING',      'status=CANCELLED'),
(1, 'UPDATE', 'parts',   3, 'stock_quantity=17',  'stock_quantity=15'),
(4, 'DELETE', 'return_requests', 2, 'status=PENDING', NULL);
GO

/* ---- STOCK_ADJUSTMENTS (0 rows before) ---- */
INSERT INTO stock_adjustments (part_id, change_amount, reason, adjusted_by)
VALUES
(1, -2, 'Damaged in warehouse',      'Zee'),
(2, 10, 'New delivery received',     'Zee'),
(3, -1, 'Sold via walk-in customer', 'Nadeesha Perera'),
(4,  5, 'Stock count correction',    'Zee'),
(5, -3, 'Return processed as damaged','Kasun Jayasinghe');
GO

/* ---- PART_HISTORY (0 rows before) ---- */
INSERT INTO part_history (part_id, event_type, quantity, notes)
VALUES
(1, 'DAMAGED',  2, 'Two brake pad sets damaged during storage'),
(2, 'RECEIVED', 10, 'Restocked from Lanka Motor Supplies'),
(3, 'SOLD',     1, 'Sold in ORD003'),
(4, 'ADJUSTED', 5, 'Manual correction after stock count'),
(5, 'RETURNED', 3, 'Returned by customer, found damaged');
GO

/* ---- PURCHASE_ORDERS (0 rows before) ----
   Note: supplier_id 1 used twice on purpose, for the HAVING query below */
INSERT INTO purchase_orders (order_number, supplier_id, order_date, expected_delivery_date, delivery_date, status, notes)
VALUES
('PO001', 1, '2026-08-01', '2026-08-10', '2026-08-09', 'RECEIVED', 'Routine brake pad restock'),
('PO002', 1, '2026-08-15', '2026-08-25', NULL,          'PENDING',  'Battery restock'),
('PO003', 2, '2026-08-05', '2026-08-15', '2026-08-14', 'RECEIVED', 'Filter restock'),
('PO004', 3, '2026-08-20', '2026-08-30', NULL,          'APPROVED', 'Spark plug restock'),
('PO005', 4, '2026-08-22', '2026-09-01', NULL,          'PENDING',  'Air filter restock');
GO

/* ---- PURCHASE_ORDER_ITEMS (0 rows before) ---- */
INSERT INTO purchase_order_items (purchase_order_id, part_id, ordered_quantity, received_quantity, unit_cost)
VALUES
(1, 1, 20, 20, 9500.00),
(2, 3, 10,  0, 21000.00),
(3, 2, 50, 50, 2600.00),
(4, 4, 30,  0, 4800.00),
(5, 5, 25,  0, 3300.00);
GO

/* ---- RETURN_REQUESTS (0 rows before) ---- */
INSERT INTO return_requests (customer_name, customer_contact, part_id, quantity, return_reason, claim_type, status, resolution, decision_notes, inventory_processed)
VALUES
('Kasun Perera',       'kasun@example.com',       1, 1, 'Wrong size supplied',          'RETURN',   'COMPLETED',  'REFUNDED',   'Verified against invoice', 1),
('Nimal Silva',        'nimal@example.com',       3, 1, 'Battery not holding charge',   'WARRANTY', 'APPROVED',   'REPLACED',   'Within 6-month warranty',  0),
('Tharindu Fernando',  'tharindu@example.com',    5, 1, 'Filter arrived damaged',       'RETURN',   'PENDING',    NULL,         NULL,                        0),
('Amal Perera',        'amal@example.com',        4, 1, 'Ordered wrong spark plug set', 'RETURN',   'REJECTED',   'DENIED',     'Item was used, not eligible',0),
('Sahan Jayasuriya',   'sahan@example.com',       5, 1, 'Changed mind after purchase',  'RETURN',   'PROCESSING', NULL,         'Awaiting inspection',       0);
GO

/* ---- INVOICES (0 rows before) ---- */
INSERT INTO invoices (invoice_number, order_id, customer_name, total_amount, amount_paid, payment_status)
VALUES
('INV001', 1, 'Kasun Perera',      25000.00, 25000.00, 'PAID'),
('INV002', 2, 'Nimal Silva',       35000.00, 20000.00, 'PARTIAL'),
('INV003', 3, 'Tharindu Fernando', 28500.00, 0.00,     'PENDING'),
('INV004', 4, 'Amal Perera',       11000.00, 11000.00, 'PAID'),
('INV005', 5, 'Sahan Jayasuriya',  4500.00,  0.00,     'PENDING');
GO

/* ---- PAYMENTS (0 rows before) ---- */
INSERT INTO payments (invoice_id, amount, payment_method, reference_no)
VALUES
(1, 25000.00, 'CARD',   'TXN-0001'),
(2, 20000.00, 'ONLINE', 'TXN-0002'),
(4, 11000.00, 'CASH',   NULL),
(2, 5000.00,  'CASH',   'TXN-0003');
GO
-- NOTE: CK_payments_amount requires amount > 0, so the 0.00 row above will be
-- REJECTED by the CHECK constraint. Delete that line before running, it was
-- left in deliberately so you can see the constraint firing if you want a
-- screenshot of a rejected insert for your report.


/* ================================================================
   PART D — 5 REQUIRED QUERIES (Simple SELECT, JOIN, Aggregation,
   GROUP BY/HAVING, Subquery)
   ================================================================ */

-- 1. Simple SELECT — parts at or below their reorder level (low-stock alert)
SELECT product_code, name, stock_quantity, reorder_level
FROM parts
WHERE stock_quantity <= reorder_level;

-- 2. JOIN — order line items with customer and part detail
SELECT o.order_number, o.customer_name, p.name AS part_name, oi.quantity, oi.subtotal
FROM orders o
INNER JOIN order_items oi ON o.id = oi.order_id
INNER JOIN parts p        ON oi.part_id = p.id
ORDER BY o.order_number;

-- 3. Aggregation — overall sales summary (no grouping)
SELECT COUNT(*) AS total_orders,
       SUM(total_amount) AS total_revenue,
       AVG(total_amount) AS avg_order_value
FROM orders;

-- 4. GROUP BY / HAVING — suppliers with more than 1 purchase order
SELECT s.supplier_name, COUNT(po.id) AS purchase_order_count
FROM purchase_orders po
INNER JOIN suppliers s ON po.supplier_id = s.id
GROUP BY s.supplier_name
HAVING COUNT(po.id) > 1;

-- 5. Subquery — parts priced above the average part price
SELECT name, price
FROM parts
WHERE price > (SELECT AVG(price) FROM parts);


/* ================================================================
   PART E — STORED PROCEDURE
   Adjusts stock for a part, guards against negative stock,
   and writes both a stock_adjustments row and a part_history row.
   ================================================================ */

CREATE PROCEDURE sp_AdjustStock
    @PartID BIGINT,
    @ChangeAmount INT,
    @Reason VARCHAR(50),
    @AdjustedBy VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (SELECT 1 FROM parts WHERE id = @PartID)
    BEGIN
        RAISERROR('Part not found.', 16, 1);
        RETURN;
    END

    DECLARE @CurrentStock INT;
    SELECT @CurrentStock = stock_quantity FROM parts WHERE id = @PartID;

    IF (@CurrentStock + @ChangeAmount) < 0
    BEGIN
        RAISERROR('Adjustment would result in negative stock.', 16, 1);
        RETURN;
    END

    BEGIN TRANSACTION;

    UPDATE parts
    SET stock_quantity = stock_quantity + @ChangeAmount
    WHERE id = @PartID;

    INSERT INTO stock_adjustments (part_id, change_amount, reason, adjusted_by)
    VALUES (@PartID, @ChangeAmount, @Reason, @AdjustedBy);

    INSERT INTO part_history (part_id, event_type, quantity, notes)
    VALUES (
        @PartID,
        CASE WHEN @ChangeAmount >= 0 THEN 'RECEIVED' ELSE 'ADJUSTED' END,
        ABS(@ChangeAmount),
        @Reason
    );

    COMMIT TRANSACTION;
END
GO

-- Sample execution:
EXEC sp_AdjustStock
    @PartID = 1,
    @ChangeAmount = -3,
    @Reason = 'Damaged in warehouse',
    @AdjustedBy = 'Zee';
GO

-- Verify it worked:
SELECT id, name, stock_quantity FROM parts WHERE id = 1;
SELECT * FROM stock_adjustments WHERE part_id = 1 ORDER BY id DESC;
SELECT * FROM part_history WHERE part_id = 1 ORDER BY id DESC;


/* ================================================================
   PART F — TRIGGER
   After a new order_item is inserted: reduce the part's stock,
   recalculate the parent order's total_amount, and log the sale
   into part_history automatically.
   ================================================================ */

CREATE TRIGGER trg_OrderItems_AfterInsert
ON order_items
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE p
    SET p.stock_quantity = p.stock_quantity - i.quantity
    FROM parts p
    INNER JOIN inserted i ON p.id = i.part_id;

    UPDATE o
    SET o.total_amount = (
        SELECT SUM(subtotal) FROM order_items WHERE order_id = o.id
    )
    FROM orders o
    INNER JOIN inserted i ON o.id = i.order_id;

    INSERT INTO part_history (part_id, event_type, quantity, notes)
    SELECT part_id, 'SOLD', quantity, 'Auto-logged by trg_OrderItems_AfterInsert'
    FROM inserted;
END
GO

-- Demonstration: add a new line item to ORD001 and watch stock + total update
SELECT stock_quantity FROM parts WHERE id = 2;         -- BEFORE
SELECT total_amount FROM orders WHERE id = 1;          -- BEFORE

INSERT INTO order_items (order_id, part_id, quantity, unit_price, subtotal)
VALUES (1, 2, 2, 3500.00, 7000.00);

SELECT stock_quantity FROM parts WHERE id = 2;         -- AFTER (should drop by 2)
SELECT total_amount FROM orders WHERE id = 1;          -- AFTER (should increase by 7000)
SELECT * FROM part_history WHERE part_id = 2 ORDER BY id DESC; -- new SOLD row
GO

UPDATE users
SET password_hash = '$2b$10$SvcocV6UnQ3SNQ8ao7YBgeiPH6RoX0msoQSWgxJpfgubTMoAyrE5K'
WHERE email = 'admin@comspare.com';
GO

SELECT * FROM roles;
SELECT * FROM users;
SELECT * FROM audit_logs;
SELECT * FROM parts;
SELECT * FROM stock_adjustments;
SELECT * FROM part_history;
SELECT * FROM suppliers;
SELECT * FROM purchase_orders;
SELECT * FROM purchase_order_items;
SELECT * FROM return_requests;
SELECT * FROM orders;
SELECT * FROM order_items;
SELECT * FROM invoices;
SELECT * FROM payments;

/* 1. The app now reduces stock itself (and checks availability + writes part_history).
      The old trigger did the same thing again, so every order took stock off TWICE. */
IF OBJECT_ID('trg_OrderItems_AfterInsert', 'TR') IS NOT NULL
    DROP TRIGGER trg_OrderItems_AfterInsert;
GO

/* 2. The seed users had fake password hashes, so they could never log in.
      This gives them all the password:  Comspare@123  */
UPDATE users
SET password_hash = '$2b$10$WDWnWpBh4iXQcBLFyqulYuUyoMrZEUFWOdY7xr7PqG21G77f2dXsm'
WHERE password_hash LIKE '$2a$10$exampleBCryptHash%';
GO

SELECT id, name, email FROM users;

/* 1. "Delete" a part = discontinue it (kept for history, hidden from lists).
      Adds ONE column; no foreign keys are touched. */
IF COL_LENGTH('parts', 'is_active') IS NULL
    ALTER TABLE parts ADD is_active BIT NOT NULL CONSTRAINT DF_parts_is_active DEFAULT 1;
GO

/* 2. Part history can now also record these events */
ALTER TABLE part_history DROP CONSTRAINT CK_parthist_event_type;
GO
ALTER TABLE part_history ADD CONSTRAINT CK_parthist_event_type
    CHECK (event_type IN ('RECEIVED','SOLD','RETURNED','ADJUSTED','DAMAGED',
                          'CREATED','UPDATED','DISCONTINUED','RESTORED'));
GO



/* Users: soft delete (Deactivate / Reactivate) */
IF COL_LENGTH('users', 'active') IS NULL
    ALTER TABLE users ADD active BIT NOT NULL CONSTRAINT DF_users_active DEFAULT 1;
GO

/* Audit trail table used by the new AuditLog entity (append-only) */
IF OBJECT_ID('audit_log', 'U') IS NULL
CREATE TABLE audit_log (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    performed_by  VARCHAR(150) NOT NULL,
    action        VARCHAR(60)  NOT NULL,
    target_record VARCHAR(255) NULL,
    old_value     VARCHAR(500) NULL,
    new_value     VARCHAR(500) NULL,
    logged_at     DATETIME2    NOT NULL DEFAULT SYSDATETIME()
);
GO
IF OBJECT_ID('trg_audit_log_append_only', 'TR') IS NOT NULL
    DROP TRIGGER trg_audit_log_append_only;
GO
CREATE TRIGGER trg_audit_log_append_only ON audit_log INSTEAD OF UPDATE, DELETE AS
BEGIN
    RAISERROR('audit_log is append-only: UPDATE and DELETE are not allowed.', 16, 1);
    ROLLBACK TRANSACTION;
END;
GO

/* Remove duplicate roles (e.g. 'Store Keeper' next to 'STORE_KEEPER') if an old script created them */
DELETE FROM roles
WHERE role_name IN ('Operations Manager','Inventory Supervisor','Store Keeper',
                    'Customer Service Executive','Finance Officer')
  AND id NOT IN (SELECT role_id FROM users);
GO

SELECT id, role_name FROM roles;
SELECT id, name, email, active FROM users;
GO

/* ============================================================
   END OF COMSPARE DATABASE
   ============================================================ */