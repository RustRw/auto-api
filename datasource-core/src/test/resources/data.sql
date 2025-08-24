-- 插入测试数据

-- 用户测试数据
INSERT INTO test_users (username, email, full_name, age, is_active) VALUES
('john_doe', 'john@example.com', 'John Doe', 30, true),
('jane_smith', 'jane@example.com', 'Jane Smith', 25, true),
('bob_wilson', 'bob@example.com', 'Bob Wilson', 35, false),
('alice_brown', 'alice@example.com', 'Alice Brown', 28, true),
('charlie_davis', 'charlie@example.com', 'Charlie Davis', 32, true);

-- 产品测试数据
INSERT INTO test_products (product_name, description, price, category, stock_quantity) VALUES
('Laptop Pro', 'High-performance laptop for professionals', 1999.99, 'Electronics', 50),
('Smartphone X', 'Latest smartphone with advanced features', 899.99, 'Electronics', 100),
('Office Chair', 'Ergonomic office chair for comfort', 299.99, 'Furniture', 25),
('Coffee Maker', 'Automatic coffee maker with timer', 129.99, 'Appliances', 75),
('Running Shoes', 'Professional running shoes for athletes', 159.99, 'Sports', 80),
('Wireless Headphones', 'Noise-cancelling wireless headphones', 249.99, 'Electronics', 60),
('Standing Desk', 'Adjustable standing desk for office', 499.99, 'Furniture', 15),
('Fitness Tracker', 'Smart fitness tracker with heart rate monitor', 199.99, 'Electronics', 40);

-- 订单测试数据
INSERT INTO test_orders (user_id, product_id, quantity, total_amount, order_status) VALUES
(1, 1, 1, 1999.99, 'COMPLETED'),
(1, 4, 2, 259.98, 'COMPLETED'),
(2, 2, 1, 899.99, 'PENDING'),
(2, 5, 1, 159.99, 'SHIPPED'),
(3, 3, 1, 299.99, 'CANCELLED'),
(4, 6, 1, 249.99, 'COMPLETED'),
(4, 7, 1, 499.99, 'PENDING'),
(5, 8, 2, 399.98, 'SHIPPED'),
(1, 6, 1, 249.99, 'COMPLETED'),
(2, 3, 1, 299.99, 'PENDING');

-- 更新时间戳以确保有不同的创建时间
UPDATE test_users SET created_at = DATEADD('DAY', -10, created_at) WHERE id = 1;
UPDATE test_users SET created_at = DATEADD('DAY', -8, created_at) WHERE id = 2;
UPDATE test_users SET created_at = DATEADD('DAY', -5, created_at) WHERE id = 3;

UPDATE test_products SET created_at = DATEADD('DAY', -15, created_at) WHERE id <= 3;
UPDATE test_products SET created_at = DATEADD('DAY', -7, created_at) WHERE id > 3 AND id <= 6;

UPDATE test_orders SET order_date = DATEADD('DAY', -3, order_date) WHERE id <= 3;
UPDATE test_orders SET order_date = DATEADD('DAY', -1, order_date) WHERE id > 3 AND id <= 6;