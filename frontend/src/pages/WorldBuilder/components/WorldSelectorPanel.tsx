import React, { useMemo, useState } from 'react';
import {
    Badge,
    Button,
    Card,
    Drawer,
    Input,
    List,
    Modal,
    Popconfirm,
    Select,
    Space,
    Tag,
    Typography,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { SelectProps } from 'antd';
import type { WorldStatus, WorldSummary, WorldModuleStatus } from '../../../types';

type WorldSelectorPanelProps = {
    worlds: WorldSummary[];
    drafts: WorldSummary[];
    selectedWorldId: number | null;
    selectedWorld?: WorldSummary;
    loading?: boolean;
    creating?: boolean;
    onSelectWorld: (worldId: number) => void;
    onCreateWorld: () => void;
    onRenameDraft: (worldId: number, nextName: string) => Promise<void>;
    onDeleteDraft: (worldId: number) => Promise<void>;
};

const statusLabels: Record<WorldStatus, string> = {
    DRAFT: '草稿',
    GENERATING: '生成中',
    ACTIVE: '已发布',
    ARCHIVED: '已归档',
};

const statusColors: Record<WorldStatus, string> = {
    DRAFT: 'default',
    GENERATING: 'processing',
    ACTIVE: 'success',
    ARCHIVED: 'warning',
};

const moduleStatusLabelMap: Record<WorldModuleStatus, string> = {
    EMPTY: '未填写',
    IN_PROGRESS: '填写中',
    READY: '待发布',
    AWAITING_GENERATION: '待生成',
    GENERATING: '生成中',
    COMPLETED: '已生成',
    FAILED: '生成失败',
};

const formatDateTime = (value?: string | null) => {
    if (!value) return '—';
    try {
        return new Date(value).toLocaleString();
    } catch {
        return value;
    }
};

const WorldSelectorPanel: React.FC<WorldSelectorPanelProps> = ({
    worlds,
    drafts,
    selectedWorldId,
    selectedWorld,
    loading,
    creating,
    onSelectWorld,
    onCreateWorld,
    onRenameDraft,
    onDeleteDraft,
}) => {
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const [renameTarget, setRenameTarget] = useState<WorldSummary | null>(null);
    const [renameValue, setRenameValue] = useState('');
    const [renameLoading, setRenameLoading] = useState(false);
    const [deletingId, setDeletingId] = useState<number | null>(null);

    const publishedOptions = useMemo<SelectProps['options']>(() => (
        worlds.map(world => ({
            value: world.id,
            label: (
                <Space size={8}>
                    <span>{world.name}</span>
                    <Tag color={statusColors[world.status]}>{statusLabels[world.status]}</Tag>
                </Space>
            ),
        }))
    ), [worlds]);

    const statusCounts = useMemo(() => {
        if (!selectedWorld) return [] as React.ReactNode[];
        const counts = new Map<string, number>();
        Object.values(selectedWorld.moduleProgress || {}).forEach(status => {
            const key = moduleStatusLabelMap[status] || status;
            counts.set(key, (counts.get(key) || 0) + 1);
        });
        return Array.from(counts.entries()).map(([label, count]) => (
            <Tag key={label}>{label}：{count}</Tag>
        ));
    }, [selectedWorld]);

    const handleRenameConfirm = async () => {
        if (!renameTarget) return;
        const nextName = renameValue.trim();
        if (!nextName) {
            return;
        }
        setRenameLoading(true);
        try {
            await onRenameDraft(renameTarget.id, nextName);
            setRenameTarget(null);
            setRenameValue('');
        } finally {
            setRenameLoading(false);
        }
    };

    const handleDeleteDraft = async (worldId: number) => {
        setDeletingId(worldId);
        try {
            await onDeleteDraft(worldId);
        } finally {
            setDeletingId(null);
        }
    };

    const draftList = (
        <List
            dataSource={drafts}
            locale={{ emptyText: '暂无草稿世界' }}
            renderItem={draft => (
                <List.Item
                    key={draft.id}
                    actions={[
                        <Button key="edit" type="link" onClick={() => { onSelectWorld(draft.id); setIsDrawerOpen(false); }}>
                            继续编辑
                        </Button>,
                        <Button
                            key="rename"
                            type="link"
                            icon={<EditOutlined />}
                            onClick={() => {
                                setRenameTarget(draft);
                                setRenameValue(draft.name);
                            }}
                        >
                            重命名
                        </Button>,
                        <Popconfirm
                            key="delete"
                            title="删除草稿"
                            description="删除后不可恢复，确定要删除该草稿吗？"
                            okType="danger"
                            onConfirm={() => handleDeleteDraft(draft.id)}
                        >
                            <Button type="link" danger icon={<DeleteOutlined />} loading={deletingId === draft.id}>
                                删除
                            </Button>
                        </Popconfirm>,
                    ]}
                >
                    <List.Item.Meta
                        title={draft.name}
                        description={
                            <Space direction="vertical" size={0}>
                                <Typography.Text type="secondary">最近更新：{formatDateTime(draft.updatedAt)}</Typography.Text>
                                <Space size={4} wrap>
                                    {draft.themes.map(theme => (
                                        <Tag key={theme} color="blue">{theme}</Tag>
                                    ))}
                                </Space>
                            </Space>
                        }
                    />
                </List.Item>
            )}
        />
    );

    return (
        <Card>
            <Space style={{ width: '100%', justifyContent: 'space-between', flexWrap: 'wrap' }}>
                <Space size={12} wrap>
                    <Button type="primary" icon={<PlusOutlined />} onClick={onCreateWorld} loading={creating}>
                        新建世界
                    </Button>
                    <Select
                        placeholder="选择已创建的世界"
                        style={{ minWidth: 260 }}
                        value={selectedWorldId ?? undefined}
                        onChange={value => onSelectWorld(Number(value))}
                        options={publishedOptions}
                        loading={loading}
                    />
                    <Badge count={drafts.length} offset={[4, -2]}>
                        <Button onClick={() => setIsDrawerOpen(true)}>草稿列表</Button>
                    </Badge>
                </Space>
            </Space>

            <div style={{ marginTop: 16 }}>
                {selectedWorld ? (
                    <Card size="small" bordered={false} style={{ background: '#f7f9fc' }}>
                        <Space direction="vertical" style={{ width: '100%' }} size={8}>
                            <Space wrap>
                                <Tag color={statusColors[selectedWorld.status]}>{statusLabels[selectedWorld.status]}</Tag>
                                {typeof selectedWorld.version === 'number' && (
                                    <Tag color="purple">版本 {selectedWorld.version}</Tag>
                                )}
                                <Typography.Text type="secondary">
                                    最近更新：{formatDateTime(selectedWorld.updatedAt)}
                                </Typography.Text>
                                {selectedWorld.publishedAt && (
                                    <Typography.Text type="secondary">
                                        首次发布：{formatDateTime(selectedWorld.publishedAt)}
                                    </Typography.Text>
                                )}
                            </Space>
                            <Typography.Paragraph style={{ marginBottom: 0 }}>
                                <Typography.Text strong>一句话概述：</Typography.Text> {selectedWorld.tagline || '—'}
                            </Typography.Paragraph>
                            <Space size={4} wrap>
                                {selectedWorld.themes.map(theme => (
                                    <Tag color="geekblue" key={theme}>{theme}</Tag>
                                ))}
                            </Space>
                            {statusCounts.length > 0 && (
                                <Space size={4} wrap>
                                    <Typography.Text type="secondary">模块状态：</Typography.Text>
                                    {statusCounts.map(node => node)}
                                </Space>
                            )}
                        </Space>
                    </Card>
                ) : (
                    <Typography.Text type="secondary">请选择或新建一个世界以开始编辑。</Typography.Text>
                )}
            </div>

            <Drawer
                title="草稿世界"
                open={isDrawerOpen}
                onClose={() => setIsDrawerOpen(false)}
                width={420}
            >
                {draftList}
            </Drawer>

            <Modal
                title="重命名草稿世界"
                open={!!renameTarget}
                onCancel={() => setRenameTarget(null)}
                onOk={handleRenameConfirm}
                confirmLoading={renameLoading}
                okText="保存"
            >
                <Input
                    placeholder="请输入新的世界名称"
                    value={renameValue}
                    onChange={e => setRenameValue(e.target.value)}
                    maxLength={80}
                />
            </Modal>
        </Card>
    );
};

export default WorldSelectorPanel;
