import React from 'react';
import {
    Form,
    Input,
    AutoComplete,
    Select,
    Button,
    Spin,
    Card,
    Typography,
    Divider,
    Alert,
    Empty,
    Row,
    Col
} from 'antd';

const { Title, Paragraph } = Typography;
const GENRE_OPTIONS = ['科幻', '奇幻', '悬疑', '恐怖', '爱情'];
const TONE_OPTIONS = ['黑暗', '幽默', '严肃', '冒险', '异想天开'];

interface ConceptionFormValues {
    idea: string;
    genre: string;
    tone: string;
    tags: string[];
}

interface StoryCard {
    id: number;
    title: string;
    synopsis: string;
    storyArc: string;
    genre: string;
    tone: string;
}

interface CharacterCard {
    id: number;
    name: string;
    synopsis: string;
    details: string;
    relationships: string;
}

interface StoryConceptionProps {
    onFinish: (values: ConceptionFormValues) => Promise<void>;
    isLoading: boolean;
    error: string;
    storyCard: StoryCard | null;
    characterCards: CharacterCard[];
    renderStoryCard: (card: StoryCard) => React.ReactNode;
    renderCharacterCard: (char: CharacterCard) => React.ReactNode;
    setIsAddCharacterModalVisible: (visible: boolean) => void;
}

const StoryConception: React.FC<StoryConceptionProps> = ({
    onFinish,
    isLoading,
    error,
    storyCard,
    characterCards,
    renderStoryCard,
    renderCharacterCard,
    setIsAddCharacterModalVisible
}) => {
    const [form] = Form.useForm();
    const [tagOptions, setTagOptions] = React.useState<{ value: string; label: string }[]>([]);

    const handleValuesChange = (_: unknown, allValues: ConceptionFormValues) => {
        if (Array.isArray(allValues.tags)) {
            const sanitized = Array.from(new Set(
                allValues.tags
                    .map(tag => (typeof tag === 'string' ? tag.trim() : ''))
                    .filter(tag => tag.length > 0)
            ));
            setTagOptions(sanitized.map(tag => ({ value: tag, label: tag })));
        } else {
            setTagOptions([]);
        }
    };

    const handleFinish = (values: ConceptionFormValues) => {
        const normalizedTags = Array.from(new Set(
            (values.tags || [])
                .map(tag => tag.trim())
                .filter(tag => tag.length > 0)
        ));

        setTagOptions(normalizedTags.map(tag => ({ value: tag, label: tag })));
        form.setFieldsValue({ ...values, tags: normalizedTags });
        onFinish({ ...values, tags: normalizedTags });
    };

    return (
        <Row gutter={24} style={{ height: '100%' }}>
            <Col span={8}>
                <Card style={{ marginBottom: 24 }}>
                    <Title level={4}>故事构思</Title>
                    <Paragraph type="secondary">打造您的下一部杰作。</Paragraph>
                    <Divider />
                    <Form
                        form={form}
                        layout="vertical"
                        onFinish={handleFinish}
                        onValuesChange={handleValuesChange}
                        initialValues={{ genre: '科幻', tone: '黑暗', tags: [] }}
                    >
                        <Form.Item
                            name="idea"
                            label="您的一句话想法"
                            rules={[{ required: true, message: '请输入您的想法！' }]}
                        >
                            <Input.TextArea
                                rows={4}
                                placeholder="例如：在一个由梦境驱动的城市里，一名侦探必须侦破一起受害者在睡梦中死亡的谋杀案。"
                            />
                        </Form.Item>
                        <Form.Item name="genre" label="类型">
                            <AutoComplete
                                options={GENRE_OPTIONS.map(g => ({ value: g }))}
                                placeholder="输入或选择类型（如：科幻）"
                                filterOption={(inputValue, option) =>
                                    (option?.value ?? '').toLowerCase().includes(inputValue.toLowerCase())
                                }
                            />
                        </Form.Item>
                        <Form.Item name="tone" label="基调">
                            <AutoComplete
                                options={TONE_OPTIONS.map(t => ({ value: t }))}
                                placeholder="输入或选择基调（如：黑暗）"
                                filterOption={(inputValue, option) =>
                                    (option?.value ?? '').toLowerCase().includes(inputValue.toLowerCase())
                                }
                            />
                        </Form.Item>
                        <Form.Item name="tags" label="标签">
                            <Select
                                mode="tags"
                                placeholder="输入标签并按回车添加，可选择已有标签"
                                options={tagOptions}
                                allowClear
                            />
                        </Form.Item>
                        <Form.Item>
                            <Button type="primary" htmlType="submit" loading={isLoading} style={{ width: '100%' }}>
                                {isLoading ? '生成中...' : '生成故事'}
                            </Button>
                        </Form.Item>
                    </Form>
                </Card>
            </Col>
            <Col span={16}>
                <Spin spinning={isLoading} tip="正在构想一个新宇宙..." size="large">
                    <div style={{ padding: 24, background: '#fff', borderRadius: '8px', height: '100%' }}>
                        {error && <Alert message="错误" description={error} type="error" showIcon closable style={{ marginBottom: 24 }} />}
                        
                        {!storyCard && !isLoading && (
                            <Empty description="从左侧生成或选择一个故事开始。" />
                        )}

                        {storyCard && (
                            <Row gutter={[24, 24]}>
                                <Col xs={24} lg={8}>
                                    {renderStoryCard(storyCard)}
                                </Col>
                                <Col xs={24} lg={16}>
                                    <Title level={4} style={{ marginBottom: 16 }}>角色</Title>
                                    <Row gutter={[16, 16]}>
                                        {characterCards.map((char) => (
                                            <Col key={char.id} xs={24} md={12}>
                                                {renderCharacterCard(char)}
                                            </Col>
                                        ))}
                                        <Col xs={24} md={12}>
                                            <Button type="dashed" onClick={() => setIsAddCharacterModalVisible(true)} style={{ width: '100%', height: '100%', minHeight: '150px' }}>
                                                + 添加新角色
                                            </Button>
                                        </Col>
                                    </Row>
                                </Col>
                            </Row>
                        )}
                    </div>
                </Spin>
            </Col>
        </Row>
    );
};

export default StoryConception;
