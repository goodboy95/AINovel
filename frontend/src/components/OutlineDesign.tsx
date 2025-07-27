import React, { useState, useEffect } from 'react';
import { Form, Select, InputNumber, Button, Spin, Alert, Card, Typography, List, Divider } from 'antd';
import type { Outline, StoryCard, Chapter } from '../types';
import { generateChapter, createEmptyOutlineForStory } from '../services/api';
import OutlineTreeView from './OutlineTreeView';

const { Title, Text } = Typography;
const { Option } = Select;

interface GenerateChapterParams {
    sectionsPerChapter: number;
    wordsPerSection: number;
}

interface OutlineDesignProps {
    storyCards: StoryCard[];
    selectedStoryId: string | null;
    onStoryChange: (storyId: string) => void;
    isLoading: boolean;
    setIsLoading: (isLoading: boolean) => void;
    outline: Outline | null;
    setOutline: (outline: Outline | null) => void;
    error: string | null;
    setError: (error: string | null) => void;
    outlines: Outline[];
    onSelectOutline: (outline: Outline) => void;
    fetchOutlines: (storyId: string) => void;
}

const OutlineDesign: React.FC<OutlineDesignProps> = ({
    storyCards,
    selectedStoryId,
    onStoryChange,
    isLoading,
    setIsLoading, // Assuming this is passed from parent
    outline,
    setOutline,
    error,
    setError,
    outlines,
    onSelectOutline,
    fetchOutlines,
}) => {
    const [form] = Form.useForm();
    const [currentChapter, setCurrentChapter] = useState(1);

    const handleCreateEmptyOutline = async () => {
        if (!selectedStoryId) {
            setError("请先选择一个故事。");
            return;
        }
        setIsLoading(true);
        setError(null);
        try {
            const newOutline = await createEmptyOutlineForStory(selectedStoryId);
            onSelectOutline(newOutline); // Automatically select the new outline
            fetchOutlines(selectedStoryId); // Refresh the list of outlines
        } catch (err) {
            if (err instanceof Error) {
                setError(err.message);
            } else {
                setError('创建新大纲失败。');
            }
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (outline) {
            setCurrentChapter(outline.chapters.length + 1);
        } else {
            setCurrentChapter(1);
        }
    }, [outline]);

    const handleFinish = async (values: GenerateChapterParams) => {
        if (!outline) {
            setError("请先选择或创建一个大纲。");
            return;
        }
        setIsLoading(true);
        setError(null);
        try {
            const newChapter: Chapter = await generateChapter(outline.id, {
                ...values,
                chapterNumber: currentChapter,
            });
            setOutline({
                ...outline,
                chapters: [...outline.chapters, newChapter],
            });
            setCurrentChapter(currentChapter + 1);
        } catch (err) {
            if (err instanceof Error) {
                setError(err.message);
            } else {
                setError('发生未知错误，生成章节失败。');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Spin spinning={isLoading} tip={`正在生成第 ${currentChapter} 章...`} size="large">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                <Card>
                    <Title level={4}>大纲设计</Title>
                    <Form.Item label="选择故事">
                        <Select
                            value={selectedStoryId}
                            onChange={onStoryChange}
                            placeholder="-- 请选择一个故事以查看或创建大纲 --"
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

                    {selectedStoryId && (
                        <Card>
                            <Title level={5}>历史大纲</Title>
                            {outlines.length > 0 ? (
                                <List
                                    itemLayout="horizontal"
                                    dataSource={outlines}
                                    renderItem={item => (
                                        <List.Item
                                            actions={[<Button type="link" onClick={() => onSelectOutline(item)}>选择此大纲</Button>]}
                                        >
                                            <List.Item.Meta
                                                title={item.title || `大纲 #${item.id}`}
                                                description={`创建于: ${new Date(item.created_at).toLocaleString()}`}
                                            />
                                        </List.Item>
                                    )}
                                />
                            ) : (
                                <div>
                                    <p>暂无历史大纲。</p>
                                    <Button type="primary" onClick={handleCreateEmptyOutline}>
                                        创建新大纲
                                    </Button>
                                </div>
                            )}
                        </Card>
                    )}
                </Card>

                {outline && (
                    <Card>
                        <Title level={4}>为 "{outline.title}" 生成新章节</Title>
                        <Text>当前正在生成: <b>第 {currentChapter} 章</b></Text>
                        <Divider />
                        <Form
                            form={form}
                            layout="vertical"
                            onFinish={handleFinish}
                            initialValues={{ sectionsPerChapter: 5, wordsPerSection: 800 }}
                        >
                            <Form.Item
                                name="sectionsPerChapter"
                                label="本章节数"
                                rules={[{ required: true, message: '请输入本章包含的节数！' }]}
                            >
                                <InputNumber min={1} max={20} style={{ width: '100%' }} />
                            </Form.Item>

                            <Form.Item
                                name="wordsPerSection"
                                label="每节大概字数"
                                rules={[{ required: true, message: '请输入每节的大概字数！' }]}
                            >
                                <InputNumber min={100} max={5000} step={100} style={{ width: '100%' }} />
                            </Form.Item>

                            <Form.Item>
                                <Button type="primary" htmlType="submit" style={{ width: '100%' }}>
                                    生成第 {currentChapter} 章
                                </Button>
                            </Form.Item>
                        </Form>
                    </Card>
                )}

                {error && <Alert message="操作失败" description={error} type="error" showIcon />}

                {outline && (
                    <Card>
                        <Title level={4}>当前大纲预览</Title>
                        <OutlineTreeView outline={outline} />
                    </Card>
                )}
            </div>
        </Spin>
    );
};

export default OutlineDesign;
