import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
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
import CharacterStatusSidebar from './CharacterStatusSidebar';
import RelationshipGraphModal from './modals/RelationshipGraphModal';
import CharacterGrowthPath from './modals/CharacterGrowthPath';
import GenerateDialogueModal from './modals/GenerateDialogueModal';
import WorldSelect from './WorldSelect';
import WorldReferenceDrawer from './WorldReferenceDrawer';
import type { Outline, Scene, ManuscriptSection, StoryCard, Manuscript, CharacterChangeLog, CharacterDialogueRequestPayload, CharacterDialogueResponsePayload, CharacterCard, WorldSummary } from '../types';
import { useOutlineData } from '../hooks/useOutlineData';
import {
  fetchStories,
  fetchManuscriptsForOutline,
  fetchManuscriptWithSections,
  createManuscript,
  deleteManuscript as apiDeleteManuscript,
  analyzeCharacterChanges,
  fetchCharacterChangeLogs,
  generateDialogue,
  fetchCharactersForStory,
} from '../services/api';

const { Content } = Layout;
const { TextArea } = Input;
const { Text, Paragraph } = Typography;

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

  const activeStory = useMemo(() => {
    if (!selectedStoryId) {
      return null;
    }
    const numericId = Number(selectedStoryId);
    if (Number.isNaN(numericId)) {
      return null;
    }
    return stories.find((s) => s.id === numericId) ?? null;
  }, [stories, selectedStoryId]);

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
  const [storyCharacters, setStoryCharacters] = useState<CharacterCard[]>([]);
  const [characterChangeLogs, setCharacterChangeLogs] = useState<CharacterChangeLog[]>([]);
  const [isLoadingCharacterLogs, setIsLoadingCharacterLogs] = useState(false);
  const [isAnalyzingCharacters, setIsAnalyzingCharacters] = useState(false);
  const [isRelationshipModalOpen, setIsRelationshipModalOpen] = useState(false);
  const [isGrowthPathOpen, setIsGrowthPathOpen] = useState(false);
  const [dialogueModalState, setDialogueModalState] = useState<{ visible: boolean; log: CharacterChangeLog | null }>({ visible: false, log: null });
  const [isGeneratingDialogue, setIsGeneratingDialogue] = useState(false);

  const [worldIdForGeneration, setWorldIdForGeneration] = useState<number | null>(null);
  const [selectedWorld, setSelectedWorld] = useState<WorldSummary | undefined>();
  const [worldPreviewId, setWorldPreviewId] = useState<number | null>(null);
  const [isWorldDrawerOpen, setWorldDrawerOpen] = useState(false);
  const [hasUserModifiedWorld, setHasUserModifiedWorld] = useState(false);

  const lastManuscriptIdRef = useRef<number | null>(null);

  const handleOpenRelationshipGraph = useCallback(() => setIsRelationshipModalOpen(true), []);
  const handleCloseRelationshipGraph = useCallback(() => setIsRelationshipModalOpen(false), []);
  const handleOpenGrowthPath = useCallback(() => setIsGrowthPathOpen(true), []);
  const handleCloseGrowthPath = useCallback(() => setIsGrowthPathOpen(false), []);
  const handleOpenDialogueModal = useCallback((log: CharacterChangeLog) => {
    setDialogueModalState({ visible: true, log });
  }, []);

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
      loadOutlines();
      // 清理上一次选择
      selectOutline(null);
      setSelectedOutlineDetail(null);
      setManuscripts([]);
      setSelectedManuscript(null);
      setManuscriptMap({});
      setManuscriptContent('');
      setCharacterChangeLogs([]);
      onSelectScene(null);
    } else {
      setSelectedOutlineDetail(null);
      setManuscripts([]);
      setSelectedManuscript(null);
      setManuscriptMap({});
      setManuscriptContent('');
      onSelectScene(null);
    }
    setWorldIdForGeneration(null);
    setSelectedWorld(undefined);
    setHasUserModifiedWorld(false);
    setWorldPreviewId(null);
    setWorldDrawerOpen(false);
  }, [selectedStoryId, loadOutlines, selectOutline, onSelectScene]);

  useEffect(() => {
    if (!selectedStoryId) {
      setStoryCharacters([]);
      return;
    }
    const storyNumericId = Number(selectedStoryId);
    if (Number.isNaN(storyNumericId)) {
      setStoryCharacters([]);
      return;
    }
    fetchCharactersForStory(storyNumericId)
      .then((list) => setStoryCharacters(list || []))
      .catch((err) => {
        console.error(err);
        setStoryCharacters([]);
      });
  }, [selectedStoryId]);

  useEffect(() => {
    const currentManuscriptId = selectedManuscript?.id ?? null;
    if (lastManuscriptIdRef.current !== currentManuscriptId) {
      lastManuscriptIdRef.current = currentManuscriptId;
      const derivedWorld = selectedManuscript?.worldId ?? selectedOutlineDetail?.worldId ?? activeStory?.worldId ?? null;
      setWorldIdForGeneration(derivedWorld ?? null);
      setWorldPreviewId(derivedWorld ?? null);
      setHasUserModifiedWorld(false);
    }
  }, [selectedManuscript, selectedOutlineDetail, activeStory]);

  useEffect(() => {
    if (selectedManuscript) {
      return;
    }
    if (hasUserModifiedWorld) {
      return;
    }
    const derivedWorld = selectedOutlineDetail?.worldId ?? activeStory?.worldId ?? null;
    setWorldIdForGeneration(derivedWorld ?? null);
    setWorldPreviewId(derivedWorld ?? null);
  }, [selectedOutlineDetail, activeStory, selectedManuscript, hasUserModifiedWorld]);

  useEffect(() => {
    if (worldIdForGeneration == null) {
      setSelectedWorld(undefined);
    }
  }, [worldIdForGeneration]);

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
  const loadCharacterLogs = useCallback(async (manuscriptId: number) => {
    setIsLoadingCharacterLogs(true);
    try {
      const logs = await fetchCharacterChangeLogs(manuscriptId);
      setCharacterChangeLogs(logs || []);
    } catch (err) {
      console.error(err);
      message.error('加载角色变化记录失败');
      setCharacterChangeLogs([]);
    } finally {
      setIsLoadingCharacterLogs(false);
    }
  }, []);
  const extractCharacterIds = useCallback((scene: Scene | null): number[] => {
    if (!scene) return [];
    if (scene.sceneCharacters && scene.sceneCharacters.length > 0) {
      const ids = scene.sceneCharacters
        .map((sc) => sc.characterCardId)
        .filter((id): id is number => typeof id === 'number');
      if (ids.length > 0) {
        return ids;
      }
    }
    if (scene.presentCharacterIds && scene.presentCharacterIds.length > 0) {
      return scene.presentCharacterIds.filter((id): id is number => typeof id === 'number');
    }
    if (scene.presentCharacters) {
      const candidates = scene.presentCharacters.split(/[，,、]/).map((name) => name.trim()).filter(Boolean);
      if (candidates.length > 0) {
        const nameMap = new Map(storyCharacters.map((c) => [c.name.trim(), c.id]));
        return candidates.map((name) => nameMap.get(name)).filter((id): id is number => typeof id === 'number');
      }
    }
    return [];
  }, [storyCharacters]);

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
      setCharacterChangeLogs([]);
      onSelectScene(null);
    } else {
      setSelectedOutlineDetail(null);
      setManuscripts([]);
      setSelectedManuscript(null);
      setManuscriptMap({});
      setManuscriptContent('');
      setCharacterChangeLogs([]);
      onSelectScene(null);
    }
  }, [selectedOutline, loadSelectedOutlineDetail, loadManuscripts, onSelectScene]);

  // 选择某部小说：获取该小说的章节稿件映射
  const handleSelectManuscript = useCallback(
    async (m: Manuscript) => {
      if (!selectedOutline) return;
      setSelectedManuscript(m);
      setIsFetchingManuscript(true);
      try {
        const dto = await fetchManuscriptWithSections(m.id);
        // dto.sections: Record<sceneId, ManuscriptSection>
        setManuscriptMap(dto.sections || {});
        // 同步一下最新的详细大纲（确保树与内容对应）
        await loadSelectedOutlineDetail(selectedOutline.id);
        await loadCharacterLogs(m.id);
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

  const runCharacterAnalysis = useCallback(async (overrideContent?: string) => {
    if (!selectedManuscript || !selectedOutlineDetail || !selectedSceneId) return;
    let chapterNumber: number | null = null;
    let sectionNumber: number | null = null;
    let sceneForAnalysis: Scene | null = null;
    for (const chapter of selectedOutlineDetail.chapters) {
      const foundScene = chapter.scenes.find((scene) => scene.id === selectedSceneId);
      if (foundScene) {
        chapterNumber = chapter.chapterNumber;
        sectionNumber = foundScene.sceneNumber;
        sceneForAnalysis = foundScene;
        break;
      }
    }
    if (!sceneForAnalysis || chapterNumber == null || sectionNumber == null) {
      message.warning('无法定位当前场景，无法进行角色分析');
      return;
    }
    const characterIds = extractCharacterIds(sceneForAnalysis);
    if (characterIds.length === 0) {
      message.info('当前场景未配置出场角色，跳过角色分析。');
      return;
    }
    const sectionContent = overrideContent ?? manuscriptContent;
    setIsAnalyzingCharacters(true);
    try {
      await analyzeCharacterChanges(selectedManuscript.id, {
        chapterNumber,
        sectionNumber,
        sectionContent,
        characterIds,
      });
      await loadCharacterLogs(selectedManuscript.id);
    } catch (error) {
      console.error(error);
      message.error('角色变化分析失败');
    } finally {
      setIsAnalyzingCharacters(false);
    }
  }, [selectedManuscript, selectedOutlineDetail, selectedSceneId, manuscriptContent, extractCharacterIds, loadCharacterLogs]);
  const handleCloseDialogueModal = useCallback(() => {
    setDialogueModalState({ visible: false, log: null });
  }, []);

  const handleDialogueSubmit = useCallback(async (values: { dialogueTopic: string; currentSceneDescription: string }) => {
    if (!dialogueModalState.log || !selectedManuscript) return;
    setIsGeneratingDialogue(true);
    try {
      const payload: CharacterDialogueRequestPayload = {
        characterId: dialogueModalState.log.characterId,
        manuscriptId: selectedManuscript.id,
        dialogueTopic: values.dialogueTopic,
        currentSceneDescription: values.currentSceneDescription,
      };
      const response: CharacterDialogueResponsePayload = await generateDialogue(payload);
      Modal.success({
        title: `${dialogueModalState.log.characterName} 的对话`,
        content: <Paragraph style={{ whiteSpace: 'pre-wrap' }}>{(response.dialogue || '').trim() || 'AI 未返回内容'}</Paragraph>,
        width: 520,
      });
      setDialogueModalState({ visible: false, log: null });
    } catch (err) {
      console.error(err);
      message.error(err instanceof Error ? err.message : '对话生成失败');
    } finally {
      setIsGeneratingDialogue(false);
    }
  }, [dialogueModalState, selectedManuscript]);

  const handleWorldChange = useCallback((worldId?: number, world?: WorldSummary) => {
    const normalized = worldId ?? null;
    setWorldIdForGeneration(normalized);
    setSelectedWorld(world ?? undefined);
    setHasUserModifiedWorld(true);
    setWorldPreviewId(normalized);
    if (selectedManuscript) {
      const updated = { ...selectedManuscript, worldId: normalized };
      setSelectedManuscript(updated);
      setManuscripts((prev) => prev.map((m) => (m.id === updated.id ? { ...m, worldId: normalized } : m)));
    } else {
      setSelectedOutlineDetail((prev) => (prev ? { ...prev, worldId: normalized } : prev));
    }
  }, [selectedManuscript]);

  const handleWorldResolved = useCallback((world?: WorldSummary) => {
    setSelectedWorld(world ?? undefined);
  }, []);
  const handleCreateManuscript = useCallback(async () => {
    if (!selectedOutline) return;
    try {
      const payloadWorldId = worldIdForGeneration ?? null;
      const created = await createManuscript(selectedOutline.id, { title: '新小说稿件', worldId: payloadWorldId });
      message.success('已创建新小说稿件');
      await loadManuscripts(selectedOutline.id);
      // 自动选中新建的稿件并拉取其内容
      setSelectedManuscript(created);
      const dto = await fetchManuscriptWithSections(created.id);
      setManuscriptMap(dto.sections || {});
      setCharacterChangeLogs([]);
      await loadCharacterLogs(created.id);
    } catch (e) {
      console.error(e);
      message.error('创建小说失败');
    }
  }, [selectedOutline, loadManuscripts, loadCharacterLogs, worldIdForGeneration]);

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
            setCharacterChangeLogs([]);
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
      const resolvedWorldId = worldIdForGeneration ?? selectedManuscript.worldId ?? selectedOutlineDetail?.worldId ?? activeStory?.worldId ?? null;
      const payload = resolvedWorldId != null ? { worldId: resolvedWorldId } : {};
      const response = await fetch(`/api/v1/manuscript/scenes/${selectedSceneId}/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
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
      await runCharacterAnalysis(newSection.content || '');
      if (selectedManuscript && selectedManuscript.worldId !== resolvedWorldId) {
        const updatedManuscript = { ...selectedManuscript, worldId: resolvedWorldId ?? null };
        setSelectedManuscript(updatedManuscript);
        setManuscripts((prev) => prev.map((m) => (m.id === updatedManuscript.id ? { ...m, worldId: resolvedWorldId ?? null } : m)));
      }
      if (selectedOutlineDetail && selectedOutlineDetail.worldId !== resolvedWorldId) {
        setSelectedOutlineDetail({ ...selectedOutlineDetail, worldId: resolvedWorldId ?? null });
      }
      setWorldIdForGeneration(resolvedWorldId ?? null);
      setHasUserModifiedWorld(false);
      message.success('内容已生成！现在您可以继续编辑。');
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
        await runCharacterAnalysis(manuscriptContent);
      message.success('内容已保存！');
    } catch (error) {
      console.error('Failed to save content:', error);
      message.error(error instanceof Error ? error.message : '保存失败');
    } finally {
      setIsLoading(false);
    }
  };

  const selectedSceneInfo = useMemo(() => {
    if (!selectedOutlineDetail || !selectedSceneId) return null;
    for (const chapter of selectedOutlineDetail.chapters) {
      const scene = chapter.scenes.find((s) => s.id === selectedSceneId);
      if (scene) {
        return { scene, chapterNumber: chapter.chapterNumber };
      }
    }
    return null;
  }, [selectedOutlineDetail, selectedSceneId]);

  const selectedScene = selectedSceneInfo?.scene ?? null;
  const selectedChapterNumber = selectedSceneInfo?.chapterNumber ?? null;
  const selectedSectionNumber = selectedScene?.sceneNumber ?? null;
  const defaultDialogueSceneDescription = useMemo(() => {
    if (!selectedScene) return '';
    if (selectedScene.synopsis && selectedScene.synopsis.trim()) {
      return selectedScene.synopsis;
    }
    return manuscriptContent ? manuscriptContent.slice(0, 300) : '';
  }, [selectedScene, manuscriptContent]);

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
        <Col span={8} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Card title={selectedOutlineDetail.title} style={{ height: '100%', overflow: 'auto' }}>
            <OutlineTreeView
              outline={selectedOutlineDetail}
              onNodeSelect={(node) => onSelectScene(node && node.type === 'scene' ? node.data.id : null)}
            />
          </Card>
        </Col>
        <Col span={10} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Card
            title={`场景内容（当前小说：${selectedManuscript.title}）`}
            style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
          >
            <Space direction="vertical" size="small" style={{ marginBottom: 16 }}>
              <Text strong>引用世界</Text>
              <Space.Compact style={{ width: '100%' }}>
                <WorldSelect
                  allowClear
                  value={worldIdForGeneration ?? undefined}
                  onChange={handleWorldChange}
                  onWorldResolved={handleWorldResolved}
                  placeholder="可选：选择一个已发布的世界观"
                />
                <Button
                  type="default"
                  disabled={!worldIdForGeneration}
                  onClick={() => {
                    if (worldIdForGeneration) {
                      setWorldPreviewId(worldIdForGeneration);
                      setWorldDrawerOpen(true);
                    }
                  }}
                >
                  查看设定
                </Button>
              </Space.Compact>
              {selectedWorld && (
                <Text type="secondary" style={{ display: 'block' }}>
                  {selectedWorld.tagline}
                </Text>
              )}
            </Space>
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
              </div>
              <span className="text-sm text-gray-500">预期字数: {selectedScene?.expectedWords || 'N/A'}</span>
            </div>
          </Card>
        </Col>
        <Col span={6} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <CharacterStatusSidebar
            currentChapterNumber={selectedChapterNumber}
            currentSectionNumber={selectedSectionNumber}
            changeLogs={characterChangeLogs}
            allLogs={characterChangeLogs}
            loading={isLoadingCharacterLogs || isAnalyzingCharacters}
            onOpenRelationshipGraph={handleOpenRelationshipGraph}
            onOpenGrowthPath={handleOpenGrowthPath}
            onGenerateDialogue={handleOpenDialogueModal}
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
        <RelationshipGraphModal
          open={isRelationshipModalOpen}
          onClose={handleCloseRelationshipGraph}
          logs={characterChangeLogs}
        />
        <CharacterGrowthPath
          open={isGrowthPathOpen}
          onClose={handleCloseGrowthPath}
          logs={characterChangeLogs}
        />
        <GenerateDialogueModal
          open={dialogueModalState.visible}
          characterName={dialogueModalState.log?.characterName ?? ''}
          submitting={isGeneratingDialogue}
          defaultSceneDescription={defaultDialogueSceneDescription}
          onCancel={handleCloseDialogueModal}
          onSubmit={handleDialogueSubmit}
        />
        <WorldReferenceDrawer
          worldId={worldPreviewId ?? undefined}
          open={isWorldDrawerOpen}
          onClose={() => {
            setWorldDrawerOpen(false);
            setWorldPreviewId(null);
          }}
        />
      </Content>
    </Layout>
  );
};

export default ManuscriptWriter;














