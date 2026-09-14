USE ComSpareDB;
GO

CREATE TABLE return_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_name VARCHAR(150) NOT NULL,
    customer_contact VARCHAR(100) NOT NULL,
    part_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    return_reason VARCHAR(500) NOT NULL,
    claim_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution VARCHAR(20),
    decision_notes VARCHAR(500),
    inventory_processed BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_return_part
        FOREIGN KEY (part_id)
        REFERENCES parts(id)
);
