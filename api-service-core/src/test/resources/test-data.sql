-- 测试数据初始化脚本

-- 插入数据源数据
INSERT INTO data_sources (id, name, description, type, host, port, database_name, username, password, connection_url, created_by, updated_by) VALUES
(1, 'Test MySQL DataSource', 'MySQL测试数据源', 'MYSQL', 'localhost', 3306, 'testdb', 'testuser', 'testpass', 'jdbc:h2:mem:datasource_test;MODE=MySQL', 1, 1),
(2, 'Test PostgreSQL DataSource', 'PostgreSQL测试数据源', 'POSTGRESQL', 'localhost', 5432, 'testdb', 'postgres', 'testpass', 'jdbc:postgresql://localhost:5432/testdb', 1, 1);

-- 插入测试用户数据
INSERT INTO test_users (id, username, email, password, status) VALUES
(1, 'testuser1', 'user1@test.com', 'password123', 'ACTIVE'),
(2, 'testuser2', 'user2@test.com', 'password123', 'ACTIVE'),
(3, 'testuser3', 'user3@test.com', 'password123', 'INACTIVE');

-- 插入产品分类数据
INSERT INTO test_categories (id, name, description, parent_id) VALUES
(1, '电子产品', '各种电子设备', NULL),
(2, '服装', '各种服装商品', NULL),
(3, '手机', '智能手机', 1),
(4, '笔记本电脑', '便携式电脑', 1);

-- 插入产品数据
INSERT INTO test_products (id, name, description, price, category_id, status) VALUES
(1, 'iPhone 15', '苹果最新款手机', 8999.00, 3, 'ACTIVE'),
(2, 'MacBook Pro', '苹果专业笔记本电脑', 16999.00, 4, 'ACTIVE'),
(3, 'Samsung Galaxy', '三星旗舰手机', 7999.00, 3, 'ACTIVE'),
(4, '联想ThinkPad', '商务笔记本电脑', 8999.00, 4, 'ACTIVE'),
(5, '休闲T恤', '纯棉休闲T恤', 99.00, 2, 'ACTIVE'),
(6, '牛仔裤', '经典牛仔裤', 299.00, 2, 'INACTIVE');

-- 插入订单数据
INSERT INTO test_orders (id, user_id, product_id, quantity, unit_price, total_price, status, order_date) VALUES
(1, 1, 1, 1, 8999.00, 8999.00, 'COMPLETED', '2024-01-15 10:30:00'),
(2, 1, 5, 2, 99.00, 198.00, 'COMPLETED', '2024-01-16 14:20:00'),
(3, 2, 2, 1, 16999.00, 16999.00, 'PENDING', '2024-01-17 09:15:00'),
(4, 2, 3, 1, 7999.00, 7999.00, 'CANCELLED', '2024-01-18 16:45:00'),
(5, 3, 4, 1, 8999.00, 8999.00, 'PENDING', '2024-01-19 11:00:00');