import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Empty, List, Modal, Space, Spin, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
    fetchMaterials,
    findMaterialDuplicates,
    mergeMaterials,
    fetchMaterialCitations,
} from '../services/api';
import type {
    MaterialResponse,
    MaterialDuplicateCandidate,
    MaterialCitationDto,
} from '../types';
import Can from './Can';

const { Text } = Typography;

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
        loadMaterials();
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
            loadMaterials();
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
        },
        {
            title: '标签',
            dataIndex: 'tags',
            key: 'tags',
            render: (tags?: string | null) => {
                if (!tags) return <Text type="secondary">无</Text>;
                return (
                    <Space size={[0, 4]} wrap>
                        {tags.split(',').map(tag => (
                            <Tag key={tag.trim()} color="blue">{tag.trim()}</Tag>
                        ))}
                    </Space>
                );
            },
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
        },
        {
            title: '操作',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button size="small" onClick={() => handleShowCitations(record)}>引用历史</Button>
                </Space>
            ),
        },
    ]), [handleShowCitations]);

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
                                                onClick={() => handleMerge(item.duplicateMaterialId, item.materialId)}
                                            >
                                                合并到 {item.materialTitle}
                                            </Button>
                                        </Can>,
                                        <Can perform="material:write" on={item.duplicateMaterialId} key="merge-right">
                                            <Button
                                                size="small"
                                                loading={mergingKey === key}
                                                onClick={() => handleMerge(item.materialId, item.duplicateMaterialId)}
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
