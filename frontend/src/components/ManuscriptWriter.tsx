import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Card,
  Input,
  Button,
  message,
  Empty,
  Layout,
  Spin,
  Row,
  Col,
  List,
  Select,
  Modal,
  Typography,
  Space,
} from 'antd';
import OutlineTreeView from './OutlineTreeView';
import AiRefineButton from './AiRefineButton';
import RefineModal from './modals/RefineModal';
import type { Outline, ManuscriptSection, StoryCard, Manuscript, CharacterCard, CharacterChangeLog } from '../types';
import { useOutlineData } from '../hooks/useOutlineData';
import {
  fetchStories,
  fetchStoryDetails,
  fetchManuscriptsForOutline,
  fetchManuscriptWithSections,
  createManuscript,
  deleteManuscript as apiDeleteManuscript,
  analyzeCharacterChanges,
  fetchCharacterChangeLogs,
} from '../services/api';
import CharacterStatusSidebar from './CharacterStatusSidebar';

const { Content } = Layout;
const { TextArea } = Input;
const { Text } = Typography;

export interface ManuscriptWriterProps {
  storyId: string | null;
  selectedSceneId: number | null;
  onSelectScene: (sceneId: number | null) => void;
}

const getSectionSceneId = (s: ManuscriptSection): number | undefined =>
  (s.scene && s.scene.id) !== undefined ? s.scene!.id : s.sceneId;

