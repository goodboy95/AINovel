import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Card, Col, Empty, Form, Input, List, message, Row, Space, Spin, Tag, Typography } from 'antd';
import { approveMaterialReview, fetchPendingMaterials, rejectMaterialReview } from '../../services/api';
import type { MaterialReviewDecisionPayload, MaterialReviewItem } from '../../types';

const { Paragraph, Text } = Typography;
const { TextArea } = Input;

const DEFAULT_FORM_VALUES: MaterialReviewDecisionPayload = {
    title: '',
    summary: '',
    tags: '',
    type: '',
    entitiesJson: '',
    reviewNotes: '',
};

const ReviewDashboard = () => {
    const [items, setItems] = useState<MaterialReviewItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [form] = Form.useForm<MaterialReviewDecisionPayload>();

    const selectedItem = useMemo(
        () => (selectedId == null ? null : items.find(item => item.id === selectedId) ?? null),
        [items, selectedId]
    );

    const loadData = useCallback(async () => {
        setLoading(true);
        try {
            const data = await fetchPendingMaterials();
            setItems(data);
            if (data.length === 0) {
                setSelectedId(null);
            } else if (!data.some(item => item.id === selectedId)) {
                setSelectedId(data[0].id);
            }
        } catch (error) {
            console.error('加载待审核素材失败', error);
            message.error('加载待审核素材失败');
        } finally {
            setLoading(false);
        }
    }, [selectedId]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    useEffect(() => {
        if (!selectedItem) {
            form.resetFields();
            return;
        }
        form.setFieldsValue({
            title: selectedItem.title,
            summary: selectedItem.summary ?? '',
            tags: selectedItem.tags ?? '',
            type: selectedItem.type,
            entitiesJson: selectedItem.entitiesJson ?? '',
            reviewNotes: selectedItem.reviewNotes ?? '',
        });
    }, [form, selectedItem]);

    const handleDecision = async (approve: boolean) => {
        if (!selectedItem) return;
        try {
            const values = form.getFieldsValue();
            setSaving(true);
            if (approve) {
                await approveMaterialReview(selectedItem.id, values);
                message.success('审核已通过');
            } else {
                await rejectMaterialReview(selectedItem.id, values);
                message.success('审核已驳回');
            }
            await loadData();
        } catch (error) {
            console.error('提交审核结果失败', error);
            message.error('提交审核结果失败，请稍后重试');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div style={{ padding: 24 }}>
            <Card title="素材审核工作台">
                <Row gutter={24}>
                    <Col span={8}>
                        <Card title="待审核列表" bordered={false} bodyStyle={{ padding: 0 }}>
                            <Spin spinning={loading}>
                                {items.length === 0 ? (
                                    <Empty description="暂无待审核素材" style={{ margin: '32px 0' }} />
                                ) : (
                                    <List
                                        itemLayout="vertical"
                                        dataSource={items}
                                        renderItem={item => (
                                            <List.Item
                                                key={item.id}
                                                onClick={() => setSelectedId(item.id)}
                                                style={{
                                                    cursor: 'pointer',
                                                    background: item.id === selectedId ? '#f0f5ff' : 'transparent',
                                                    padding: '12px 16px',
                                                }}
                                            >
                                                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                                                    <Text strong>{item.title || '未命名素材'}</Text>
                                                    <Space size={8}>
                                                        <Tag color="blue">{item.type || 'file'}</Tag>
                                                        <Text type="secondary">
                                                            {new Date(item.createdAt).toLocaleString()}
                                                        </Text>
                                                    </Space>
                                                    {item.summary && (
                                                        <Paragraph ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>
                                                            {item.summary}
                                                        </Paragraph>
                                                    )}
                                                </Space>
                                            </List.Item>
                                        )}
                                    />
                                )}
                            </Spin>
                        </Card>
                    </Col>
                    <Col span={16}>
                        {!selectedItem ? (
                            <Empty description="请选择一条素材进行审核" />
                        ) : (
                            <Space direction="vertical" size="large" style={{ width: '100%' }}>
                                <Card title="原始内容" bordered={false}>
                                    <Paragraph style={{ maxHeight: 240, overflowY: 'auto', whiteSpace: 'pre-wrap' }}>
                                        {selectedItem.content || '（无原始文本）'}
                                    </Paragraph>
                                </Card>
                                <Card title="解析结果校对" bordered={false}>
                                    <Form
                                        form={form}
                                        layout="vertical"
                                        initialValues={DEFAULT_FORM_VALUES}
                                        disabled={saving}
                                    >
                                        <Form.Item label="标题" name="title">
                                            <Input placeholder="素材标题" />
                                        </Form.Item>
                                        <Form.Item label="类型" name="type">
                                            <Input placeholder="例如：character/place/event" />
                                        </Form.Item>
                                        <Form.Item label="摘要" name="summary">
                                            <TextArea rows={3} placeholder="素材摘要" />
                                        </Form.Item>
                                        <Form.Item label="标签" name="tags" tooltip="使用逗号分隔多个标签">
                                            <Input placeholder="示例：主角，帝国，危机" />
                                        </Form.Item>
                                        <Form.Item label="结构化 JSON" name="entitiesJson">
                                            <TextArea rows={6} placeholder="编辑或校正解析出的 JSON 结构" />
                                        </Form.Item>
                                        <Form.Item label="审核备注" name="reviewNotes">
                                            <TextArea rows={2} placeholder="给出审核通过/驳回的说明" />
                                        </Form.Item>
                                    </Form>
                                    <Space style={{ marginTop: 16 }}>
                                        <Button
                                            type="primary"
                                            loading={saving}
                                            onClick={() => handleDecision(true)}
                                        >
                                            批准
                                        </Button>
                                        <Button danger loading={saving} onClick={() => handleDecision(false)}>
                                            驳回
                                        </Button>
                                        <Button type="default" onClick={loadData} disabled={loading || saving}>
                                            刷新列表
                                        </Button>
                                    </Space>
                                </Card>
                            </Space>
                        )}
                    </Col>
                </Row>
            </Card>
        </div>
    );
};

export default ReviewDashboard;

