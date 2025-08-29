import React, { useState, useEffect } from 'react';
import { message } from 'antd';
import { history } from '@umijs/max';
import './index.css';

interface ApiService {
  id: number;
  name: string;
  path: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  status: 'DRAFT' | 'TESTING' | 'PUBLISHED' | 'DISABLED';
  enabled: boolean;
  cacheEnabled: boolean;
  datasourceId: number;
  datasourceName: string;
  description: string;
  createdAt: string;
  updatedAt?: string;
  sqlContent?: string;
  requestParams?: string;
  responseExample?: string;
  responseTime?: number;
  requestCount?: number;
  successRate?: number;
}

const ApiServiceList: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [services, setServices] = useState<ApiService[]>([]);
  const [searchParams, setSearchParams] = useState<any>({});
  const [serviceDetailVisible, setServiceDetailVisible] = useState(false);
  const [currentService, setCurrentService] = useState<ApiService | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [userInfo, setUserInfo] = useState<any>(null);

  useEffect(() => {
    // 检查登录状态
    const token = localStorage.getItem('token');
    if (!token) {
      history.push('/user/login');
      return;
    }
    
    // 获取用户信息
    const storedUserInfo = localStorage.getItem('userInfo');
    if (storedUserInfo) {
      setUserInfo(JSON.parse(storedUserInfo));
    }
    
    loadServices();
  }, [currentPage, pageSize, searchParams]);
  
  // 加载API服务列表
  const loadServices = async () => {
    try {
      setLoading(true);
      
      // 构建查询参数
      const params = new URLSearchParams({
        page: (currentPage - 1).toString(),
        size: pageSize.toString(),
        ...searchParams
      });

      const response = await fetch(`http://localhost:8080/api/services?${params}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      
      if (result.success) {
        const data = result.data;
        // 处理数据格式
        if (data.content) {
          // 分页对象格式
          setServices(data.content);
          setTotalCount(data.totalElements);
          setCurrentPage(data.number + 1);
          setTotalPages(data.totalPages);
        } else if (Array.isArray(data)) {
          // 简单数组格式
          setServices(data);
          setTotalCount(data.length);
          setTotalPages(Math.ceil(data.length / pageSize));
        } else {
          setServices([]);
        }
      } else {
        throw new Error(result.message || '获取API服务列表失败');
      }
    } catch (error: any) {
      console.error('Failed to load services:', error);
      message.error('加载API服务列表失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`确定要删除API服务 "${name}" 吗？此操作不可恢复！`)) {
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/api/services/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      const result = await response.json();
      
      if (result.success) {
        message.success('删除成功！');
        loadServices();
      } else {
        message.error('删除失败：' + result.message);
      }
    } catch (error: any) {
      message.error('删除失败：' + error.message);
    }
  };

  // 查看服务详情
  const handleViewService = async (id: number) => {
    try {
      const response = await fetch(`http://localhost:8080/api/services/${id}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setCurrentService(result.data);
          setServiceDetailVisible(true);
        } else {
          message.error('获取API服务信息失败: ' + result.message);
        }
      } else {
        message.error('获取API服务信息失败');
      }
    } catch (error: any) {
      message.error('获取API服务信息失败: ' + error.message);
    }
  };

  // 切换服务状态
  const toggleServiceStatus = async (id: number, currentStatus: string) => {
    const newStatus = currentStatus === 'PUBLISHED' ? 'DISABLED' : 'PUBLISHED';
    
    try {
      const response = await fetch(`http://localhost:8080/api/services/${id}/status?status=${newStatus}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      const result = await response.json();
      
      if (result.success) {
        message.success('状态更新成功！');
        loadServices();
      } else {
        message.error('状态更新失败：' + result.message);
      }
    } catch (error: any) {
      message.error('状态更新失败：' + error.message);
    }
  };

  const getStatusClass = (status: string) => {
    const classes: Record<string, string> = {
      'DRAFT': 'status-draft',
      'TESTING': 'status-inactive', 
      'PUBLISHED': 'status-active',
      'DISABLED': 'status-error',
    };
    return classes[status] || 'status-draft';
  };

  const getStatusText = (status: string) => {
    const textMap: Record<string, string> = {
      'DRAFT': '草稿',
      'TESTING': '测试中',
      'PUBLISHED': '已发布',
      'DISABLED': '已禁用',
    };
    return textMap[status] || '草稿';
  };

  const getMethodClass = (method: string) => {
    const classes: Record<string, string> = {
      'GET': 'method-get',
      'POST': 'method-post',
      'PUT': 'method-put',
      'DELETE': 'method-delete',
    };
    return classes[method] || 'method-get';
  };
  
  // 搜索API服务
  const searchServices = () => {
    const name = (document.getElementById('searchName') as HTMLInputElement)?.value.trim();
    const method = (document.getElementById('searchMethod') as HTMLSelectElement)?.value;
    const path = (document.getElementById('searchPath') as HTMLInputElement)?.value.trim();
    const status = (document.getElementById('searchStatus') as HTMLSelectElement)?.value;
    
    const newSearchParams: any = {};
    if (name) newSearchParams.name = name;
    if (method) newSearchParams.method = method;
    if (path) newSearchParams.path = path;
    if (status) newSearchParams.status = status;

    setSearchParams(newSearchParams);
    setCurrentPage(1);
  };

  // 重置搜索
  const resetSearch = () => {
    (document.getElementById('searchName') as HTMLInputElement).value = '';
    (document.getElementById('searchMethod') as HTMLSelectElement).value = '';
    (document.getElementById('searchPath') as HTMLInputElement).value = '';
    (document.getElementById('searchStatus') as HTMLSelectElement).value = '';
    
    setSearchParams({});
    setCurrentPage(1);
  };
  
  // 格式化日期
  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('zh-CN');
  };
  
  // 分页相关函数
  const goToPage = (page: number) => {
    if (page < 1 || page > totalPages || page === currentPage) return;
    setCurrentPage(page);
  };

  const changePageSize = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setPageSize(parseInt(e.target.value));
    setCurrentPage(1);
  };
  
  // 生成分页按钮
  const generatePaginationButtons = () => {
    const pages: (number | string)[] = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);
      
      if (currentPage > 3) {
        pages.push('...');
      }
      
      const start = Math.max(2, currentPage - 1);
      const end = Math.min(totalPages - 1, currentPage + 1);
      
      for (let i = start; i <= end; i++) {
        if (!pages.includes(i)) {
          pages.push(i);
        }
      }
      
      if (currentPage < totalPages - 2) {
        pages.push('...');
      }
      
      if (!pages.includes(totalPages)) {
        pages.push(totalPages);
      }
    }

    return pages;
  };
  
  // 退出登录
  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    history.push('/user/login');
  };



  return (
    <div>
      {/* Header */}
      <div className="header">
        <div className="header-left">
          <a onClick={() => history.push('/welcome')} className="logo">
            <i className="fas fa-rocket"></i> Auto API Platform
          </a>
          <nav className="nav-menu">
            <a onClick={() => history.push('/welcome')} className="nav-item">
              <i className="fas fa-tachometer-alt"></i> 仪表板
            </a>
            <a onClick={() => history.push('/datasource/list')} className="nav-item">
              <i className="fas fa-database"></i> 数据源
            </a>
            <a className="nav-item active">
              <i className="fas fa-cogs"></i> API服务
            </a>
            <a onClick={() => history.push('/apiservice/develop')} className="nav-item">
              <i className="fas fa-code"></i> 服务开发
            </a>
          </nav>
        </div>
        <div className="header-right">
          {userInfo && (
            <div className="user-info">
              <div className="user-avatar">{userInfo.username?.charAt(0) || 'U'}</div>
              <span>{userInfo.username || '用户'}</span>
            </div>
          )}
          <button className="logout-btn" onClick={logout}>
            <i className="fas fa-sign-out-alt"></i> 退出
          </button>
        </div>
      </div>

      <div className="container">
        <div className="page-header">
          <h1 className="page-title">API服务管理</h1>
          <button className="btn btn-primary" onClick={() => history.push('/apiservice/develop')}>
            <i className="fas fa-plus"></i> 新建API服务
          </button>
        </div>

        {/* 搜索筛选区域 */}
        <div className="search-filters">
          <div className="search-row">
            <div className="search-item">
              <label>服务名称</label>
              <input 
                type="text" 
                id="searchName" 
                placeholder="请输入服务名称"
                onKeyPress={(e) => e.key === 'Enter' && searchServices()}
              />
            </div>
            <div className="search-item">
              <label>请求方法</label>
              <select id="searchMethod">
                <option value="">全部方法</option>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
              </select>
            </div>
            <div className="search-item">
              <label>API路径</label>
              <input 
                type="text" 
                id="searchPath" 
                placeholder="请输入API路径"
                onKeyPress={(e) => e.key === 'Enter' && searchServices()}
              />
            </div>
            <div className="search-item">
              <label>状态</label>
              <select id="searchStatus">
                <option value="">全部状态</option>
                <option value="PUBLISHED">已发布</option>
                <option value="DRAFT">草稿</option>
                <option value="DISABLED">已禁用</option>
                <option value="TESTING">测试中</option>
              </select>
            </div>
            <div className="search-item" style={{alignSelf: 'flex-end'}}>
              <button className="btn btn-primary" onClick={searchServices}>
                <i className="fas fa-search"></i> 搜索
              </button>
              <button className="btn btn-secondary" onClick={resetSearch} style={{marginLeft: '8px'}}>
                <i className="fas fa-undo"></i> 重置
              </button>
            </div>
          </div>
        </div>

        {/* 表格区域 */}
        <div className="table-container">
          {loading ? (
            <div className="loading">
              <div className="spinner"></div>
              正在加载API服务...
            </div>
          ) : (
            <div>
              {services.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-cogs"></i>
                  <h3>暂无API服务</h3>
                  <p>还没有创建任何API服务，点击上方按钮创建第一个API服务</p>
                </div>
              ) : (
                <table className="table">
                  <thead>
                    <tr>
                      <th>服务名称</th>
                      <th>方法</th>
                      <th>API路径</th>
                      <th>数据源</th>
                      <th>状态</th>
                      <th>创建时间</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {services.map(service => (
                      <tr key={service.id}>
                        <td>
                          <div style={{fontWeight: 500, color: '#333', marginBottom: '4px'}}>
                            {service.name || 'N/A'}
                          </div>
                          <div style={{fontSize: '12px', color: '#999'}}>
                            {service.description || '无描述'}
                          </div>
                        </td>
                        <td>
                          <span className={`method-tag ${getMethodClass(service.method)}`}>
                            {service.method || 'GET'}
                          </span>
                        </td>
                        <td>
                          <code style={{background: '#f5f5f5', padding: '2px 6px', borderRadius: '3px', fontSize: '13px'}}>
                            {service.path || 'N/A'}
                          </code>
                        </td>
                        <td>{service.datasourceName || 'N/A'}</td>
                        <td>
                          <span className={`status-tag ${getStatusClass(service.status)}`}>
                            {getStatusText(service.status)}
                          </span>
                        </td>
                        <td>{formatDate(service.createdAt)}</td>
                        <td>
                          <div className="actions">
                            <button 
                              className="btn btn-secondary btn-sm" 
                              onClick={() => handleViewService(service.id)}
                            >
                              <i className="fas fa-eye"></i> 查看
                            </button>
                            <button 
                              className="btn btn-secondary btn-sm" 
                              onClick={() => history.push(`/apiservice/develop?id=${service.id}`)}
                            >
                              <i className="fas fa-edit"></i> 编辑
                            </button>
                            <button 
                              className="btn btn-secondary btn-sm" 
                              onClick={() => toggleServiceStatus(service.id, service.status)}
                            >
                              <i className={`fas fa-${service.status === 'PUBLISHED' ? 'pause' : 'play'}`}></i> 
                              {service.status === 'PUBLISHED' ? '停用' : '启用'}
                            </button>
                            <button 
                              className="btn btn-danger btn-sm" 
                              onClick={() => handleDelete(service.id, service.name)}
                            >
                              <i className="fas fa-trash"></i> 删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>

        {/* 分页区域 */}
        {totalPages > 1 && (
          <div className="pagination-container">
            <div className="pagination-info">
              显示第 {totalCount > 0 ? ((currentPage - 1) * pageSize + 1) : 0} - {Math.min(currentPage * pageSize, totalCount)} 条，共 {totalCount} 条数据
            </div>
            <div style={{display: 'flex', alignItems: 'center'}}>
              <div className="pagination">
                <button 
                  className="page-btn" 
                  onClick={() => goToPage(currentPage - 1)}
                  disabled={currentPage <= 1}
                >
                  <i className="fas fa-chevron-left"></i>
                </button>
                {generatePaginationButtons().map((page, index) => (
                  page === '...' ? (
                    <span key={index} className="page-btn" style={{border: 'none', cursor: 'default'}}>...</span>
                  ) : (
                    <button
                      key={index}
                      className={`page-btn ${page === currentPage ? 'active' : ''}`}
                      onClick={() => goToPage(page as number)}
                    >
                      {page}
                    </button>
                  )
                ))}
                <button 
                  className="page-btn" 
                  onClick={() => goToPage(currentPage + 1)}
                  disabled={currentPage >= totalPages}
                >
                  <i className="fas fa-chevron-right"></i>
                </button>
              </div>
              <select className="page-size-select" value={pageSize} onChange={changePageSize}>
                <option value={10}>10条/页</option>
                <option value={20}>20条/页</option>
                <option value={50}>50条/页</option>
                <option value={100}>100条/页</option>
              </select>
            </div>
          </div>
        )}
      </div>
      
      {/* 服务详情侧边面板 */}
      {serviceDetailVisible && (
        <>
          <div className="sidebar-overlay" onClick={() => setServiceDetailVisible(false)}></div>
          <div className="sidebar-panel">
            <div className="sidebar-header">
              <h2 className="sidebar-title">API服务详情</h2>
              <button className="sidebar-close" onClick={() => setServiceDetailVisible(false)}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            
            <div className="sidebar-content">
              {currentService && (
                <>
                  <div className="detail-section">
                    <h3 className="detail-title">基本信息</h3>
                    <div className="detail-item">
                      <label>服务名称：</label>
                      <span>{currentService.name || '-'}</span>
                    </div>
                    <div className="detail-item">
                      <label>描述：</label>
                      <span>{currentService.description || '无描述'}</span>
                    </div>
                    <div className="detail-item">
                      <label>请求方法：</label>
                      <span className={`method-tag ${getMethodClass(currentService.method)}`}>
                        {currentService.method || 'GET'}
                      </span>
                    </div>
                    <div className="detail-item">
                      <label>API路径：</label>
                      <code>{currentService.path || '-'}</code>
                    </div>
                    <div className="detail-item">
                      <label>数据源：</label>
                      <span>{currentService.datasourceName || '-'}</span>
                    </div>
                    <div className="detail-item">
                      <label>状态：</label>
                      <span className={`status-tag ${getStatusClass(currentService.status)}`}>
                        {getStatusText(currentService.status)}
                      </span>
                    </div>
                  </div>

                  {currentService.sqlContent && (
                    <div className="detail-section">
                      <h3 className="detail-title">SQL查询</h3>
                      <pre className="code-block">{currentService.sqlContent}</pre>
                    </div>
                  )}

                  {currentService.requestParams && (
                    <div className="detail-section">
                      <h3 className="detail-title">请求参数</h3>
                      <pre className="code-block">{currentService.requestParams}</pre>
                    </div>
                  )}

                  {currentService.responseExample && (
                    <div className="detail-section">
                      <h3 className="detail-title">响应示例</h3>
                      <pre className="code-block">{currentService.responseExample}</pre>
                    </div>
                  )}

                  <div className="detail-section">
                    <h3 className="detail-title">配置信息</h3>
                    <div className="detail-item">
                      <label>缓存状态：</label>
                      <span>{currentService.cacheEnabled ? '已启用' : '未启用'}</span>
                    </div>
                    <div className="detail-item">
                      <label>启用状态：</label>
                      <span>{currentService.enabled ? '已启用' : '已禁用'}</span>
                    </div>
                  </div>

                  <div className="detail-section">
                    <h3 className="detail-title">创建信息</h3>
                    <div className="detail-item">
                      <label>创建时间：</label>
                      <span>{formatDate(currentService.createdAt)}</span>
                    </div>
                    <div className="detail-item">
                      <label>更新时间：</label>
                      <span>{formatDate(currentService.updatedAt)}</span>
                    </div>
                  </div>
                </>
              )}
            </div>
            
            <div className="sidebar-footer">
              <button 
                className="btn btn-primary" 
                onClick={() => {
                  if (currentService) {
                    history.push(`/apiservice/develop?id=${currentService.id}`);
                  }
                }}
              >
                <i className="fas fa-edit"></i> 编辑服务
              </button>
              <button className="btn btn-secondary" onClick={() => setServiceDetailVisible(false)}>
                关闭
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default ApiServiceList;