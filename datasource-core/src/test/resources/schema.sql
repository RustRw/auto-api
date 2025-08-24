-- 创建测试数据库结构
-- 禁用外键检查（H2语法）
SET REFERENTIAL_INTEGRITY FALSE;

-- 按依赖关系逆序删除表
DROP TABLE IF EXISTS test_orders; 
DROP TABLE IF EXISTS test_products;
DROP TABLE IF EXISTS test_users;

-- 重新启用外键检查
SET REFERENTIAL_INTEGRITY TRUE;

-- 用户表
CREATE TABLE test_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    age INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- 产品表
CREATE TABLE test_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    stock_quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE test_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    order_status VARCHAR(20) DEFAULT 'PENDING',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES test_users(id),
    FOREIGN KEY (product_id) REFERENCES test_products(id)
);

-- 创建索引
CREATE INDEX idx_users_username ON test_users(username);
CREATE INDEX idx_users_email ON test_users(email);
CREATE INDEX idx_orders_user_id ON test_orders(user_id);
CREATE INDEX idx_orders_product_id ON test_orders(product_id);
CREATE INDEX idx_orders_status ON test_orders(order_status);
CREATE INDEX idx_products_category ON test_products(category);

-- 创建复合索引
CREATE INDEX idx_orders_user_date ON test_orders(user_id, order_date);
CREATE INDEX idx_products_category_price ON test_products(category, price);

-- 创建唯一索引
CREATE UNIQUE INDEX uk_users_email ON test_users(email);