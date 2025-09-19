import React from 'react';
import { Card, Button, List, Popconfirm } from 'antd';
import type { StoryCard } from '../types';

interface StoryListProps {
  stories: StoryCard[];
  selectedId: number | 'new' | null;
  onSelect: (id: number | 'new') => void;
  loading?: boolean;
  onDelete?: (id: number) => void;
}

const StoryList: React.FC<StoryListProps> = ({ stories, selectedId, onSelect, loading, onDelete }) => {

  return (
    <Card
      title="故事列表"
      extra={
        <Button type="primary" onClick={() => onSelect('new')}>
          新建故事
        </Button>
      }
    >
      <List
        loading={loading}
        itemLayout="horizontal"
        dataSource={stories}
        renderItem={(item) => (
          <List.Item
            actions={[
              <Popconfirm
                key="delete"
                title="删除故事确认"
                description={`删除故事《${item.title}》将一并删除其下的所有大纲和角色，此操作不可恢复。是否继续？`}
                okText="删除"
                cancelText="取消"
                okButtonProps={{ danger: true }}
                onConfirm={(e) => {
                  e?.stopPropagation?.();
                  onDelete?.(item.id);
                }}
                onCancel={(e) => e?.stopPropagation?.()}
              >
                <Button
                  type="link"
                  danger
                  onClick={(e) => {
                    e.stopPropagation();
                  }}
                >
                  删除
                </Button>
              </Popconfirm>,
            ]}
            onClick={() => onSelect(item.id)}
            style={{
              cursor: 'pointer',
              background: selectedId === item.id ? '#e6f7ff' : 'transparent',
              borderRadius: 6,
              paddingLeft: 12,
              paddingRight: 12,
            }}
          >
            <List.Item.Meta
              title={item.title}
              description={`类型: ${item.genre} | 基调: ${item.tone}`}
            />
          </List.Item>
        )}
      />
    </Card>
  );
};

export default StoryList;
