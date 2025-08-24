-- 测试环境数据库初始化脚本
-- 禁用外键检查
SET REFERENTIAL_INTEGRITY FALSE;

-- 删除可能存在的表
DROP TABLE IF EXISTS api_service_table_selections;
DROP TABLE IF EXISTS api_service_audit_logs;
DROP TABLE IF EXISTS api_service_versions;
DROP TABLE IF EXISTS api_services;
DROP TABLE IF EXISTS data_sources;

-- 业务测试表
DROP TABLE IF EXISTS test_orders;
DROP TABLE IF EXISTS test_products;
DROP TABLE IF EXISTS test_users;
DROP TABLE IF EXISTS test_categories;

-- 重新启用外键检查
SET REFERENTIAL_INTEGRITY TRUE;

-- 创建数据源表（从datasource-api模块）
CREATE TABLE data_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    type VARCHAR(20) NOT NULL,
    host VARCHAR(100) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(64),
    username VARCHAR(100),
    password VARCHAR(100),
    connection_url VARCHAR(500),
    max_pool_size INT DEFAULT 10,
    min_pool_size INT DEFAULT 1,
    connection_timeout INT DEFAULT 30000,
    idle_timeout INT DEFAULT 600000,
    max_lifetime INT DEFAULT 1800000,
    ssl_enabled BOOLEAN DEFAULT FALSE,
    connection_pool_enabled BOOLEAN DEFAULT TRUE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL
);

-- 创建API服务核心表
CREATE TABLE api_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    path VARCHAR(200) NOT NULL,
    method VARCHAR(10) NOT NULL DEFAULT 'GET',
    datasource_id BIGINT NOT NULL,
    sql_content TEXT NOT NULL,
    request_params TEXT,
    response_example TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cache_enabled BOOLEAN DEFAULT FALSE,
    cache_duration INT DEFAULT 300,
    rate_limit INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    
    INDEX idx_created_by (created_by),
    INDEX idx_status (status),
    INDEX idx_path_method (path, method),
    INDEX idx_datasource_id (datasource_id),
    UNIQUE KEY uk_name_created_by (name, created_by)
);

-- 创建API服务版本表
CREATE TABLE api_service_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_service_id BIGINT NOT NULL,
    version VARCHAR(50) NOT NULL,
    version_description VARCHAR(500),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    path VARCHAR(200) NOT NULL,
    method VARCHAR(10) NOT NULL,
    datasource_id BIGINT NOT NULL,
    sql_content TEXT NOT NULL,
    request_params TEXT,
    response_example TEXT,
    status VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    unpublished_at TIMESTAMP,
    cache_enabled BOOLEAN DEFAULT FALSE,
    cache_duration INT DEFAULT 300,
    rate_limit INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    
    INDEX idx_api_service_id (api_service_id),
    INDEX idx_version (api_service_id, version),
    INDEX idx_active (api_service_id, is_active),
    UNIQUE KEY uk_api_service_version (api_service_id, version)
);

-- 创建审计日志表
CREATE TABLE api_service_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_service_id BIGINT,
    operation_type VARCHAR(20) NOT NULL,
    operation_description VARCHAR(200) NOT NULL,
    operation_details TEXT,
    before_data TEXT,
    after_data TEXT,
    operation_result VARCHAR(20) NOT NULL,
    error_message TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    
    INDEX idx_api_service_id (api_service_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_by (created_by),
    INDEX idx_created_at (created_at)
);

-- 创建表选择配置表
CREATE TABLE api_service_table_selections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_service_id BIGINT NOT NULL,
    database_name VARCHAR(64),
    schema_name VARCHAR(64),
    table_name VARCHAR(64) NOT NULL,
    table_alias VARCHAR(32),
    table_type VARCHAR(20),
    selected_columns TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    join_condition TEXT,
    join_type VARCHAR(10),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    
    INDEX idx_api_service_id (api_service_id),
    INDEX idx_table_name (table_name),
    INDEX idx_is_primary (is_primary)
);

-- 创建业务测试表
-- 用户表
CREATE TABLE test_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 产品分类表
CREATE TABLE test_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 产品表
CREATE TABLE test_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES test_categories(id)
);

-- 订单表
CREATE TABLE test_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES test_users(id),
    FOREIGN KEY (product_id) REFERENCES test_products(id)
);

-- 创建索引
CREATE INDEX idx_users_username ON test_users(username);
CREATE INDEX idx_users_email ON test_users(email);
CREATE INDEX idx_users_status ON test_users(status);
CREATE INDEX idx_products_category ON test_products(category_id);
CREATE INDEX idx_products_status ON test_products(status);
CREATE INDEX idx_orders_user ON test_orders(user_id);
CREATE INDEX idx_orders_product ON test_orders(product_id);
CREATE INDEX idx_orders_status ON test_orders(status);
CREATE INDEX idx_orders_date ON test_orders(order_date);