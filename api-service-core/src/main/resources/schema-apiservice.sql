-- API Service Core 模块数据库表结构

-- API服务主表
CREATE TABLE api_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT 'API服务名称',
    description VARCHAR(200) COMMENT 'API服务描述',
    path VARCHAR(200) NOT NULL COMMENT 'API路径',
    method VARCHAR(10) NOT NULL DEFAULT 'GET' COMMENT 'HTTP方法',
    datasource_id BIGINT NOT NULL COMMENT '数据源ID',
    sql_content TEXT NOT NULL COMMENT 'SQL内容',
    request_params TEXT COMMENT '请求参数配置(JSON)',
    response_example TEXT COMMENT '响应示例(JSON)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '服务状态',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    cache_enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用缓存',
    cache_duration INT DEFAULT 300 COMMENT '缓存时长(秒)',
    rate_limit INT DEFAULT 100 COMMENT '限流配置(每分钟请求数)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    updated_by BIGINT NOT NULL COMMENT '更新人ID',
    
    INDEX idx_created_by (created_by),
    INDEX idx_status (status),
    INDEX idx_path_method (path, method),
    INDEX idx_datasource_id (datasource_id),
    UNIQUE KEY uk_name_created_by (name, created_by)
) COMMENT='API服务表';

-- API服务版本表
CREATE TABLE api_service_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_service_id BIGINT NOT NULL COMMENT '关联的API服务ID',
    version VARCHAR(50) NOT NULL COMMENT '版本号',
    version_description VARCHAR(500) COMMENT '版本描述',
    name VARCHAR(100) NOT NULL COMMENT 'API名称快照',
    description VARCHAR(200) COMMENT 'API描述快照',
    path VARCHAR(200) NOT NULL COMMENT 'API路径快照',
    method VARCHAR(10) NOT NULL COMMENT 'HTTP方法快照',
    datasource_id BIGINT NOT NULL COMMENT '数据源ID快照',
    sql_content TEXT NOT NULL COMMENT 'SQL内容快照',
    request_params TEXT COMMENT '请求参数快照',
    response_example TEXT COMMENT '响应示例快照',
    status VARCHAR(20) NOT NULL COMMENT '版本状态',
    is_active BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为激活版本',
    published_at TIMESTAMP COMMENT '发布时间',
    unpublished_at TIMESTAMP COMMENT '下线时间',
    cache_enabled BOOLEAN DEFAULT FALSE COMMENT '缓存配置快照',
    cache_duration INT DEFAULT 300 COMMENT '缓存时长快照',
    rate_limit INT DEFAULT 100 COMMENT '限流配置快照',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    updated_by BIGINT NOT NULL COMMENT '更新人ID',
    
    INDEX idx_api_service_id (api_service_id),
    INDEX idx_version (api_service_id, version),
    INDEX idx_active (api_service_id, is_active),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_api_service_version (api_service_id, version)
) COMMENT='API服务版本表';

-- API服务审计日志表
CREATE TABLE api_service_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_service_id BIGINT COMMENT '关联的API服务ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型',
    operation_description VARCHAR(200) NOT NULL COMMENT '操作描述',
    operation_details TEXT COMMENT '操作详情(JSON)',
    before_data TEXT COMMENT '操作前数据快照(JSON)',
    after_data TEXT COMMENT '操作后数据快照(JSON)',
    operation_result VARCHAR(20) NOT NULL COMMENT '操作结果',
    error_message TEXT COMMENT '错误信息',
    ip_address VARCHAR(45) COMMENT '操作人IP',
    user_agent VARCHAR(500) COMMENT '用户代理',
    duration_ms BIGINT COMMENT '操作耗时(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NOT NULL COMMENT '操作人ID',
    updated_by BIGINT NOT NULL COMMENT '更新人ID',
    
    INDEX idx_api_service_id (api_service_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_by (created_by),
    INDEX idx_created_at (created_at),
    INDEX idx_operation_result (operation_result)
) COMMENT='API服务审计日志表';

-- API服务表选择配置表
CREATE TABLE api_service_table_selections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_service_id BIGINT NOT NULL COMMENT '关联的API服务ID',
    database_name VARCHAR(64) COMMENT '数据库名称',
    schema_name VARCHAR(64) COMMENT '模式名称',
    table_name VARCHAR(64) NOT NULL COMMENT '表名称',
    table_alias VARCHAR(32) COMMENT '表别名',
    table_type VARCHAR(20) COMMENT '表类型',
    selected_columns TEXT COMMENT '选择的字段列表(JSON)',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为主表',
    join_condition TEXT COMMENT '关联条件',
    join_type VARCHAR(10) COMMENT '关联类型',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    updated_by BIGINT NOT NULL COMMENT '更新人ID',
    
    INDEX idx_api_service_id (api_service_id),
    INDEX idx_table_name (table_name),
    INDEX idx_is_primary (is_primary),
    INDEX idx_sort_order (sort_order)
) COMMENT='API服务表选择配置表';

-- 创建外键约束
ALTER TABLE api_service_versions 
ADD CONSTRAINT fk_version_api_service 
FOREIGN KEY (api_service_id) REFERENCES api_services(id) ON DELETE CASCADE;

ALTER TABLE api_service_audit_logs 
ADD CONSTRAINT fk_audit_api_service 
FOREIGN KEY (api_service_id) REFERENCES api_services(id) ON DELETE CASCADE;

ALTER TABLE api_service_table_selections 
ADD CONSTRAINT fk_table_selection_api_service 
FOREIGN KEY (api_service_id) REFERENCES api_services(id) ON DELETE CASCADE;