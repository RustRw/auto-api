/* Auto API Platform - 公共工具函数 */

/**
 * 获取数据源类型对应的图标
 * @param {string} type - 数据源类型
 * @returns {string} FontAwesome图标类名
 */
function getDatabaseIcon(type) {
    if (!type) return 'fas fa-database';
    
    const iconMap = {
        'MYSQL': 'fas fa-database',
        'POSTGRESQL': 'fas fa-elephant',
        'H2': 'fas fa-memory',
        'ORACLE': 'fas fa-building',
        'MONGODB': 'fas fa-leaf',
        'ELASTICSEARCH': 'fas fa-search',
        'CLICKHOUSE': 'fas fa-chart-line',
        'STARROCKS': 'fas fa-star',
        'TDENGINE': 'fas fa-clock',
        'NEBULA_GRAPH': 'fas fa-project-diagram',
        'KAFKA': 'fas fa-stream',
        'HTTP_API': 'fas fa-globe',
        'HTTPS_API': 'fas fa-lock'
    };
    
    return iconMap[type.toUpperCase()] || 'fas fa-database';
}

/**
 * 获取数据源类型对应的CSS类名
 * @param {string} type - 数据源类型
 * @returns {string} CSS类名
 */
function getDatabaseTypeClass(type) {
    if (!type) return 'type-default';
    
    const classMap = {
        'MYSQL': 'type-mysql',
        'POSTGRESQL': 'type-postgresql',
        'H2': 'type-h2',
        'ORACLE': 'type-oracle',
        'MONGODB': 'type-mongodb',
        'ELASTICSEARCH': 'type-elasticsearch',
        'CLICKHOUSE': 'type-clickhouse',
        'STARROCKS': 'type-default',
        'TDENGINE': 'type-default',
        'NEBULA_GRAPH': 'type-default',
        'KAFKA': 'type-default',
        'HTTP_API': 'type-default',
        'HTTPS_API': 'type-default'
    };
    
    return classMap[type.toUpperCase()] || 'type-default';
}

/**
 * 获取数据源类型对应的图标颜色类
 * @param {string} type - 数据源类型
 * @returns {string} 图标颜色CSS类名
 */
function getDatabaseIconClass(type) {
    if (!type) return 'db-icon-default';
    
    const classMap = {
        'MYSQL': 'db-icon-mysql',
        'POSTGRESQL': 'db-icon-postgresql',
        'H2': 'db-icon-h2',
        'ORACLE': 'db-icon-oracle',
        'MONGODB': 'db-icon-mongodb',
        'ELASTICSEARCH': 'db-icon-elasticsearch',
        'CLICKHOUSE': 'db-icon-clickhouse'
    };
    
    return classMap[type.toUpperCase()] || 'db-icon-default';
}

/**
 * 生成带图标的数据源类型标签HTML
 * @param {string} type - 数据源类型
 * @returns {string} HTML字符串
 */
function generateTypeTag(type) {
    const icon = getDatabaseIcon(type);
    const typeClass = getDatabaseTypeClass(type);
    const iconClass = getDatabaseIconClass(type);
    
    return `
        <span class="type-tag ${typeClass}">
            <i class="${icon} db-icon ${iconClass}"></i>
            ${type}
        </span>
    `;
}

/**
 * 获取数据源类型的显示名称
 * @param {string} type - 数据源类型
 * @returns {string} 显示名称
 */
function getDatabaseDisplayName(type) {
    if (!type) return 'Unknown';
    
    const nameMap = {
        'MYSQL': 'MySQL',
        'POSTGRESQL': 'PostgreSQL', 
        'H2': 'H2 Database',
        'ORACLE': 'Oracle',
        'MONGODB': 'MongoDB',
        'ELASTICSEARCH': 'Elasticsearch',
        'CLICKHOUSE': 'ClickHouse',
        'STARROCKS': 'StarRocks',
        'TDENGINE': 'TDengine',
        'NEBULA_GRAPH': 'NebulaGraph',
        'KAFKA': 'Apache Kafka',
        'HTTP_API': 'HTTP API',
        'HTTPS_API': 'HTTPS API'
    };
    
    return nameMap[type.toUpperCase()] || type;
}

