import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Button,
    Descriptions,
    Drawer,
    Empty,
    Form,
    Input,
    List,
    Modal,
    Popconfirm,
    Select,
    Space,
    Spin,
    Table,
    Tag,
    Typography,
    Divider,
    message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
    fetchMaterials,
    findMaterialDuplicates,
    mergeMaterials,
    fetchMaterialCitations,
    fetchMaterialDetail,
    updateMaterial,
    deleteMaterial,
} from '../services/api';
import type {
    MaterialResponse,
    MaterialDuplicateCandidate,
    MaterialCitationDto,
    MaterialDetail,
    MaterialUpdatePayload,
    MaterialStatus,
} from '../types';
import Can from './Can';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

const MATERIAL_TYPES = [
    { label: '手动录入', value: 'manual' },
    { label: '设定资料', value: 'lore' },
    { label: '灵感碎片', value: 'idea' },
    { label: '上传文件', value: 'file' },
];

const MATERIAL_TYPE_LABELS: Record<string, string> = MATERIAL_TYPES.reduce((acc, item) => {
    acc[item.value] = item.label;
    return acc;
}, {} as Record<string, string>);

const STATUS_LABEL_MAP: Record<MaterialStatus, string> = {
    PUBLISHED: '已发布',
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    REJECTED: '已驳回',
    ARCHIVED: '已归档',
};

const STATUS_COLOR_MAP: Partial<Record<MaterialStatus, string>> = {
    PUBLISHED: 'green',
    DRAFT: 'gold',
    PENDING_REVIEW: 'orange',
    REJECTED: 'red',
    ARCHIVED: 'purple',
};

const STATUS_OPTIONS = (Object.keys(STATUS_LABEL_MAP) as MaterialStatus[]).map(status => ({
    value: status,
    label: STATUS_LABEL_MAP[status],
}));

