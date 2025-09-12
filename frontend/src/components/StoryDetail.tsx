import React, { useEffect } from 'react';
import { Card, Form, Input, Button, message } from 'antd';
import type { StoryCard } from '../types';

const { TextArea } = Input;

export interface StoryCreatePayload {
  title: string;
  synopsis: string;
  genre: string;
  tone: string;
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

  useEffect(() => {
    if (mode === 'edit' && story) {
      form.setFieldsValue(story);
    } else if (mode === 'create') {
      form.resetFields();
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
        };
        await onUpdate(updated);
        message.success('故事已保存');
      }
    } catch {
      // validation or request error already surfaced via antd
    }
  };

  return (
    <Card
      title={mode === 'create' ? '新建故事' : '故事详情'}
      extra={
        <Button type="primary" onClick={handleSubmit} loading={loading}>
          {mode === 'create' ? '创建' : '保存'}
        </Button>
      }
    >
      <Form form={form} layout="vertical">
        <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
          <Input placeholder="请输入故事标题" />
        </Form.Item>
        {mode === 'edit' && (
          <Form.Item name="storyArc" label="故事弧线">
            <TextArea rows={3} placeholder="故事主线与发展轨迹" />
          </Form.Item>
        )}
        <Form.Item name="synopsis" label="简介">
          <TextArea rows={4} placeholder="故事的初步梗概..." />
        </Form.Item>
        <Form.Item name="genre" label="类型">
          <Input placeholder="如：奇幻、科幻、悬疑" />
        </Form.Item>
        <Form.Item name="tone" label="基调">
          <Input placeholder="如：轻松、黑暗、史诗" />
        </Form.Item>
      </Form>
    </Card>
  );
};

export default StoryDetail;