import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Row, Col, Statistic, Card, Timeline, List, Avatar, Tag, Space } from 'antd';
import React from 'react';
import { 
  ApiOutlined,
  DatabaseOutlined,
  UserOutlined,
  ClockCircleOutlined,
  BugOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';

const Dashboard: React.FC = () => {
  // 模拟数据
  const quickStats = {
    apiCount: 24,
    datasourceCount: 8,
    activeUsers: 12,
    todayRequests: 1563,
  };

  const recentActivities = [
    {
      time: '2024-01-15 14:30',
      type: 'success',
      title: 'API服务发布',
      description: '用户管理API v1.2.0 发布成功',
    },
    {
      time: '2024-01-15 13:45',
      type: 'info',
      title: '数据源创建',
      description: '新建MySQL数据源 "订单数据库"',
    },
    {
      time: '2024-01-15 12:20',
      type: 'warning',
      title: '性能告警',
      description: 'API响应时间超过阈值',
    },
    {
      time: '2024-01-15 11:10',
      type: 'success',
      title: 'API测试完成',
      description: '商品查询API测试通过',
    },
  ];

  const todoList = [
    {
      id: 1,
      title: '优化订单查询API性能',
      priority: 'high',
      assignee: 'John Doe',
      status: 'pending',
    },
    {
      id: 2,
      title: '添加用户权限验证',
      priority: 'medium',
      assignee: 'Jane Smith',
      status: 'in-progress',
    },
    {
      id: 3,
      title: '完善API文档',
      priority: 'low',
      assignee: 'Bob Wilson',
      status: 'pending',
    },
    {
      id: 4,
      title: '数据库连接池优化',
      priority: 'high',
      assignee: 'Alice Brown',
      status: 'completed',
    },
  ];

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'success':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      case 'warning':
        return <ExclamationCircleOutlined style={{ color: '#faad14' }} />;
      case 'error':
        return <BugOutlined style={{ color: '#ff4d4f' }} />;
      default:
        return <ClockCircleOutlined style={{ color: '#1890ff' }} />;
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high':
        return '#ff4d4f';
      case 'medium':
        return '#faad14';
      case 'low':
        return '#52c41a';
      default:
        return '#d9d9d9';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed':
        return 'success';
      case 'in-progress':
        return 'processing';
      case 'pending':
        return 'default';
      default:
        return 'default';
    }
  };

  return (
    <PageContainer
      header={{
        title: '工作台',
        subTitle: '欢迎使用Auto API平台，这里是您的工作概览',
      }}
    >
      {/* 快速统计 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="API服务"
              value={quickStats.apiCount}
              prefix={<ApiOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="数据源"
              value={quickStats.datasourceCount}
              prefix={<DatabaseOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="在线用户"
              value={quickStats.activeUsers}
              prefix={<UserOutlined style={{ color: '#faad14' }} />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="今日请求"
              value={quickStats.todayRequests}
              prefix={<ClockCircleOutlined style={{ color: '#f5222d' }} />}
              valueStyle={{ color: '#f5222d' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 主要内容区域 */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <ProCard title="最近活动" style={{ height: '500px' }}>
            <Timeline>
              {recentActivities.map((activity, index) => (
                <Timeline.Item
                  key={index}
                  dot={getActivityIcon(activity.type)}
                >
                  <div style={{ marginBottom: 8 }}>
                    <div style={{ fontWeight: 'bold', marginBottom: 4 }}>
                      {activity.title}
                    </div>
                    <div style={{ color: '#666', fontSize: '12px', marginBottom: 4 }}>
                      {activity.time}
                    </div>
                    <div style={{ color: '#999' }}>
                      {activity.description}
                    </div>
                  </div>
                </Timeline.Item>
              ))}
            </Timeline>
          </ProCard>
        </Col>

        <Col xs={24} lg={12}>
          <ProCard title="待办事项" style={{ height: '500px' }}>
            <List
              dataSource={todoList}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Tag key="status" color={getStatusColor(item.status)}>
                      {item.status === 'completed' ? '已完成' : 
                       item.status === 'in-progress' ? '进行中' : '待处理'}
                    </Tag>,
                  ]}
                >
                  <List.Item.Meta
                    avatar={<Avatar icon={<UserOutlined />} />}
                    title={
                      <Space>
                        <span style={{ 
                          textDecoration: item.status === 'completed' ? 'line-through' : 'none',
                          color: item.status === 'completed' ? '#999' : 'inherit'
                        }}>
                          {item.title}
                        </span>
                        <Tag 
                          color={getPriorityColor(item.priority)}
                          size="small"
                        >
                          {item.priority === 'high' ? '高' :
                           item.priority === 'medium' ? '中' : '低'}
                        </Tag>
                      </Space>
                    }
                    description={`负责人: ${item.assignee}`}
                  />
                </List.Item>
              )}
            />
          </ProCard>
        </Col>
      </Row>

      {/* 快速操作 */}
      <ProCard title="快速操作" style={{ marginTop: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Card 
              hoverable
              onClick={() => window.location.href = '/datasource/create'}
              style={{ textAlign: 'center', cursor: 'pointer' }}
            >
              <DatabaseOutlined style={{ fontSize: '32px', color: '#1890ff', marginBottom: 8 }} />
              <div>创建数据源</div>
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card 
              hoverable
              onClick={() => window.location.href = '/apiservice/create'}
              style={{ textAlign: 'center', cursor: 'pointer' }}
            >
              <ApiOutlined style={{ fontSize: '32px', color: '#52c41a', marginBottom: 8 }} />
              <div>创建API服务</div>
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card 
              hoverable
              onClick={() => window.location.href = '/apiservice/testing'}
              style={{ textAlign: 'center', cursor: 'pointer' }}
            >
              <BugOutlined style={{ fontSize: '32px', color: '#faad14', marginBottom: 8 }} />
              <div>在线测试</div>
            </Card>
          </Col>
        </Row>
      </ProCard>
    </PageContainer>
  );
};

export default Dashboard;