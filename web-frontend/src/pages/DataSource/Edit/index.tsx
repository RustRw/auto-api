import React, { useState, useEffect } from 'react';
import { history, useParams } from '@umijs/max';
import './index.css';

const DataSourceEdit: React.FC = () => {
  const params = useParams();
  const [loading, setLoading] = useState(true);
  const [testing, setTesting] = useState(false);
  const [dataSource, setDataSource] = useState<any>(null);
  const [testResult, setTestResult] = useState<{
    connected: boolean;
    message: string;
    responseTime?: number;
  } | null>(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const dataSourceTypes = [
    { label: 'MySQL', value: 'MYSQL' },
    { label: 'PostgreSQL', value: 'POSTGRESQL' },
    { label: 'Oracle', value: 'ORACLE' },
    { label: 'H2 Database', value: 'H2' },
    { label: 'Elasticsearch', value: 'ELASTICSEARCH' },
  ];

  useEffect(() => {
    fetchDataSource();
  }, [params.id]);

  const fetchDataSource = async () => {
    if (!params.id) return;
    
    try {
      // 模拟获取数据源信息
      await new Promise(resolve => setTimeout(resolve, 500));
      const mockDataSource = {
        id: parseInt(params.id as string),
        name: 'MySQL主数据库',
        type: 'MYSQL',
        host: 'localhost',
        port: 3306,
        database: 'autoapi',
        username: 'root',
        enabled: true,
        description: '主要业务数据库',
        connectionUrl: 'jdbc:mysql://localhost:3306/autoapi',
        maxPoolSize: 10,
        connectionTimeout: 30000,
        sslEnabled: false,
        createdAt: '2024-01-15 10:30:00',
        updatedAt: '2024-01-27 09:15:00'
      };
      setDataSource(mockDataSource);
    } catch (error) {
      showAlert('获取数据源信息失败', 'error');
      history.goBack();
    } finally {
      setLoading(false);
    }
  };

  const handleTestConnection = async () => {
    if (!dataSource) return;
    
    setTesting(true);
    setTestResult(null);
    
    try {
      // 模拟测试连接
      await new Promise(resolve => setTimeout(resolve, 1000));
      const mockResult = {
        connected: true,
        message: '连接成功',
        responseTime: 125
      };
      setTestResult(mockResult);
      showAlert(`连接成功！响应时间: ${mockResult.responseTime}ms`, 'success');
    } catch (error) {
      showAlert('连接测试失败', 'error');
      setTestResult({
        connected: false,
        message: '网络错误或服务不可用',
      });
    } finally {
      setTesting(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!params.id) return;
    
    try {
      setLoading(true);
      const formData = new FormData(e.currentTarget);
      const values = {
        name: formData.get('name'),
        description: formData.get('description'),
        type: formData.get('type'),
        host: formData.get('host'),
        port: parseInt(formData.get('port') as string),
        database: formData.get('database'),
        username: formData.get('username'),
        password: formData.get('password'),
        connectionUrl: formData.get('connectionUrl'),
        maxPoolSize: parseInt(formData.get('maxPoolSize') as string || '10'),
        connectionTimeout: parseInt(formData.get('connectionTimeout') as string || '30000'),
        enabled: formData.get('enabled') === 'on',
        sslEnabled: formData.get('sslEnabled') === 'on'
      };
      
      // 模拟更新数据源
      await new Promise(resolve => setTimeout(resolve, 500));
      console.log('更新数据源:', parseInt(params.id as string), values);
      showAlert('数据源更新成功！', 'success');
      setTimeout(() => {
        history.push('/datasource/list');
      }, 1500);
    } catch (error) {
      showAlert('数据源更新失败', 'error');
    } finally {
      setLoading(false);
    }
  };

  const showAlert = (type: 'success' | 'error', message: string) => {
    if (type === 'success') {
      setSuccessMessage(message);
      setErrorMessage('');
    } else {
      setErrorMessage(message);
      setSuccessMessage('');
    }
    
    // 5秒后自动隐藏
    setTimeout(() => {
      setSuccessMessage('');
      setErrorMessage('');
    }, 5000);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    history.push('/user/login');
  };

  if (loading) {
    return (
      <div className="edit-page">
        <div className="loading-container">
          <div className="spinner"></div>
          <span>正在加载...</span>
        </div>
      </div>
    );
  }

  if (!dataSource) {
    return null;
  }

  return (
    <div className="edit-page">
      <div className="header">
        <div className="header-left">
          <a href="#" onClick={() => history.push('/welcome')} className="logo">
            <i className="fas fa-rocket"></i> Auto API Platform
          </a>
          <nav className="nav-menu">
            <a href="#" onClick={() => history.push('/welcome')} className="nav-item">
              <i className="fas fa-tachometer-alt"></i> 仪表板
            </a>
            <a href="#" onClick={() => history.push('/datasource/list')} className="nav-item active">
              <i className="fas fa-database"></i> 数据源
            </a>
            <a href="#" onClick={() => history.push('/apiservice/list')} className="nav-item">
              <i className="fas fa-cogs"></i> API服务
            </a>
          </nav>
        </div>
        <div className="header-right">
          <button className="logout-btn" onClick={logout}>
            <i className="fas fa-sign-out-alt"></i> 退出
          </button>
        </div>
      </div>

      <div className="container">
        <div className="form-card">
          <div className="form-header">
            <h1>编辑数据源</h1>
            <p>配置和管理数据源连接信息</p>
          </div>

          {successMessage && (
            <div className="alert alert-success" style={{ display: 'block' }}>
              {successMessage}
            </div>
          )}

          {errorMessage && (
            <div className="alert alert-error" style={{ display: 'block' }}>
              {errorMessage}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="name">数据源名称 *</label>
              <input
                type="text"
                id="name"
                name="name"
                required
                placeholder="请输入数据源名称"
                defaultValue={dataSource.name}
              />
            </div>

            <div className="form-group">
              <label htmlFor="description">描述</label>
              <textarea
                id="description"
                name="description"
                rows={3}
                placeholder="请输入数据源描述"
                defaultValue={dataSource.description}
              />
            </div>

            <div className="form-group">
              <label htmlFor="type">数据源类型 *</label>
              <select id="type" name="type" required disabled defaultValue={dataSource.type}>
                <option value="">请选择数据源类型</option>
                <option value="MYSQL">MySQL</option>
                <option value="POSTGRESQL">PostgreSQL</option>
                <option value="H2">H2 Database</option>
                <option value="ORACLE">Oracle</option>
                <option value="MONGODB">MongoDB</option>
                <option value="ELASTICSEARCH">Elasticsearch</option>
              </select>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="host">主机地址 *</label>
                <input
                  type="text"
                  id="host"
                  name="host"
                  required
                  placeholder="localhost"
                  defaultValue={dataSource.host}
                />
              </div>
              <div className="form-group">
                <label htmlFor="port">端口 *</label>
                <input
                  type="number"
                  id="port"
                  name="port"
                  required
                  placeholder="3306"
                  defaultValue={dataSource.port}
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="database">数据库名称</label>
              <input
                type="text"
                id="database"
                name="database"
                placeholder="请输入数据库名称"
                defaultValue={dataSource.database}
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="username">用户名</label>
                <input
                  type="text"
                  id="username"
                  name="username"
                  placeholder="请输入用户名"
                  defaultValue={dataSource.username}
                />
              </div>
              <div className="form-group">
                <label htmlFor="password">密码</label>
                <input
                  type="password"
                  id="password"
                  name="password"
                  placeholder="留空则不修改密码"
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="connectionUrl">连接URL</label>
              <input
                type="text"
                id="connectionUrl"
                name="connectionUrl"
                placeholder="jdbc:mysql://localhost:3306/database"
                defaultValue={dataSource.connectionUrl}
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="maxPoolSize">最大连接池大小</label>
                <input
                  type="number"
                  id="maxPoolSize"
                  name="maxPoolSize"
                  min="1"
                  max="100"
                  defaultValue={dataSource.maxPoolSize}
                />
              </div>
              <div className="form-group">
                <label htmlFor="connectionTimeout">连接超时时间(ms)</label>
                <input
                  type="number"
                  id="connectionTimeout"
                  name="connectionTimeout"
                  min="1000"
                  defaultValue={dataSource.connectionTimeout}
                />
              </div>
            </div>

            <div className="form-group">
              <div className="checkbox-group">
                <input
                  type="checkbox"
                  id="enabled"
                  name="enabled"
                  defaultChecked={dataSource.enabled}
                />
                <label htmlFor="enabled">启用此数据源</label>
              </div>
            </div>

            <div className="form-group">
              <div className="checkbox-group">
                <input
                  type="checkbox"
                  id="sslEnabled"
                  name="sslEnabled"
                  defaultChecked={dataSource.sslEnabled}
                />
                <label htmlFor="sslEnabled">启用SSL连接</label>
              </div>
            </div>

            <div className="test-connection-section">
              <h3>连接测试</h3>
              <div className="test-button-container">
                <button
                  type="button"
                  className={`btn btn-test ${testing ? 'loading' : ''}`}
                  onClick={handleTestConnection}
                  disabled={testing}
                >
                  {testing ? (
                    <>
                      <div className="spinner"></div>
                      <span>测试中...</span>
                    </>
                  ) : (
                    <>
                      <i className="fas fa-play-circle"></i>
                      <span>测试连接</span>
                    </>
                  )}
                </button>
              </div>
              
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

            <div className="datasource-info">
              <h3>数据源信息</h3>
              <div className="info-grid">
                <div className="info-item">
                  <strong>创建时间:</strong> {dataSource.createdAt}
                </div>
                <div className="info-item">
                  <strong>更新时间:</strong> {dataSource.updatedAt}
                </div>
                <div className="info-item">
                  <strong>数据源ID:</strong> {dataSource.id}
                </div>
              </div>
            </div>

            {loading && (
              <div className="loading">
                <div className="spinner"></div>
                正在处理...
              </div>
            )}

            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => history.push('/datasource/list')}
              >
                返回
              </button>
              <div>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  保存数据源
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default DataSourceEdit;