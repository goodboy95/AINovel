import React from 'react';
import { Card, Button, List } from 'antd';
import type { StoryCard } from '../types';

interface StoryListProps {
  stories: StoryCard[];
  selectedId: number | 'new' | null;
  onSelect: (id: number | 'new') => void;
  loading?: boolean;
}

const StoryList: React.FC<StoryListProps> = ({ stories, selectedId, onSelect, loading }) => {
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