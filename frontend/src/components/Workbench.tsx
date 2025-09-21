import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Layout,
    Input,
    Button,
    Spin,
    Card,
    Row,
    Col,
    Typography,
    Alert,
    Divider,
    Empty,
    Tabs
} from 'antd';
import OutlinePage from './OutlinePage';
import ManuscriptWriter from './ManuscriptWriter';
import StoryList from './StoryList';
import StoryDetail from './StoryDetail';
import StoryConception from './StoryConception';
import type { Outline, StoryCard, CharacterCard, ConceptionFormValues } from '../types';
import Icon from '@ant-design/icons';
import { useStoryData } from '../hooks/useStoryData';
import { useOutlineData } from '../hooks/useOutlineData';
import { deleteStory as apiDeleteStory } from '../services/api';
import { useRefineModal } from '../hooks/useRefineModal';
import AddCharacterModal from './modals/AddCharacterModal';
import EditStoryCardModal from './modals/EditStoryCardModal';
import EditCharacterCardModal from './modals/EditCharacterCardModal';
import EditOutlineModal from './modals/EditOutlineModal';
import RefineModal from './modals/RefineModal';
import WorkbenchHeader from './WorkbenchHeader';
import { useAuth } from '../contexts/AuthContext';

const SparklesSvg = () => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" fill="currentColor" width="1em" height="1em">
        <path d="M512 900.7c-26.5 0-48-21.5-48-48V640c0-26.5 21.5-48 48-48s48 21.5 48 48v212.7c0 26.5-21.5 48-48 48zM221.3 550.7c-18.7 18.7-49.1 18.7-67.9 0s-18.7-49.1 0-67.9l150.4-150.4c18.7-18.7 49.1-18.7 67.9 0s18.7 49.1 0 67.9L221.3 550.7zM802.7 550.7L652.3 400.3c-18.7-18.7-18.7-49.1 0-67.9s49.1-18.7 67.9 0l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0zM512 432c-26.5 0-48-21.5-48-48V171.3c0-26.5 21.5-48 48-48s48 21.5 48 48V384c0 26.5-21.5 48-48 48zM123.3 221.3c-18.7-18.7-18.7-49.1 0-67.9s49.1-18.7 67.9 0l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0L123.3 221.3zM868.6 221.3l-150.4-150.4c-18.7-18.7-49.1-18.7-67.9 0s-18.7 49.1 0 67.9l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0z" />
    </svg>
);
const SparklesIcon = (props: React.ComponentProps<typeof Icon>) => <Icon component={SparklesSvg} {...props} />;

const { Content } = Layout;
const { Title } = Typography;
const { TabPane } = Tabs;
// Type definitions are now in src/types.ts

