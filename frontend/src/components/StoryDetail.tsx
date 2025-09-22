import React, { useEffect, useState } from 'react';
import { Card, Form, Input, Button, message, Space, Typography } from 'antd';
import type { StoryCard, WorldSummary } from '../types';
import AiRefineButton from './AiRefineButton';
import RefineModal from './modals/RefineModal';
import WorldSelect from './WorldSelect';
import WorldReferenceDrawer from './WorldReferenceDrawer';

const { TextArea } = Input;
const { Text } = Typography;

export interface StoryCreatePayload {
  title: string;
  synopsis: string;
  genre: string;
  tone: string;
  worldId?: number | null;
}

interface StoryDetailProps {
  mode: 'create' | 'edit';
  story: StoryCard | null;
  loading?: boolean;
  onCreate: (payload: StoryCreatePayload) => Promise<StoryCard | null>;
  onUpdate: (story: StoryCard) => Promise<void> | void;
  onAfterCreate?: (newStory: StoryCard) => void;
}

const StoryDetail: React.FC<StoryDetailProps> = ({
  mode,
  story,
  loading = false,
  onCreate,
  onUpdate,
  onAfterCreate,
}) => {
  const [form] = Form.useForm();
  const [isRefineModalOpen, setIsRefineModalOpen] = useState(false);
  const [refineTarget, setRefineTarget] = useState<{ field: keyof StoryCard; context: string } | null>(null);
  const [selectedWorld, setSelectedWorld] = useState<WorldSummary | undefined>();
  const [worldPreviewId, setWorldPreviewId] = useState<number | null>(null);
  const [isWorldDrawerOpen, setWorldDrawerOpen] = useState(false);

  useEffect(() => {
    if (mode === 'edit' && story) {
      form.setFieldsValue(story);
      setSelectedWorld(undefined);
      setWorldPreviewId(story.worldId ?? null);
    } else if (mode === 'create') {
      form.resetFields();
      form.setFieldsValue({ worldId: null });
      setSelectedWorld(undefined);
      setWorldPreviewId(null);
    }
  }, [mode, story, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (mode === 'create') {
        const payload: StoryCreatePayload = {
          title: values.title,
          synopsis: values.synopsis || '',
          genre: values.genre || '',
          tone: values.tone || '',
          worldId: values.worldId ?? null,
        };
        const created = await onCreate(payload);
        if (created) {
          message.success('新故事创建成功');
          onAfterCreate?.(created);
        }
      } else if (mode === 'edit' && story) {
        const updated: StoryCard = {
          ...story,
          title: values.title,
          synopsis: values.synopsis || '',
          storyArc: values.storyArc || '',
          genre: values.genre || '',
          tone: values.tone || '',
          worldId: values.worldId ?? null,
        };
        await onUpdate(updated);
        message.success('故事已保存');
      }
    } catch {
      // validation or request error already surfaced via antd
    }
  };

  const openRefine = (field: keyof StoryCard, context: string) => {
    setRefineTarget({ field, context });
    setIsRefineModalOpen(true);
  };

  const applyRefined = (newText: string) => {
    if (!refineTarget) return;
    form.setFieldsValue({ [refineTarget.field]: newText });
  };

  return (
    <>
      <Card
      title={mode === 'create' ? '新建故事' : '故事详情'}
      extra={
        <Button type="primary" onClick={handleSubmit} loading={loading}>
          {mode === 'create' ? '创建' : '保存'}
        </Button>
      }
    >
      <Form form={form} layout="vertical">
        <div style={{ position: 'relative' }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="请输入故事标题" />
          </Form.Item>
          <AiRefineButton onClick={() => openRefine('title', '故事标题')} />
        </div>
        {mode === 'edit' && (
          <div style={{ position: 'relative' }}>
            <Form.Item name="storyArc" label="故事弧线">
              <TextArea rows={3} placeholder="故事主线与发展轨迹" />
            </Form.Item>
            <AiRefineButton onClick={() => openRefine('storyArc', '故事弧线')} />
          </div>
        )}
        <div style={{ position: 'relative' }}>
          <Form.Item name="synopsis" label="简介">
            <TextArea rows={4} placeholder="故事的初步梗概..." />
          </Form.Item>
          <AiRefineButton onClick={() => openRefine('synopsis', '故事简介')} />
        </div>
        <Form.Item name="genre" label="类型">
          <Input placeholder="如：奇幻、科幻、悬疑" />
        </Form.Item>
        <Form.Item name="tone" label="基调">
          <Input placeholder="如：轻松、黑暗、史诗" />
        </Form.Item>
        <Form.Item label="引用世界">
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item name="worldId" noStyle>
              <WorldSelect
                allowClear
                value={form.getFieldValue('worldId') ?? undefined}
                onChange={(worldId, world) => {
                  form.setFieldsValue({ worldId: worldId ?? null });
                  setSelectedWorld(world);
                }}
                onWorldResolved={(world) => {
                  setSelectedWorld(world ?? undefined);
                }}
                placeholder="可选：选择一个已发布的世界观"
              />
            </Form.Item>
            <Button
              type="default"
              disabled={!form.getFieldValue('worldId')}
              onClick={() => {
                const id = form.getFieldValue('worldId');
                if (id) {
                  setWorldPreviewId(id);
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
        </Form.Item>
      </Form>
      {refineTarget && (
        <RefineModal
          open={isRefineModalOpen}
          onCancel={() => setIsRefineModalOpen(false)}
          originalText={form.getFieldValue(refineTarget.field) || ''}
          contextType={refineTarget.context}
          onRefined={applyRefined}
        />
      )}
      </Card>
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

export default StoryDetail;