const ManuscriptWriter: React.FC<ManuscriptWriterProps> = ({
  storyId,
  selectedSceneId,
  onSelectScene,
}) => {
  // 顶部下拉：故事列表
  const [stories, setStories] = useState<StoryCard[]>([]);
  const [selectedStoryId, setSelectedStoryId] = useState<string | null>(storyId);
  const [characterMap, setCharacterMap] = useState<Record<number, CharacterCard>>({});

  // 大纲数据（依赖所选故事）
  const {
    outlines,
    selectedOutline,
    selectOutline,
    getOutlineForWriting,
    loadOutlines,
  } = useOutlineData(selectedStoryId);

  // 详情与小说稿件
  const [selectedOutlineDetail, setSelectedOutlineDetail] = useState<Outline | null>(null);
  const [manuscriptMap, setManuscriptMap] = useState<Record<number, ManuscriptSection>>({});
  const [manuscriptContent, setManuscriptContent] = useState('');
  const [isRefineModalOpen, setIsRefineModalOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isFetchingManuscript, setIsFetchingManuscript] = useState(false);
  const [isLoadingOutlineDetail, setIsLoadingOutlineDetail] = useState(false);

  const [manuscripts, setManuscripts] = useState<Manuscript[]>([]);
  const [selectedManuscript, setSelectedManuscript] = useState<Manuscript | null>(null);
  const [characterChangeLogs, setCharacterChangeLogs] = useState<Record<number, CharacterChangeLog[]>>({});
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  // 初始化故事列表
  useEffect(() => {
    const init = async () => {
      try {
        const list = await fetchStories();
        setStories(list);
      } catch (e) {
        console.error(e);
        message.error('获取故事列表失败');
      }
    };
    init();
  }, []);

  // 同步父层传入的 storyId
  useEffect(() => {
    setSelectedStoryId(storyId);
  }, [storyId]);

  // 切换故事后清理状态并加载对应大纲
  useEffect(() => {
    if (selectedStoryId) {
      const storyNumericId = Number(selectedStoryId);
      loadOutlines();
      loadStoryCharacters(storyNumericId);
      selectOutline(null);
      setSelectedOutlineDetail(null);
      setManuscripts([]);
      setSelectedManuscript(null);
      setManuscriptMap({});
      setManuscriptContent('');
      setCharacterChangeLogs({});
      onSelectScene(null);
    } else {
      setSelectedOutlineDetail(null);
      setManuscripts([]);
      setSelectedManuscript(null);
      setManuscriptMap({});
      setManuscriptContent('');
      setCharacterMap({});
      setCharacterChangeLogs({});
      onSelectScene(null);
    }
  }, [selectedStoryId, loadOutlines, loadStoryCharacters, selectOutline, onSelectScene]);

  // 加载所选大纲的详情
  const loadSelectedOutlineDetail = useCallback(
    async (outlineId: number) => {
      setIsLoadingOutlineDetail(true);
      try {
        const full = await getOutlineForWriting(outlineId);
        setSelectedOutlineDetail(full);
      } finally {
        setIsLoadingOutlineDetail(false);
      }
    },
    [getOutlineForWriting]
  );

  const loadStoryCharacters = useCallback(async (storyId: number) => {
    try {
      const { characterCards } = await fetchStoryDetails(storyId);
      const map: Record<number, CharacterCard> = {};
      characterCards.forEach((card) => {
        map[card.id] = card;
      });
      setCharacterMap(map);
    } catch (error) {
      console.error(error);
      setCharacterMap({});
      message.error('获取角色信息失败');
    }
  }, []);

  const loadManuscripts = useCallback(async (outlineId: number) => {
    try {
      const list = await fetchManuscriptsForOutline(outlineId);
      setManuscripts(list);
    } catch (e) {
      console.error(e);
      message.error('获取小说列表失败');
    }
  }, []);

  // 选中大纲后：加载详细结构与该大纲下的所有小说
  useEffect(() => {
    if (selectedOutline) {
      loadSelectedOutlineDetail(selectedOutline.id);
      loadManuscripts(selectedOutline.id);
      setSelectedManuscript(null);
      setManuscriptMap({});
      setManuscriptContent('');
      setCharacterChangeLogs({});
      onSelectScene(null);
    } else {
      setSelectedOutlineDetail(null);
      setManuscripts([]);
      setSelectedManuscript(null);
      setManuscriptMap({});
      setManuscriptContent('');
      setCharacterChangeLogs({});
      onSelectScene(null);
    }
  }, [selectedOutline, loadSelectedOutlineDetail, loadManuscripts, onSelectScene]);

  // 选择某部小说：获取该小说的章节稿件映射
  const handleSelectManuscript = useCallback(
    async (m: Manuscript) => {
      if (!selectedOutline) return;
      setSelectedManuscript(m);
      setIsFetchingManuscript(true);
      setCharacterChangeLogs({});
      try {
        const dto = await fetchManuscriptWithSections(m.id);
        // dto.sections: Record<sceneId, ManuscriptSection>
        setManuscriptMap(dto.sections || {});
        // 同步一下最新的详细大纲（确保树与内容对应）
        await loadSelectedOutlineDetail(selectedOutline.id);
      } catch (e) {
        console.error(e);
        message.error('加载小说内容失败');
        setSelectedManuscript(null);
        setManuscriptMap({});
      } finally {
        setIsFetchingManuscript(false);
      }
    },
    [selectedOutline, loadSelectedOutlineDetail]
  );

  const handleCreateManuscript = useCallback(async () => {
    if (!selectedOutline) return;
    try {
      const created = await createManuscript(selectedOutline.id, { title: '新小说稿件' });
      message.success('已创建新小说稿件');
      await loadManuscripts(selectedOutline.id);
      // 自动选中新建的稿件并拉取其内容
      setSelectedManuscript(created);
      const dto = await fetchManuscriptWithSections(created.id);
      setManuscriptMap(dto.sections || {});
    } catch (e) {
      console.error(e);
      message.error('创建小说失败');
    }
  }, [selectedOutline, loadManuscripts]);

  const handleDeleteManuscript = useCallback(
    (m: Manuscript) => {
      Modal.confirm({
        title: '删除小说确认',
        content: `确定删除小说《${m.title}》吗？此操作不可恢复。`,
        okText: '删除',
        okButtonProps: { danger: true },
        cancelText: '取消',
        onOk: async () => {
          try {
            await apiDeleteManuscript(m.id);
            message.success('删除成功');
            if (selectedOutline) {
              await loadManuscripts(selectedOutline.id);
            }
            if (selectedManuscript?.id === m.id) {
              setSelectedManuscript(null);
              setManuscriptMap({});
              setManuscriptContent('');
              onSelectScene(null);
            }
          } catch (e) {
            console.error(e);
            message.error('删除失败');
          }
        },
      });
    },
    [selectedOutline, selectedManuscript, loadManuscripts, onSelectScene]
  );

  // 当选中场景变化时，同步编辑器内容
  useEffect(() => {
    if (selectedSceneId && manuscriptMap[selectedSceneId]) {
      const newContent = manuscriptMap[selectedSceneId].content;
      setManuscriptContent(newContent || '');
    } else {
      setManuscriptContent('');
    }
  }, [selectedSceneId, manuscriptMap]);

  useEffect(() => {
    const loadLogsForScene = async () => {
      if (!selectedManuscript || !selectedSceneId) return;
      try {
        const logs = await fetchCharacterChangeLogs(selectedManuscript.id, selectedSceneId);
        setCharacterChangeLogs((prev) => ({ ...prev, [selectedSceneId]: logs }));
      } catch (error) {
        console.error(error);
        message.error('加载角色状态失败');
      }
    };
    loadLogsForScene();
  }, [selectedManuscript, selectedSceneId]);

  const runCharacterAnalysis = useCallback(
    async (sectionText: string, sceneId: number | null) => {
      if (!selectedManuscript || !sceneId) return;
      const context = getSceneContext(sceneId);
      if (!context) {
        message.error('无法获取场景信息');
        return;
      }
      const { scene, chapterNumber, sectionNumber } = context;
      const presentIds = Array.isArray(scene.presentCharacterIds) ? scene.presentCharacterIds : [];
      const validIds = presentIds.filter((id): id is number => typeof id === 'number' && !!characterMap[id]);
      if (validIds.length === 0) {
        message.warning('当前场景缺少出场角色信息，请在大纲中补充角色设置。');
        return;
      }

      setIsAnalyzing(true);
      try {
        const logs = await analyzeCharacterChanges(selectedManuscript.id, {
          sceneId,
          chapterNumber,
          sectionNumber,
          sectionContent: sectionText || '',
          characterIds: validIds,
          outlineId: selectedOutlineDetail?.id,
        });
        setCharacterChangeLogs((prev) => ({ ...prev, [sceneId]: logs }));
        message.success('角色状态已更新');
      } catch (error) {
        console.error(error);
        message.error(error instanceof Error ? error.message : '分析角色变化失败');
      } finally {
        setIsAnalyzing(false);
      }
    },
    [selectedManuscript, getSceneContext, characterMap, selectedOutlineDetail]
  );

  const openRefineModal = () => {
    setIsRefineModalOpen(true);
  };

  const applyRefinedContent = (newText: string) => {
    setManuscriptContent(newText);
  };

  // 生成与保存依旧使用兼容接口（生成会写入“最新”或默认小说）
  const handleGenerateContent = async () => {
    if (!selectedSceneId) {
      message.warning('请先在大纲树中选择一个场景！');
      return;
    }
    if (!selectedManuscript) {
      message.warning('请先从“已有小说”中选择一部小说，或创建新小说。');
      return;
    }
    setIsLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/v1/manuscript/scenes/${selectedSceneId}/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || '生成内容失败。');
      }

      const newSection: ManuscriptSection = await response.json();
      const sceneKey = getSectionSceneId(newSection) ?? selectedSceneId;
      setManuscriptMap((prev) => {
        const newMap = { ...prev, [sceneKey]: newSection };
        return newMap;
      });
      setManuscriptContent(newSection.content || '');
      message.success('内容已生成！现在您可以继续编辑。');
      await runCharacterAnalysis(newSection.content || '', sceneKey);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '内容生成失败，请检查后台服务。');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveContent = async () => {
    if (!selectedSceneId) return;

    const sectionToUpdate = manuscriptMap[selectedSceneId];
    if (!sectionToUpdate) {
      message.warning('没有可保存的内容。请先生成内容。');
      return;
    }

    setIsLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/v1/manuscript/sections/${sectionToUpdate.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ content: manuscriptContent }),
      });

      if (!response.ok) {
        console.error('Failed to save content: Response not OK', response.status);
        throw new Error('保存失败');
      }

      const updatedSection: ManuscriptSection = await response.json();
      const sceneKey = getSectionSceneId(updatedSection) ?? selectedSceneId;
      setManuscriptMap((prev) => ({ ...prev, [sceneKey]: updatedSection }));
      message.success('内容已保存！');
      await runCharacterAnalysis(manuscriptContent, sceneKey);
    } catch (error) {
      console.error('Failed to save content:', error);
      message.error(error instanceof Error ? error.message : '保存失败');
    } finally {
      setIsLoading(false);
    }
  };

  const getSceneContext = useCallback(
    (sceneId: number | null) => {
      if (!selectedOutlineDetail || !sceneId) return null;
      for (const chapter of selectedOutlineDetail.chapters) {
        const scene = chapter.scenes.find((s) => s.id === sceneId);
        if (scene) {
          return {
            scene,
            chapterNumber: chapter.chapterNumber,
            sectionNumber: scene.sceneNumber,
          };
        }
      }
      return null;
    },
    [selectedOutlineDetail]
  );

  const selectedSceneContext = useMemo(
    () => getSceneContext(selectedSceneId),
    [getSceneContext, selectedSceneId]
  );

  const selectedScene = selectedSceneContext?.scene ?? null;
  const selectedChapterNumber = selectedSceneContext?.chapterNumber;
  const selectedSectionNumber = selectedSceneContext?.sectionNumber;

  // 渲染顶部筛选区域：选择故事 + 选择大纲
  const renderTopSelectors = () => (
    <Card style={{ marginBottom: 16 }}>
      <Space wrap size="large">
        <div>
          <Text strong>选择故事</Text>
          <div style={{ width: 300, marginTop: 8 }}>
            <Select
              showSearch
              placeholder="-- 请选择一个故事 --"
              value={selectedStoryId || undefined}
              onChange={(v) => setSelectedStoryId(v)}
              allowClear
              filterOption={(input, option) =>
                ((option?.label as string) || '').toLowerCase().includes(input.toLowerCase())
              }
              options={stories.map((s) => ({ label: s.title, value: s.id.toString() }))}
            />
          </div>
        </div>

        <div>
          <Text strong>选择大纲</Text>
          <div style={{ width: 300, marginTop: 8 }}>
            <Select
              placeholder={selectedStoryId ? '-- 请选择大纲 --' : '请先选择故事'}
              value={selectedOutline?.id}
              disabled={!selectedStoryId || outlines.length === 0}
              onChange={(outlineId) => {
                const found = outlines.find((o) => o.id === outlineId) || null;
                selectOutline(found || null);
              }}
              options={outlines.map((o) => ({ label: o.title || `大纲 #${o.id}`, value: o.id }))}
            />
            {!selectedStoryId ? (
              <Text type="secondary">请选择故事以加载大纲。</Text>
            ) : outlines.length === 0 ? (
              <Text type="secondary">当前故事下暂无大纲。</Text>
            ) : null}
          </div>
        </div>
      </Space>
    </Card>
  );

  // 渲染“已有小说”列表 + 新建按钮
  const renderManuscriptList = () => {
    if (!selectedOutline) return null;
    return (
      <Card
        title="已有小说"
        extra={
          <Button type="primary" onClick={handleCreateManuscript}>
            为当前大纲新建小说
          </Button>
        }
        style={{ marginBottom: 16 }}
      >
        <List
          dataSource={manuscripts}
          locale={{ emptyText: <Empty description="暂无小说稿件" /> }}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button
                  key="delete"
                  type="link"
                  danger
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDeleteManuscript(item);
                  }}
                >
                  删除
                </Button>,
              ]}
              onClick={() => handleSelectManuscript(item)}
              style={{
                cursor: 'pointer',
                background: selectedManuscript?.id === item.id ? '#e6f7ff' : 'transparent',
                borderRadius: 6,
                paddingLeft: 8,
                paddingRight: 8,
              }}
            >
              <List.Item.Meta
                title={item.title || `小说 #${item.id}`}
                description={`创建于: ${new Date(item.createdAt).toLocaleString()}`}
              />
            </List.Item>
          )}
        />
      </Card>
    );
  };

  // 主视图：只有选择了“小说”后才显示写作界面
  const renderWritingView = () => {
    if (!selectedOutlineDetail || !selectedManuscript) {
      return <Empty description="请选择上方的故事与大纲，并在“已有小说”中选择一部小说或点击新建。" />;
    }

    return (
      <>
      <Row gutter={24} style={{ height: '100%' }}>
        <Col span={6} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Card title={selectedOutlineDetail.title} style={{ height: '100%', overflow: 'auto' }}>
            <OutlineTreeView
              outline={selectedOutlineDetail}
              onNodeSelect={(node) => onSelectScene(node && node.type === 'scene' ? node.data.id : null)}
            />
          </Card>
        </Col>
        <Col span={12} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Card
            title={`场景内容（当前小说：${selectedManuscript.title}）`}
            style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
          >
            <Card.Meta
              title="当前场景概要"
              description={selectedScene?.synopsis || '请先在左侧大纲树中选择一个场景。'}
              style={{ marginBottom: '16px' }}
            />
            <div style={{ position: 'relative' }}>
              <TextArea
                value={manuscriptContent}
                onChange={(e) => setManuscriptContent(e.target.value)}
                placeholder="这里是您创作的故事内容..."
                rows={20}
                style={{ resize: 'none' }}
                disabled={!selectedSceneId}
              />
              <AiRefineButton onClick={openRefineModal} />
            </div>
            <div className="mt-4 flex items-center justify-between">
              <div>
                <Button type="primary" loading={isLoading} onClick={handleGenerateContent} disabled={!selectedSceneId}>
                  ✨ 生成内容
                </Button>
                <Button style={{ marginLeft: 8 }} onClick={handleSaveContent} disabled={!selectedSceneId || isLoading}>
                  保存
                </Button>
                <Button
                  style={{ marginLeft: 8 }}
                  onClick={() => runCharacterAnalysis(manuscriptContent, selectedSceneId)}
                  disabled={!selectedSceneId || isLoading}
                  loading={isAnalyzing}
                >
                  分析角色变化
                </Button>
              </div>
              <span className="text-sm text-gray-500">预期字数: {selectedScene?.expectedWords || 'N/A'}</span>
            </div>
          </Card>
        </Col>
        <Col span={6} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <CharacterStatusSidebar
            characterMap={characterMap}
            logs={selectedSceneId ? characterChangeLogs[selectedSceneId] || [] : []}
            chapterNumber={selectedChapterNumber}
            sectionNumber={selectedSectionNumber}
            isAnalyzing={isAnalyzing}
            onAnalyze={selectedSceneId ? () => runCharacterAnalysis(manuscriptContent, selectedSceneId) : undefined}
          />
        </Col>
      </Row>
      {isRefineModalOpen && (
        <RefineModal
          open={isRefineModalOpen}
          onCancel={() => setIsRefineModalOpen(false)}
          originalText={manuscriptContent || ''}
          contextType={selectedSceneId ? '小说正文' : '文本'}
          onRefined={applyRefinedContent}
        />
      )}
      </>
    );
  };

  return (
    <Layout style={{ minHeight: 'calc(100vh - 150px)', background: '#fff' }}>
      <Content style={{ padding: '24px' }}>
        {renderTopSelectors()}
        {selectedOutline && renderManuscriptList()}
        <Spin spinning={isFetchingManuscript || isLoadingOutlineDetail} tip="加载中...">
          {renderWritingView()}
        </Spin>
      </Content>
    </Layout>
  );
};

export default ManuscriptWriter;
