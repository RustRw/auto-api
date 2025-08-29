import React, { useState, useEffect } from 'react';
import { message } from 'antd';
import './Welcome.css';

interface DashboardStats {
  datasourceCount: number;
  apiServiceCount: number;
  activeServiceCount: number;
  requestCount: number;
}

const Welcome: React.FC = () => {
  const API_BASE_URL = 'http://localhost:8080';
  
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats>({
    datasourceCount: 0,
    apiServiceCount: 0,
    activeServiceCount: 0,
    requestCount: 0
  });

  // 获取认证头
  const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  };

  // 检查登录状态
  const checkAuth = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      window.location.href = '/user/login';
      return false;
    }
    return true;
  };

  // 获取用户信息
  const getUserInfo = () => {
    const username = localStorage.getItem('username');
    const realName = localStorage.getItem('realName');
    const role = localStorage.getItem('role');
    
    return {
      username: username || '',
      realName: realName || '',
      role: role || '',
      displayName: realName || username || '',
      roleText: role === 'ADMIN' ? '管理员' : '普通用户',
      avatar: (realName || username || 'U').charAt(0).toUpperCase()
    };
  };

  // 获取当前日期
  const getCurrentDate = () => {
    const now = new Date();
    const options: Intl.DateTimeFormatOptions = { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric',
      weekday: 'long'
    };
    return now.toLocaleDateString('zh-CN', options);
  };

  // 加载统计数据
  const loadStats = async () => {
    try {
      setLoading(true);
      const newStats: DashboardStats = {
        datasourceCount: 0,
        apiServiceCount: 0,
        activeServiceCount: 0,
        requestCount: 0
      };

      // 加载数据源数量
      try {
        const datasourceResponse = await fetch(`${API_BASE_URL}/api/datasources`, {
          headers: getAuthHeaders()
        });
        if (datasourceResponse.ok) {
          const datasourceData = await datasourceResponse.json();
          newStats.datasourceCount = datasourceData.data?.length || 0;
        }
      } catch (error) {
        console.error('Failed to load datasource count:', error);
      }

      // 加载API服务数量
      try {
        const apiServiceResponse = await fetch(`${API_BASE_URL}/api/services`, {
          headers: getAuthHeaders()
        });
        if (apiServiceResponse.ok) {
          const apiServiceData = await apiServiceResponse.json();
          const totalServices = apiServiceData.data?.totalElements || 0;
          newStats.apiServiceCount = totalServices;
          // 假设运行中的服务数量（这里可以根据实际状态过滤）
          newStats.activeServiceCount = Math.floor(totalServices * 0.8);
        }
      } catch (error) {
        console.error('Failed to load API service count:', error);
      }

      // 模拟请求数（实际项目中应该从真实的监控数据获取）
      newStats.requestCount = Math.floor(Math.random() * 1000) + 100;

      setStats(newStats);
    } catch (error) {
      console.error('Failed to load dashboard stats:', error);
      message.error('加载统计数据失败');
    } finally {
      setLoading(false);
    }
  };

  // 退出登录
  const handleLogout = () => {
    if (window.confirm('确定要退出登录吗？')) {
      localStorage.clear();
      window.location.href = '/user/login';
    }
  };

  // 页面初始化
  useEffect(() => {
    if (!checkAuth()) return;
    loadStats();
  }, []);

  const userInfo = getUserInfo();

  return (
    <div>
      {/* 页面头部 */}
      <div className="header">
        <div className="header-left">
          <a href="/welcome" className="logo">
            <i className="fas fa-rocket"></i> Auto API Platform
          </a>
          <nav className="nav-menu">
            <a href="/welcome" className="nav-item active">
              <i className="fas fa-tachometer-alt"></i> 仪表板
            </a>
            <a href="/datasource/list" className="nav-item">
              <i className="fas fa-database"></i> 数据源
            </a>
            <a href="/apiservice/list" className="nav-item">
              <i className="fas fa-cogs"></i> API服务
            </a>
            <a href="/apiservice/develop" className="nav-item">
              <i className="fas fa-code"></i> 服务开发
            </a>
            <a href="/monitoring/overview" className="nav-item">
              <i className="fas fa-chart-bar"></i> 监控
            </a>
          </nav>
          <button className="mobile-nav">
            <i className="fas fa-bars"></i>
          </button>
        </div>
        <div className="header-right">
          <div className="user-info">
            <div className="user-avatar">{userInfo.avatar}</div>
            <span>{userInfo.displayName}</span>
            <span className="user-role">{userInfo.roleText}</span>
          </div>
          <button className="logout-btn" onClick={handleLogout}>
            <i className="fas fa-sign-out-alt"></i> 退出
          </button>
        </div>
      </div>

      <div className="main-content">
        {/* 欢迎区域 */}
        <div className="welcome-section">
          <h1 className="welcome-title">欢迎回来！{userInfo.displayName}</h1>
          <p className="welcome-subtitle">今天是 {getCurrentDate()}，祝您工作愉快！</p>
        </div>

        {/* 统计卡片 */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-header">
              <div className="stat-icon blue">
                <i className="fas fa-database"></i>
              </div>
            </div>
            <div className="stat-value">
              {loading ? <div className="loading"></div> : stats.datasourceCount}
            </div>
            <div className="stat-label">数据源总数</div>
          </div>

          <div className="stat-card">
            <div className="stat-header">
              <div className="stat-icon green">
                <i className="fas fa-cogs"></i>
              </div>
            </div>
            <div className="stat-value">
              {loading ? <div className="loading"></div> : stats.apiServiceCount}
            </div>
            <div className="stat-label">API服务总数</div>
          </div>

          <div className="stat-card">
            <div className="stat-header">
              <div className="stat-icon orange">
                <i className="fas fa-play-circle"></i>
              </div>
            </div>
            <div className="stat-value">
              {loading ? <div className="loading"></div> : stats.activeServiceCount}
            </div>
            <div className="stat-label">运行中服务</div>
          </div>

          <div className="stat-card">
            <div className="stat-header">
              <div className="stat-icon purple">
                <i className="fas fa-chart-line"></i>
              </div>
            </div>
            <div className="stat-value">
              {loading ? <div className="loading"></div> : stats.requestCount}
            </div>
            <div className="stat-label">今日请求数</div>
          </div>
        </div>

        {/* 快速操作 */}
        <div className="quick-actions">
          <h2 className="section-title">
            <i className="fas fa-bolt"></i> 快速操作
          </h2>
          <div className="actions-grid">
            <a href="/datasource/create" className="action-btn">
              <div className="action-icon blue">
                <i className="fas fa-plus"></i>
              </div>
              <div>
                <div style={{ fontWeight: 600 }}>创建数据源</div>
                <div style={{ fontSize: 12, color: '#999' }}>连接新的数据库</div>
              </div>
            </a>

            <a href="/apiservice/create" className="action-btn">
              <div className="action-icon green">
                <i className="fas fa-code"></i>
              </div>
              <div>
                <div style={{ fontWeight: 600 }}>创建API服务</div>
                <div style={{ fontSize: 12, color: '#999' }}>快速生成API</div>
              </div>
            </a>

            <a href="/apiservice/testing" className="action-btn">
              <div className="action-icon orange">
                <i className="fas fa-flask"></i>
              </div>
              <div>
                <div style={{ fontWeight: 600 }}>API测试</div>
                <div style={{ fontSize: 12, color: '#999' }}>测试现有接口</div>
              </div>
            </a>

            <a href="http://localhost:8080/h2-console" target="_blank" className="action-btn">
              <div className="action-icon purple">
                <i className="fas fa-database"></i>
              </div>
              <div>
                <div style={{ fontWeight: 600 }}>数据库控制台</div>
                <div style={{ fontSize: 12, color: '#999' }}>管理数据库</div>
              </div>
            </a>
          </div>
        </div>

        {/* 最近活动 */}
        <div className="recent-activity">
          <h2 className="section-title">
            <i className="fas fa-history"></i> 最近活动
          </h2>
          <div className="activity-list">
            <div className="activity-item">
              <div className="activity-icon">
                <i className="fas fa-user"></i>
              </div>
              <div className="activity-content">
                <div className="activity-title">用户登录系统</div>
                <div className="activity-time">刚刚</div>
              </div>
            </div>
            <div className="activity-item">
              <div className="activity-icon" style={{ background: '#52c41a' }}>
                <i className="fas fa-server"></i>
              </div>
              <div className="activity-content">
                <div className="activity-title">系统服务启动完成</div>
                <div className="activity-time">几分钟前</div>
              </div>
            </div>
            <div className="activity-item">
              <div className="activity-icon" style={{ background: '#fa8c16' }}>
                <i className="fas fa-database"></i>
              </div>
              <div className="activity-content">
                <div className="activity-title">数据库初始化完成</div>
                <div className="activity-time">几分钟前</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Welcome;