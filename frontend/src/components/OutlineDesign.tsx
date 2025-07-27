import React from 'react';
import { Form, Select, InputNumber, Button, Spin, Alert, Card, Typography, List } from 'antd';
import type { Outline, StoryCard } from '../types';
import OutlineTreeView from './OutlineTreeView';

const { Title } = Typography;
const { Option } = Select;

interface GenerateOutlineParams {
    numberOfChapters: number;
    pointOfView: string;
}

interface OutlineDesignProps {
    storyCards: StoryCard[];
    selectedStoryId: string | null;
    onStoryChange: (storyId: string) => void;
    onGenerate: (params: GenerateOutlineParams) => void;
    isLoading: boolean;
    outline: Outline | null;
    error: string | null;
    outlines: Outline[];
    onSelectOutline: (outline: Outline) => void;
}

const OutlineDesign: React.FC<OutlineDesignProps> = ({
    storyCards,
    selectedStoryId,
    onStoryChange,
    onGenerate,
    isLoading,
    outline,
    error,
    outlines,
    onSelectOutline,
}) => {
    const [form] = Form.useForm();

    const handleFinish = (values: GenerateOutlineParams) => {
        onGenerate(values);
    };

    return (
        <Spin spinning={isLoading} tip="正在生成大纲..." size="large">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                <Card>
                    <Title level={4}>生成设置</Title>
                    <Form
                        form={form}
                        layout="vertical"
                        onFinish={handleFinish}
                        initialValues={{ numberOfChapters: 10, pointOfView: '第三人称' }}
                    >
                        <Form.Item
                            label="选择故事"
                            rules={[{ required: true, message: '请选择一个故事！' }]}
                        >
                            <Select
                                value={selectedStoryId}
                                onChange={onStoryChange}
                                placeholder="-- 请选择一个故事 --"
                                showSearch
                                filterOption={(input, option) =>
                                    (option?.children as unknown as string).toLowerCase().includes(input.toLowerCase())
                                }
                            >
                                {storyCards.map(story => (
                                    <Option key={story.id} value={story.id.toString()}>{story.title}</Option>
                                ))}
                            </Select>
                        </Form.Item>

                        <Form.Item
                            name="numberOfChapters"
                            label="章节数"
                            rules={[{ required: true, message: '请输入章节数！' }]}
                        >
                            <InputNumber min={1} max={100} style={{ width: '100%' }} />
                        </Form.Item>

                        <Form.Item
                            name="pointOfView"
                            label="视角"
                            rules={[{ required: true, message: '请选择一个视角！' }]}
                        >
                            <Select>
                                <Option value="第一人称">第一人称</Option>
                                <Option value="第三人称">第三人称</Option>
                            </Select>
                        </Form.Item>

                        <Form.Item>
                            <Button
                                type="primary"
                                htmlType="submit"
                                disabled={!selectedStoryId}
                                style={{ width: '100%' }}
                            >
                                生成大纲
                            </Button>
                        </Form.Item>
                    </Form>
                </Card>

                {error && (
                    <Alert
                        message="生成失败"
                        description={error}
                        type="error"
                        showIcon
                    />
                )}

                {selectedStoryId && !isLoading && (
                    <Card>
                        <Title level={4}>历史大纲</Title>
                        {outlines.length > 0 ? (
                            <List
                                itemLayout="horizontal"
                                dataSource={outlines}
                                renderItem={item => (
                                    <List.Item
                                        actions={[<Button type="link" onClick={() => onSelectOutline(item)}>查看</Button>]}
                                    >
                                        <List.Item.Meta
                                            title={item.title || `大纲 #${item.id}`}
                                            description={`创建于: ${new Date(item.created_at).toLocaleString()}`}
                                        />
                                    </List.Item>
                                )}
                            />
                        ) : (
                            <p>暂无历史大纲，请先生成一个。</p>
                        )}
                    </Card>
                )}

                {outline && (
                    <Card>
                        <Title level={4}>{outline.title || `大纲 #${outline.id}`}</Title>
                        <OutlineTreeView outline={outline} />
                    </Card>
                )}
            </div>
        </Spin>
    );
};

export default OutlineDesign;
