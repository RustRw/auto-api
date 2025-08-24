import { 
  testDraftApi, 
  validateSql, 
  explainSql 
} from '@/services/testing';
import { getDataSources } from '@/services/datasource';
import { PageContainer, ProCard } from '@ant-design/pro-components';
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
  Divider
} from 'antd';
import React, { useState, useEffect } from 'react';
import { 
  PlayCircleOutlined, 
  CheckCircleOutlined, 
  CloseCircleOutlined,
  EyeOutlined,
  DatabaseOutlined,
  CodeOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';

const { Title, Text } = Typography;
const { TextArea } = Input;

const ApiTesting: React.FC = () => {
  const [dataSources, setDataSources] = useState<API.DataSource[]>([]);
  const [selectedDataSource, setSelectedDataSource] = useState<number | undefined>();
  const [sqlCode, setSqlCode] = useState('SELECT * FROM users LIMIT 10;');
  const [parameters, setParameters] = useState('{}');
  const [loading, setLoading] = useState(false);
  const [testResult, setTestResult] = useState<API.ApiTestResponse | null>(null);
  const [validationResult, setValidationResult] = useState<any>(null);
  const [explainResult, setExplainResult] = useState<any>(null);

  useEffect(() => {
    fetchDataSources();
  }, []);

  const fetchDataSources = async () => {
    try {
      const response = await getDataSources({ pageSize: 100 });
      setDataSources(response.data.content.filter(ds => ds.enabled));
      if (response.data.content.length > 0) {
        setSelectedDataSource(response.data.content[0].id);
      }
    } catch (error) {
      message.error('获取数据源列表失败');
    }
  };

  const handleTest = async () => {
    if (!selectedDataSource) {
      message.warning('请选择数据源');
      return;
    }

    setLoading(true);
    try {
      let parsedParams = {};
      try {
        parsedParams = JSON.parse(parameters);
      } catch (e) {
        message.warning('参数格式不正确，将使用空对象');
      }

      const response = await testDraftApi({
        dataSourceId: selectedDataSource,
        sqlContent: sqlCode,
        parameters: parsedParams,
      });

      setTestResult(response.data);
      
      if (response.data.success) {
        message.success(`执行成功! 耗时: ${response.data.executionTime}ms`);
      } else {
        message.error(`执行失败: ${response.data.message}`);
      }
    } catch (error) {
      message.error('测试失败');
      setTestResult({
        success: false,
        message: '网络错误或服务不可用',
        executionTime: 0,
        statusCode: 500,
        testTime: new Date().toISOString(),
      });
    } finally {
      setLoading(false);
    }
  };

  const handleValidate = async () => {
    if (!selectedDataSource) {
      message.warning('请选择数据源');
      return;
    }

    try {
      const response = await validateSql({
        dataSourceId: selectedDataSource,
        sql: sqlCode,
      });
      setValidationResult(response.data);
      
      if (response.data.valid) {
        message.success('SQL语法验证通过');
      } else {
        message.warning('SQL语法验证失败');
      }
    } catch (error) {
      message.error('SQL验证失败');
    }
  };

  const handleExplain = async () => {
    if (!selectedDataSource) {
      message.warning('请选择数据源');
      return;
    }

    try {
      const response = await explainSql({
        dataSourceId: selectedDataSource,
        sql: sqlCode,
      });
      setExplainResult(response.data);
      
      if (response.data.success) {
        message.success('SQL执行计划获取成功');
      } else {
        message.warning('获取执行计划失败');
      }
    } catch (error) {
      message.error('获取执行计划失败');
    }
  };

  const renderResult = () => {
    if (!testResult) return null;

    const tabItems = [];

    // 结果数据
    if (testResult.success && testResult.data) {
      const data = Array.isArray(testResult.data) ? testResult.data : [testResult.data];
      const columns = data.length > 0 ? Object.keys(data[0]).map(key => ({
        title: key,
        dataIndex: key,
        key,
        ellipsis: true,
        width: 150,
      })) : [];

      tabItems.push({
        key: 'data',
        label: `数据结果 (${data.length} 条)`,
        children: (
          <Table
            size="small"
            dataSource={data}
            columns={columns}
            rowKey={(record, index) => index}
            scroll={{ x: true, y: 400 }}
            pagination={{ pageSize: 50, showSizeChanger: true }}
          />
        ),
      });
    }

    // 执行信息
    tabItems.push({
      key: 'info',
      label: '执行信息',
      children: (
        <div style={{ padding: '16px' }}>
          <Row gutter={[16, 16]}>
            <Col span={6}>
              <div>
                <Text type="secondary">执行状态</Text>
                <div>
                  <Tag color={testResult.success ? 'success' : 'error'} 
                       icon={testResult.success ? <CheckCircleOutlined /> : <CloseCircleOutlined />}>
                    {testResult.success ? '成功' : '失败'}
                  </Tag>
                </div>
              </div>
            </Col>
            <Col span={6}>
              <div>
                <Text type="secondary">执行时间</Text>
                <div>{testResult.executionTime}ms</div>
              </div>
            </Col>
            <Col span={6}>
              <div>
                <Text type="secondary">状态码</Text>
                <div>{testResult.statusCode}</div>
              </div>
            </Col>
            <Col span={6}>
              <div>
                <Text type="secondary">记录数</Text>
                <div>{testResult.recordCount || 0}</div>
              </div>
            </Col>
            {testResult.message && (
              <Col span={24}>
                <div>
                  <Text type="secondary">消息</Text>
                  <div style={{ 
                    background: testResult.success ? '#f6ffed' : '#fff2f0',
                    border: `1px solid ${testResult.success ? '#b7eb8f' : '#ffccc7'}`,
                    padding: '8px 12px',
                    borderRadius: '6px',
                    marginTop: '4px'
                  }}>
                    {testResult.message}
                  </div>
                </div>
              </Col>
            )}
            {testResult.errorDetail && (
              <Col span={24}>
                <div>
                  <Text type="secondary">错误详情</Text>
                  <div style={{ 
                    background: '#fff2f0',
                    border: '1px solid #ffccc7',
                    padding: '8px 12px',
                    borderRadius: '6px',
                    marginTop: '4px',
                    fontFamily: 'monospace',
                    fontSize: '12px'
                  }}>
                    {testResult.errorDetail}
                  </div>
                </div>
              </Col>
            )}
          </Row>
        </div>
      ),
    });

    return (
      <ProCard title="执行结果" style={{ marginTop: 24 }}>
        <Tabs items={tabItems} />
      </ProCard>
    );
  };

  return (
    <PageContainer
      header={{
        title: 'API在线测试',
        subTitle: '在线编辑和测试SQL查询，快速验证API逻辑',
      }}
    >
      {/* 工具栏 */}
      <ProCard style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col>
            <Space>
              <DatabaseOutlined />
              <span>数据源:</span>
            </Space>
          </Col>
          <Col flex={1}>
            <Select
              style={{ width: '300px' }}
              placeholder="请选择数据源"
              value={selectedDataSource}
              onChange={setSelectedDataSource}
              options={dataSources.map(ds => ({
                label: `${ds.name} (${ds.type})`,
                value: ds.id,
              }))}
            />
          </Col>
          <Col>
            <Space>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                loading={loading}
                onClick={handleTest}
              >
                执行查询
              </Button>
              <Button
                icon={<CheckCircleOutlined />}
                onClick={handleValidate}
              >
                验证SQL
              </Button>
              <Button
                icon={<EyeOutlined />}
                onClick={handleExplain}
              >
                执行计划
              </Button>
            </Space>
          </Col>
        </Row>
      </ProCard>

      <Row gutter={16}>
        {/* SQL编辑器 */}
        <Col span={16}>
          <ProCard title={
            <Space>
              <CodeOutlined />
              <span>SQL编辑器</span>
            </Space>
          }>
            <div style={{ height: '400px', border: '1px solid #d9d9d9' }}>
              <Editor
                height="100%"
                language="sql"
                value={sqlCode}
                onChange={(value) => setSqlCode(value || '')}
                theme="vs"
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  lineNumbers: 'on',
                  roundedSelection: false,
                  scrollBeyondLastLine: false,
                  readOnly: false,
                  automaticLayout: true,
                }}
              />
            </div>
          </ProCard>
        </Col>

        {/* 参数配置 */}
        <Col span={8}>
          <ProCard title="请求参数" style={{ marginBottom: 16 }}>
            <TextArea
              rows={6}
              value={parameters}
              onChange={(e) => setParameters(e.target.value)}
              placeholder="请输入JSON格式的参数，例如：&#10;{&#10;  &quot;id&quot;: 1,&#10;  &quot;name&quot;: &quot;测试&quot;&#10;}"
            />
          </ProCard>

          {/* SQL验证结果 */}
          {validationResult && (
            <ProCard title="SQL验证结果" style={{ marginBottom: 16 }}>
              <div style={{
                padding: '12px',
                background: validationResult.valid ? '#f6ffed' : '#fff2f0',
                border: `1px solid ${validationResult.valid ? '#b7eb8f' : '#ffccc7'}`,
                borderRadius: '6px'
              }}>
                <Space>
                  {validationResult.valid ? (
                    <CheckCircleOutlined style={{ color: '#52c41a' }} />
                  ) : (
                    <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                  )}
                  <Text style={{ color: validationResult.valid ? '#52c41a' : '#ff4d4f' }}>
                    {validationResult.valid ? '语法正确' : '语法错误'}
                  </Text>
                </Space>
                {validationResult.message && (
                  <div style={{ marginTop: '8px', fontSize: '12px' }}>
                    {validationResult.message}
                  </div>
                )}
              </div>
            </ProCard>
          )}

          {/* 执行计划 */}
          {explainResult && (
            <ProCard title={
              <Space>
                <ThunderboltOutlined />
                <span>执行计划</span>
              </Space>
            }>
              {explainResult.success ? (
                <div style={{ fontSize: '12px' }}>
                  {explainResult.executionPlan?.map((plan: any, index: number) => (
                    <div key={index} style={{ 
                      padding: '8px', 
                      background: '#f5f5f5', 
                      margin: '4px 0',
                      borderRadius: '4px'
                    }}>
                      <div><strong>{plan.operation}</strong></div>
                      {plan.cost && <div>Cost: {plan.cost}</div>}
                      {plan.rows && <div>Rows: {plan.rows}</div>}
                      {plan.details && <div>{plan.details}</div>}
                    </div>
                  ))}
                  {explainResult.estimatedCost && (
                    <div style={{ marginTop: '8px', color: '#666' }}>
                      预估成本: {explainResult.estimatedCost} | 
                      预估行数: {explainResult.estimatedRows}
                    </div>
                  )}
                </div>
              ) : (
                <Text type="secondary">获取执行计划失败</Text>
              )}
            </ProCard>
          )}
        </Col>
      </Row>

      {/* 执行结果 */}
      {renderResult()}
    </PageContainer>
  );
};

export default ApiTesting;