/**
 * 格式化日期
 * @param {*} dateInput - 日期输入（可能是数组或字符串）
 * @returns {string} 格式化后的日期字符串
 */
function formatDate(dateInput) {
    if (!dateInput) return 'N/A';
    try {
        let date;
        
        // 检查是否为数组格式 [year, month, day, hour, minute, second, nano]
        if (Array.isArray(dateInput)) {
            if (dateInput.length >= 3) {
                const [year, month, day, hour = 0, minute = 0, second = 0] = dateInput;
                date = new Date(year, month - 1, day, hour, minute, second);
            } else {
                return 'N/A';
            }
        } else {
            date = new Date(dateInput);
            if (isNaN(date.getTime())) {
                date = new Date(dateInput + 'Z');
            }
            if (isNaN(date.getTime())) {
                date = new Date(dateInput.replace('T', ' '));
            }
        }
        
        if (isNaN(date.getTime())) {
            return 'N/A';
        }
        
        return date.toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    } catch (error) {
        console.error('日期格式化错误:', error, dateInput);
        return 'N/A';
    }
}

/**
 * 获取状态样式类
 * @param {string} status - 状态
 * @param {string} type - 类型 ('service' 或 'datasource')
 * @returns {string} CSS类名
 */
function getStatusClass(status, type = 'datasource') {
    if (type === 'service') {
        switch(status) {
            case 'PUBLISHED':
                return 'status-active';
            case 'DRAFT':
                return 'status-draft';
            case 'DISABLED':
                return 'status-inactive';
            case 'ARCHIVED':
                return 'status-error';
            default:
                return 'status-draft';
        }
    } else {
        // datasource
        return status ? 'status-enabled' : 'status-disabled';
    }
}

/**
 * 获取状态显示文本
 * @param {string|boolean} status - 状态
 * @param {string} type - 类型 ('service' 或 'datasource')
 * @returns {string} 显示文本
 */
function getStatusText(status, type = 'datasource') {
    if (type === 'service') {
        switch(status) {
            case 'PUBLISHED':
                return '已发布';
            case 'DRAFT':
                return '草稿';
            case 'DISABLED':
                return '已禁用';
            case 'ARCHIVED':
                return '已归档';
            default:
                return '未知';
        }
    } else {
        // datasource
        return status ? '启用' : '禁用';
    }
}

/**
 * 获取HTTP方法样式类
 * @param {string} method - HTTP方法
 * @returns {string} CSS类名
 */
function getMethodClass(method) {
    switch(method) {
        case 'GET':
            return 'method-get';
        case 'POST':
            return 'method-post';
        case 'PUT':
            return 'method-put';
        case 'DELETE':
            return 'method-delete';
        default:
            return 'method-get';
    }
}

/**
 * 显示通用的加载状态
 * @param {boolean} show - 是否显示
 * @param {string} loadingId - 加载元素ID
 * @param {string} contentId - 内容元素ID
 */
function showLoading(show, loadingId = 'loading', contentId = 'table-content') {
    const loading = document.getElementById(loadingId);
    const content = document.getElementById(contentId);
    
    if (loading && content) {
        if (show) {
            loading.style.display = 'block';
            content.style.display = 'none';
        } else {
            loading.style.display = 'none';
            content.style.display = 'block';
        }
    }
}

/**
 * 显示错误信息
 * @param {string} message - 错误消息
 */
function showError(message) {
    alert(message);
}

/**
 * 退出登录
 */
function logout() {
    if (confirm('确定要退出登录吗？')) {
        localStorage.clear();
        window.location.href = 'login.html';
    }
}

/**
 * 侧边面板管理
 */
