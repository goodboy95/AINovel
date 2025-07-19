import { useState, useEffect } from 'react';
import {
    Layout,
    Form,
    Input,
    Select,
    Button,
    Spin,
    Card,
    Row,
    Col,
    Typography,
    Alert,
    Divider,
    Empty,
    Tabs,
    List,
    Modal
} from 'antd';

const { Header, Content, Sider } = Layout;
const { Title, Paragraph } = Typography;
const { Option } = Select;
const { TabPane } = Tabs;

// Define types for the data structures
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

interface ConceptionFormValues {
    idea: string;
    genre: string;
    tone: string;
}

const StoryConception = () => {
    const [form] = Form.useForm();
    const [storyCard, setStoryCard] = useState<StoryCard | null>(null);
    const [characterCards, setCharacterCards] = useState<CharacterCard[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [storyList, setStoryList] = useState<StoryCard[]>([]);
    const [isListLoading, setIsListLoading] = useState(false);
    const [editingStory, setEditingStory] = useState<StoryCard | null>(null);
    const [editingCharacter, setEditingCharacter] = useState<CharacterCard | null>(null);

    useEffect(() => {
        fetchStoryList();
    }, []);

    const fetchStoryList = async () => {
        setIsListLoading(true);
        try {
            const token = localStorage.getItem('token');
            const headers: HeadersInit = { 'Content-Type': 'application/json' };
            if (token) headers['Authorization'] = `Bearer ${token}`;

            const response = await fetch('/api/v1/story-cards', { headers });
            if (!response.ok) throw new Error('获取故事列表失败');
            
            const data = await response.json();
            setStoryList(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : '发生未知错误。');
        } finally {
            setIsListLoading(false);
        }
    };

    const onFinish = async (values: ConceptionFormValues) => {
        setIsLoading(true);
        setError('');
        setStoryCard(null);
        setCharacterCards([]);

        try {
            const token = localStorage.getItem('token');
            const headers: HeadersInit = {
                'Content-Type': 'application/json',
            };
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const response = await fetch('/api/v1/conception', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(values),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '生成故事失败。');
            }

            const data = await response.json();
            setStoryCard(data.storyCard);
            setCharacterCards(data.characterCards);
            await fetchStoryList(); // Refresh the list after generating a new story
        } catch (err) {
            setError(err instanceof Error ? err.message : '发生未知错误。');
        } finally {
            setIsLoading(false);
        }
    };

    const handleUpdateStoryCard = async () => {
        if (!editingStory) return;
        setIsLoading(true);
        try {
            const token = localStorage.getItem('token');
            const headers: HeadersInit = { 'Content-Type': 'application/json' };
            if (token) headers['Authorization'] = `Bearer ${token}`;

            const response = await fetch(`/api/v1/story-cards/${editingStory.id}`, {
                method: 'PUT',
                headers,
                body: JSON.stringify(editingStory),
            });

            if (!response.ok) throw new Error('更新故事卡失败');
            
            setStoryCard(await response.json());
            setEditingStory(null);
            await fetchStoryList();
        } catch (err) {
            setError(err instanceof Error ? err.message : '发生未知错误。');
        } finally {
            setIsLoading(false);
        }
    };

    const handleUpdateCharacterCard = async () => {
        if (!editingCharacter) return;
        setIsLoading(true);
        try {
            const token = localStorage.getItem('token');
            const headers: HeadersInit = { 'Content-Type': 'application/json' };
            if (token) headers['Authorization'] = `Bearer ${token}`;

            const response = await fetch(`/api/v1/character-cards/${editingCharacter.id}`, {
                method: 'PUT',
                headers,
                body: JSON.stringify(editingCharacter),
            });

            if (!response.ok) throw new Error('更新角色卡失败');
            
            const updatedChar = await response.json();
            setCharacterCards(prev => prev.map(c => c.id === updatedChar.id ? updatedChar : c));
            setEditingCharacter(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : '发生未知错误。');
        } finally {
            setIsLoading(false);
        }
    };

    const renderStoryCard = (card: StoryCard) => (
        <Card title={<Input value={card.title} onChange={e => setEditingStory({...card, title: e.target.value})} />} extra={<Button onClick={() => setEditingStory(card)}>编辑</Button>}>
            <Title level={5}>概要</Title>
            <Input.TextArea value={card.synopsis} autoSize onChange={e => setEditingStory({...card, synopsis: e.target.value})} />
            <Divider />
            <Title level={5}>故事弧线</Title>
            <Input.TextArea value={card.storyArc} autoSize onChange={e => setEditingStory({...card, storyArc: e.target.value})} />
        </Card>
    );

    const renderCharacterCard = (char: CharacterCard) => (
         <Card title={<Input value={char.name} onChange={e => setEditingCharacter({...char, name: e.target.value})} />} extra={<Button onClick={() => setEditingCharacter(char)}>编辑</Button>}>
            <Title level={5}>概要</Title>
            <Input.TextArea value={char.synopsis} autoSize onChange={e => setEditingCharacter({...char, synopsis: e.target.value})} />
            <Divider />
            <Title level={5}>详情</Title>
            <Input.TextArea value={char.details} autoSize onChange={e => setEditingCharacter({...char, details: e.target.value})} />
            <Divider />
            <Title level={5}>人物关系</Title>
            <Input.TextArea value={char.relationships} autoSize onChange={e => setEditingCharacter({...char, relationships: e.target.value})} />
        </Card>
    );

    return (
        <Layout style={{ minHeight: '100vh' }}>
            <Sider width={350} theme="light" style={{ padding: '24px', borderRight: '1px solid #f0f0f0' }}>
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
            </Sider>
            <Layout>
                <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }}>
                    <Title level={3} style={{ margin: '16px 0' }}>AI 小说家工作台</Title>
                </Header>
                <Content style={{ padding: '24px', margin: 0, minHeight: 280, background: '#f0f2f5' }}>
                    <Tabs defaultActiveKey="1">
                        <TabPane tab="创作" key="1">
                            <Spin spinning={isLoading} tip="正在构想一个新宇宙..." size="large">
                                <div style={{ padding: 24, background: '#fff', borderRadius: '8px' }}>
                                    {error && <Alert message="错误" description={error} type="error" showIcon closable onClose={() => setError('')} style={{ marginBottom: 24 }} />}
                                    
                                    {!storyCard && !isLoading && (
                                        <Empty description="您生成的故事将显示在这里。让我们一起创造一些惊人的东西吧！" />
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
                                                </Row>
                                            </Col>
                                        </Row>
                                    )}
                                </div>
                            </Spin>
                        </TabPane>
                        <TabPane tab="管理" key="2">
                            <Spin spinning={isListLoading}>
                                <List
                                    itemLayout="horizontal"
                                    dataSource={storyList}
                                    renderItem={item => (
                                        <List.Item
                                            actions={[<Button type="link" onClick={() => { setStoryCard(item); /* TODO: fetch characters */ }}>查看</Button>]}
                                        >
                                            <List.Item.Meta
                                                title={<a href="#">{item.title}</a>}
                                                description={`类型: ${item.genre} | 基调: ${item.tone}`}
                                            />
                                        </List.Item>
                                    )}
                                />
                            </Spin>
                        </TabPane>
                    </Tabs>
                </Content>
            </Layout>
            <Modal
                title="编辑故事卡"
                visible={!!editingStory}
                onOk={handleUpdateStoryCard}
                onCancel={() => setEditingStory(null)}
                confirmLoading={isLoading}
            >
                {editingStory && renderStoryCard(editingStory)}
            </Modal>
            <Modal
                title="编辑角色卡"
                visible={!!editingCharacter}
                onOk={handleUpdateCharacterCard}
                onCancel={() => setEditingCharacter(null)}
                confirmLoading={isLoading}
            >
                {editingCharacter && renderCharacterCard(editingCharacter)}
            </Modal>
        </Layout>
    );
};

export default StoryConception;
