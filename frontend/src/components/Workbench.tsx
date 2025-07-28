import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Layout,
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
    List
} from 'antd';
import OutlineDesign from './OutlineDesign';
import ManuscriptWriter from './ManuscriptWriter';
import StoryConception from './StoryConception';
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import type { Outline, StoryCard, CharacterCard, ConceptionFormValues } from '../types';
import Icon from '@ant-design/icons';
import { useStoryData } from '../hooks/useStoryData';
import { useOutlineData } from '../hooks/useOutlineData';
import { useRefineModal } from '../hooks/useRefineModal';
import AddCharacterModal from './modals/AddCharacterModal';
import EditStoryCardModal from './modals/EditStoryCardModal';
import EditCharacterCardModal from './modals/EditCharacterCardModal';
import EditOutlineModal from './modals/EditOutlineModal';
import RefineModal from './modals/RefineModal';

const SparklesSvg = () => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" fill="currentColor" width="1em" height="1em">
        <path d="M512 900.7c-26.5 0-48-21.5-48-48V640c0-26.5 21.5-48 48-48s48 21.5 48 48v212.7c0 26.5-21.5 48-48 48zM221.3 550.7c-18.7 18.7-49.1 18.7-67.9 0s-18.7-49.1 0-67.9l150.4-150.4c18.7-18.7 49.1-18.7 67.9 0s18.7 49.1 0 67.9L221.3 550.7zM802.7 550.7L652.3 400.3c-18.7-18.7-18.7-49.1 0-67.9s49.1-18.7 67.9 0l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0zM512 432c-26.5 0-48-21.5-48-48V171.3c0-26.5 21.5-48 48-48s48 21.5 48 48V384c0 26.5-21.5 48-48 48zM123.3 221.3c-18.7-18.7-18.7-49.1 0-67.9s49.1-18.7 67.9 0l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0L123.3 221.3zM868.6 221.3l-150.4-150.4c-18.7-18.7-49.1-18.7-67.9 0s-18.7 49.1 0 67.9l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0z" />
    </svg>
);
const SparklesIcon = (props: React.ComponentProps<typeof Icon>) => <Icon component={SparklesSvg} {...props} />;

const { Header, Content } = Layout;
const { Title, Paragraph } = Typography;
const { Option } = Select;
const { TabPane } = Tabs;
// Type definitions are now in src/types.ts

