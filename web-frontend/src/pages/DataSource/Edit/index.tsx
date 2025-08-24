import { 
  getDataSource, 
  updateDataSource, 
  testDataSourceConnection 
} from '@/services/datasource';
import { 
  PageContainer,
  ProCard,
  ProForm,
  ProFormText,
  ProFormTextArea,
  ProFormSelect,
  ProFormDigit,
  ProFormSwitch,
} from '@ant-design/pro-components';
import { history, useParams } from '@umijs/max';
import { Button, message, Space, Divider, Row, Col, Spin } from 'antd';
import React, { useState, useEffect } from 'react';
import { 
  ArrowLeftOutlined, 
  SaveOutlined, 
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined 
} from '@ant-design/icons';

const DataSourceEdit: React.FC = () => {
  const params = useParams();
  const [loading, setLoading] = useState(true);
  const [testing, setTesting] = useState(false);
  const [dataSource, setDataSource] = useState<API.DataSource | null>(null);
  const [testResult, setTestResult] = useState<{
    connected: boolean;
    message: string;
    responseTime?: number;
  } | null>(null);

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
      const response = await getDataSource(parseInt(params.id as string));
      setDataSource(response.data);
    } catch (error) {
      message.error('获取数据源信息失败');
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
      const result = await testDataSourceConnection(dataSource.id);
      setTestResult(result.data);
      
      if (result.data.connected) {
        message.success(`连接成功！响应时间: ${result.data.responseTime}ms`);
      } else {
        message.error(`连接失败: ${result.data.message}`);
      }
    } catch (error) {
      message.error('连接测试失败');
      setTestResult({
        connected: false,
        message: '网络错误或服务不可用',
      });
    } finally {
      setTesting(false);
    }
  };

  const handleSubmit = async (values: Partial<API.DataSourceCreateRequest>) => {
    if (!params.id) return;
    
    try {
      await updateDataSource(parseInt(params.id as string), values);
      message.success('数据源更新成功！');
      history.push('/datasource/list');
    } catch (error) {
      message.error('数据源更新失败');
    }
  };

  if (loading) {
    return (
      <PageContainer>
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
        </div>
      </PageContainer>
    );
  }

  if (!dataSource) {
    return null;
  }

  return (
    <PageContainer
      header={{
        title: `编辑数据源: ${dataSource.name}`,
        breadcrumb: {
          items: [
            { path: '/datasource/list', title: '数据源管理' },
            { title: '编辑数据源' },
          ],
        },
        extra: [
          <Button 
            key="back" 
            icon={<ArrowLeftOutlined />}
            onClick={() => history.goBack()}
          >
            返回
          </Button>,
        ],
      }}
    >
      <ProCard>
        <ProForm<Partial<API.DataSourceCreateRequest>>
          layout="horizontal"
          labelCol={{ span: 4 }}
          wrapperCol={{ span: 16 }}
          initialValues={{
            name: dataSource.name,
            description: dataSource.description,
            type: dataSource.type,
            host: dataSource.host,
            port: dataSource.port,
            database: dataSource.database,
            username: dataSource.username,
            enabled: dataSource.enabled,
          }}
          submitter={{
            render: (props, doms) => (
              <Row justify="center">
                <Col>
                  <Space size="middle">
                    <Button onClick={() => history.goBack()}>
                      取消
                    </Button>
                    <Button
                      type="primary"
                      onClick={() => props.form?.submit()}
                      icon={<SaveOutlined />}
                    >
                      保存更改
                    </Button>
                  </Space>
                </Col>
              </Row>
            ),
          }}
          onFinish={handleSubmit}
        >
          <ProCard title="基本信息" style={{ marginBottom: 24 }}>
            <ProFormText
              name="name"
              label="数据源名称"
              rules={[{ required: true, message: '请输入数据源名称' }]}
              placeholder="请输入数据源名称"
            />
            
            <ProFormTextArea
              name="description"
              label="描述"
              placeholder="请输入数据源描述"
              fieldProps={{ rows: 3 }}
            />

            <ProFormSelect
              name="type"
              label="数据源类型"
              rules={[{ required: true, message: '请选择数据源类型' }]}
              options={dataSourceTypes}
              placeholder="请选择数据源类型"
              disabled
            />

            <ProFormSwitch
              name="enabled"
              label="启用状态"
              checkedChildren="启用"
              unCheckedChildren="禁用"
            />
          </ProCard>

          <ProCard title="连接配置" style={{ marginBottom: 24 }}>
            <Row gutter={16}>
              <Col span={16}>
                <ProFormText
                  name="host"
                  label="主机地址"
                  rules={[{ required: true, message: '请输入主机地址' }]}
                  placeholder="请输入主机地址，如: localhost"
                />
              </Col>
              <Col span={8}>
                <ProFormDigit
                  name="port"
                  label="端口"
                  rules={[{ required: true, message: '请输入端口' }]}
                  placeholder="端口"
                  min={1}
                  max={65535}
                />
              </Col>
            </Row>

            <ProFormText
              name="database"
              label="数据库名"
              placeholder="请输入数据库名称"
            />

            <ProFormText
              name="username"
              label="用户名"
              rules={[{ required: true, message: '请输入用户名' }]}
              placeholder="请输入用户名"
            />

            <ProFormText.Password
              name="password"
              label="密码"
              placeholder="留空则不修改密码"
            />
          </ProCard>

          <Divider />

          {/* 连接测试区域 */}
          <ProCard title="连接测试" style={{ marginBottom: 24 }}>
            <div style={{ textAlign: 'center' }}>
              <Space direction="vertical" align="center">
                <Button
                  type="primary"
                  ghost
                  icon={<PlayCircleOutlined />}
                  loading={testing}
                  onClick={handleTestConnection}
                >
                  测试连接
                </Button>
                
                {testResult && (
                  <div style={{ 
                    padding: '12px 16px', 
                    borderRadius: '6px',
                    background: testResult.connected ? '#f6ffed' : '#fff2f0',
                    border: `1px solid ${testResult.connected ? '#b7eb8f' : '#ffccc7'}`,
                    minWidth: '300px'
                  }}>
                    <Space>
                      {testResult.connected ? (
                        <CheckCircleOutlined style={{ color: '#52c41a' }} />
                      ) : (
                        <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                      )}
                      <span style={{ 
                        color: testResult.connected ? '#52c41a' : '#ff4d4f',
                        fontWeight: 'bold' 
                      }}>
                        {testResult.connected ? '连接成功' : '连接失败'}
                      </span>
                    </Space>
                    <div style={{ marginTop: '8px', fontSize: '12px', color: '#666' }}>
                      {testResult.message}
                      {testResult.responseTime && (
                        <span> (响应时间: {testResult.responseTime}ms)</span>
                      )}
                    </div>
                  </div>
                )}
              </Space>
            </div>
          </ProCard>

          {/* 数据源信息 */}
          <ProCard title="数据源信息">
            <div style={{ color: '#666', fontSize: '14px' }}>
              <Row gutter={[16, 8]}>
                <Col span={8}>
                  <strong>创建时间:</strong> {dataSource.createdAt}
                </Col>
                <Col span={8}>
                  <strong>更新时间:</strong> {dataSource.updatedAt}
                </Col>
                <Col span={8}>
                  <strong>数据源ID:</strong> {dataSource.id}
                </Col>
              </Row>
            </div>
          </ProCard>
        </ProForm>
      </ProCard>
    </PageContainer>
  );
};

export default DataSourceEdit;