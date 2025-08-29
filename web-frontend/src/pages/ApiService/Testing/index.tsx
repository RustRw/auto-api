import React, { useState, useEffect } from 'react';
import { 
  PageContainer, 
  ProCard 
} from '@ant-design/pro-components';
import { 
  Button, 
  message, 
  Space, 
  Select, 
  Row, 
  Col, 
  Tabs, 
  Table, 
  Tag,
  Typography,
  Input,
  Divider,
  Card,
  Form,
  Tooltip,
  Alert
} from 'antd';
import { 
  PlayCircleOutlined, 
  CheckCircleOutlined, 
  CloseCircleOutlined,
  EyeOutlined,
  DatabaseOutlined,
  CodeOutlined,
  ThunderboltOutlined,
  ApiOutlined,
  BugOutlined,
  HistoryOutlined,
  SaveOutlined,
  SettingOutlined
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { TabPane } = Tabs;

interface ApiService {
  id: number;
  name: string;
  path: string;
  method: string;
  status: string;
  datasourceName: string;
  description: string;
}

interface TestHistory {
  id: number;
  timestamp: string;
  method: string;
  path: string;
  statusCode: number;
  responseTime: number;
  success: boolean;
}

const ApiTesting: React.FC = () => {
  const [currentService, setCurrentService] = useState<ApiService | null>(null);
  const [requestMethod, setRequestMethod] = useState('GET');
  const [requestUrl, setRequestUrl] = useState('/api/users');
  const [requestHeaders, setRequestHeaders] = useState('{\n  "Content-Type": "application/json",\n  "Authorization": "Bearer your-token"\n}');
  const [requestBody, setRequestBody] = useState('{}');
  const [queryParams, setQueryParams] = useState('{}');
  const [loading, setLoading] = useState(false);
  const [testResult, setTestResult] = useState<any>(null);
  const [testHistory, setTestHistory] = useState<TestHistory[]>([]);
  const [activeTab, setActiveTab] = useState('request');
  
  // 模拟API服务数据
  const mockService: ApiService = {
    id: 1,
    name: '用户查询服务',
    path: '/api/users',
    method: 'GET',
    status: 'PUBLISHED',
    datasourceName: 'MySQL主数据库',
    description: '查询用户信息列表'
  };

  // 模拟测试历史
  const mockHistory: TestHistory[] = [
    {
      id: 1,
      timestamp: '2024-01-27 14:30:15',
      method: 'GET',
      path: '/api/users',
      statusCode: 200,
      responseTime: 125,
      success: true
    },
    {
      id: 2,
      timestamp: '2024-01-27 14:28:42',
      method: 'POST',
      path: '/api/users',
      statusCode: 201,
      responseTime: 89,
      success: true
    },
    {
      id: 3,
      timestamp: '2024-01-27 14:25:33',
      method: 'GET',
      path: '/api/users/123',
      statusCode: 404,
      responseTime: 45,
      success: false
    }
  ];

  useEffect(() => {
    setCurrentService(mockService);
    setTestHistory(mockHistory);
    setRequestMethod(mockService.method);
    setRequestUrl(mockService.path);
  }, []);

  // 执行测试
  const handleTest = async () => {
    setLoading(true);
    try {
      // 验证JSON格式
      let parsedHeaders = {};
      let parsedBody = {};
      let parsedParams = {};
      
      try {
        parsedHeaders = JSON.parse(requestHeaders);
        parsedBody = JSON.parse(requestBody);
        parsedParams = JSON.parse(queryParams);
      } catch (e) {
        message.error('JSON格式错误，请检查请求参数');
        setLoading(false);
        return;
      }

      // 模拟API请求
      await new Promise(resolve => setTimeout(resolve, Math.random() * 1000 + 500));
      
      const success = Math.random() > 0.2; // 80%成功率
      const statusCode = success ? (requestMethod === 'POST' ? 201 : 200) : (Math.random() > 0.5 ? 404 : 500);
      const responseTime = Math.floor(Math.random() * 500 + 50);
      
      const mockResult = {
        success,
        statusCode,
        responseTime,
        timestamp: new Date().toISOString(),
        request: {
          method: requestMethod,
          url: requestUrl,
          headers: parsedHeaders,
          body: parsedBody,
          params: parsedParams
        },
        response: success ? {
          data: requestMethod === 'GET' ? [
            { id: 1, name: '张三', email: 'zhangsan@example.com', age: 25, created_at: '2024-01-15 10:30:00' },
            { id: 2, name: '李四', email: 'lisi@example.com', age: 30, created_at: '2024-01-16 11:20:00' },
            { id: 3, name: '王五', email: 'wangwu@example.com', age: 28, created_at: '2024-01-17 14:15:00' }
          ] : { id: Math.floor(Math.random() * 1000), message: '操作成功' },
          headers: {
            'Content-Type': 'application/json',
            'X-Request-ID': `req-${Date.now()}`,
            'Date': new Date().toISOString()
          }
        } : {
          error: statusCode === 404 ? '资源不存在' : '服务器内部错误',
          message: statusCode === 404 ? 'User not found' : 'Internal server error',
          code: statusCode
        }
      };

      setTestResult(mockResult);
      setActiveTab('response');
      
      // 添加到历史记录
      const historyItem: TestHistory = {
        id: Date.now(),
        timestamp: new Date().toLocaleString(),
        method: requestMethod,
        path: requestUrl,
        statusCode,
        responseTime,
        success
      };
      setTestHistory([historyItem, ...testHistory]);

      if (success) {
        message.success(`测试成功！响应时间: ${responseTime}ms`);
      } else {
        message.error(`测试失败！状态码: ${statusCode}`);
      }
    } catch (error) {
      message.error('测试请求失败');
    } finally {
      setLoading(false);
    }
  };

  // 保存测试配置
  const handleSave = () => {
    message.success('测试配置已保存');
  };

  // 重置请求参数
  const handleReset = () => {
    setRequestHeaders('{\n  "Content-Type": "application/json",\n  "Authorization": "Bearer your-token"\n}');
    setRequestBody('{}');
    setQueryParams('{}');
    message.info('已重置为默认配置');
  };

  // 获取状态码颜色
  const getStatusCodeColor = (code: number) => {
    if (code >= 200 && code < 300) return 'green';
    if (code >= 300 && code < 400) return 'blue';
    if (code >= 400 && code < 500) return 'orange';
    return 'red';
  };

  // 获取方法颜色
  const getMethodColor = (method: string) => {
    const colorMap: Record<string, string> = {
      'GET': '#52c41a',
      'POST': '#1890ff',
      'PUT': '#faad14',
      'DELETE': '#ff4d4f',
    };
    return colorMap[method] || '#d9d9d9';
  };

  return (
    <PageContainer
      title="API服务测试"
      subTitle={currentService ? `${currentService.name} - ${currentService.description}` : '在线API测试工具'}
    >
      <Row gutter={[16, 16]}>
        {/* 左侧请求配置区 */}
        <Col span={12}>
          <Card title={
            <Space>
              <ApiOutlined />
              请求配置
            </Space>
          } extra={
            <Space>
              <Button icon={<SaveOutlined />} onClick={handleSave}>保存</Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          }>
            {/* 请求URL */}
            <div style={{ marginBottom: 16 }}>
              <Space style={{ width: '100%' }}>
                <Select
                  value={requestMethod}
                  onChange={setRequestMethod}
                  style={{ width: 100 }}
                >
                  <Select.Option value="GET">GET</Select.Option>
                  <Select.Option value="POST">POST</Select.Option>
                  <Select.Option value="PUT">PUT</Select.Option>
                  <Select.Option value="DELETE">DELETE</Select.Option>
                </Select>
                <Input
                  value={requestUrl}
                  onChange={(e) => setRequestUrl(e.target.value)}
                  placeholder="请输入API路径"
                  style={{ flex: 1 }}
                />
                <Button 
                  type="primary" 
                  icon={<PlayCircleOutlined />} 
                  loading={loading}
                  onClick={handleTest}
                  size="large"
                >
                  发送请求
                </Button>
              </Space>
            </div>

            {/* 请求详情标签页 */}
            <Tabs defaultActiveKey="headers" size="small">
              <TabPane tab="请求头" key="headers">
                <Editor
                  height="200px"
                  language="json"
                  value={requestHeaders}
                  onChange={(value) => setRequestHeaders(value || '{}')}
                  theme="vs"
                  options={{
                    minimap: { enabled: false },
                    fontSize: 12,
                    wordWrap: 'on',
                    scrollBeyondLastLine: false
                  }}
                />
              </TabPane>
              <TabPane tab="查询参数" key="params">
                <Editor
                  height="200px"
                  language="json"
                  value={queryParams}
                  onChange={(value) => setQueryParams(value || '{}')}
                  theme="vs"
                  options={{
                    minimap: { enabled: false },
                    fontSize: 12,
                    wordWrap: 'on',
                    scrollBeyondLastLine: false
                  }}
                />
              </TabPane>
              <TabPane tab="请求体" key="body">
                <Editor
                  height="200px"
                  language="json"
                  value={requestBody}
                  onChange={(value) => setRequestBody(value || '{}')}
                  theme="vs"
                  options={{
                    minimap: { enabled: false },
                    fontSize: 12,
                    wordWrap: 'on',
                    scrollBeyondLastLine: false
                  }}
                />
              </TabPane>
            </Tabs>
          </Card>

          {/* 测试历史 */}
          <Card 
            title={
              <Space>
                <HistoryOutlined />
                测试历史
              </Space>
            }
            style={{ marginTop: 16 }}
            size="small"
          >
            <Table
              dataSource={testHistory}
              rowKey="id"
              size="small"
              pagination={false}
              scroll={{ y: 200 }}
              columns={[
                {
                  title: '时间',
                  dataIndex: 'timestamp',
                  width: 120,
                  render: (time) => time.split(' ')[1]
                },
                {
                  title: '方法',
                  dataIndex: 'method',
                  width: 60,
                  render: (method) => (
                    <Tag color={getMethodColor(method)} size="small">{method}</Tag>
                  )
                },
                {
                  title: '路径',
                  dataIndex: 'path',
                  ellipsis: true
                },
                {
                  title: '状态',
                  dataIndex: 'statusCode',
                  width: 60,
                  render: (code) => (
                    <Tag color={getStatusCodeColor(code)} size="small">{code}</Tag>
                  )
                },
                {
                  title: '耗时',
                  dataIndex: 'responseTime',
                  width: 70,
                  render: (time) => `${time}ms`
                }
              ]}
            />
          </Card>
        </Col>

        {/* 右侧响应结果区 */}
        <Col span={12}>
          <Card title={
            <Space>
              <ThunderboltOutlined />
              响应结果
            </Space>
          }>
            {testResult ? (
              <Tabs activeKey={activeTab} onChange={setActiveTab}>
                <TabPane tab="响应数据" key="response">
                  {/* 响应状态 */}
                  <Alert
                    type={testResult.success ? 'success' : 'error'}
                    message={
                      <Space>
                        {testResult.success ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
                        <span>状态码: {testResult.statusCode}</span>
                        <span>响应时间: {testResult.responseTime}ms</span>
                        <span>时间: {new Date(testResult.timestamp).toLocaleString()}</span>
                      </Space>
                    }
                    style={{ marginBottom: 16 }}
                  />

                  {/* 响应内容 */}
                  <Editor
                    height="400px"
                    language="json"
                    value={JSON.stringify(testResult.response, null, 2)}
                    options={{
                      readOnly: true,
                      minimap: { enabled: false },
                      fontSize: 12,
                      wordWrap: 'on'
                    }}
                    theme="vs"
                  />
                </TabPane>
                <TabPane tab="响应头" key="headers">
                  <Editor
                    height="400px"
                    language="json"
                    value={JSON.stringify(testResult.response?.headers || {}, null, 2)}
                    options={{
                      readOnly: true,
                      minimap: { enabled: false },
                      fontSize: 12,
                      wordWrap: 'on'
                    }}
                    theme="vs"
                  />
                </TabPane>
                <TabPane tab="请求详情" key="request">
                  <Divider orientation="left">请求信息</Divider>
                  <Editor
                    height="150px"
                    language="json"
                    value={JSON.stringify(testResult.request, null, 2)}
                    options={{
                      readOnly: true,
                      minimap: { enabled: false },
                      fontSize: 12,
                      wordWrap: 'on'
                    }}
                    theme="vs"
                  />
                  <Divider orientation="left">原始SQL</Divider>
                  <div style={{ 
                    background: '#f8f9fa', 
                    padding: 12, 
                    borderRadius: 4,
                    fontFamily: 'monospace',
                    fontSize: 12,
                    border: '1px solid #e9ecef'
                  }}>
                    SELECT id, name, email, age, created_at FROM users WHERE status = 1 ORDER BY created_at DESC LIMIT 100;
                  </div>
                </TabPane>
              </Tabs>
            ) : (
              <div style={{ 
                height: 400, 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center',
                color: '#999',
                textAlign: 'center'
              }}>
                <div>
                  <BugOutlined style={{ fontSize: 48, color: '#d9d9d9', marginBottom: 16 }} />
                  <h3 style={{ color: '#666', margin: '0 0 8px 0' }}>点击"发送请求"开始测试</h3>
                  <p>测试结果将在这里显示</p>
                </div>
              </div>
            )}
          </Card>
        </Col>
      </Row>
      
      {/* 服务信息面板 */}
      {currentService && (
        <Card 
          title={
            <Space>
              <DatabaseOutlined />
              当前服务信息
            </Space>
          }
          size="small"
          style={{ marginTop: 16 }}
        >
          <Row gutter={[16, 8]}>
            <Col span={6}>
              <Text type="secondary">服务名称:</Text> {currentService.name}
            </Col>
            <Col span={6}>
              <Text type="secondary">请求路径:</Text> <code>{currentService.path}</code>
            </Col>
            <Col span={6}>
              <Text type="secondary">请求方法:</Text> 
              <Tag color={getMethodColor(currentService.method)} style={{ marginLeft: 8 }}>
                {currentService.method}
              </Tag>
            </Col>
            <Col span={6}>
              <Text type="secondary">数据源:</Text> {currentService.datasourceName}
            </Col>
            <Col span={24}>
              <Text type="secondary">描述:</Text> {currentService.description}
            </Col>
          </Row>
        </Card>
      )}
    </PageContainer>
  );
};

export default ApiTesting;