const Workbench = () => {
    const { tab } = useParams<{ tab: string }>();
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const normalizeTab = (t?: string) => {
        if (!t) return "story-conception";
        if (t === "outline-design" || t === "outline-management") return "outline-workspace";
        return t;
    };

    const [activeTab, setActiveTab] = useState(normalizeTab(tab));
    
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
        createStory,
        createCharacter,
        updateCharacter,
        deleteCharacter,
        setSelectedStory: setStoryCard,
        setCharacterCards,
    } = useStoryData();

    const [selectedStoryForOutline, setSelectedStoryForOutline] = useState<string | null>(null);
    const [selectedStoryForMgmt, setSelectedStoryForMgmt] = useState<number | 'new' | null>(null);

    const {
        outlines: outlinesForStory,
        selectedOutline,
        isLoading: isOutlineLoading,
        error: outlineError,
        loadOutlines,
        updateOutline,
        selectOutline,
        updateLocalOutline,
        setSelectedOutline,
        setIsLoading: setOutlineLoading,
        setError: setOutlineError,
        deleteOutline,
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
    const [selectedSceneId, setSelectedSceneId] = useState<number | null>(null);
    const [error, setError] = useState(''); // General error, can be deprecated if hooks handle all errors

    const [didLoadStories, setDidLoadStories] = useState(false);
    useEffect(() => {
        if (!didLoadStories) {
            setDidLoadStories(true);
            loadStoryList();
        }
    }, [didLoadStories, loadStoryList]);

    useEffect(() => {
        const next = normalizeTab(tab);
        if (next !== activeTab) {
            setActiveTab(next);
            if (tab !== next) {
                navigate(`/workbench/${next}`, { replace: true });
            }
        }
    }, [tab, activeTab, navigate]);

    useEffect(() => {
        if (selectedStoryForOutline) {
            loadOutlines();
        }
    }, [selectedStoryForOutline, loadOutlines]);


    const onFinish = async (values: ConceptionFormValues) => {
        const newStory = await generateStory(values);
        if (newStory) {
            setSelectedStoryForOutline(newStory.id.toString());
            setSelectedStoryForMgmt(newStory.id);
            setActiveTab("story-management");
            navigate('/workbench/story-management', { replace: true });
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
                    navigate('/workbench/outline-workspace');
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

    const handleSelectStoryFromList = async (id: number | 'new') => {
        if (id === 'new') {
            setSelectedStoryForMgmt('new');
            setStoryCard(null);
            return;
        }
        setSelectedStoryForMgmt(id);
        await viewStory(id);
    };
    
    /**
     * 删除故事（含确认与刷新）
     */
    const handleDeleteStory = async (id: number) => {
        try {
            await apiDeleteStory(id);
            // 如果当前选中的是被删除的故事，清空右侧详情与选中态
            if (selectedStoryForMgmt === id) {
                setSelectedStoryForMgmt(null);
                setStoryCard(null);
            }
            if (selectedStoryForOutline === id.toString()) {
                setSelectedStoryForOutline(null);
            }
            await loadStoryList();
        } catch (e) {
            // 错误已在 api 层抛出，控制台打印即可
            console.error(e);
        }
    };

    const handleAfterCreateStory = (newStory: StoryCard) => {
        setSelectedStoryForMgmt(newStory.id);
        setSelectedStoryForOutline(newStory.id.toString());
    };

    return (
        <Layout style={{ minHeight: '100vh' }}>
            <WorkbenchHeader
                username={user?.username}
                onOpenSettings={() => navigate('/settings')}
                onLogout={() => { logout(); navigate('/'); }}
            />
            <Content style={{ padding: '24px', margin: 0, background: '#f0f2f5' }}>
                <Tabs destroyInactiveTabPane activeKey={activeTab} onChange={handleTabChange} type="card" style={{ height: '100%' }}>
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
                    <TabPane tab="故事管理" key="story-management">
                        <div style={{ padding: 24, background: '#fff', borderRadius: '8px' }}>
                            {storyError && <Alert message="错误" description={storyError} type="error" showIcon closable onClose={() => setError('')} style={{ marginBottom: 24 }} />}
                            <Row gutter={24}>
                                <Col xs={24} lg={8}>
                                    <StoryList
                                        stories={storyList}
                                        selectedId={selectedStoryForMgmt}
                                        onSelect={handleSelectStoryFromList}
                                        loading={isStoryLoading}
                                        onDelete={handleDeleteStory}
                                    />
                                </Col>
                                <Col xs={24} lg={16}>
                                    <Spin spinning={isStoryLoading} tip="正在加载..." size="large">
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                                            {selectedStoryForMgmt === 'new' && (
                                                <StoryDetail
                                                    mode="create"
                                                    story={null}
                                                    loading={isStoryLoading}
                                                    onCreate={createStory}
                                                    onUpdate={() => {}}
                                                    onAfterCreate={handleAfterCreateStory}
                                                />
                                            )}
                                            {selectedStoryForMgmt !== 'new' && storyCard && (
                                                <>
                                                    <StoryDetail
                                                        mode="edit"
                                                        story={storyCard}
                                                        loading={isStoryLoading}
                                                        onCreate={createStory}
                                                        onUpdate={updateStory}
                                                    />
                                                    <Card>
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
                                                    </Card>
                                                </>
                                            )}
                                            {!selectedStoryForMgmt && (
                                                <Empty description="请选择左侧故事或点击“新建故事”" />
                                            )}
                                        </div>
                                    </Spin>
                                </Col>
                            </Row>
                        </div>
                    </TabPane>
                    <TabPane tab="大纲工作台" key="outline-workspace">
                        <div style={{ padding: 24, background: '#fff', borderRadius: '8px' }}>
                            <OutlinePage
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
                                loadOutlines={loadOutlines}
                                updateLocal={updateLocalOutline}
                                updateOutlineRemote={updateOutline}
                                onDeleteOutline={deleteOutline}
                            />
                        </div>
                    </TabPane>
                    <TabPane tab="小说创作" key="manuscript-writer">
                        <div style={{ padding: 24, background: '#fff', borderRadius: '8px', height: '100%' }}>
                            <ManuscriptWriter
                                storyId={selectedStoryForOutline}
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
