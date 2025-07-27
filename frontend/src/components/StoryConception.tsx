import React from 'react';
import {
    Form,
    Input,
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
const { Option } = Select;

interface ConceptionFormValues {
    idea: string;
    genre: string;
    tone: string;
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
                        onFinish={onFinish}
                        initialValues={{ genre: 'Sci-Fi', tone: 'Dark' }}
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
                            <Select>
                                <Option value="Sci-Fi">科幻</Option>
                                <Option value="Fantasy">奇幻</Option>
                                <Option value="Mystery">悬疑</Option>
                                <Option value="Horror">恐怖</Option>
                                <Option value="Romance">爱情</Option>
                            </Select>
                        </Form.Item>
                        <Form.Item name="tone" label="基调">
                            <Select>
                                <Option value="Dark">黑暗</Option>
                                <Option value="Humorous">幽默</Option>
                                <Option value="Serious">严肃</Option>
                                <Option value="Adventurous">冒险</Option>
                                <Option value="Whimsical">异想天开</Option>
                            </Select>
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
