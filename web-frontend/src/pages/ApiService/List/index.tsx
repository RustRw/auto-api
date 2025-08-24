import { 
  getApiServices, 
  deleteApiService, 
  updateApiServiceStatus,
  publishApiService,
  unpublishApiService 
} from '@/services/apiservice';
import {
  ActionType,
  PageContainer,
  ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tag, Space, Tooltip, Modal, Form, Input } from 'antd';
import React, { useRef, useState } from 'react';
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  ApiOutlined,
  StopOutlined,
  HistoryOutlined
} from '@ant-design/icons';
import { history } from '@umijs/max';

const { TextArea } = Input;

const ApiServiceList: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [publishModalVisible, setPublishModalVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<API.ApiService | null>(null);
  const [form] = Form.useForm();
  const actionRef = useRef<ActionType>();

  const handleDelete = async (id: number) => {
    try {
      await deleteApiService(id);
      message.success('删除成功');
      actionRef.current?.reload();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleStatusChange = async (record: API.ApiService, status: API.ApiStatus) => {
    try {
      await updateApiServiceStatus(record.id, status);
      message.success('状态更新成功');
      actionRef.current?.reload();
    } catch (error) {
      message.error('状态更新失败');
    }
  };

  const handlePublish = (record: API.ApiService) => {
    setCurrentRecord(record);
    setPublishModalVisible(true);
    form.resetFields();
  };

  const handlePublishConfirm = async () => {
    if (!currentRecord) return;
    
    try {
      const values = await form.validateFields();
      await publishApiService(currentRecord.id, values);
      message.success('API发布成功');
      setPublishModalVisible(false);
      actionRef.current?.reload();
    } catch (error) {
      message.error('API发布失败');
    }
  };

  const handleUnpublish = async (record: API.ApiService) => {
    try {
      await unpublishApiService(record.id);
      message.success('API取消发布成功');
      actionRef.current?.reload();
    } catch (error) {
      message.error('API取消发布失败');
    }
  };

  const getStatusColor = (status: API.ApiStatus) => {
    const colorMap: Record<API.ApiStatus, string> = {
      'DRAFT': '#d9d9d9',
      'TESTING': '#faad14',
      'PUBLISHED': '#52c41a',
      'DISABLED': '#ff4d4f',
    };
    return colorMap[status] || '#d9d9d9';
  };

  const getStatusText = (status: API.ApiStatus) => {
    const textMap: Record<API.ApiStatus, string> = {
      'DRAFT': '草稿',
      'TESTING': '测试中',
      'PUBLISHED': '已发布',
      'DISABLED': '已禁用',
    };
    return textMap[status] || '未知';
  };

  const getMethodColor = (method: API.HttpMethod) => {
    const colorMap: Record<API.HttpMethod, string> = {
      'GET': '#52c41a',
      'POST': '#1890ff',
      'PUT': '#faad14',
      'DELETE': '#ff4d4f',
    };
    return colorMap[method] || '#d9d9d9';
  };

  const columns: ProColumns<API.ApiService>[] = [
    {
      title: 'API名称',
      dataIndex: 'name',
      fixed: 'left',
      width: 200,
      render: (text, record) => (
        <a onClick={() => history.push(`/apiservice/edit/${record.id}`)}>
          {text}
        </a>
      ),
    },
    {
      title: '路径',
      dataIndex: 'path',
      width: 200,
      render: (text, record) => (
        <Space>
          <Tag color={getMethodColor(record.method)}>{record.method}</Tag>
          <code style={{ background: '#f5f5f5', padding: '2px 4px', borderRadius: '2px' }}>
            {text}
          </code>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (status: API.ApiStatus) => (
        <Tag color={getStatusColor(status)}>
          {getStatusText(status)}
        </Tag>
      ),
      valueEnum: {
        'DRAFT': { text: '草稿', status: 'Default' },
        'TESTING': { text: '测试中', status: 'Processing' },
        'PUBLISHED': { text: '已发布', status: 'Success' },
        'DISABLED': { text: '已禁用', status: 'Error' },
      },
    },
    {
      title: '启用状态',
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
      title: '缓存',
      dataIndex: 'cacheEnabled',
      width: 80,
      search: false,
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'blue' : 'default'}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '数据源ID',
      dataIndex: 'dataSourceId',
      width: 100,
      search: false,
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
      width: 280,
      render: (_, record) => {
        const actions = [
          <Tooltip key="edit" title="编辑">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => history.push(`/apiservice/edit/${record.id}`)}
            />
          </Tooltip>,
        ];

        if (record.status === 'DRAFT' || record.status === 'TESTING') {
          actions.push(
            <Tooltip key="publish" title="发布">
              <Button
                type="text"
                size="small"
                icon={<ApiOutlined />}
                onClick={() => handlePublish(record)}
              />
            </Tooltip>
          );
        }

        if (record.status === 'PUBLISHED') {
          actions.push(
            <Tooltip key="unpublish" title="取消发布">
              <Button
                type="text"
                size="small"
                icon={<StopOutlined />}
                onClick={() => handleUnpublish(record)}
              />
            </Tooltip>
          );
        }

        if (record.status !== 'PUBLISHED') {
          actions.push(
            <Tooltip key="test" title="测试">
              <Button
                type="text"
                size="small"
                icon={<PlayCircleOutlined />}
                onClick={() => history.push(`/apiservice/testing?id=${record.id}`)}
              />
            </Tooltip>
          );
        }

        actions.push(
          <Tooltip key="history" title="版本历史">
            <Button
              type="text"
              size="small"
              icon={<HistoryOutlined />}
              onClick={() => message.info('版本历史功能开发中')}
            />
          </Tooltip>,
          <Popconfirm
            key="delete"
            title="确定要删除这个API服务吗？"
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
          </Popconfirm>
        );

        return actions;
      },
    },
  ];

  return (
    <PageContainer>
      <ProTable<API.ApiService>
        headerTitle="API服务列表"
        actionRef={actionRef}
        rowKey="id"
        search={{
          labelWidth: 120,
        }}
        scroll={{ x: 1400 }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => history.push('/apiservice/create')}
          >
            新建API服务
          </Button>,
        ]}
        request={async (params = {}, sort, filter) => {
          try {
            const response = await getApiServices({
              current: params.current,
              pageSize: params.pageSize,
              status: params.status,
              keyword: params.name || params.description,
            });
            
            return {
              data: response.data.content,
              success: true,
              total: response.data.totalElements,
            };
          } catch (error) {
            message.error('获取API服务列表失败');
            return {
              data: [],
              success: false,
              total: 0,
            };
          }
        }}
        columns={columns}
      />

      {/* 发布Modal */}
      <Modal
        title="发布API服务"
        open={publishModalVisible}
        onOk={handlePublishConfirm}
        onCancel={() => setPublishModalVisible(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="version"
            label="版本号"
            rules={[{ required: true, message: '请输入版本号' }]}
          >
            <Input placeholder="请输入版本号，如: v1.0.0" />
          </Form.Item>
          <Form.Item
            name="releaseNotes"
            label="发布说明"
          >
            <TextArea rows={4} placeholder="请输入发布说明（可选）" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default ApiServiceList;