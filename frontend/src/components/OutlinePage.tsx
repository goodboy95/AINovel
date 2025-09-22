import React, { useEffect, useState } from 'react';
import {
  Form,
  Select,
  InputNumber,
  Button,
  Spin,
  Alert,
  Card,
  Typography,
  List,
  Divider,
  Row,
  Col,
  Empty,
  Modal,
  Space,
} from 'antd';
import type { Outline, StoryCard, Chapter, Scene, WorldSummary } from '../types';
import { generateChapter, createEmptyOutlineForStory } from '../services/api';
import OutlineTreeView from './OutlineTreeView';
import ChapterEditForm from './ChapterEditForm';
import SceneEditForm from './SceneEditForm';
import WorldSelect from './WorldSelect';
import WorldReferenceDrawer from './WorldReferenceDrawer';

const { Title, Text } = Typography;
const { Option } = Select;

interface GenerateChapterParams {
  sectionsPerChapter: number;
  wordsPerSection: number;
}

type SelectedNode =
  | { type: 'chapter'; data: Chapter }
  | { type: 'scene'; data: Scene }
  | null;

interface OutlinePageProps {
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
  loadOutlines: () => void;

  updateLocal: (updatedData: Chapter | Scene) => void;
  updateOutlineRemote: (outline: Outline) => Promise<void>;
  onDeleteOutline: (outlineId: number) => void;
}

