-- 创建测试表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    age INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_details (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    address VARCHAR(200),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    category VARCHAR(50),
    price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试数据
INSERT INTO users (id, name, email, age) VALUES 
(1, 'John Doe', 'john@example.com', 30),
(2, 'Jane Smith', 'jane@example.com', 25),
(3, 'Bob Johnson', 'bob@example.com', 35);

INSERT INTO user_details (id, user_id, address, phone) VALUES 
(1, 1, '123 Main St', '555-0101'),
(2, 2, '456 Oak Ave', '555-0102'),
(3, 3, '789 Pine Rd', '555-0103');

INSERT INTO products (id, name, category, price) VALUES 
(1, 'Laptop', 'electronics', 999.99),
(2, 'Smartphone', 'electronics', 699.99),
(3, 'Book', 'education', 29.99),
(4, 'Chair', 'furniture', 149.99);