const MaterialList: React.FC = () => {
    const [materials, setMaterials] = useState<MaterialResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [duplicateLoading, setDuplicateLoading] = useState(false);
    const [duplicates, setDuplicates] = useState<MaterialDuplicateCandidate[]>([]);
    const [duplicatesVisible, setDuplicatesVisible] = useState(false);
    const [citationsVisible, setCitationsVisible] = useState(false);
    const [citationsLoading, setCitationsLoading] = useState(false);
    const [citations, setCitations] = useState<MaterialCitationDto[]>([]);
    const [selectedMaterial, setSelectedMaterial] = useState<MaterialResponse | null>(null);
    const [mergingKey, setMergingKey] = useState<string | null>(null);

    const [detailVisible, setDetailVisible] = useState(false);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detail, setDetail] = useState<MaterialDetail | null>(null);
    const [isEditing, setIsEditing] = useState(false);
    const [saving, setSaving] = useState(false);
    const [deletingId, setDeletingId] = useState<number | null>(null);
    const [form] = Form.useForm<MaterialUpdatePayload>();

    const populateFormFromDetail = useCallback((material: MaterialDetail) => {
        form.setFieldsValue({
            title: material.title,
            type: material.type,
            summary: material.summary ?? '',
            tags: material.tags ?? '',
            content: material.content,
            status: material.status,
        });
    }, [form]);

    const loadMaterials = useCallback(async () => {
        setLoading(true);
        try {
            const data = await fetchMaterials();
            setMaterials(data);
        } catch (err) {
            message.error((err as Error).message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void loadMaterials();
    }, [loadMaterials]);

    useEffect(() => {
        const refreshListener = () => {
            void loadMaterials();
        };
        window.addEventListener('material:refresh', refreshListener);
        return () => window.removeEventListener('material:refresh', refreshListener);
    }, [loadMaterials]);

    const handleFindDuplicates = useCallback(async () => {
        setDuplicateLoading(true);
        try {
            const data = await findMaterialDuplicates();
            setDuplicates(data);
            setDuplicatesVisible(true);
        } catch (err) {
            message.error((err as Error).message);
        } finally {
            setDuplicateLoading(false);
        }
    }, []);

    const handleMerge = useCallback(async (sourceId: number, targetId: number) => {
        const key = `${sourceId}-${targetId}`;
        setMergingKey(key);
        try {
            await mergeMaterials({ sourceMaterialId: sourceId, targetMaterialId: targetId });
            message.success('素材已合并');
            setDuplicates(prev => prev.filter(item => {
                const a = `${item.materialId}-${item.duplicateMaterialId}`;
                const b = `${item.duplicateMaterialId}-${item.materialId}`;
                return a !== key && b !== key;
            }));
            void loadMaterials();
        } catch (err) {
            message.error((err as Error).message);
        } finally {
            setMergingKey(null);
        }
    }, [loadMaterials]);

    const handleShowCitations = useCallback(async (material: MaterialResponse) => {
        setSelectedMaterial(material);
        setCitationsVisible(true);
        setCitationsLoading(true);
        try {
            const data = await fetchMaterialCitations(material.id);
            setCitations(data);
        } catch (err) {
            message.error((err as Error).message);
        } finally {
            setCitationsLoading(false);
        }
    }, []);

    const handleCloseDrawer = useCallback(() => {
        setDetailVisible(false);
        setDetail(null);
        setIsEditing(false);
        setDetailLoading(false);
        form.resetFields();
    }, [form]);

    const handleOpenDetail = useCallback(async (materialId: number, editable = false) => {
        setDetailVisible(true);
        setDetailLoading(true);
        setIsEditing(editable);
        try {
            const data = await fetchMaterialDetail(materialId);
            setDetail(data);
            if (editable) {
                populateFormFromDetail(data);
            } else {
                form.resetFields();
            }
        } catch (err) {
            setDetail(null);
            setDetailVisible(false);
            setIsEditing(false);
            message.error((err as Error).message);
        } finally {
            setDetailLoading(false);
        }
    }, [form, populateFormFromDetail]);

    const enterEditMode = useCallback(() => {
        if (!detail) {
            return;
        }
        populateFormFromDetail(detail);
        setIsEditing(true);
    }, [detail, populateFormFromDetail]);

    const handleCancelEdit = useCallback(() => {
        if (detail) {
            populateFormFromDetail(detail);
        } else {
            form.resetFields();
        }
        setIsEditing(false);
    }, [detail, form, populateFormFromDetail]);

    const handleSubmitEdit = useCallback(async (values: MaterialUpdatePayload) => {
        if (!detail) {
            return;
        }
        setSaving(true);
        try {
            const payload: MaterialUpdatePayload = {
                title: values.title?.trim(),
                type: values.type,
                summary: values.summary !== undefined ? values.summary.trim() : undefined,
                tags: values.tags !== undefined ? values.tags.trim() : undefined,
                content: values.content,
                status: values.status,
            };
            const updated = await updateMaterial(detail.id, payload);
            setDetail(updated);
            populateFormFromDetail(updated);
            message.success('素材已更新');
            setIsEditing(false);
            void loadMaterials();
        } catch (err) {
            message.error((err as Error).message);
        } finally {
            setSaving(false);
        }
    }, [detail, loadMaterials, populateFormFromDetail]);

    const handleDelete = useCallback(async (materialId: number) => {
        setDeletingId(materialId);
        try {
            await deleteMaterial(materialId);
            message.success('素材已删除');
            setMaterials(prev => prev.filter(item => item.id !== materialId));
            if (detail?.id === materialId) {
                handleCloseDrawer();
            }
        } catch (err) {
            message.error((err as Error).message);
        } finally {
            setDeletingId(null);
        }
    }, [detail, handleCloseDrawer]);

    const columns: ColumnsType<MaterialResponse> = useMemo(() => ([
        {
            title: '标题',
            dataIndex: 'title',
            key: 'title',
            render: (value: string) => <Text strong>{value}</Text>,
        },
        {
            title: '类型',
            dataIndex: 'type',
            key: 'type',
            render: (value: string) => MATERIAL_TYPE_LABELS[value] ?? value,
        },
        {
            title: '标签',
            dataIndex: 'tags',
            key: 'tags',
            render: (tags?: string | null) => {
                if (!tags) return <Text type="secondary">无</Text>;
                return (
                    <Space size={[0, 4]} wrap>
                        {tags.split(',').map(tag => {
                            const trimmed = tag.trim();
                            return <Tag key={trimmed} color="blue">{trimmed}</Tag>;
                        })}
                    </Space>
                );
            },
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: MaterialStatus) => (
                <Tag color={STATUS_COLOR_MAP[status] ?? 'default'}>
                    {STATUS_LABEL_MAP[status] ?? status}
                </Tag>
            ),
        },
        {
            title: '操作',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button size="small" onClick={() => { void handleOpenDetail(record.id, false); }}>
                        查看
                    </Button>
                    <Can perform="material:write" on={record.id}>
                        <Button size="small" onClick={() => { void handleOpenDetail(record.id, true); }}>
                            编辑
                        </Button>
                    </Can>
                    <Can perform="material:write" on={record.id}>
                        <Popconfirm
                            title="删除素材"
                            description="删除后将无法恢复，确定要删除该素材吗？"
                            okText="删除"
                            cancelText="取消"
                            okButtonProps={{ danger: true, loading: deletingId === record.id }}
                            onConfirm={() => { void handleDelete(record.id); }}
                        >
                            <Button size="small" danger loading={deletingId === record.id}>
                                删除
                            </Button>
                        </Popconfirm>
                    </Can>
                    <Button size="small" onClick={() => { void handleShowCitations(record); }}>
                        引用历史
                    </Button>
                </Space>
            ),
        },
    ]), [deletingId, handleDelete, handleOpenDetail, handleShowCitations]);

    return (
        <div>
            <Space style={{ marginBottom: 16 }}>
                <Can perform="workspace:write">
                    <Button type="primary" loading={duplicateLoading} onClick={handleFindDuplicates}>
                        查找相似素材
                    </Button>
                </Can>
            </Space>
            <Table
                rowKey="id"
                loading={loading}
                columns={columns}
                dataSource={materials}
                pagination={{ pageSize: 10 }}
                locale={{ emptyText: loading ? <Spin /> : <Empty description="暂无素材" /> }}
            />

            <Drawer
                open={detailVisible}
                onClose={handleCloseDrawer}
                width={720}
                title={
                    isEditing
                        ? detail ? `编辑素材：${detail.title}` : '编辑素材'
                        : detail ? `素材详情：${detail.title}` : '素材详情'
                }
                extra={
                    !isEditing && detail ? (
                        <Can perform="material:write" on={detail.id}>
                            <Button type="primary" onClick={enterEditMode}>
                                编辑
                            </Button>
                        </Can>
                    ) : undefined
                }
                footer={
                    isEditing ? (
                        <Space>
                            <Button onClick={handleCancelEdit} disabled={saving}>
                                取消
                            </Button>
                            <Button type="primary" loading={saving} onClick={() => form.submit()}>
                                保存
                            </Button>
                        </Space>
                    ) : null
                }
            >
                {detailLoading ? (
                    <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>
                ) : !detail ? (
                    <Empty description="当前未选择素材" />
                ) : isEditing ? (
                    <Form
                        layout="vertical"
                        form={form}
                        initialValues={{
                            title: detail.title,
                            type: detail.type,
                            summary: detail.summary ?? '',
                            tags: detail.tags ?? '',
                            content: detail.content,
                            status: detail.status,
                        }}
                        onFinish={handleSubmitEdit}
                    >
                        <Form.Item
                            label="标题"
                            name="title"
                            rules={[{ required: true, message: '请输入素材标题' }]}
                        >
                            <Input placeholder="请输入素材标题" />
                        </Form.Item>
                        <Form.Item
                            label="类型"
                            name="type"
                            rules={[{ required: true, message: '请选择素材类型' }]}
                        >
                            <Select options={MATERIAL_TYPES} />
                        </Form.Item>
                        <Form.Item label="概要" name="summary">
                            <TextArea rows={3} placeholder="简要描述素材内容，可选" />
                        </Form.Item>
                        <Form.Item
                            label="正文内容"
                            name="content"
                            rules={[{ required: true, message: '请输入素材正文内容' }]}
                        >
                            <TextArea rows={12} placeholder="支持 Markdown 或纯文本" />
                        </Form.Item>
                        <Form.Item label="标签" name="tags" tooltip="使用英文逗号分隔多个标签">
                            <Input placeholder="例如：主角, 转折, 战斗场景" />
                        </Form.Item>
                        <Form.Item
                            label="状态"
                            name="status"
                            rules={[{ required: true, message: '请选择素材状态' }]}
                        >
                            <Select options={STATUS_OPTIONS} />
                        </Form.Item>
                    </Form>
                ) : (
                    <>
                        <Descriptions column={1} size="small" bordered>
                            <Descriptions.Item label="类型">
                                {MATERIAL_TYPE_LABELS[detail.type] ?? detail.type}
                            </Descriptions.Item>
                            <Descriptions.Item label="状态">
                                <Tag color={STATUS_COLOR_MAP[detail.status] ?? 'default'}>
                                    {STATUS_LABEL_MAP[detail.status] ?? detail.status}
                                </Tag>
                            </Descriptions.Item>
                            <Descriptions.Item label="标签">
                                {detail.tags ? (
                                    <Space size={[0, 4]} wrap>
                                        {detail.tags.split(',').map(tag => {
                                            const trimmed = tag.trim();
                                            return <Tag key={trimmed} color="blue">{trimmed}</Tag>;
                                        })}
                                    </Space>
                                ) : <Text type="secondary">无</Text>}
                            </Descriptions.Item>
                            <Descriptions.Item label="概要">
                                {detail.summary ? detail.summary : <Text type="secondary">无</Text>}
                            </Descriptions.Item>
                            <Descriptions.Item label="创建时间">
                                {new Date(detail.createdAt).toLocaleString()}
                            </Descriptions.Item>
                            {detail.updatedAt && (
                                <Descriptions.Item label="更新时间">
                                    {new Date(detail.updatedAt).toLocaleString()}
                                </Descriptions.Item>
                            )}
                        </Descriptions>
                        <Divider />
                        <Paragraph style={{ whiteSpace: 'pre-wrap' }}>
                            {detail.content}
                        </Paragraph>
                    </>
                )}
            </Drawer>

            <Modal
                open={duplicatesVisible}
                onCancel={() => setDuplicatesVisible(false)}
                width={720}
                footer={null}
                title="相似素材候选"
            >
                {duplicateLoading ? (
                    <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>
                ) : duplicates.length === 0 ? (
                    <Empty description="暂无相似项" />
                ) : (
                    <List
                        dataSource={duplicates}
                        renderItem={item => {
                            const key = `${item.materialId}-${item.duplicateMaterialId}`;
                            return (
                                <List.Item
                                    key={key}
                                    actions={[
                                        <Can perform="material:write" on={item.materialId} key="merge-left">
                                            <Button
                                                size="small"
                                                loading={mergingKey === `${item.duplicateMaterialId}-${item.materialId}`}
                                                onClick={() => { void handleMerge(item.duplicateMaterialId, item.materialId); }}
                                            >
                                                合并到 {item.materialTitle}
                                            </Button>
                                        </Can>,
                                        <Can perform="material:write" on={item.duplicateMaterialId} key="merge-right">
                                            <Button
                                                size="small"
                                                loading={mergingKey === key}
                                                onClick={() => { void handleMerge(item.materialId, item.duplicateMaterialId); }}
                                            >
                                                合并到 {item.duplicateTitle}
                                            </Button>
                                        </Can>,
                                    ]}
                                >
                                    <List.Item.Meta
                                        title={
                                            <Space direction="vertical">
                                                <Text>{item.materialTitle} ↔ {item.duplicateTitle}</Text>
                                                <Text type="secondary">相似度：{item.similarity.toFixed(3)}</Text>
                                            </Space>
                                        }
                                        description={
                                            <div>
                                                {item.overlappingChunks.map(chunk => (
                                                    <div key={`${chunk.materialChunkId}-${chunk.duplicateChunkId}`} style={{ marginBottom: 8 }}>
                                                        <Text type="secondary">
                                                            段落匹配（相似度 {chunk.similarity.toFixed(3)}）
                                                        </Text>
                                                        <div>{chunk.snippet}</div>
                                                    </div>
                                                ))}
                                            </div>
                                        }
                                    />
                                </List.Item>
                            );
                        }}
                    />
                )}
            </Modal>

            <Modal
                open={citationsVisible}
                onCancel={() => setCitationsVisible(false)}
                footer={null}
                width={680}
                title={selectedMaterial ? `引用历史：${selectedMaterial.title}` : '引用历史'}
            >
                {citationsLoading ? (
                    <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>
                ) : citations.length === 0 ? (
                    <Empty description="暂无引用记录" />
                ) : (
                    <List
                        dataSource={citations}
                        renderItem={item => (
                            <List.Item key={item.id}>
                                <Space direction="vertical" style={{ width: '100%' }}>
                                    <Text strong>{item.documentType} #{item.documentId}</Text>
                                    <Text type="secondary">段落序号：{item.chunkSeq ?? '未知'}</Text>
                                    {item.usageContext && <Text>{item.usageContext}</Text>}
                                    <Text type="secondary">引用于：{new Date(item.createdAt).toLocaleString()}</Text>
                                </Space>
                            </List.Item>
                        )}
                    />
                )}
            </Modal>
        </div>
    );
};

export default MaterialList;

