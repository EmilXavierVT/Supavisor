CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    address VARCHAR(255),
    postalCode VARCHAR(255),
    city VARCHAR(255),
    country VARCHAR(255),
    corporateIdentificationNumber VARCHAR(255),
    currency VARCHAR(255) NOT NULL,
    economic_customer_number INTEGER UNIQUE,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    createdAt TIMESTAMP,
    updatedAt TIMESTAMP
);
