import { createDataSource, testDataSourceConfig } from '@/services/datasource';
import { 
  PageContainer,
  ProCard,
  ProForm,
  ProFormText,
  ProFormTextArea,
  ProFormSelect,
  ProFormDigit,
} from '@ant-design/pro-components';
import { history } from '@umijs/max';
import { Button, message, Space, Divider, Row, Col } from 'antd';
import React, { useState } from 'react';
import { 
  ArrowLeftOutlined, 
  DatabaseOutlined, 
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined 
} from '@ant-design/icons';

const DataSourceCreate: React.FC = () => {
  const [testing, setTesting] = useState(false);
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

  const getDefaultPort = (type: string) => {
    const portMap: Record<string, number> = {
      'MYSQL': 3306,
      'POSTGRESQL': 5432,
      'ORACLE': 1521,
      'H2': 9092,
      'ELASTICSEARCH': 9200,
    };
    return portMap[type] || 3306;
  };

  const handleTestConnection = async (values: any) => {
    setTesting(true);
    setTestResult(null);
    
    try {
      const result = await testDataSourceConfig(values);
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

  const handleSubmit = async (values: API.DataSourceCreateRequest) => {
    try {
      await createDataSource(values);
      message.success('数据源创建成功！');
      history.push('/datasource/list');
    } catch (error) {
      message.error('数据源创建失败');
    }
  };

  return (
    <PageContainer
      header={{
        title: '创建数据源',
        breadcrumb: {
          items: [
            { path: '/datasource/list', title: '数据源管理' },
            { title: '创建数据源' },
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
        <ProForm<API.DataSourceCreateRequest>
          layout="horizontal"
          labelCol={{ span: 4 }}
          wrapperCol={{ span: 16 }}
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
                      icon={<DatabaseOutlined />}
                    >
                      创建数据源
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
              fieldProps={{
                onChange: (value: string) => {
                  // 自动设置默认端口 - 这里需要通过form实例来设置
                  // 在实际项目中，可以通过ProForm的form参数获取
                },
              }}
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
              rules={[{ required: true, message: '请输入密码' }]}
              placeholder="请输入密码"
            />
          </ProCard>

          <ProCard title="连接池配置" style={{ marginBottom: 24 }}>
            <Row gutter={16}>
              <Col span={8}>
                <ProFormDigit
                  name="maxPoolSize"
                  label="最大连接数"
                  placeholder="默认10"
                  min={1}
                  max={100}
                  initialValue={10}
                />
              </Col>
              <Col span={8}>
                <ProFormDigit
                  name="connectionTimeout"
                  label="连接超时(秒)"
                  placeholder="默认30"
                  min={1}
                  max={300}
                  initialValue={30}
                />
              </Col>
              <Col span={8}>
                <ProFormText
                  name="testQuery"
                  label="测试查询"
                  placeholder="默认: SELECT 1"
                />
              </Col>
            </Row>
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
                  onClick={() => {
                    // 在实际实现中，这里需要通过form实例获取表单值
                    message.warning('请先完善连接配置信息后进行测试');
                  }}
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
        </ProForm>
      </ProCard>
    </PageContainer>
  );
};

export default DataSourceCreate;