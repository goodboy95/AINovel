import React from 'react';
import { Button, Modal, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { WorldGenerationJobStatusEntry, WorldGenerationStatus, WorldGenerationJobStatus } from '../../../types';

const { Text } = Typography;

type GenerationProgressModalProps = {
    open: boolean;
    status: WorldGenerationStatus | null;
    worldName?: string;
    loading?: boolean;
    onClose: () => void;
    onRetry: (moduleKey: string) => void;
};

const jobStatusMeta: Record<WorldGenerationJobStatus, { text: string; color: string }> = {
    WAITING: { text: '等待中', color: 'default' },
    RUNNING: { text: '生成中', color: 'processing' },
    SUCCEEDED: { text: '已完成', color: 'success' },
    FAILED: { text: '失败', color: 'error' },
    CANCELLED: { text: '已取消', color: 'warning' },
};

const formatDateTime = (value?: string | null) => {
    if (!value) return '—';
    try {
        return new Date(value).toLocaleString();
    } catch {
        return value;
    }
};

const GenerationProgressModal: React.FC<GenerationProgressModalProps> = ({ open, status, worldName, loading, onClose, onRetry }) => {
    const columns: ColumnsType<WorldGenerationJobStatusEntry> = [
        {
            title: '模块',
            dataIndex: 'moduleLabel',
            key: 'moduleLabel',
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: value => {
                const meta = jobStatusMeta[value as WorldGenerationJobStatus];
                return <Tag color={meta?.color}>{meta?.text ?? value}</Tag>;
            },
        },
        {
            title: '重试次数',
            dataIndex: 'attempts',
            key: 'attempts',
            width: 100,
            render: value => value ?? 0,
        },
        {
            title: '开始时间',
            dataIndex: 'startedAt',
            key: 'startedAt',
            render: value => formatDateTime(value as string | undefined),
        },
        {
            title: '结束时间',
            dataIndex: 'finishedAt',
            key: 'finishedAt',
            render: value => formatDateTime(value as string | undefined),
        },
        {
            title: '错误信息',
            dataIndex: 'error',
            key: 'error',
            render: value => (value ? <Text type="danger">{value}</Text> : '—'),
        },
        {
            title: '操作',
            key: 'action',
            width: 140,
            render: (_, record) => (
                <Button
                    size="small"
                    onClick={() => onRetry(record.moduleKey)}
                    disabled={record.status !== 'FAILED'}
                >
                    重新排队
                </Button>
            ),
        },
    ];

    const isGenerating = status?.status === 'GENERATING';

    return (
        <Modal
            title="世界完整信息生成进度"
            open={open}
            onCancel={isGenerating ? undefined : onClose}
            closable={!isGenerating}
            maskClosable={false}
            footer={
                <Button type="primary" onClick={onClose} disabled={isGenerating}>
                    {isGenerating ? '生成完成后可关闭' : '关闭'}
                </Button>
            }
            width={800}
        >
            <Space direction="vertical" style={{ width: '100%' }} size={16}>
                <div>
                    <Text strong>{worldName || '当前世界'}</Text>
                    <Text style={{ marginLeft: 12 }}>状态：{status?.status || '—'}</Text>
                </div>
                <Table
                    size="small"
                    rowKey="moduleKey"
                    columns={columns}
                    dataSource={status?.queue ?? []}
                    loading={loading}
                    pagination={false}
                />
            </Space>
        </Modal>
    );
};

export default GenerationProgressModal;