const Workbench = () => {
    const { tab } = useParams<{ tab: string }>();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState(tab || "story-conception");
    
    // State Management Hooks
    const {
        storyList,
        selectedStory: storyCard,
        characterCards,
        isLoading: isStoryLoading,
        error: storyError,
        loadStoryList,
        viewStory,
        generateStory,
        updateStory,
        createCharacter,
        updateCharacter,
        deleteCharacter,
        setSelectedStory: setStoryCard,
        setCharacterCards,
    } = useStoryData();

    const [selectedStoryForOutline, setSelectedStoryForOutline] = useState<string | null>(null);

    const {
        outlines: outlinesForStory,
        selectedOutline,
        isLoading: isOutlineLoading,
        error: outlineError,
        loadOutlines,
        updateOutline,
        deleteOutline,
        selectOutline,
        getOutlineForWriting,
        setSelectedOutline,
        setIsLoading: setOutlineLoading,
        setError: setOutlineError,
    } = useOutlineData(selectedStoryForOutline);
    
    const {
        isModalVisible: isRefineModalVisible,
        originalText,
        openModal: handleOpenRefineModal,
        closeModal: closeRefineModal,
        acceptRefinement: handleAcceptRefinement,
        context,
    } = useRefineModal();

    // Local UI State
    const [editingStory, setEditingStory] = useState<StoryCard | null>(null);
    const [editingCharacter, setEditingCharacter] = useState<CharacterCard | null>(null);
    const [isAddCharacterModalVisible, setIsAddCharacterModalVisible] = useState(false);
    const [editingOutline, setEditingOutline] = useState<Outline | null>(null);
    const [outlineForWriting, setOutlineForWriting] = useState<Outline | null>(null);
    const [selectedSceneId, setSelectedSceneId] = useState<number | null>(null);
    const [error, setError] = useState(''); // General error, can be deprecated if hooks handle all errors

    useEffect(() => {
        loadStoryList();
    }, [loadStoryList]);

    useEffect(() => {
        if (tab && tab !== activeTab) {
            setActiveTab(tab);
        }
    }, [tab, activeTab]);

    useEffect(() => {
        if (selectedStoryForOutline) {
            loadOutlines();
        }
    }, [selectedStoryForOutline, loadOutlines]);

    const handleViewStory = async (storyId: number) => {
        const story = await viewStory(storyId);
        if (story) {
            setSelectedStoryForOutline(story.id.toString());
            navigate('/workbench/story-details');
        }
    };

    const onFinish = async (values: ConceptionFormValues) => {
        const newStory = await generateStory(values);
        if (newStory) {
            setSelectedStoryForOutline(newStory.id.toString());
            setActiveTab("story-details");
        }
    };

    const handleUpdateStoryCard = async () => {
        if (!editingStory) return;
        await updateStory(editingStory);
        setEditingStory(null);
    };

    const handleStoryChangeInOutlineTabs = (storyId: string) => {
        setSelectedStoryForOutline(storyId);
        selectOutline(null); // Also clear selected outline
    };

    const handleSelectOutline = (outline: Outline) => {
        selectOutline(outline);
    };

    // This handler is now obsolete with the new per-chapter generation flow.
    // const handleGenerateOutline = async (params: { numberOfChapters: number; pointOfView: string; }) => {
    //     await generateOutline(params);
    // };


    const handleUpdateOutline = async () => {
        if (!editingOutline) return;
        await updateOutline(editingOutline);
        setEditingOutline(null);
    };

    const handleDeleteOutline = async (outlineId: number) => {
        if (window.confirm('您确定要删除此大纲吗？这将无法恢复。')) {
            await deleteOutline(outlineId);
        }
    };

    const handleUpdateCharacterCard = async () => {
        if (!editingCharacter) return;
        await updateCharacter(editingCharacter);
        setEditingCharacter(null);
    };

    const handleCreateCharacter = async (characterData: Omit<CharacterCard, 'id'>) => {
        if (!storyCard) return;
        await createCharacter(storyCard.id, characterData);
        setIsAddCharacterModalVisible(false);
    };

    const renderStoryCard = (card: StoryCard) => (
        <Card
            title={<Input value={card.title} onChange={e => setEditingStory({ ...card, title: e.target.value })} />}
            extra={<Button onClick={() => setEditingStory(card)}>编辑</Button>}
            actions={[
                <Button type="primary" onClick={() => {
                    setSelectedStoryForOutline(card.id.toString());
                    navigate('/workbench/outline-design');
                }}>
                    设计大纲
                </Button>
            ]}
        >
            <div style={{ position: 'relative' }}>
                <Title level={5}>概要</Title>
                <Input.TextArea value={card.synopsis} autoSize onChange={e => setStoryCard({ ...card, synopsis: e.target.value })} />
                <Button icon={<SparklesIcon />} size="small" style={{ position: 'absolute', top: 0, right: 0 }} onClick={() => handleOpenRefineModal(card.synopsis, { endpoint: `/api/v1/story-cards/${card.id}/refine`, fieldName: 'synopsis', onSuccess: (newText) => setStoryCard({ ...card, synopsis: newText }) })} />
            </div>
            <Divider />
            <div style={{ position: 'relative' }}>
                <Title level={5}>故事弧线</Title>
                <Input.TextArea value={card.storyArc} autoSize onChange={e => setStoryCard({ ...card, storyArc: e.target.value })} />
                <Button icon={<SparklesIcon />} size="small" style={{ position: 'absolute', top: 0, right: 0 }} onClick={() => handleOpenRefineModal(card.storyArc, { endpoint: `/api/v1/story-cards/${card.id}/refine`, fieldName: 'storyArc', onSuccess: (newText) => setStoryCard({ ...card, storyArc: newText }) })} />
            </div>
        </Card>
    );

    const handleDeleteCharacter = async (characterId: number) => {
        if (window.confirm('您确定要删除此角色吗？')) {
            await deleteCharacter(characterId);
        }
    };

    const handleStartWriting = async (outlineId: number) => {
        const outline = await getOutlineForWriting(outlineId);
        if (outline) {
            setOutlineForWriting(outline);
            navigate('/workbench/manuscript-writer');
        }
    };

    const handleEditOutline = async (outlineId: number) => {
        const outline = await getOutlineForWriting(outlineId); // Can reuse the same fetch logic
        if (outline) {
            setEditingOutline(outline);
        }
    };



    const renderCharacterCard = (char: CharacterCard) => (
        <Card
            title={<Input value={char.name} onChange={e => setEditingCharacter({ ...char, name: e.target.value })} />}
            actions={[
                <Button type="primary" ghost onClick={() => setEditingCharacter(char)}>编辑</Button>,
                <Button danger onClick={() => handleDeleteCharacter(char.id)}>删除</Button>
            ]}
        >
            <div style={{ position: 'relative' }}>
                <Title level={5}>概要</Title>
                <Input.TextArea value={char.synopsis} autoSize onChange={e => setCharacterCards(prev => prev.map(c => c.id === char.id ? { ...c, synopsis: e.target.value } : c))} />
                <Button icon={<SparklesIcon />} size="small" style={{ position: 'absolute', top: 0, right: 0 }} onClick={() => handleOpenRefineModal(char.synopsis, { endpoint: `/api/v1/character-cards/${char.id}/refine`, fieldName: 'synopsis', onSuccess: (newText) => setCharacterCards(prev => prev.map(c => c.id === char.id ? { ...c, synopsis: newText } : c)) })} />
            </div>
            <Divider />
            <div style={{ position: 'relative' }}>
                <Title level={5}>详情</Title>
                <Input.TextArea value={char.details} autoSize onChange={e => setCharacterCards(prev => prev.map(c => c.id === char.id ? { ...c, details: e.target.value } : c))} />
                <Button icon={<SparklesIcon />} size="small" style={{ position: 'absolute', top: 0, right: 0 }} onClick={() => handleOpenRefineModal(char.details, { endpoint: `/api/v1/character-cards/${char.id}/refine`, fieldName: 'details', onSuccess: (newText) => setCharacterCards(prev => prev.map(c => c.id === char.id ? { ...c, details: newText } : c)) })} />
            </div>
            <Divider />
            <div style={{ position: 'relative' }}>
                <Title level={5}>人物关系</Title>
                <Input.TextArea value={char.relationships} autoSize onChange={e => setCharacterCards(prev => prev.map(c => c.id === char.id ? { ...c, relationships: e.target.value } : c))} />
                <Button icon={<SparklesIcon />} size="small" style={{ position: 'absolute', top: 0, right: 0 }} onClick={() => handleOpenRefineModal(char.relationships, { endpoint: `/api/v1/character-cards/${char.id}/refine`, fieldName: 'relationships', onSuccess: (newText) => setCharacterCards(prev => prev.map(c => c.id === char.id ? { ...c, relationships: newText } : c)) })} />
            </div>
        </Card>
    );

    const handleTabChange = (key: string) => {
        setActiveTab(key);
        navigate(`/workbench/${key}`);
    };

    return (
        <Layout style={{ minHeight: '100vh' }}>
            <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title level={3} style={{ margin: 0 }}>AI 小说家工作台</Title>
            </Header>
            <Content style={{ padding: '24px', margin: 0, background: '#f0f2f5' }}>
                <Tabs activeKey={activeTab} onChange={handleTabChange} type="card" style={{ height: '100%' }}>
                    <TabPane tab="故事构思" key="story-conception">
                        <StoryConception
                            onFinish={onFinish}
                            isLoading={isStoryLoading}
                            error={error}
                            storyCard={storyCard}
                            characterCards={characterCards}
                            renderStoryCard={renderStoryCard}
                            renderCharacterCard={renderCharacterCard}
                            setIsAddCharacterModalVisible={setIsAddCharacterModalVisible}
                        />
                    </TabPane>
                    <TabPane tab="故事列表" key="story-list">
                        <Card title="故事列表">
                            <Spin spinning={isStoryLoading}>
                                    <List
                                        itemLayout="horizontal"
                                        dataSource={storyList}
                                        renderItem={item => (
                                            <List.Item
                                                actions={[
                                                    <Button 
                                                        type="link" 
                                                        onClick={() => {
                                                            handleViewStory(item.id);
                                                        }}
                                                    >
                                                        查看
                                                    </Button>
                                                ]}
                                            >
                                                <List.Item.Meta
                                                    title={item.title}
                                                    description={`类型: ${item.genre} | 基调: ${item.tone}`}
                                                />
                                            </List.Item>
                                        )}
                                    />
                            </Spin>
                        </Card>
                    </TabPane>
                    <TabPane tab="故事详情" key="story-details">
                        <Spin spinning={isStoryLoading} tip="正在加载故事..." size="large">
                            <div style={{ padding: 24, background: '#fff', borderRadius: '8px', height: '100%' }}>
                                {storyError && <Alert message="错误" description={storyError} type="error" showIcon closable onClose={() => setError('')} style={{ marginBottom: 24 }} />}
                                
                                {!storyCard && !isStoryLoading && (
                                    <Empty description="请从“故事列表”中选择一个故事查看详情。" />
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
                    </TabPane>
                    <TabPane tab="大纲设计" key="outline-design">
                        <div style={{ padding: 24, background: '#fff', borderRadius: '8px' }}>
                            <OutlineDesign
                                storyCards={storyList}
                                selectedStoryId={selectedStoryForOutline}
                                onStoryChange={handleStoryChangeInOutlineTabs}
                                isLoading={isOutlineLoading}
                                setIsLoading={setOutlineLoading}
                                outline={selectedOutline}
                                setOutline={setSelectedOutline}
                                error={outlineError}
                                setError={setOutlineError}
                                outlines={outlinesForStory}
                                onSelectOutline={handleSelectOutline}
                                fetchOutlines={loadOutlines}
                            />
                        </div>
                    </TabPane>
                    <TabPane tab="大纲管理" key="outline-management">
                        <div style={{ padding: 24, background: '#fff', borderRadius: '8px' }}>
                            <Title level={4}>选择一个故事以管理其大纲</Title>
                            <Select
                                showSearch
                                style={{ width: '100%', marginBottom: 24 }}
                                placeholder="选择故事"
                                value={selectedStoryForOutline}
                                onChange={handleStoryChangeInOutlineTabs}
                                filterOption={(input, option) =>
                                    (option?.children as unknown as string)?.toLowerCase().includes(input.toLowerCase())
                                }
                            >
                                {storyList.map(story => (
                                    <Option key={story.id} value={story.id.toString()}>{story.title}</Option>
                                ))}
                            </Select>
                            <Button 
                                type="primary" 
                                icon={<PlusOutlined />} 
                                onClick={() => setActiveTab("outline-design")}
                                disabled={!selectedStoryForOutline}
                                style={{marginBottom: 24}}
                            >
                                为当前故事创建新大纲
                            </Button>
                            <Spin spinning={isOutlineLoading}>
                                <List
                                    grid={{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4 }}
                                    dataSource={outlinesForStory}
                                    renderItem={item => (
                                        <List.Item>
                                            <Card
                                                title={item.title}
                                                actions={[
                                                    <Button type="primary" icon={<EyeOutlined />} onClick={() => handleStartWriting(item.id)}>创作</Button>,
                                                    <Button icon={<EditOutlined />} onClick={() => handleEditOutline(item.id)}>编辑</Button>,
                                                    <Button danger icon={<DeleteOutlined />} onClick={() => handleDeleteOutline(item.id)}>删除</Button>,
                                                ]}
                                            >
                                                <Paragraph>视角: {item.pointOfView}</Paragraph>
                                                <Paragraph>章节数: {item.chapters.length}</Paragraph>
                                            </Card>
                                        </List.Item>
                                    )}
                                />
                            </Spin>
                        </div>
                    </TabPane>
                    <TabPane tab="故事创作" key="manuscript-writer">
                        <div style={{ padding: 24, background: '#fff', borderRadius: '8px', height: '100%' }}>
                            <ManuscriptWriter 
                                selectedOutline={outlineForWriting}
                                selectedSceneId={selectedSceneId}
                                onSelectScene={setSelectedSceneId}
                            />
                        </div>
                    </TabPane>
                </Tabs>
            </Content>
            <EditStoryCardModal
                open={!!editingStory}
                onOk={handleUpdateStoryCard}
                onCancel={() => setEditingStory(null)}
                confirmLoading={isStoryLoading}
                storyCard={editingStory}
            />
            <EditCharacterCardModal
                open={!!editingCharacter}
                onOk={handleUpdateCharacterCard}
                onCancel={() => setEditingCharacter(null)}
                confirmLoading={isStoryLoading}
                characterCard={editingCharacter}
            />
            <EditOutlineModal
                open={!!editingOutline}
                onOk={handleUpdateOutline}
                onCancel={() => setEditingOutline(null)}
                confirmLoading={isOutlineLoading}
                outline={editingOutline}
            />
            <AddCharacterModal
                open={isAddCharacterModalVisible}
                onOk={handleCreateCharacter}
                onCancel={() => setIsAddCharacterModalVisible(false)}
                confirmLoading={isStoryLoading}
            />
            <RefineModal
                open={isRefineModalVisible}
                onCancel={closeRefineModal}
                originalText={originalText}
                contextType={context?.fieldName || ''}
                onRefined={handleAcceptRefinement}
            />
        </Layout>
    );
};

export default Workbench;
