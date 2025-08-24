import { PageContainer } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';
import { Card, Row, Col, Statistic, Typography, Space, Button } from 'antd';
import React from 'react';
import { 
  ApiOutlined, 
  DatabaseOutlined, 
  MonitorOutlined, 
  RocketOutlined,
  UserOutlined,
  ClockCircleOutlined 
} from '@ant-design/icons';

const { Title, Paragraph } = Typography;

const Welcome: React.FC = () => {
  const { initialState } = useModel('@@initialState');

  return (
    <PageContainer>
      <div style={{ padding: '24px' }}>
        {/* 欢迎区域 */}
        <Card style={{ marginBottom: '24px' }}>
          <Row>
            <Col span={16}>
              <Title level={2}>
                欢迎使用 Auto API Platform 🚀
              </Title>
              <Paragraph style={{ fontSize: '16px', color: '#666' }}>
                企业级API服务管理平台，提供数据源管理、API服务开发、在线测试、监控告警等全流程解决方案
              </Paragraph>
              <Space>
                <Button type="primary" icon={<RocketOutlined />} size="large">
                  开始创建API
                </Button>
                <Button icon={<DatabaseOutlined />} size="large">
                  管理数据源
                </Button>
              </Space>
            </Col>
            <Col span={8} style={{ textAlign: 'center' }}>
              <div style={{ 
                width: '200px', 
                height: '200px', 
                background: 'linear-gradient(135deg, #1890ff 0%, #722ed1 100%)',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto'
              }}>
                <ApiOutlined style={{ fontSize: '80px', color: 'white' }} />
              </div>
            </Col>
          </Row>
        </Card>

        {/* 统计卡片 */}
        <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="数据源总数"
                value={8}
                prefix={<DatabaseOutlined style={{ color: '#1890ff' }} />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="API服务"
                value={24}
                prefix={<ApiOutlined style={{ color: '#52c41a' }} />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="在线用户"
                value={12}
                prefix={<UserOutlined style={{ color: '#faad14' }} />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="今日请求"
                value={1563}
                prefix={<MonitorOutlined style={{ color: '#f5222d' }} />}
                valueStyle={{ color: '#f5222d' }}
              />
            </Card>
          </Col>
        </Row>

        {/* 功能介绍 */}
        <Row gutter={[24, 24]}>
          <Col xs={24} lg={8}>
            <Card 
              hoverable
              cover={
                <div style={{ 
                  height: '120px', 
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <DatabaseOutlined style={{ fontSize: '48px', color: 'white' }} />
                </div>
              }
            >
              <Card.Meta
                title="数据源管理"
                description="支持MySQL、PostgreSQL、Oracle、Elasticsearch等多种数据库，提供连接测试、元数据查询、在线SQL执行等功能。"
              />
            </Card>
          </Col>
          <Col xs={24} lg={8}>
            <Card 
              hoverable
              cover={
                <div style={{ 
                  height: '120px', 
                  background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <ApiOutlined style={{ fontSize: '48px', color: 'white' }} />
                </div>
              }
            >
              <Card.Meta
                title="API服务开发"
                description="通过可视化界面快速创建RESTful API，支持SQL到API的自动转换，提供版本管理和发布控制。"
              />
            </Card>
          </Col>
          <Col xs={24} lg={8}>
            <Card 
              hoverable
              cover={
                <div style={{ 
                  height: '120px', 
                  background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <MonitorOutlined style={{ fontSize: '48px', color: 'white' }} />
                </div>
              }
            >
              <Card.Meta
                title="监控告警"
                description="实时监控API性能、请求量、错误率等关键指标，支持自定义告警规则和多种通知方式。"
              />
            </Card>
          </Col>
        </Row>

        {/* 快速开始 */}
        <Card style={{ marginTop: '24px' }}>
          <Title level={3}>
            <ClockCircleOutlined /> 快速开始
          </Title>
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Paragraph>
                <strong>第一步：</strong> 添加数据源 - 
                <a href="/datasource/create" style={{ marginLeft: '8px' }}>
                  去创建数据源 →
                </a>
              </Paragraph>
              <Paragraph>
                <strong>第二步：</strong> 创建API服务 - 
                <a href="/apiservice/create" style={{ marginLeft: '8px' }}>
                  去创建API →
                </a>
              </Paragraph>
              <Paragraph>
                <strong>第三步：</strong> 在线测试API - 
                <a href="/apiservice/testing" style={{ marginLeft: '8px' }}>
                  去测试API →
                </a>
              </Paragraph>
              <Paragraph>
                <strong>第四步：</strong> 查看监控数据 - 
                <a href="/monitoring/overview" style={{ marginLeft: '8px' }}>
                  去查看监控 →
                </a>
              </Paragraph>
            </Col>
          </Row>
        </Card>
      </div>
    </PageContainer>
  );
};

export default Welcome;