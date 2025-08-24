import { 
  getDataSources, 
  deleteDataSource, 
  testDataSourceConnection 
} from '@/services/datasource';
import {
  ActionType,
  PageContainer,
  ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tag, Space, Tooltip } from 'antd';
import React, { useRef, useState } from 'react';
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined 
} from '@ant-design/icons';
import { history } from '@umijs/max';

const DataSourceList: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const actionRef = useRef<ActionType>();

  const handleDelete = async (id: number) => {
    try {
      await deleteDataSource(id);
      message.success('删除成功');
      actionRef.current?.reload();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleTestConnection = async (record: API.DataSource) => {
    setLoading(true);
    try {
      const result = await testDataSourceConnection(record.id);
      if (result.data.connected) {
        message.success(`连接成功 (${result.data.responseTime}ms)`);
      } else {
        message.error(`连接失败: ${result.data.message}`);
      }
    } catch (error) {
      message.error('连接测试失败');
    } finally {
      setLoading(false);
    }
  };

  const getTypeColor = (type: API.DataSourceType) => {
    const colorMap: Record<API.DataSourceType, string> = {
      'MYSQL': '#FF6B6B',
      'POSTGRESQL': '#4ECDC4',
      'ORACLE': '#FFE66D',
      'H2': '#95E1D3',
      'ELASTICSEARCH': '#A8E6CF',
    };
    return colorMap[type] || '#95A5A6';
  };

  const columns: ProColumns<API.DataSource>[] = [
    {
      title: '数据源名称',
      dataIndex: 'name',
      fixed: 'left',
      width: 200,
      render: (text, record) => (
        <a onClick={() => history.push(`/datasource/edit/${record.id}`)}>
          {text}
        </a>
      ),
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 120,
      render: (type: API.DataSourceType) => (
        <Tag color={getTypeColor(type)}>{type}</Tag>
      ),
      valueEnum: {
        'MYSQL': { text: 'MySQL' },
        'POSTGRESQL': { text: 'PostgreSQL' },
        'ORACLE': { text: 'Oracle' },
        'H2': { text: 'H2' },
        'ELASTICSEARCH': { text: 'Elasticsearch' },
      },
    },
    {
      title: '主机',
      dataIndex: 'host',
      width: 150,
      search: false,
      render: (text, record) => `${text}:${record.port}`,
    },
    {
      title: '数据库',
      dataIndex: 'database',
      width: 120,
      search: false,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      width: 120,
      search: false,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 100,
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'green' : 'red'} icon={enabled ? <CheckCircleOutlined /> : <CloseCircleOutlined />}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      ),
      valueEnum: {
        true: { text: '启用', status: 'Success' },
        false: { text: '禁用', status: 'Error' },
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      search: false,
      ellipsis: true,
      width: 200,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      search: false,
      valueType: 'dateTime',
    },
    {
      title: '操作',
      valueType: 'option',
      fixed: 'right',
      width: 200,
      render: (_, record) => [
        <Tooltip key="test" title="测试连接">
          <Button
            type="text"
            size="small"
            icon={<PlayCircleOutlined />}
            loading={loading}
            onClick={() => handleTestConnection(record)}
          />
        </Tooltip>,
        <Tooltip key="edit" title="编辑">
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => history.push(`/datasource/edit/${record.id}`)}
          />
        </Tooltip>,
        <Popconfirm
          key="delete"
          title="确定要删除这个数据源吗？"
          onConfirm={() => handleDelete(record.id)}
          okText="确定"
          cancelText="取消"
        >
          <Tooltip title="删除">
            <Button
              type="text"
              size="small"
              icon={<DeleteOutlined />}
              danger
            />
          </Tooltip>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<API.DataSource>
        headerTitle="数据源列表"
        actionRef={actionRef}
        rowKey="id"
        search={{
          labelWidth: 120,
        }}
        scroll={{ x: 1200 }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => history.push('/datasource/create')}
          >
            新建数据源
          </Button>,
        ]}
        request={async (params = {}, sort, filter) => {
          try {
            const response = await getDataSources({
              current: params.current,
              pageSize: params.pageSize,
              type: params.type,
              keyword: params.name || params.description,
            });
            
            return {
              data: response.data.content,
              success: true,
              total: response.data.totalElements,
            };
          } catch (error) {
            message.error('获取数据源列表失败');
            return {
              data: [],
              success: false,
              total: 0,
            };
          }
        }}
        columns={columns}
      />
    </PageContainer>
  );
};

export default DataSourceList;