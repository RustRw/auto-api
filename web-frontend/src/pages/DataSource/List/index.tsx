import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { message } from 'antd';
import './index.css';

interface DataSource {
  id: number;
  name: string;
  type: string;
  host: string;
  port: number;
  database?: string;
  username?: string;
  password?: string;
  connectionUrl?: string;
  maxPoolSize?: number;
  connectionTimeout?: number;
  enabled: boolean;
  sslEnabled?: boolean;
  description?: string;
  createdAt: string;
  updatedAt?: string;
}

interface TableInfo {
  name: string;
  type?: string;
  comment?: string;
}

interface ColumnInfo {
  name: string;
  type: string;
  size?: number;
  nullable: boolean;
  primaryKey: boolean;
  autoIncrement: boolean;
  defaultValue?: string;
  comment?: string;
}

const DataSourceList: React.FC = () => {
  // API 基础配置
  const API_BASE_URL = 'http://localhost:8080';
  const getAuthHeaders = useCallback(() => {
    const token = localStorage.getItem('token');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  }, []);

  // 分页和搜索状态
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchParams, setSearchParams] = useState<any>({});
  
  // 使用 useMemo 来稳定 searchParams 对象引用
  const stableSearchParams = useMemo(() => searchParams, [JSON.stringify(searchParams)]);
  
  // 数据状态
  const [loading, setLoading] = useState(false);
  const [dataList, setDataList] = useState<DataSource[]>([]);
  
  // 侧边面板状态
  const [sidebarVisible, setSidebarVisible] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [currentEditingId, setCurrentEditingId] = useState<number | null>(null);
  
  // 数据查看状态
  const [dataViewVisible, setDataViewVisible] = useState(false);
  const [currentDataSource, setCurrentDataSource] = useState<DataSource | null>(null);
  const [databases, setDatabases] = useState<string[]>([]);
  const [expandedDatabases, setExpandedDatabases] = useState<string[]>([]);
  const [databaseSearch, setDatabaseSearch] = useState<string>('');
  const [databaseTables, setDatabaseTables] = useState<Record<string, TableInfo[]>>({});
  const [databasesLoading, setDatabasesLoading] = useState(false);
  
  // 模态框状态
  const [tableStructureVisible, setTableStructureVisible] = useState(false);
  const [sampleDataVisible, setSampleDataVisible] = useState(false);
  const [createStatementVisible, setCreateStatementVisible] = useState(false);
  const [currentTable, setCurrentTable] = useState<{ database: string; table: string } | null>(null);
  const [tableStructure, setTableStructure] = useState<ColumnInfo[]>([]);
  const [sampleData, setSampleData] = useState<any>(null);
  const [createStatement, setCreateStatement] = useState<string>('');
  
  // Refs
  const databaseSearchTimeout = useRef<NodeJS.Timeout | null>(null);
  const tableSearchTimeouts = useRef<Record<string, NodeJS.Timeout>>({});
  
  // 使用 useCallback 稳定数据库搜索处理函数
  const handleDatabaseSearch = useCallback((value: string) => {
    setDatabaseSearch(value);
    if (databaseSearchTimeout.current) {
      clearTimeout(databaseSearchTimeout.current);
    }
    databaseSearchTimeout.current = setTimeout(() => {
      if (currentDataSource) {
        loadDatabases(currentDataSource.id);
      }
    }, 500);
  }, [currentDataSource?.id, loadDatabases]);
  
  // 使用 useCallback 稳定刷新数据库列表函数
  const refreshDatabases = useCallback(() => {
    if (currentDataSource) {
      loadDatabases(currentDataSource.id);
    }
  }, [currentDataSource?.id, loadDatabases]);
  
  // 使用 useCallback 稳定数据库展开处理函数
  const toggleDatabaseExpansion = useCallback((database: string) => {
    console.log('🔧 toggleDatabaseExpansion clicked:', database);
    console.log('🔧 当前currentDataSource:', currentDataSource);
    setExpandedDatabases(prev => {
      console.log('🔧 当前expandedDatabases:', prev);
      if (prev.includes(database)) {
        console.log('🔧 折叠数据库:', database);
        return prev.filter(db => db !== database);
      } else {
        console.log('🔧 展开数据库，开始加载表列表:', database);
        // 加载表列表
        loadTables(database);
        const newExpanded = [...prev, database];
        console.log('🔧 新的expandedDatabases:', newExpanded);
        return newExpanded;
      }
    });
  }, [loadTables, currentDataSource]);
  
  // 使用 useCallback 稳定表搜索处理函数
  const handleTableSearch = useCallback((database: string) => {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      searchTables(database, e.target.value);
    };
  }, [searchTables]);
  
  // 使用 useCallback 稳定表操作处理函数
  const handleViewTableStructure = useCallback((database: string, table: string) => {
    // console.log('🔧 handleViewTableStructure 被点击:', { database, table });
    viewTableStructure(database, table);
  }, [viewTableStructure]);
  
  const handleViewCreateStatement = useCallback((database: string, table: string) => {
    // console.log('🔧 handleViewCreateStatement 被点击:', { database, table });
    viewCreateStatement(database, table);
  }, [viewCreateStatement]);
  
  const handleViewSampleData = useCallback((database: string, table: string) => {
    // console.log('🔧 handleViewSampleData 被点击:', { database, table });
    viewSampleData(database, table);
  }, [viewSampleData]);
  
  // 当前数据库上下文
  const [currentDatabaseContext, setCurrentDatabaseContext] = useState<string | null>(null);

  // 使用 useCallback 稳定 loadDataSources 函数
  const loadDataSources = useCallback(async () => {
    try {
      setLoading(true);
      
      const params = new URLSearchParams({
        page: (currentPage - 1).toString(),
        size: pageSize.toString(),
        ...stableSearchParams
      });

      const response = await fetch(`${API_BASE_URL}/api/datasources?${params}`, {
        method: 'GET',
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();

      if (result.success) {
        const data = result.data;
        if (Array.isArray(data)) {
          setDataList(data);
          setTotalCount(data.length);
          setTotalPages(Math.ceil(data.length / pageSize));
        } else if (data.content) {
          setDataList(data.content);
          setTotalCount(data.totalElements);
          setTotalPages(data.totalPages);
        }
      } else {
        throw new Error(result.message || '获取数据源列表失败');
      }
    } catch (error: any) {
      message.error('加载数据源列表失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, stableSearchParams, getAuthHeaders]);

  // 初始化
  useEffect(() => {
    // 检查登录状态
    const token = localStorage.getItem('token');
    if (!token) {
      window.location.href = '/user/login';
      return;
    }
    
    loadDataSources();
  }, [loadDataSources]);

  // 搜索数据源
  const handleSearch = () => {
    const newSearchParams: any = {};
    
    const searchName = (document.getElementById('searchName') as HTMLInputElement)?.value.trim();
    if (searchName) newSearchParams.name = searchName;
    
    const searchType = (document.getElementById('searchType') as HTMLSelectElement)?.value;
    if (searchType) newSearchParams.type = searchType;
    
    const searchHost = (document.getElementById('searchHost') as HTMLInputElement)?.value.trim();
    if (searchHost) newSearchParams.host = searchHost;
    
    const searchStatus = (document.getElementById('searchStatus') as HTMLSelectElement)?.value;
    if (searchStatus !== '') newSearchParams.enabled = searchStatus;

    setSearchParams(newSearchParams);
    setCurrentPage(1);
  };

  // 重置搜索
  const handleReset = () => {
    const searchName = document.getElementById('searchName') as HTMLInputElement;
    const searchType = document.getElementById('searchType') as HTMLSelectElement;
    const searchHost = document.getElementById('searchHost') as HTMLInputElement;
    const searchStatus = document.getElementById('searchStatus') as HTMLSelectElement;
    
    if (searchName) searchName.value = '';
    if (searchType) searchType.value = '';
    if (searchHost) searchHost.value = '';
    if (searchStatus) searchStatus.value = '';
    
    setSearchParams({});
    setCurrentPage(1);
  };

  // 测试连接
  const testConnection = async (id: number) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/datasources/${id}/test`, {
        method: 'POST',
        headers: getAuthHeaders()
      });

      console.log('Test connection response status:', response.status);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const text = await response.text();
      console.log('Response text:', text);
      
      if (!text) {
        throw new Error('Empty response from server');
      }
      
      const result = JSON.parse(text);
      
      if (result.success) {
        message.success('连接测试成功！');
      } else {
        message.error('连接测试失败：' + result.message);
      }
    } catch (error: any) {
      console.error('Test connection error:', error);
      if (error.message.includes('JSON')) {
        message.error('连接测试失败：服务器响应格式错误');
      } else {
        message.error('连接测试失败：' + error.message);
      }
    }
  };

  // 删除数据源
  const deleteDataSource = async (id: number, name: string) => {
    if (!window.confirm(`确定要删除数据源 "${name}" 吗？此操作不可恢复！`)) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/datasources/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });

      const result = await response.json();
      
      if (result.success) {
        message.success('删除成功！');
        loadDataSources();
      } else {
        message.error('删除失败：' + result.message);
      }
    } catch (error: any) {
      message.error('删除失败：' + error.message);
    }
  };

  // 分页跳转
  const goToPage = (page: number) => {
    if (page < 1 || page > totalPages || page === currentPage) return;
    setCurrentPage(page);
  };

  // 改变页面大小
  const changePageSize = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setPageSize(parseInt(e.target.value));
    setCurrentPage(1);
  };

  // 侧边面板管理
  const openCreateSidebar = () => {
    setIsEditMode(false);
    setCurrentEditingId(null);
    setSidebarVisible(true);
    clearForm();
  };

  const openEditSidebar = async (id: number) => {
    setIsEditMode(true);
    setCurrentEditingId(id);
    
    try {
      const response = await fetch(`${API_BASE_URL}/api/datasources/${id}`, {
        method: 'GET',
        headers: getAuthHeaders()
      });

      if (response.ok) {
        const result = await response.json();
        console.log('API Response:', result);
        if (result.success) {
          setSidebarVisible(true);
          // 延迟填充表单，确保DOM元素已经渲染
          setTimeout(() => populateForm(result.data), 100);
        } else {
          message.error('获取数据源信息失败: ' + result.message);
        }
      } else {
        message.error('获取数据源信息失败');
      }
    } catch (error: any) {
      message.error('获取数据源信息失败: ' + error.message);
    }
  };

  const closeSidebar = () => {
    setSidebarVisible(false);
    clearForm();
    setIsEditMode(false);
    setCurrentEditingId(null);
  };

  const clearForm = () => {
    const form = document.getElementById('datasource-form') as HTMLFormElement;
    if (form) {
      form.reset();
      // 安全设置默认值
      const enabledInput = document.getElementById('form-enabled') as HTMLInputElement;
      if (enabledInput) enabledInput.checked = true;
      
      const maxPoolSizeInput = document.getElementById('form-maxPoolSize') as HTMLInputElement;
      if (maxPoolSizeInput) maxPoolSizeInput.value = '10';
      
      const connectionTimeoutInput = document.getElementById('form-connectionTimeout') as HTMLInputElement;
      if (connectionTimeoutInput) connectionTimeoutInput.value = '30000';
    }
  };

  const populateForm = (datasource: DataSource) => {
    console.log('Populating form with data:', datasource);
    // 安全地设置表单值，检查元素是否存在
    const setElementValue = (id: string, value: string) => {
      const element = document.getElementById(id) as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;
      if (element) {
        element.value = value;
        console.log(`Set ${id} = ${value}`);
      } else {
        console.warn(`Element with id '${id}' not found`);
      }
    };

    const setCheckboxValue = (id: string, checked: boolean) => {
      const element = document.getElementById(id) as HTMLInputElement;
      if (element) {
        element.checked = checked;
        console.log(`Set ${id} = ${checked}`);
      } else {
        console.warn(`Checkbox element with id '${id}' not found`);
      }
    };

    setElementValue('datasource-id', datasource.id.toString());
    setElementValue('form-name', datasource.name || '');
    setElementValue('form-description', datasource.description || '');
    setElementValue('form-type', datasource.type || '');
    setElementValue('form-host', datasource.host || '');
    setElementValue('form-port', datasource.port?.toString() || '');
    setElementValue('form-database', datasource.database || '');
    setElementValue('form-username', datasource.username || '');
    setElementValue('form-password', '');
    setElementValue('form-connectionUrl', datasource.connectionUrl || '');
    setElementValue('form-maxPoolSize', (datasource.maxPoolSize || 10).toString());
    setElementValue('form-connectionTimeout', (datasource.connectionTimeout || 30000).toString());
    setCheckboxValue('form-enabled', datasource.enabled !== false);
    setCheckboxValue('form-sslEnabled', datasource.sslEnabled === true);
  };

  // 保存数据源
  const saveDatasource = async () => {
    const form = document.getElementById('datasource-form') as HTMLFormElement;
    const formData = new FormData(form);
    
    const requestData = {
      name: formData.get('name') as string,
      description: formData.get('description') as string,
      type: formData.get('type') as string,
      host: formData.get('host') as string,
      port: parseInt(formData.get('port') as string),
      database: formData.get('database') as string,
      username: formData.get('username') as string,
      password: formData.get('password') as string,
      connectionUrl: formData.get('connectionUrl') as string,
      maxPoolSize: parseInt((formData.get('maxPoolSize') as string) || '10'),
      connectionTimeout: parseInt((formData.get('connectionTimeout') as string) || '30000'),
      enabled: formData.get('enabled') === 'on',
      sslEnabled: formData.get('sslEnabled') === 'on'
    };

    if (!requestData.name || !requestData.type || !requestData.host || !requestData.port) {
      message.error('请填写必填字段（名称、类型、主机地址、端口）');
      return;
    }

    try {
      const saveBtn = document.getElementById('save-btn') as HTMLButtonElement;
      const originalText = saveBtn.innerHTML;
      saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 保存中...';
      saveBtn.disabled = true;

      let response;
      if (isEditMode && currentEditingId) {
        response = await fetch(`${API_BASE_URL}/api/datasources/${currentEditingId}`, {
          method: 'PUT',
          headers: getAuthHeaders(),
          body: JSON.stringify(requestData)
        });
      } else {
        response = await fetch(`${API_BASE_URL}/api/datasources`, {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(requestData)
        });
      }

      const result = await response.json();

      if (result.success) {
        message.success(isEditMode ? '数据源更新成功！' : '数据源创建成功！');
        closeSidebar();
        loadDataSources();
      } else {
        message.error((isEditMode ? '数据源更新失败: ' : '数据源创建失败: ') + result.message);
      }
    } catch (error: any) {
      message.error((isEditMode ? '数据源更新失败: ' : '数据源创建失败: ') + error.message);
    } finally {
      const saveBtn = document.getElementById('save-btn') as HTMLButtonElement;
      saveBtn.innerHTML = '<i class="fas fa-save"></i> 保存';
      saveBtn.disabled = false;
    }
  };

  // 测试数据源连接（侧边面板中的测试）
  const testDataSourceConnection = async () => {
    const form = document.getElementById('datasource-form') as HTMLFormElement;
    const formData = new FormData(form);
    
    const testData = {
      name: formData.get('name') as string,
      type: formData.get('type') as string,
      host: formData.get('host') as string,
      port: parseInt(formData.get('port') as string),
      database: formData.get('database') as string,
      username: formData.get('username') as string,
      password: formData.get('password') as string,
      connectionUrl: formData.get('connectionUrl') as string,
      sslEnabled: formData.get('sslEnabled') === 'on'
    };

    if (!testData.name || !testData.type || !testData.host || !testData.port) {
      message.error('请先填写基本连接信息（名称、类型、主机地址、端口）');
      return;
    }

    try {
      const testBtn = document.getElementById('test-btn') as HTMLButtonElement;
      const originalText = testBtn.innerHTML;
      testBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 测试中...';
      testBtn.disabled = true;

      console.log('Sending test data:', testData);
      
      const response = await fetch(`${API_BASE_URL}/api/datasources/test-config`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(testData)
      });

      console.log('Test config response status:', response.status);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const text = await response.text();
      console.log('Response text:', text);
      
      if (!text) {
        throw new Error('Empty response from server');
      }
      
      const result = JSON.parse(text);

      if (result.success) {
        message.success('连接测试成功！');
      } else {
        message.error('连接测试失败: ' + result.message);
      }
    } catch (error: any) {
      console.error('Test config error:', error);
      if (error.message.includes('JSON')) {
        message.error('连接测试失败：服务器响应格式错误');
      } else {
        message.error('连接测试失败: ' + error.message);
      }
    } finally {
      const testBtn = document.getElementById('test-btn') as HTMLButtonElement;
      testBtn.innerHTML = '<i class="fas fa-plug"></i> 测试连接';
      testBtn.disabled = false;
    }
  };

  // 数据查看功能
  const openDataViewSidebar = useCallback(async (id: number, name: string) => {
    // console.log('🔧 openDataViewSidebar 被调用:', { id, name });
    const dataSource = { id, name } as DataSource;
    setCurrentDataSource(dataSource);
    console.log('🔧 设置currentDataSource为:', dataSource);
    setDataViewVisible(true);
    // 使用 setTimeout 避免状态更新冲突
    setTimeout(() => {
      console.log('🔧 setTimeout中的loadDatabases开始执行, id:', id);
      loadDatabases(id);
    }, 0);
  }, [loadDatabases]);

  const closeDataViewSidebar = useCallback(() => {
    setDataViewVisible(false);
    setCurrentDataSource(null);
    setDatabases([]);
    setExpandedDatabases([]);
    setDatabaseTables({});
    setDatabaseSearch('');
    // 关闭所有模态框
    setTableStructureVisible(false);
    setSampleDataVisible(false);
    setCreateStatementVisible(false);
  }, []);

  // 使用 useCallback 稳定 loadDatabases 函数
  const loadDatabases = useCallback(async (dataSourceId: number) => {
    try {
      setDatabasesLoading(true);
      const params = databaseSearch ? new URLSearchParams({ search: databaseSearch }) : new URLSearchParams();
      const response = await fetch(`${API_BASE_URL}/api/datasources/${dataSourceId}/databases?${params}`, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      const result = await response.json();
      if (result.success) {
        setDatabases(result.data || []);
      } else {
        message.error('加载数据库列表失败: ' + result.message);
      }
    } catch (error: any) {
      message.error('加载数据库列表失败: ' + error.message);
    } finally {
      setDatabasesLoading(false);
    }
  }, [databaseSearch, getAuthHeaders]);
  
  // 使用 useCallback 稳定 loadTables 函数
  const loadTables = useCallback(async (database: string, search = '') => {
    if (!currentDataSource) {
      console.log('🔧 loadTables: 没有currentDataSource, 跳过');
      return;
    }
    
    // console.log('🔧 loadTables 开始:', { database, search, currentDataSourceId: currentDataSource.id });
    
    try {
      const params = search ? new URLSearchParams({ search }) : new URLSearchParams();
      const url = `${API_BASE_URL}/api/datasources/${currentDataSource.id}/databases/${encodeURIComponent(database)}/tables?${params}`;
      
      console.log('🔧 发送请求到:', url);
      
      const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      console.log('🔧 响应状态:', response.status);
      
      const result = await response.json();
      console.log('🔧 响应结果:', result);
      
      if (result.success) {
        // 过滤系统表
        const userTables = filterUserTables(result.data || []);
        console.log('🔧 过滤后的用户表:', userTables);
        setDatabaseTables(prev => {
          const newTables = { ...prev, [database]: userTables };
          console.log('🔧 更新databaseTables:', newTables);
          return newTables;
        });
      } else {
        console.log('🔧 loadTables 失败:', result.message);
        message.error(`加载表列表失败: ${result.message}`);
      }
    } catch (error: any) {
      console.log('🔧 loadTables 错误:', error);
      message.error(`加载表列表失败: ${error.message}`);
    }
  }, [currentDataSource?.id, getAuthHeaders]);
  
  // 过滤系统表，只显示用户表
  const filterUserTables = (tables: string[]): TableInfo[] => {
    if (!tables) return [];
    
    // H2数据库系统表和信息架构表（精确匹配）
    const systemTables = [
      'INFORMATION_SCHEMA_CATALOG_NAME', 'CONSTANTS', 'ENUM_VALUES', 'INDEXES',
      'INDEX_COLUMNS', 'IN_DOUBT', 'LOCKS', 'QUERY_STATISTICS', 'RIGHTS',
      'ROLES', 'SESSIONS', 'SESSION_STATE', 'SETTINGS', 'SYNONYMS',
      'CHECK_CONSTRAINTS', 'COLLATIONS', 'COLUMNS', 'COLUMN_PRIVILEGES',
      'CONSTRAINT_COLUMN_USAGE', 'DOMAINS', 'DOMAIN_CONSTRAINTS', 'ELEMENT_TYPES',
      'FIELDS', 'KEY_COLUMN_USAGE', 'PARAMETERS', 'REFERENTIAL_CONSTRAINTS',
      'ROUTINES', 'SCHEMATA', 'SEQUENCES', 'TABLES', 'TABLE_CONSTRAINTS',
      'TABLE_PRIVILEGES', 'TRIGGERS', 'VIEWS'
    ];
    
    // 去重并过滤系统表
    const uniqueTables = [...new Set(tables)];
    return uniqueTables
      .filter(table => {
        const isSystemTable = systemTables.includes(table.toUpperCase()) || 
                            table.toUpperCase().startsWith('INFORMATION_SCHEMA');
        return !isSystemTable;
      })
      .map(table => ({ name: table }));
  };
  
  // 使用 useCallback 稳定 searchTables 函数
  const searchTables = useCallback((database: string, searchValue: string) => {
    if (tableSearchTimeouts.current[database]) {
      clearTimeout(tableSearchTimeouts.current[database]);
    }
    tableSearchTimeouts.current[database] = setTimeout(() => {
      loadTables(database, searchValue);
    }, 500);
  }, [loadTables]);
  
  // 使用 useCallback 稳定 viewTableStructure 函数
  const viewTableStructure = useCallback(async (database: string, table: string) => {
    // console.log('🔧 viewTableStructure 开始执行:', { database, table, currentDataSource });
    
    // 获取当前数据源ID，使用实时状态或从DOM中获取
    let dataSourceId = currentDataSource?.id;
    if (!dataSourceId) {
      // 尝试从页面状态中获取数据源ID
      const tableElements = document.querySelectorAll('.table-item');
      if (tableElements.length > 0) {
        // 从URL或页面上下文推断数据源ID（这里假设是1，实际项目中需要更好的方案）
        dataSourceId = 1; // 临时解决方案
        console.log('🔧 从上下文推断dataSourceId:', dataSourceId);
      }
    }
    
    if (!dataSourceId) {
      console.log('🔧 viewTableStructure: 无法获取数据源ID, 跳过');
      message.error('无法获取数据源信息');
      return;
    }
    
    // 关闭其他模态框
    setSampleDataVisible(false);
    setCreateStatementVisible(false);
    
    setCurrentTable({ database, table });
    setTableStructureVisible(true);
    
    try {
      const url = `${API_BASE_URL}/api/datasources/${dataSourceId}/databases/${encodeURIComponent(database)}/tables/${encodeURIComponent(table)}/structure`;
      console.log('🔧 viewTableStructure 发送请求到:', url);
      
      const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      console.log('🔧 viewTableStructure 响应状态:', response.status);
      
      const result = await response.json();
      console.log('🔧 viewTableStructure 响应结果:', result);
      
      if (result.success) {
        // 验证和清理数据结构
        const structureData = result.data || [];
        console.log('🔧 原始表结构数据:', structureData);
        
        // 确保数据是数组格式，并且每个元素都是有效的ColumnInfo对象
        const cleanedStructure = Array.isArray(structureData) ? 
          structureData.map((col: any) => ({
            name: String(col.name || ''),
            type: String(col.type || ''),
            size: col.size ? Number(col.size) : undefined,
            nullable: Boolean(col.nullable),
            primaryKey: Boolean(col.primaryKey),
            autoIncrement: Boolean(col.autoIncrement),
            defaultValue: col.defaultValue ? String(col.defaultValue) : undefined,
            comment: col.comment ? String(col.comment) : undefined
          })) : [];
        
        console.log('🔧 清理后的表结构数据:', cleanedStructure);
        setTableStructure(cleanedStructure);
      } else {
        console.log('🔧 viewTableStructure 失败:', result.message);
        message.error('加载表结构失败: ' + result.message);
        setTableStructure([]);
      }
    } catch (error: any) {
      console.log('🔧 viewTableStructure 错误:', error);
      message.error('加载表结构失败: ' + error.message);
      setTableStructure([]);
    }
  }, [currentDataSource, getAuthHeaders]);
  
  // 使用 useCallback 稳定 viewSampleData 函数
  const viewSampleData = useCallback(async (database: string, table: string) => {
    console.log('viewSampleData 开始执行:', { database, table, currentDataSource });
    
    // 输入验证
    if (!database || !table) {
      message.error('数据库名称或表名称无效');
      return;
    }
    
    // 获取当前数据源ID
    let dataSourceId = currentDataSource?.id;
    if (!dataSourceId) {
      const tableElements = document.querySelectorAll('.table-item');
      if (tableElements.length > 0) {
        dataSourceId = 1; // 临时解决方案
      }
    }
    
    if (!dataSourceId) {
      message.error('无法获取数据源信息');
      return;
    }
    
    try {
      // 关闭其他模态框
      setTableStructureVisible(false);
      setCreateStatementVisible(false);
      
      // 先设置基本状态
      setCurrentTable({ database, table });
      setCurrentDatabaseContext(database);
      
      // 初始化样本数据为安全的空状态
      setSampleData({ 
        success: false, 
        data: [], 
        columns: [], 
        count: 0,
        errorMessage: '正在加载...'
      });
      
      // 显示模态框
      setSampleDataVisible(true);
      
      // 发送API请求
      const url = `${API_BASE_URL}/api/datasources/${dataSourceId}/databases/${encodeURIComponent(database)}/tables/${encodeURIComponent(table)}/sample-data?limit=20`;
      console.log('发送请求到:', url);
      
      const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      console.log('响应状态:', response.status);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const result = await response.json();
      console.log('API响应结果:', result);
      
      if (result.success && result.data) {
        // 数据清理和验证
        const rawData = result.data.data || [];
        const rawColumns = result.data.columns || [];
        
        // 提取列名（从对象数组中提取name属性）
        const columnNames = Array.isArray(rawColumns) ? 
          rawColumns.map(col => col && typeof col === 'object' ? String(col.name || '') : String(col || '')) : [];
        
        const cleanedData = {
          success: true,
          data: Array.isArray(rawData) ? rawData : [],
          columns: columnNames,
          count: result.data.count || 0,
          executionTime: result.data.executionTime || 0
        };
        
        console.log('原始API数据:', result.data);
        console.log('提取的列名:', columnNames);
        console.log('清理后的数据:', cleanedData);
        setSampleData(cleanedData);
      } else {
        setSampleData({
          success: false,
          data: [],
          columns: [],
          count: 0,
          errorMessage: result.message || '未知错误'
        });
        message.error('加载示例数据失败: ' + (result.message || '未知错误'));
      }
    } catch (error: any) {
      console.error('viewSampleData 错误:', error);
      setSampleData({
        success: false,
        data: [],
        columns: [],
        count: 0,
        errorMessage: error.message || '网络错误'
      });
      message.error('加载示例数据失败: ' + (error.message || '网络错误'));
    }
  }, [currentDataSource, getAuthHeaders]);
  
  // 使用 useCallback 稳定 viewCreateStatement 函数
  const viewCreateStatement = useCallback(async (database: string, table: string) => {
    // console.log('🔧 viewCreateStatement 开始执行:', { database, table, currentDataSource });
    
    // 获取当前数据源ID
    let dataSourceId = currentDataSource?.id;
    if (!dataSourceId) {
      const tableElements = document.querySelectorAll('.table-item');
      if (tableElements.length > 0) {
        dataSourceId = 1; // 临时解决方案
        console.log('🔧 从上下文推断dataSourceId:', dataSourceId);
      }
    }
    
    if (!dataSourceId) {
      console.log('🔧 viewCreateStatement: 无法获取数据源ID, 跳过');
      message.error('无法获取数据源信息');
      return;
    }
    
    // 关闭其他模态框
    setTableStructureVisible(false);
    setSampleDataVisible(false);
    
    setCurrentTable({ database, table });
    setCreateStatementVisible(true);
    
    try {
      // 首先尝试获取专门的创建语句API
      let url = `${API_BASE_URL}/api/datasources/${dataSourceId}/databases/${encodeURIComponent(database)}/tables/${encodeURIComponent(table)}/create-statement`;
      console.log('🔧 viewCreateStatement 发送请求到:', url);
      
      let response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      if (response.ok) {
        const result = await response.json();
        console.log('🔧 viewCreateStatement 创建语句API响应:', result);
        if (result.success && result.data) {
          setCreateStatement(result.data);
          return;
        }
      }
      
      // 如果专门API不存在或失败，使用表结构生成基本的创建语句
      url = `${API_BASE_URL}/api/datasources/${dataSourceId}/databases/${encodeURIComponent(database)}/tables/${encodeURIComponent(table)}/structure`;
      console.log('🔧 viewCreateStatement 使用结构API:', url);
      
      response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      const structureResult = await response.json();
      console.log('🔧 viewCreateStatement 结构API响应:', structureResult);
      
      if (structureResult.success && structureResult.data) {
        const generatedStatement = generateCreateStatement(table, structureResult.data);
        setCreateStatement(generatedStatement);
      } else {
        console.log('🔧 viewCreateStatement 获取结构失败:', structureResult.message);
        message.error('无法获取表结构信息: ' + structureResult.message);
        setCreateStatement('');
      }
    } catch (error: any) {
      console.log('🔧 viewCreateStatement 错误:', error);
      message.error('获取创建语句失败: ' + error.message);
      setCreateStatement('');
    }
  }, [currentDataSource, getAuthHeaders]);
  
  // 基于表结构生成基本的创建语句
  const generateCreateStatement = (tableName: string, columns: ColumnInfo[]): string => {
    if (!columns || columns.length === 0) {
      return `-- 无法获取表 ${tableName} 的结构信息`;
    }
    
    let sql = `CREATE TABLE ${tableName} (\n`;
    const columnDefs = columns.map(col => {
      let def = `    ${col.name} ${col.type}`;
      
      if (col.size && col.type.toUpperCase().includes('VARCHAR')) {
        def += `(${col.size})`;
      }
      
      if (!col.nullable) {
        def += ' NOT NULL';
      }
      
      if (col.autoIncrement) {
        def += ' AUTO_INCREMENT';
      }
      
      if (col.defaultValue) {
        def += ` DEFAULT '${col.defaultValue}'`;
      }
      
      if (col.comment) {
        def += ` COMMENT '${col.comment}'`;
      }
      
      return def;
    });
    
    sql += columnDefs.join(',\n');
    
    // 添加主键约束
    const primaryKeys = columns.filter(col => col.primaryKey);
    if (primaryKeys.length > 0) {
      sql += ',\n    PRIMARY KEY (' + primaryKeys.map(pk => pk.name).join(', ') + ')';
    }
    
    sql += '\n);';
    
    // 添加注释说明
    const disclaimer = `\n\n-- 注意: 此创建语句是基于表结构信息生成的简化版本\n-- 实际的表创建语句可能包含更多约束、索引和其他数据库特定的设置`;
    
    return sql + disclaimer;
  };
  
  // 使用 useCallback 稳定 refreshTableData 函数
  const refreshTableData = useCallback(() => {
    if (currentDataSource && currentDatabaseContext && currentTable) {
      viewSampleData(currentDatabaseContext, currentTable.table);
    }
  }, [currentDataSource, currentDatabaseContext, currentTable, viewSampleData]);
  
  // 导出表格数据
  const exportTableData = () => {
    message.info('数据导出功能开发中...');
  };
  
  // 复制创建语句
  const copyCreateStatement = () => {
    if (createStatement) {
      navigator.clipboard.writeText(createStatement).then(() => {
        message.success('创建语句已复制到剪贴板');
      }).catch(() => {
        // 降级处理
        const textArea = document.createElement('textarea');
        textArea.value = createStatement;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        message.success('创建语句已复制到剪贴板');
      });
    }
  };

  // 工具函数
  const generateTypeTag = (type: string) => {
    const typeMap: Record<string, string> = {
      MYSQL: '🐬 MySQL',
      POSTGRESQL: '🐘 PostgreSQL',
      H2: '💾 H2',
      ORACLE: '🏢 Oracle',
      MONGODB: '🍃 MongoDB',
      ELASTICSEARCH: '🔍 Elasticsearch',
      CLICKHOUSE: '📊 ClickHouse'
    };
    return `<span class="type-tag">${typeMap[type] || type}</span>`;
  };

  const getStatusClass = (enabled: boolean, type: string) => {
    return enabled ? 'status-enabled' : 'status-disabled';
  };

  const getStatusText = (enabled: boolean, type: string) => {
    return enabled ? '启用' : '禁用';
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN');
  };

  const getPageNumbers = () => {
    const pages = [];
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

  // 键盘事件处理
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.key === 'Enter' && (e.target as HTMLInputElement)?.id === 'searchName' || (e.target as HTMLInputElement)?.id === 'searchHost') {
        handleSearch();
      }
      if (e.key === 'Escape') {
        if (dataViewVisible) {
          closeDataViewSidebar();
        }
        if (sidebarVisible) {
          closeSidebar();
        }
        if (tableStructureVisible) {
          setTableStructureVisible(false);
        }
        if (sampleDataVisible) {
          setSampleDataVisible(false);
        }
        if (createStatementVisible) {
          setCreateStatementVisible(false);
        }
      }
    };

    document.addEventListener('keydown', handleKeyPress);
    return () => document.removeEventListener('keydown', handleKeyPress);
  }, [dataViewVisible, sidebarVisible, tableStructureVisible, sampleDataVisible, createStatementVisible]);

  return (
    <div>
      {/* 页面头部 */}
      <div className="header">
        <div className="header-left">
          <a href="/welcome" className="logo">
            <i className="fas fa-rocket"></i> Auto API Platform
          </a>
          <nav className="nav-menu">
            <a href="/welcome" className="nav-item">
              <i className="fas fa-tachometer-alt"></i> 仪表板
            </a>
            <a href="#" className="nav-item active">
              <i className="fas fa-database"></i> 数据源
            </a>
            <a href="/apiservice/list" className="nav-item">
              <i className="fas fa-cogs"></i> API服务
            </a>
            <a href="/apiservice/develop" className="nav-item">
              <i className="fas fa-code"></i> 服务开发
            </a>
          </nav>
        </div>
        <div className="header-right">
          <button className="logout-btn" onClick={() => {
            localStorage.removeItem('token');
            window.location.href = '/user/login';
          }}>
            <i className="fas fa-sign-out-alt"></i> 退出
          </button>
        </div>
      </div>

      <div className="container">
        <div className="page-header">
          <h1 className="page-title">数据源管理</h1>
          <button className="btn btn-primary" onClick={openCreateSidebar}>
            <i className="fas fa-plus"></i> 新建数据源
          </button>
        </div>

        {/* 搜索筛选区域 */}
        <div className="search-filters">
          <div className="search-row">
            <div className="search-item">
              <label>数据源名称</label>
              <input type="text" id="searchName" placeholder="请输入数据源名称" />
            </div>
            <div className="search-item">
              <label>数据源类型</label>
              <select id="searchType">
                <option value="">全部类型</option>
                <option value="MYSQL">🐬 MySQL</option>
                <option value="POSTGRESQL">🐘 PostgreSQL</option>
                <option value="H2">💾 H2</option>
                <option value="ORACLE">🏢 Oracle</option>
                <option value="MONGODB">🍃 MongoDB</option>
                <option value="ELASTICSEARCH">🔍 Elasticsearch</option>
                <option value="CLICKHOUSE">📊 ClickHouse</option>
              </select>
            </div>
            <div className="search-item">
              <label>主机地址</label>
              <input type="text" id="searchHost" placeholder="请输入主机地址" />
            </div>
            <div className="search-item">
              <label>状态</label>
              <select id="searchStatus">
                <option value="">全部状态</option>
                <option value="true">启用</option>
                <option value="false">禁用</option>
              </select>
            </div>
            <div className="search-item" style={{ alignSelf: 'flex-end' }}>
              <button className="btn btn-primary" onClick={handleSearch}>
                <i className="fas fa-search"></i> 搜索
              </button>
              <button className="btn btn-secondary" onClick={handleReset} style={{ marginLeft: '8px' }}>
                <i className="fas fa-undo"></i> 重置
              </button>
            </div>
          </div>
        </div>

        {/* 表格区域 */}
        <div className="table-container">
          {loading && (
            <div className="loading">
              <div className="spinner"></div>
              正在加载数据源...
            </div>
          )}

          {!loading && (
            <div id="table-content">
              <table className="table">
                <thead>
                  <tr>
                    <th>数据源名称</th>
                    <th>类型</th>
                    <th>主机地址</th>
                    <th>数据库</th>
                    <th>状态</th>
                    <th>创建时间</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {dataList.length === 0 ? (
                    <tr>
                      <td colSpan={7}>
                        <div className="empty-state">
                          <i className="fas fa-database"></i>
                          <h3>暂无数据源</h3>
                          <p>还没有配置任何数据源，点击上方按钮创建第一个数据源</p>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    dataList.map(datasource => (
                      <tr key={datasource.id}>
                        <td>
                          <div style={{ fontWeight: 500, color: '#333', marginBottom: 4 }}>{datasource.name}</div>
                          <div style={{ fontSize: 12, color: '#999' }}>{datasource.description || '无描述'}</div>
                        </td>
                        <td dangerouslySetInnerHTML={{ __html: generateTypeTag(datasource.type) }} />
                        <td>{datasource.host}:{datasource.port}</td>
                        <td>{datasource.database || 'N/A'}</td>
                        <td>
                          <span className={`status-tag ${getStatusClass(datasource.enabled, 'datasource')}`}>
                            {getStatusText(datasource.enabled, 'datasource')}
                          </span>
                        </td>
                        <td>{formatDate(datasource.createdAt)}</td>
                        <td>
                          <div className="actions">
                            <button className="btn btn-secondary btn-sm" onClick={() => openDataViewSidebar(datasource.id, datasource.name)}>
                              <i className="fas fa-database"></i> 查看数据
                            </button>
                            <button className="btn btn-secondary btn-sm" onClick={() => openEditSidebar(datasource.id)}>
                              <i className="fas fa-edit"></i> 编辑
                            </button>
                            <button className="btn btn-secondary btn-sm" onClick={() => testConnection(datasource.id)}>
                              <i className="fas fa-plug"></i> 测试
                            </button>
                            <button className="btn btn-danger btn-sm" onClick={() => deleteDataSource(datasource.id, datasource.name)}>
                              <i className="fas fa-trash"></i> 删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* 分页区域 */}
        {totalPages > 1 && (
          <div className="pagination-container">
            <div className="pagination-info">
              显示第 {totalCount > 0 ? (currentPage - 1) * pageSize + 1 : 0} - {Math.min(currentPage * pageSize, totalCount)} 条，共 {totalCount} 条数据
            </div>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <div className="pagination">
                <button className="page-btn" onClick={() => goToPage(currentPage - 1)} disabled={currentPage <= 1}>
                  <i className="fas fa-chevron-left"></i>
                </button>
                {getPageNumbers().map((page, index) => (
                  <span key={index}>
                    {page === '...' ? (
                      <span className="page-btn" style={{ border: 'none', cursor: 'default' }}>...</span>
                    ) : (
                      <button 
                        className={`page-btn ${page === currentPage ? 'active' : ''}`} 
                        onClick={() => goToPage(page as number)}
                      >
                        {page}
                      </button>
                    )}
                  </span>
                ))}
                <button className="page-btn" onClick={() => goToPage(currentPage + 1)} disabled={currentPage >= totalPages}>
                  <i className="fas fa-chevron-right"></i>
                </button>
              </div>
              <select className="page-size-select" value={pageSize} onChange={changePageSize}>
                <option value="10">10条/页</option>
                <option value="20">20条/页</option>
                <option value="50">50条/页</option>
                <option value="100">100条/页</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {/* 编辑侧边面板 */}
      {sidebarVisible && (
        <>
          <div className="sidebar-overlay" onClick={closeSidebar}></div>
          <div className="sidebar-panel">
            <div className="sidebar-header">
              <h2 className="sidebar-title">{isEditMode ? '编辑数据源' : '新建数据源'}</h2>
              <button className="sidebar-close" onClick={closeSidebar}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            
            <div className="sidebar-content">
              <form id="datasource-form">
                <input type="hidden" id="datasource-id" name="id" />
                
                <div className="form-group">
                  <label htmlFor="form-name">数据源名称 <span className="required">*</span></label>
                  <input type="text" id="form-name" name="name" required placeholder="请输入数据源名称" />
                </div>

                <div className="form-group">
                  <label htmlFor="form-description">描述</label>
                  <textarea id="form-description" name="description" placeholder="请输入数据源描述"></textarea>
                </div>

                <div className="form-group">
                  <label htmlFor="form-type">数据源类型 <span className="required">*</span></label>
                  <select id="form-type" name="type" required>
                    <option value="">请选择数据源类型</option>
                    <option value="MYSQL">🐬 MySQL</option>
                    <option value="POSTGRESQL">🐘 PostgreSQL</option>
                    <option value="H2">💾 H2</option>
                    <option value="ORACLE">🏢 Oracle</option>
                    <option value="MONGODB">🍃 MongoDB</option>
                    <option value="ELASTICSEARCH">🔍 Elasticsearch</option>
                    <option value="CLICKHOUSE">📊 ClickHouse</option>
                  </select>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="form-host">主机地址 <span className="required">*</span></label>
                    <input type="text" id="form-host" name="host" required placeholder="localhost" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="form-port">端口 <span className="required">*</span></label>
                    <input type="number" id="form-port" name="port" required placeholder="3306" />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="form-database">数据库名称</label>
                  <input type="text" id="form-database" name="database" placeholder="请输入数据库名称" />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="form-username">用户名</label>
                    <input type="text" id="form-username" name="username" placeholder="请输入用户名" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="form-password">密码</label>
                    <input type="password" id="form-password" name="password" placeholder="请输入密码" />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="form-connectionUrl">连接URL</label>
                  <input type="text" id="form-connectionUrl" name="connectionUrl" placeholder="jdbc:mysql://localhost:3306/database" />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="form-maxPoolSize">最大连接池大小</label>
                    <input type="number" id="form-maxPoolSize" name="maxPoolSize" defaultValue="10" min="1" max="100" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="form-connectionTimeout">连接超时时间(ms)</label>
                    <input type="number" id="form-connectionTimeout" name="connectionTimeout" defaultValue="30000" min="1000" />
                  </div>
                </div>

                <div className="form-group">
                  <div className="checkbox-group">
                    <input type="checkbox" id="form-enabled" name="enabled" defaultChecked />
                    <label htmlFor="form-enabled">启用此数据源</label>
                  </div>
                </div>

                <div className="form-group">
                  <div className="checkbox-group">
                    <input type="checkbox" id="form-sslEnabled" name="sslEnabled" />
                    <label htmlFor="form-sslEnabled">启用SSL连接</label>
                  </div>
                </div>
              </form>
            </div>
            
            <div className="sidebar-footer">
              <button type="button" className="btn btn-secondary" onClick={testDataSourceConnection} id="test-btn">
                <i className="fas fa-plug"></i> 测试连接
              </button>
              <button type="button" className="btn btn-secondary" onClick={closeSidebar}>
                取消
              </button>
              <button type="button" className="btn btn-primary" onClick={saveDatasource} id="save-btn">
                <i className="fas fa-save"></i> 保存
              </button>
            </div>
          </div>
        </>
      )}

      {/* 数据查看侧边面板 */}
      {dataViewVisible && (
        <>
          <div className="sidebar-overlay" onClick={closeDataViewSidebar}></div>
          <div className="sidebar-panel" style={{ width: '80%', maxWidth: '1200px' }}>
            <div className="sidebar-header">
              <h2 className="sidebar-title">{currentDataSource?.name} - 查看数据</h2>
              <button className="sidebar-close" onClick={closeDataViewSidebar}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            
            <div className="sidebar-content" style={{ padding: 0 }}>
              {/* 数据库搜索 */}
              <div style={{ padding: '20px', borderBottom: '1px solid #f0f0f0' }}>
                <div style={{ display: 'flex', gap: '15px', alignItems: 'center', marginBottom: '15px' }}>
                  <h3 style={{ margin: 0, color: '#333' }}>数据库列表</h3>
                  <div style={{ flex: 1, maxWidth: '300px' }}>
                    <input 
                      type="text" 
                      placeholder="搜索数据库..." 
                      value={databaseSearch}
                      onChange={(e) => handleDatabaseSearch(e.target.value)}
                      style={{ width: '100%', padding: '8px 12px', border: '1px solid #d9d9d9', borderRadius: '4px' }}
                    />
                  </div>
                  <button className="btn btn-secondary btn-sm" onClick={refreshDatabases}>
                    <i className="fas fa-refresh"></i> 刷新
                  </button>
                </div>
              </div>
              
              {/* 数据库列表 */}
              <div style={{ flex: 1, overflowY: 'auto' }}>
                {databasesLoading && (
                  <div className="loading" style={{ display: 'block' }}>
                    <div className="spinner"></div>
                    正在加载数据库列表...
                  </div>
                )}
                
                {!databasesLoading && databases.length === 0 && (
                  <div style={{ padding: '40px', textAlign: 'center', color: '#999' }}>暂无数据库</div>
                )}
                
                {!databasesLoading && databases.length > 0 && databases.map(database => {
                  const isExpanded = expandedDatabases.includes(database);
                  const tablesForDb = databaseTables[database];
                  // console.log('🔧 渲染数据库:', { database, isExpanded, tablesForDb, expandedDatabases });
                  
                  return (
                    <div key={database} className={`database-item ${isExpanded ? 'expanded' : ''}`}>
                      <div className="database-header" onClick={() => toggleDatabaseExpansion(database)}>
                        <span className="database-name">
                          <i className="fas fa-database" style={{ marginRight: 8 }}></i>
                          {database}
                        </span>
                        <i className="fas fa-chevron-right expand-icon"></i>
                      </div>
                      <div className="tables-container">
                        <div className="table-search" style={{ position: 'relative' }}>
                          <input 
                            type="text" 
                            placeholder="搜索表..." 
                            onChange={handleTableSearch(database)}
                            style={{ paddingLeft: '32px' }}
                          />
                          <i className="fas fa-search" style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#999', pointerEvents: 'none' }}></i>
                        </div>
                        <div className="tables-list">
                          {!tablesForDb ? (
                            <div style={{ padding: '20px', textAlign: 'center', color: '#999' }}>
                              点击展开加载表列表
                            </div>
                          ) : tablesForDb.length === 0 ? (
                            <div style={{ padding: '20px', textAlign: 'center', color: '#999' }}>
                              暂无用户数据表
                            </div>
                          ) : (
                            Array.isArray(tablesForDb) ? tablesForDb.map((table, index) => {
                              // 确保table是有效对象并且有name属性
                              if (!table || typeof table !== 'object' || !table.name) {
                                console.warn('无效的表数据项:', table);
                                return null;
                              }
                              
                              const tableName = String(table.name);
                              return (
                                <div key={`${database}-${tableName}-${index}`} className="table-item">
                                  <span className="table-name">
                                    <i className="fas fa-table" style={{ marginRight: 6 }}></i>
                                    {tableName}
                                  </span>
                                  <div className="table-actions">
                                    <button 
                                      className="btn btn-secondary btn-sm" 
                                      onClick={() => handleViewTableStructure(database, tableName)}
                                      title="查看表结构定义"
                                    >
                                      <i className="fas fa-columns"></i> 表结构
                                    </button>
                                    <button 
                                      className="btn btn-secondary btn-sm" 
                                      onClick={() => handleViewCreateStatement(database, tableName)}
                                      title="查看表的创建语句"
                                    >
                                      <i className="fas fa-code"></i> 创建语句
                                    </button>
                                    <button 
                                      className="btn btn-primary btn-sm" 
                                      onClick={() => handleViewSampleData(database, tableName)}
                                      title="查看表格数据"
                                    >
                                      <i className="fas fa-table"></i> 查看数据
                                    </button>
                                  </div>
                                </div>
                              );
                            }) : null
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </>
      )}

      {/* 表结构查看模态框 */}
      {tableStructureVisible && (
        <div className="modal-overlay" onClick={() => setTableStructureVisible(false)}>
          <div className="modal-content" style={{ width: '90%', maxWidth: '800px', maxHeight: '80vh' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{currentTable?.table} - 表结构</h3>
              <button className="modal-close" onClick={() => setTableStructureVisible(false)}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body" style={{ overflowY: 'auto' }}>
              {!Array.isArray(tableStructure) || tableStructure.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>暂无结构信息</div>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>字段名</th>
                      <th>类型</th>
                      <th>大小</th>
                      <th>可空</th>
                      <th>主键</th>
                      <th>自增</th>
                      <th>默认值</th>
                      <th>注释</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Array.isArray(tableStructure) && tableStructure.map((column, index) => {
                      // 确保column是有效对象
                      if (!column || typeof column !== 'object') {
                        console.warn('无效的表结构数据项:', column);
                        return null;
                      }
                      
                      return (
                        <tr key={index}>
                          <td><strong>{String(column.name || '')}</strong></td>
                          <td>{String(column.type || '')}</td>
                          <td>{column.size ? String(column.size) : '-'}</td>
                          <td>{column.nullable ? '是' : '否'}</td>
                          <td>{column.primaryKey ? '是' : '否'}</td>
                          <td>{column.autoIncrement ? '是' : '否'}</td>
                          <td>{column.defaultValue ? String(column.defaultValue) : '-'}</td>
                          <td>{column.comment ? String(column.comment) : '-'}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 示例数据查看模态框 */}
      {sampleDataVisible && (
        <div className="modal-overlay" onClick={() => setSampleDataVisible(false)}>
          <div className="modal-content" style={{ width: '95%', maxWidth: '1200px', maxHeight: '90vh' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{currentTable?.table} - 表格数据</h3>
              <button className="modal-close" onClick={() => setSampleDataVisible(false)}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body" style={{ overflow: 'auto' }}>
              {!sampleData ? (
                <div style={{ color: '#f56565', textAlign: 'center', padding: '20px' }}>加载示例数据失败</div>
              ) : !sampleData.success ? (
                <div style={{ color: '#f56565', textAlign: 'center', padding: '20px' }}>{sampleData.errorMessage}</div>
              ) : !sampleData.data || sampleData.data.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
                  <i className="fas fa-table" style={{ fontSize: '48px', marginBottom: '16px', color: '#d9d9d9' }}></i>
                  <h3 style={{ marginBottom: '8px' }}>表中暂无数据</h3>
                  <p>这个表目前没有任何数据记录</p>
                </div>
              ) : (
                <div>
                  <div style={{ marginBottom: '15px', padding: '12px', background: '#f8f9fa', borderRadius: '6px', borderLeft: '4px solid #1890ff' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
                      <div>
                        <strong><i className="fas fa-chart-bar"></i> 数据统计:</strong> 
                        共 <span style={{ color: '#1890ff', fontWeight: 'bold' }}>{sampleData.count || sampleData.data.length}</span> 条记录
                        {sampleData.executionTime && ` | 查询耗时: ${sampleData.executionTime}ms`}
                      </div>
                      <div>
                        <button className="btn btn-secondary btn-sm" onClick={exportTableData} title="导出数据">
                          <i className="fas fa-download"></i> 导出
                        </button>
                        <button className="btn btn-secondary btn-sm" onClick={refreshTableData} title="刷新数据" style={{ marginLeft: '8px' }}>
                          <i className="fas fa-sync-alt"></i> 刷新
                        </button>
                      </div>
                    </div>
                  </div>
                  
                  <div style={{ border: '1px solid #e8e8e8', borderRadius: '6px', overflow: 'hidden' }}>
                    <div style={{ overflowX: 'auto', maxHeight: '400px' }}>
                      <table className="data-table" style={{ margin: 0 }}>
                        <thead style={{ position: 'sticky', top: 0, background: '#fafafa', zIndex: 1 }}>
                          <tr>
                            <th style={{ minWidth: '50px', textAlign: 'center', background: '#f0f0f0' }}>#</th>
                            {Array.isArray(sampleData.columns) && sampleData.columns.map((column: string, index: number) => {
                              const columnName = String(column || '');
                              return (
                                <th key={`header-${index}`} 
                                    style={{ minWidth: '120px', maxWidth: '200px', wordBreak: 'break-all' }} 
                                    title={columnName}>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                    <i className="fas fa-columns" style={{ fontSize: '10px', opacity: 0.6 }}></i>
                                    <span>{columnName}</span>
                                  </div>
                                </th>
                              );
                            })}
                          </tr>
                        </thead>
                        <tbody>
                          {Array.isArray(sampleData.data) && sampleData.data.slice(0, 100).map((row: any, rowIndex: number) => {
                            // 确保row是有效对象
                            if (!row || typeof row !== 'object') {
                              console.warn('无效的数据行:', row);
                              return null;
                            }
                            
                            return (
                              <tr key={`row-${rowIndex}`} style={{ background: rowIndex % 2 === 0 ? '#fafafa' : 'white' }}>
                                <td style={{ textAlign: 'center', color: '#666', fontWeight: 500, background: '#f8f8f8' }}>{rowIndex + 1}</td>
                                {Array.isArray(sampleData.columns) && sampleData.columns.map((column: string, colIndex: number) => {
                                  try {
                                    const columnName = String(column || '');
                                    const value = row[columnName];
                                    let displayValue = '';
                                    let titleValue = '';
                                    
                                    if (value === null || value === undefined) {
                                      displayValue = 'NULL';
                                      titleValue = 'NULL';
                                    } else if (typeof value === 'object') {
                                      // 如果是对象，安全转换为JSON字符串
                                      try {
                                        const jsonString = JSON.stringify(value);
                                        displayValue = jsonString.length > 50 ? 
                                          jsonString.substring(0, 50) + '...' : jsonString;
                                        titleValue = jsonString;
                                      } catch (jsonError) {
                                        displayValue = '[Object]';
                                        titleValue = '[Object]';
                                      }
                                    } else {
                                      // 确保转换为字符串
                                      try {
                                        const stringValue = String(value);
                                        displayValue = stringValue.length > 50 ? 
                                          stringValue.substring(0, 50) + '...' : stringValue;
                                        titleValue = stringValue;
                                      } catch (stringError) {
                                        displayValue = '[Error]';
                                        titleValue = '[Error]';
                                      }
                                    }
                                    
                                    return (
                                      <td key={`cell-${rowIndex}-${colIndex}`} 
                                          style={{ maxWidth: '200px', wordBreak: 'break-all' }} 
                                          title={titleValue}>
                                        {value !== null && value !== undefined ? 
                                          <span>{displayValue}</span> : 
                                          <em style={{ color: '#999', fontStyle: 'italic' }}>NULL</em>
                                        }
                                      </td>
                                    );
                                  } catch (cellError) {
                                    console.error('单元格渲染错误:', cellError);
                                    return (
                                      <td key={`error-${rowIndex}-${colIndex}`} 
                                          style={{ maxWidth: '200px', color: '#f56565' }}>
                                        [渲染错误]
                                      </td>
                                    );
                                  }
                                })}
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                    {sampleData.data.length > 100 && (
                      <div style={{ padding: '10px', background: '#f8f9fa', borderTop: '1px solid #e8e8e8', textAlign: 'center', color: '#666' }}>
                        <i className="fas fa-info-circle"></i> 
                        显示前 100 条记录，共 {sampleData.data.length} 条数据
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 创建语句查看模态框 */}
      {createStatementVisible && (
        <div className="modal-overlay" onClick={() => setCreateStatementVisible(false)}>
          <div className="modal-content" style={{ width: '90%', maxWidth: '900px', maxHeight: '80vh' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{currentTable?.table} - 创建语句</h3>
              <button className="modal-close" onClick={() => setCreateStatementVisible(false)}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body" style={{ overflowY: 'auto' }}>
              {!createStatement ? (
                <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>暂无创建语句</div>
              ) : (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                    <strong>DDL创建语句：</strong>
                    <button className="btn btn-secondary btn-sm" onClick={copyCreateStatement} title="复制到剪贴板">
                      <i className="fas fa-copy"></i> 复制
                    </button>
                  </div>
                  <pre style={{
                    background: '#f8f9fa',
                    border: '1px solid #e9ecef',
                    borderRadius: '6px',
                    padding: '15px',
                    margin: 0,
                    fontFamily: "'Monaco', 'Menlo', 'Consolas', monospace",
                    fontSize: '13px',
                    lineHeight: '1.5',
                    color: '#333',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                    maxHeight: '400px',
                    overflowY: 'auto'
                  }}>
                    {createStatement}
                  </pre>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DataSourceList;