const OutlinePage: React.FC<OutlinePageProps> = ({
  storyCards,
  selectedStoryId,
  onStoryChange,

  isLoading,
  setIsLoading,

  outline,
  setOutline,

  error,
  setError,

  outlines,
  onSelectOutline,
  loadOutlines,

  updateLocal,
  updateOutlineRemote,
  onDeleteOutline,
}) => {
  const [form] = Form.useForm();
  const [selectedNode, setSelectedNode] = useState<SelectedNode>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [currentChapter, setCurrentChapter] = useState(1);
  const [isEditing, setIsEditing] = useState(false);
  const [worldIdForGeneration, setWorldIdForGeneration] = useState<number | null>(null);
  const [selectedWorld, setSelectedWorld] = useState<WorldSummary | undefined>();
  const [worldPreviewId, setWorldPreviewId] = useState<number | null>(null);
  const [isWorldDrawerOpen, setWorldDrawerOpen] = useState(false);
  const activeStory = React.useMemo(() => {
    if (!selectedStoryId) return null;
    return storyCards.find((story) => story.id.toString() === selectedStoryId) ?? null;
  }, [storyCards, selectedStoryId]);

  useEffect(() => {
    const nextWorldId = outline?.worldId ?? activeStory?.worldId ?? null;
    setWorldIdForGeneration(nextWorldId ?? null);
    if (nextWorldId == null) {
      setSelectedWorld(undefined);
    }
  }, [outline, activeStory]);

  useEffect(() => {
    if (outline) {
      setCurrentChapter(outline.chapters.length + 1);
    } else {
      setCurrentChapter(1);
    }
  }, [outline]);

  const handleCreateEmptyOutline = async () => {
    if (!selectedStoryId) {
      setError('请先选择一个故事。');
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      let newOutline = await createEmptyOutlineForStory(selectedStoryId);
      if (worldIdForGeneration !== null && newOutline.worldId !== worldIdForGeneration) {
        const updatedOutline = { ...newOutline, worldId: worldIdForGeneration };
        try {
          await updateOutlineRemote(updatedOutline);
          newOutline = updatedOutline;
        } catch (err) {
          setError(err instanceof Error ? err.message : '更新世界失败。');
        }
      }
      setOutline(newOutline);
      setWorldIdForGeneration(newOutline.worldId ?? null);
      onSelectOutline(newOutline);
      await loadOutlines();
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

  const handleNodeSelect = (node: SelectedNode) => {
    setSelectedNode(node);
    setIsEditing(!!node);
  };

  const handleWorldChange = React.useCallback(async (worldId?: number, world?: WorldSummary) => {
    const nextWorldId = worldId ?? null;
    setWorldIdForGeneration(nextWorldId);
    setSelectedWorld(world ?? undefined);
    setWorldPreviewId(nextWorldId);
    if (!outline) {
      return;
    }
    const currentWorldId = outline.worldId ?? null;
    if (currentWorldId === nextWorldId) {
      return;
    }
    const nextOutline = { ...outline, worldId: nextWorldId };
    setOutline(nextOutline);
    try {
      setIsLoading(true);
      await updateOutlineRemote(nextOutline);
    } catch (err) {
      setError(err instanceof Error ? err.message : '更新世界失败。');
    } finally {
      setIsLoading(false);
    }
  }, [outline, setOutline, updateOutlineRemote, setIsLoading, setError]);

  const handleWorldResolved = React.useCallback((world?: WorldSummary) => {
    setSelectedWorld(world ?? undefined);
  }, []);

  const handleGenerateFinish = async (values: GenerateChapterParams) => {
    if (!outline) {
      setError('请先选择或创建一个大纲。');
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const newChapter: Chapter = await generateChapter(outline.id, {
        ...values,
        chapterNumber: currentChapter,
        worldId: worldIdForGeneration ?? undefined,
      });
      setOutline({
        ...outline,
        chapters: [...outline.chapters, newChapter],
      });
      setCurrentChapter((n) => n + 1);
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

  const handleSaveOutline = async () => {
    if (!outline) return;
    setIsSaving(true);
    try {
      await updateOutlineRemote(outline);
    } catch (err) {
      setError(err instanceof Error ? err.message : '保存失败');
    } finally {
      setIsSaving(false);
    }
  };

  const renderRightPanel = () => {
    if (!outline) {
      return (
        <Card>
          <Empty description="尚未选择大纲。请在左侧选择或创建一个大纲。" />
        </Card>
      );
    }

    if (!selectedNode) {
      return (
        <Card>
          <Title level={4}>为 "{outline.title}" 生成新章节</Title>
          <Text>当前正在生成: <b>第 {currentChapter} 章</b></Text>
          <Divider />
          <Form
            form={form}
            layout="vertical"
            onFinish={handleGenerateFinish}
            initialValues={{ sectionsPerChapter: 5, wordsPerSection: 800 }}
          >
            <Form.Item
              name="sectionsPerChapter"
              label="本章小节数"
              rules={[{ required: true, message: '请输入本章包含的小节数！' }]}
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
      );
    }

    switch (selectedNode.type) {
      case 'chapter':
        return (
          <Card title="编辑章节">
            <ChapterEditForm chapter={selectedNode.data} onUpdate={updateLocal} />
          </Card>
        );
      case 'scene':
        return (
          <Card title="编辑场景">
            <SceneEditForm
              scene={selectedNode.data}
              onUpdate={updateLocal}
              storyId={selectedStoryId ? parseInt(selectedStoryId) : undefined}
            />
          </Card>
        );
      default:
        return null;
    }
  };

  return (
    <>
      <Spin spinning={isLoading || isSaving} tip="处理中..." size="large">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          {error && (
            <Alert
              message="操作失败"
              description={error}
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}

          {isEditing && (
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
              <Button onClick={() => { setSelectedNode(null); setIsEditing(false); }}>
                返回
              </Button>
            </div>
          )}
          <Row gutter={16}>
          {/* 左侧：故事选择 + 历史大纲列表 + 创建新大纲 */}
          <Col span={6}>
            <Card title="故事与大纲">
              <Form layout="vertical">
                <Form.Item label="选择故事">
                  <Select
                    value={selectedStoryId}
                    onChange={onStoryChange}
                    placeholder="-- 请选择一个故事以查看或创建大纲 --"
                    showSearch
                    filterOption={(input, option) =>
                      (option?.children as unknown as string)
                        .toLowerCase()
                        .includes(input.toLowerCase())
                    }
                  >
                    {storyCards.map((story) => (
                      <Option key={story.id} value={story.id.toString()}>
                        {story.title}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Form>

              {selectedStoryId && (
                <div style={{ marginTop: 16 }}>
                  <Text strong>引用世界</Text>
                  <Space.Compact style={{ width: '100%', marginTop: 8 }}>
                    <WorldSelect
                      allowClear
                      value={worldIdForGeneration ?? undefined}
                      onChange={handleWorldChange}
                      onWorldResolved={handleWorldResolved}
                      placeholder="可选：选择一个已发布的世界"
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
                    <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                      {selectedWorld.tagline}
                    </Text>
                  )}
                </div>
              )}

              {selectedStoryId && (
                <>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginBottom: 16,
                    }}
                  >
                    <Title level={5} style={{ marginBottom: 0 }}>
                      历史大纲
                    </Title>
                    <Button type="primary" onClick={handleCreateEmptyOutline}>
                      为当前故事创建新大纲
                    </Button>
                  </div>

                  {outlines.length > 0 ? (
                    <List
                      itemLayout="horizontal"
                      dataSource={outlines}
                      renderItem={(item) => (
                        <List.Item
                          actions={[
                            <Button
                              key="select"
                              type="link"
                              onClick={() => onSelectOutline(item)}
                            >
                              选择此大纲
                            </Button>,
                            <Button
                              key="delete"
                              type="link"
                              danger
                              onClick={() => Modal.confirm({
                                  title: '删除大纲确认',
                                  content: `确定删除大纲《${item.title || `#${item.id}`}》吗？此操作不可恢复。`,
                                  okText: '删除',
                                  okButtonProps: { danger: true },
                                  cancelText: '取消',
                                  onOk: () => onDeleteOutline(item.id),
                                })
                              }
                            >
                              删除
                            </Button>,
                          ]}
                        >
                          <List.Item.Meta
                            title={item.title || `大纲 #${item.id}`}
                            description={`创建于: ${new Date(
                              item.created_at
                            ).toLocaleString()}`}
                          />
                        </List.Item>
                      )}
                    />
                  ) : (
                    <Empty description="暂无历史大纲" />
                  )}</>
              )}
            </Card>
          </Col>

          {/* 中间：树状视图 + 保存按钮 */}
          <Col span={10}>
            <Card
              title="大纲结构"
              extra={
                <Button type="primary" onClick={handleSaveOutline}>
                  保存大纲
                </Button>
              }
            >
              <OutlineTreeView outline={outline} onNodeSelect={handleNodeSelect} />
            </Card>
          </Col>

          {/* 右侧：生成/编辑区域 */}
          <Col span={8}>{renderRightPanel()}</Col>
          </Row>

        </div>
      </Spin>
      <WorldReferenceDrawer
        worldId={worldPreviewId ?? undefined}
        open={isWorldDrawerOpen}
        onClose={() => {
          setWorldDrawerOpen(false);
          setWorldPreviewId(null);
        }}
      />
    </>
  );
};

export default OutlinePage;