class SidebarManager {
    constructor(overlayId = 'sidebar-overlay', panelId = 'sidebar-panel') {
        this.overlay = document.getElementById(overlayId);
        this.panel = document.getElementById(panelId);
        this.isOpen = false;
        
        // 绑定关闭事件
        if (this.overlay) {
            this.overlay.addEventListener('click', () => this.close());
        }
        
        // 绑定ESC键关闭
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.close();
            }
        });
    }
    
    open() {
        if (this.overlay && this.panel) {
            this.overlay.classList.add('show');
            this.panel.classList.add('show');
            this.isOpen = true;
            document.body.style.overflow = 'hidden';
        }
    }
    
    close() {
        if (this.overlay && this.panel) {
            this.overlay.classList.remove('show');
            this.panel.classList.remove('show');
            this.isOpen = false;
            document.body.style.overflow = '';
        }
    }
    
    setTitle(title) {
        const titleElement = this.panel?.querySelector('.sidebar-title');
        if (titleElement) {
            titleElement.textContent = title;
        }
    }
}

/**
 * 显示通知消息
 * @param {string} message - 消息内容
 * @param {string} type - 消息类型 ('success', 'error', 'warning', 'info')
 * @param {number} duration - 显示时长(ms)，0为不自动消失
 */
function showNotification(message, type = 'info', duration = 3000) {
    // 移除已存在的通知
    const existingNotification = document.querySelector('.notification-toast');
    if (existingNotification) {
        existingNotification.remove();
    }
    
    const notification = document.createElement('div');
    notification.className = `notification-toast alert alert-${type}`;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 10000;
        min-width: 300px;
        max-width: 500px;
        transform: translateX(100%);
        transition: transform 0.3s ease;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    `;
    
    notification.innerHTML = `
        <i class="fas fa-${getNotificationIcon(type)}"></i>
        <span>${message}</span>
        <button onclick="this.parentElement.remove()" style="
            background: none; 
            border: none; 
            margin-left: auto; 
            cursor: pointer;
            opacity: 0.7;
            padding: 0 4px;
        ">
            <i class="fas fa-times"></i>
        </button>
    `;
    
    document.body.appendChild(notification);
    
    // 触发动画
    setTimeout(() => {
        notification.style.transform = 'translateX(0)';
    }, 10);
    
    // 自动消失
    if (duration > 0) {
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => notification.remove(), 300);
        }, duration);
    }
}

/**
 * 获取通知图标
 */
function getNotificationIcon(type) {
    const iconMap = {
        'success': 'check-circle',
        'error': 'exclamation-circle',
        'warning': 'exclamation-triangle',
        'info': 'info-circle'
    };
    return iconMap[type] || 'info-circle';
}

/**
 * 表单验证工具
 */
class FormValidator {
    constructor(formElement) {
        this.form = formElement;
        this.errors = {};
    }
    
    required(fieldName, message = '此字段为必填项') {
        const field = this.form.querySelector(`[name="${fieldName}"]`);
        if (!field || !field.value.trim()) {
            this.errors[fieldName] = message;
            return false;
        }
        return true;
    }
    
    email(fieldName, message = '请输入有效的邮箱地址') {
        const field = this.form.querySelector(`[name="${fieldName}"]`);
        if (field && field.value) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(field.value)) {
                this.errors[fieldName] = message;
                return false;
            }
        }
        return true;
    }
    
    number(fieldName, message = '请输入有效的数字') {
        const field = this.form.querySelector(`[name="${fieldName}"]`);
        if (field && field.value && isNaN(field.value)) {
            this.errors[fieldName] = message;
            return false;
        }
        return true;
    }
    
    showErrors() {
        // 清除之前的错误提示
        this.form.querySelectorAll('.field-error').forEach(error => error.remove());
        
        // 显示新的错误提示
        Object.keys(this.errors).forEach(fieldName => {
            const field = this.form.querySelector(`[name="${fieldName}"]`);
            if (field) {
                field.style.borderColor = '#ff4d4f';
                const errorDiv = document.createElement('div');
                errorDiv.className = 'field-error';
                errorDiv.style.cssText = 'color: #ff4d4f; font-size: 12px; margin-top: 4px;';
                errorDiv.textContent = this.errors[fieldName];
                field.parentNode.appendChild(errorDiv);
            }
        });
    }
    
    clearErrors() {
        this.errors = {};
        this.form.querySelectorAll('.field-error').forEach(error => error.remove());
        this.form.querySelectorAll('input, select, textarea').forEach(field => {
            field.style.borderColor = '';
        });
    }
    
    isValid() {
        return Object.keys(this.errors).length === 0;
    }
}