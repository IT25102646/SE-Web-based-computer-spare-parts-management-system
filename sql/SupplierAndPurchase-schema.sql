-- =============================================
-- SUPPLIER & PURCHASE MANAGEMENT
-- Database Schema
-- =============================================

-- =============================================
-- 1. SUPPLIERS
-- =============================================

CREATE TABLE suppliers (
                           id BIGINT IDENTITY(1,1) PRIMARY KEY,
                           supplier_name VARCHAR(255) NOT NULL,
                           contact_person VARCHAR(255),
                           phone VARCHAR(50),
                           email VARCHAR(255),
                           address VARCHAR(500),
                           status VARCHAR(50)
);


-- =============================================
-- 2. PURCHASE ORDERS
-- =============================================

CREATE TABLE purchase_orders (
                                 id BIGINT IDENTITY(1,1) PRIMARY KEY,

                                 order_number VARCHAR(255) NOT NULL UNIQUE,

                                 supplier_id BIGINT NOT NULL,

                                 order_date DATE,
                                 expected_delivery_date DATE,
                                 delivery_date DATE,

                                 status VARCHAR(50),

                                 notes VARCHAR(1000),

                                 CONSTRAINT fk_purchase_order_supplier
                                     FOREIGN KEY (supplier_id)
                                         REFERENCES suppliers(id)
);


-- =============================================
-- 3. PURCHASE ORDER ITEMS
-- =============================================

CREATE TABLE purchase_order_items (
                                      id BIGINT IDENTITY(1,1) PRIMARY KEY,

                                      purchase_order_id BIGINT NOT NULL,

                                      part_id BIGINT NOT NULL,

                                      ordered_quantity INT NOT NULL,

                                      received_quantity INT,

                                      unit_cost DECIMAL(10,2),

                                      CONSTRAINT fk_purchase_order_item_order
                                          FOREIGN KEY (purchase_order_id)
                                              REFERENCES purchase_orders(id)
);