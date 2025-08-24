-- 多租户数据库迁移脚本

-- 1. 创建租户表
CREATE TABLE tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(100) NOT NULL UNIQUE COMMENT '租户代码',
    tenant_name VARCHAR(200) NOT NULL COMMENT '租户名称',
    description VARCHAR(500) COMMENT '租户描述',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '租户状态: ACTIVE, SUSPENDED, DELETED',
    max_users INT DEFAULT 10 COMMENT '最大用户数',
    max_datasources INT DEFAULT 5 COMMENT '最大数据源数',
    max_api_services INT DEFAULT 20 COMMENT '最大API服务数',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    
    INDEX idx_tenant_code (tenant_code),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) COMMENT='租户表';

-- 2. 为现有表添加tenant_id字段
ALTER TABLE users ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE data_sources ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE api_services ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE api_service_versions ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE api_service_audit_logs ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE api_service_table_selections ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';

-- 3. 添加tenant_id索引
ALTER TABLE users ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE data_sources ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE api_services ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE api_service_versions ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE api_service_audit_logs ADD INDEX idx_tenant_id (tenant_id);
ALTER TABLE api_service_table_selections ADD INDEX idx_tenant_id (tenant_id);

-- 4. 创建复合索引以支持多租户查询
ALTER TABLE users ADD INDEX idx_tenant_username (tenant_id, username);
ALTER TABLE data_sources ADD INDEX idx_tenant_enabled (tenant_id, enabled);
ALTER TABLE data_sources ADD INDEX idx_tenant_name (tenant_id, name);
ALTER TABLE api_services ADD INDEX idx_tenant_status (tenant_id, status);
ALTER TABLE api_services ADD INDEX idx_tenant_name_created_by (tenant_id, name, created_by);

-- 5. 插入默认租户
INSERT INTO tenants (tenant_code, tenant_name, description, status, max_users, max_datasources, max_api_services, contact_email, created_by, updated_by)
VALUES ('default', '默认租户', '系统默认租户', 'ACTIVE', 50, 20, 100, 'admin@example.com', 1, 1);

-- 6. 添加外键约束
ALTER TABLE users ADD CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE data_sources ADD CONSTRAINT fk_datasource_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_services ADD CONSTRAINT fk_apiservice_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_service_versions ADD CONSTRAINT fk_version_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_service_audit_logs ADD CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_service_table_selections ADD CONSTRAINT fk_selection_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- 7. 更新现有数据的租户关联
UPDATE users SET tenant_id = 1 WHERE tenant_id = 1;
UPDATE data_sources SET tenant_id = 1 WHERE tenant_id = 1;
UPDATE api_services SET tenant_id = 1 WHERE tenant_id = 1;
UPDATE api_service_versions SET tenant_id = 1 WHERE tenant_id = 1;
UPDATE api_service_audit_logs SET tenant_id = 1 WHERE tenant_id = 1;
UPDATE api_service_table_selections SET tenant_id = 1 WHERE tenant_id = 1;