import React, { useState, useEffect } from 'react';
import { history } from '@umijs/max';
import './index.css';

const DataSourceCreate: React.FC = () => {
  const [testing, setTesting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [testResult, setTestResult] = useState<{
    connected: boolean;
    message: string;
    responseTime?: number;
  } | null>(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const defaultPorts: Record<string, number> = {
    'MYSQL': 3306,
    'POSTGRESQL': 5432,
    'ORACLE': 1521,
    'H2': 9092,
    'ELASTICSEARCH': 9200,
    'MONGODB': 27017
  };

  useEffect(() => {
    // 初始化默认值
    const hostInput = document.getElementById('host') as HTMLInputElement;
    if (hostInput) {
      hostInput.value = 'localhost';
    }
  }, []);

  const handleTestConnection = async () => {
    const form = document.getElementById('datasourceForm') as HTMLFormElement;
    if (!form) return;

    const formData = new FormData(form);
    
    // 验证必填字段
    const requiredFields = ['name', 'type', 'host', 'port', 'username', 'password'];
    for (const field of requiredFields) {
      if (!formData.get(field)) {
        const fieldElement = form.querySelector(`[name="${field}"]`) as HTMLElement;
        const label = fieldElement?.previousElementSibling?.textContent?.replace(' *', '') || field;
        showAlert(`请填写${label}`, 'error');
        return;
      }
    }

    setTesting(true);
    setTestResult(null);
    hideAlert();
    
    try {
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000));
      const mockResult = {
        connected: true,
        message: '连接成功',
        responseTime: 89
      };
      setTestResult(mockResult);
      showAlert(`连接成功！响应时间: ${mockResult.responseTime}ms`, 'success');
    } catch (error) {
      const errorResult = {
        connected: false,
        message: '网络错误或服务不可用',
      };
      setTestResult(errorResult);
      showAlert('连接测试失败', 'error');
    } finally {
      setTesting(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    hideAlert();

    const formData = new FormData(e.currentTarget);
    const data = {
      name: formData.get('name'),
      description: formData.get('description'),
      type: formData.get('type'),
      host: formData.get('host'),
      port: parseInt(formData.get('port') as string),
      database: formData.get('database'),
      username: formData.get('username'),
      password: formData.get('password'),
      testQuery: formData.get('testQuery'),
      maxPoolSize: parseInt(formData.get('maxPoolSize') as string) || 10,
      connectionTimeout: parseInt(formData.get('connectionTimeout') as string) || 30,
      enabled: true
    };

    try {
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 500));
      console.log('创建数据源:', data);
      showAlert('数据源创建成功！即将跳转到数据源列表页面...', 'success');
      setTimeout(() => {
        history.push('/datasource/list');
      }, 2000);
    } catch (error) {
      showAlert('数据源创建失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const type = e.target.value;
    const portInput = document.getElementById('port') as HTMLInputElement;
    const testQueryInput = document.getElementById('testQuery') as HTMLInputElement;
    
    if (type && defaultPorts[type]) {
      portInput.value = defaultPorts[type].toString();
    }
    
    // 设置默认测试查询
    const defaultQueries: Record<string, string> = {
      'MYSQL': 'SELECT 1',
      'POSTGRESQL': 'SELECT 1',
      'ORACLE': 'SELECT 1 FROM DUAL',
      'H2': 'SELECT 1',
      'ELASTICSEARCH': '',
      'MONGODB': ''
    };
    
    if (type && defaultQueries[type] !== undefined) {
      testQueryInput.value = defaultQueries[type];
    }
  };

  const showAlert = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
    if (type === 'success') {
      setSuccessMessage(message);
      setErrorMessage('');
      setTimeout(() => {
        setSuccessMessage('');
      }, 5000);
    } else if (type === 'error') {
      setErrorMessage(message);
      setSuccessMessage('');
    } else {
      // info type handling if needed
    }
  };

  const hideAlert = () => {
    setSuccessMessage('');
    setErrorMessage('');
  };

  const logout = () => {
    if (window.confirm('确定要退出登录吗？')) {
      localStorage.clear();
      history.push('/user/login');
    }
  };

  return (
    <div className="create-page">
      <div className="header">
        <div className="header-left">
          <a href="#" onClick={() => history.push('/welcome')} className="logo">
            <i className="fas fa-rocket"></i> Auto API Platform
          </a>
          <div className="breadcrumb">
            <a href="#" onClick={() => history.push('/welcome')}>仪表盘</a>
            <i className="fas fa-chevron-right"></i>
            <a href="#" onClick={() => history.push('/datasource/list')}>数据源</a>
            <i className="fas fa-chevron-right"></i>
            <span>创建数据源</span>
          </div>
        </div>
        <div className="header-right">
          <div className="user-info">
            <div className="user-avatar">A</div>
            <span>管理员</span>
          </div>
          <button className="logout-btn" onClick={logout}>
            <i className="fas fa-sign-out-alt"></i> 退出
          </button>
        </div>
      </div>

      <div className="main-content">
        <div className="page-header">
          <h1 className="page-title">创建数据源</h1>
          <p className="page-subtitle">配置新的数据库连接</p>
        </div>

        <div className="form-container">
          <div className="form-header">
            <h2><i className="fas fa-database"></i> 数据源配置</h2>
          </div>
          
          <div className="form-body">
            {successMessage && (
              <div className="alert alert-success" style={{ display: 'block' }}>
                <i className="fas fa-check-circle"></i> {successMessage}
              </div>
            )}

            {errorMessage && (
              <div className="alert alert-error" style={{ display: 'block' }}>
                <i className="fas fa-exclamation-circle"></i> {errorMessage}
              </div>
            )}

            <form id="datasourceForm" onSubmit={handleSubmit}>
              <div className="form-section">
                <h3 className="section-title">基本信息</h3>
                
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label required" htmlFor="name">数据源名称</label>
                    <input
                      type="text"
                      id="name"
                      name="name"
                      className="form-control"
                      placeholder="请输入数据源名称"
                      required
                      maxLength={100}
                    />
                    <div className="form-help">用于识别此数据源的名称</div>
                  </div>
                  
                  <div className="form-group">
                    <label className="form-label required" htmlFor="type">数据库类型</label>
                    <select
                      id="type"
                      name="type"
                      className="form-control"
                      required
                      onChange={handleTypeChange}
                    >
                      <option value="">请选择数据库类型</option>
                      <option value="MYSQL">MySQL</option>
                      <option value="POSTGRESQL">PostgreSQL</option>
                      <option value="ORACLE">Oracle</option>
                      <option value="H2">H2 Database</option>
                      <option value="ELASTICSEARCH">Elasticsearch</option>
                      <option value="MONGODB">MongoDB</option>
                    </select>
                  </div>
                </div>
                
                <div className="form-group full-width">
                  <label className="form-label" htmlFor="description">描述</label>
                  <textarea
                    id="description"
                    name="description"
                    className="form-control"
                    placeholder="请输入数据源描述（可选）"
                    maxLength={500}
                  />
                  <div className="form-help">描述此数据源的用途和特点</div>
                </div>
              </div>

              <div className="form-section">
                <h3 className="section-title">连接配置</h3>
                
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label required" htmlFor="host">主机地址</label>
                    <input
                      type="text"
                      id="host"
                      name="host"
                      className="form-control"
                      placeholder="localhost"
                      required
                      maxLength={200}
                    />
                    <div className="form-help">数据库服务器的IP地址或域名</div>
                  </div>
                  
                  <div className="form-group">
                    <label className="form-label required" htmlFor="port">端口</label>
                    <input
                      type="number"
                      id="port"
                      name="port"
                      className="form-control"
                      placeholder="3306"
                      required
                      min="1"
                      max="65535"
                    />
                    <div className="form-help">数据库服务端口号</div>
                  </div>
                </div>
                
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label" htmlFor="database">数据库名</label>
                    <input
                      type="text"
                      id="database"
                      name="database"
                      className="form-control"
                      placeholder="请输入数据库名称"
                      maxLength={100}
                    />
                    <div className="form-help">要连接的数据库名称</div>
                  </div>
                  
                  <div className="form-group">
                    <label className="form-label required" htmlFor="username">用户名</label>
                    <input
                      type="text"
                      id="username"
                      name="username"
                      className="form-control"
                      placeholder="请输入用户名"
                      required
                      maxLength={100}
                    />
                  </div>
                </div>
                
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label required" htmlFor="password">密码</label>
                    <input
                      type="password"
                      id="password"
                      name="password"
                      className="form-control"
                      placeholder="请输入密码"
                      required
                      maxLength={500}
                    />
                  </div>
                  
                  <div className="form-group">
                    <label className="form-label" htmlFor="testQuery">测试查询</label>
                    <input
                      type="text"
                      id="testQuery"
                      name="testQuery"
                      className="form-control"
                      placeholder="SELECT 1"
                      maxLength={500}
                    />
                    <div className="form-help">用于测试连接的SQL语句</div>
                  </div>
                </div>
              </div>

              <div className="form-section">
                <h3 className="section-title">连接池配置</h3>
                
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label" htmlFor="maxPoolSize">最大连接数</label>
                    <input
                      type="number"
                      id="maxPoolSize"
                      name="maxPoolSize"
                      className="form-control"
                      placeholder="10"
                      min="1"
                      max="100"
                      defaultValue="10"
                    />
                    <div className="form-help">连接池最大连接数</div>
                  </div>
                  
                  <div className="form-group">
                    <label className="form-label" htmlFor="connectionTimeout">连接超时(秒)</label>
                    <input
                      type="number"
                      id="connectionTimeout"
                      name="connectionTimeout"
                      className="form-control"
                      placeholder="30"
                      min="1"
                      max="300"
                      defaultValue="30"
                    />
                    <div className="form-help">建立连接的超时时间</div>
                  </div>
                </div>
              </div>

              <div className="test-section">
                <h3><i className="fas fa-plug"></i> 连接测试</h3>
                <p>在保存数据源之前，建议先测试连接确保配置正确</p>
                <button
                  type="button"
                  className={`btn btn-success ${testing ? 'loading' : ''}`}
                  onClick={handleTestConnection}
                  disabled={testing}
                >
                  {testing ? (
                    <>
                      <span className="loading"></span>
                      测试中...
                    </>
                  ) : (
                    <>
                      <i className="fas fa-play"></i> 测试连接
                    </>
                  )}
                </button>
                
                {testResult && (
                  <div className={`test-result ${testResult.connected ? 'success' : 'error'}`}>
                    <div className="result-header">
                      <i className={`fas ${testResult.connected ? 'fa-check-circle' : 'fa-times-circle'}`}></i>
                      <span>{testResult.connected ? '连接成功' : '连接失败'}</span>
                    </div>
                    <div className="result-message">
                      {testResult.message}
                      {testResult.responseTime && (
                        <span> (响应时间: {testResult.responseTime}ms)</span>
                      )}
                    </div>
                  </div>
                )}
              </div>

              <div className="form-actions">
                <a
                  href="#"
                  className="btn btn-secondary"
                  onClick={(e) => {
                    e.preventDefault();
                    history.push('/datasource/list');
                  }}
                >
                  <i className="fas fa-times"></i> 取消
                </a>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? (
                    <>
                      <span className="loading"></span>
                      保存中...
                    </>
                  ) : (
                    <>
                      <i className="fas fa-save"></i> 保存数据源
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DataSourceCreate;