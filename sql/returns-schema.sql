USE ComSpareDB;
GO

IF OBJECT_ID('return_requests', 'U') IS NULL
BEGIN
    CREATE TABLE return_requests (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        customer_name VARCHAR(150) NOT NULL,

        customer_contact VARCHAR(100) NOT NULL,

        part_id BIGINT NOT NULL,

        quantity INT NOT NULL,

        return_reason VARCHAR(500) NOT NULL,

        claim_type VARCHAR(20) NOT NULL
            CONSTRAINT CK_return_claim_type
            CHECK (claim_type IN ('RETURN', 'WARRANTY')),

        status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
            CONSTRAINT CK_return_status
            CHECK (status IN (
                'PENDING',
                'APPROVED',
                'REJECTED',
                'PROCESSING',
                'COMPLETED',
                'CANCELLED'
            )),

        resolution VARCHAR(20),

        decision_notes VARCHAR(500),

        inventory_processed BIT NOT NULL DEFAULT 0,

        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),

        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_return_requests_part
            FOREIGN KEY (part_id)
            REFERENCES parts(id)
    );
END;
GO
