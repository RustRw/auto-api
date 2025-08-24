import { PageContainer, ProCard } from '@ant-design/pro-components';
import { 
  Row, 
  Col, 
  Statistic, 
  Card, 
  Progress, 
  Table, 
  Tag, 
  Space,
  DatePicker,
  Select,
  Alert
} from 'antd';
import React, { useState, useEffect } from 'react';
import { 
  ArrowUpOutlined, 
  ArrowDownOutlined, 
  ApiOutlined,
  DatabaseOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined
} from '@ant-design/icons';
import { Line, Column, Pie } from '@ant-design/charts';

const { RangePicker } = DatePicker;

const MonitoringOverview: React.FC = () => {
  const [timeRange, setTimeRange] = useState('today');
  const [loading, setLoading] = useState(false);

  // 模拟数据
  const overviewStats = {
    totalRequests: 15863,
    requestsChange: 12.5,
    avgResponseTime: 245,
    responseTimeChange: -8.3,
    successRate: 99.2,
    successRateChange: 0.5,
    errorCount: 23,
    errorChange: -15.2,
  };

  const requestTrendData = [
    { time: '00:00', requests: 120 },
    { time: '02:00', requests: 80 },
    { time: '04:00', requests: 60 },
    { time: '06:00', requests: 200 },
    { time: '08:00', requests: 450 },
    { time: '10:00', requests: 680 },
    { time: '12:00', requests: 890 },
    { time: '14:00', requests: 750 },
    { time: '16:00', requests: 920 },
    { time: '18:00', requests: 680 },
    { time: '20:00', requests: 520 },
    { time: '22:00', requests: 320 },
  ];

  const responseTimeData = [
    { time: '00:00', avg: 180, p95: 320 },
    { time: '02:00', avg: 150, p95: 280 },
    { time: '04:00', avg: 140, p95: 260 },
    { time: '06:00', avg: 200, p95: 380 },
    { time: '08:00', avg: 280, p95: 520 },
    { time: '10:00', avg: 320, p95: 680 },
    { time: '12:00', avg: 290, p95: 650 },
    { time: '14:00', avg: 260, p95: 580 },
    { time: '16:00', avg: 310, p95: 720 },
    { time: '18:00', avg: 250, p95: 580 },
    { time: '20:00', avg: 220, p95: 480 },
    { time: '22:00', avg: 180, p95: 350 },
  ];

  const apiStatusData = [
    { status: '正常', count: 45, color: '#52c41a' },
    { status: '警告', count: 8, color: '#faad14' },
    { status: '错误', count: 3, color: '#ff4d4f' },
  ];

  const topApiData = [
    {
      key: '1',
      api: '/api/v1/users',
      method: 'GET',
      requests: 2580,
      avgTime: 120,
      successRate: 99.8,
      status: 'healthy',
    },
    {
      key: '2', 
      api: '/api/v1/orders',
      method: 'POST',
      requests: 1890,
      avgTime: 280,
      successRate: 98.5,
      status: 'healthy',
    },
    {
      key: '3',
      api: '/api/v1/products',
      method: 'GET', 
      requests: 1650,
      avgTime: 95,
      successRate: 99.9,
      status: 'healthy',
    },
    {
      key: '4',
      api: '/api/v1/auth/login',
      method: 'POST',
      requests: 1200,
      avgTime: 450,
      successRate: 97.2,
      status: 'warning',
    },
    {
      key: '5',
      api: '/api/v1/reports',
      method: 'GET',
      requests: 890,
      avgTime: 1200,
      successRate: 95.8,
      status: 'error',
    },
  ];

  const alertData = [
    {
      key: '1',
      time: '2024-01-15 14:30:25',
      level: 'error',
      message: 'API响应时间超过阈值',
      api: '/api/v1/reports',
      value: '1.2s',
    },
    {
      key: '2', 
      time: '2024-01-15 13:45:12',
      level: 'warning',
      message: '数据库连接池使用率过高',
      api: 'MySQL-主库',
      value: '85%',
    },
    {
      key: '3',
      time: '2024-01-15 12:20:08',
      level: 'info',
      message: 'API请求量峰值',
      api: '/api/v1/users',
      value: '1000 req/min',
    },
  ];

  const apiColumns = [
    {
      title: 'API路径',
      dataIndex: 'api',
      key: 'api',
      render: (text: string, record: any) => (
        <Space>
          <Tag color={record.method === 'GET' ? 'green' : 'blue'}>{record.method}</Tag>
          <code>{text}</code>
        </Space>
      ),
    },
    {
      title: '请求次数',
      dataIndex: 'requests',
      key: 'requests',
      render: (value: number) => value.toLocaleString(),
      sorter: (a: any, b: any) => a.requests - b.requests,
    },
    {
      title: '平均响应时间',
      dataIndex: 'avgTime',
      key: 'avgTime',
      render: (value: number) => `${value}ms`,
      sorter: (a: any, b: any) => a.avgTime - b.avgTime,
    },
    {
      title: '成功率',
      dataIndex: 'successRate',
      key: 'successRate',
      render: (value: number) => (
        <span style={{ color: value > 99 ? '#52c41a' : value > 95 ? '#faad14' : '#ff4d4f' }}>
          {value}%
        </span>
      ),
      sorter: (a: any, b: any) => a.successRate - b.successRate,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const config = {
          healthy: { color: 'success', text: '健康' },
          warning: { color: 'warning', text: '警告' },
          error: { color: 'error', text: '错误' },
        };
        const { color, text } = config[status as keyof typeof config] || config.healthy;
        return <Tag color={color}>{text}</Tag>;
      },
    },
  ];

  const alertColumns = [
    {
      title: '时间',
      dataIndex: 'time',
      key: 'time',
      width: 180,
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 80,
      render: (level: string) => {
        const config = {
          error: { color: 'red', icon: <ExclamationCircleOutlined /> },
          warning: { color: 'orange', icon: <WarningOutlined /> },
          info: { color: 'blue', icon: <CheckCircleOutlined /> },
        };
        const { color, icon } = config[level as keyof typeof config] || config.info;
        return <Tag color={color} icon={icon}>{level.toUpperCase()}</Tag>;
      },
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
    },
    {
      title: '对象',
      dataIndex: 'api',
      key: 'api',
      render: (text: string) => <code>{text}</code>,
    },
    {
      title: '数值',
      dataIndex: 'value',
      key: 'value',
      width: 100,
    },
  ];

  return (
    <PageContainer
      header={{
        title: '监控概览',
        subTitle: '实时监控API服务性能和健康状态',
        extra: [
          <Space key="toolbar">
            <Select
              value={timeRange}
              onChange={setTimeRange}
              options={[
                { label: '今日', value: 'today' },
                { label: '昨日', value: 'yesterday' },
                { label: '近7天', value: '7days' },
                { label: '近30天', value: '30days' },
              ]}
            />
            <RangePicker />
          </Space>,
        ],
      }}
    >
      {/* 概览统计 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="总请求数"
              value={overviewStats.totalRequests}
              prefix={<ApiOutlined />}
              suffix={
                <span style={{ 
                  fontSize: 12, 
                  color: overviewStats.requestsChange > 0 ? '#52c41a' : '#ff4d4f' 
                }}>
                  {overviewStats.requestsChange > 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                  {Math.abs(overviewStats.requestsChange)}%
                </span>
              }
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="平均响应时间"
              value={overviewStats.avgResponseTime}
              suffix="ms"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ 
                color: overviewStats.avgResponseTime < 300 ? '#52c41a' : '#faad14' 
              }}
            />
            <div style={{ fontSize: 12, marginTop: 8 }}>
              <span style={{ color: '#52c41a' }}>
                <ArrowDownOutlined /> {Math.abs(overviewStats.responseTimeChange)}%
              </span>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="成功率"
              value={overviewStats.successRate}
              suffix="%"
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
            <Progress 
              percent={overviewStats.successRate} 
              size="small" 
              strokeColor="#52c41a"
              style={{ marginTop: 8 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="错误数量"
              value={overviewStats.errorCount}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
            <div style={{ fontSize: 12, marginTop: 8 }}>
              <span style={{ color: '#52c41a' }}>
                <ArrowDownOutlined /> {Math.abs(overviewStats.errorChange)}%
              </span>
            </div>
          </Card>
        </Col>
      </Row>

      {/* 图表区域 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={16}>
          <ProCard title="请求趋势">
            <Line
              data={requestTrendData}
              xField="time"
              yField="requests"
              height={300}
              smooth
              point={{
                size: 3,
              }}
              color="#1890ff"
            />
          </ProCard>
        </Col>
        <Col xs={24} lg={8}>
          <ProCard title="API状态分布">
            <Pie
              data={apiStatusData}
              angleField="count"
              colorField="status"
              height={300}
              radius={0.8}
              label={{
                type: 'outer',
                content: '{name} {percentage}',
              }}
              color={['#52c41a', '#faad14', '#ff4d4f']}
            />
          </ProCard>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <ProCard title="响应时间趋势">
            <Line
              data={responseTimeData.flatMap(item => [
                { ...item, type: '平均响应时间', value: item.avg },
                { ...item, type: 'P95响应时间', value: item.p95 },
              ])}
              xField="time"
              yField="value"
              seriesField="type"
              height={250}
              smooth
              color={['#1890ff', '#ff7875']}
            />
          </ProCard>
        </Col>
      </Row>

      {/* 数据表格区域 */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <ProCard title="Top API性能">
            <Table
              dataSource={topApiData}
              columns={apiColumns}
              pagination={false}
              size="small"
            />
          </ProCard>
        </Col>
        <Col xs={24} lg={10}>
          <ProCard title="最新告警">
            <Table
              dataSource={alertData}
              columns={alertColumns}
              pagination={false}
              size="small"
            />
          </ProCard>
        </Col>
      </Row>

      {/* 系统健康度 */}
      <ProCard title="系统健康度" style={{ marginTop: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <div style={{ textAlign: 'center' }}>
              <Progress
                type="circle"
                percent={92}
                format={() => '健康'}
                strokeColor="#52c41a"
              />
              <div style={{ marginTop: 8, fontWeight: 'bold' }}>API服务</div>
            </div>
          </Col>
          <Col xs={24} sm={8}>
            <div style={{ textAlign: 'center' }}>
              <Progress
                type="circle"
                percent={85}
                format={() => '良好'}
                strokeColor="#faad14"
              />
              <div style={{ marginTop: 8, fontWeight: 'bold' }}>数据库</div>
            </div>
          </Col>
          <Col xs={24} sm={8}>
            <div style={{ textAlign: 'center' }}>
              <Progress
                type="circle"
                percent={78}
                format={() => '一般'}
                strokeColor="#ff7875"
              />
              <div style={{ marginTop: 8, fontWeight: 'bold' }}>系统资源</div>
            </div>
          </Col>
        </Row>
      </ProCard>
    </PageContainer>
  );
};

export default MonitoringOverview;