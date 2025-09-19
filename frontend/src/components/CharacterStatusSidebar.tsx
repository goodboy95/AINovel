import React, { useMemo } from 'react';
import { Card, Empty, Tag, Typography, Space, List, Button, Tooltip } from 'antd';
import { StarFilled, StarOutlined, CommentOutlined, NodeIndexOutlined, HistoryOutlined } from '@ant-design/icons';
import type { CharacterChangeLog } from '../types';

const { Paragraph, Text, Title } = Typography;

export interface CharacterStatusSidebarProps {
  currentChapterNumber: number | null;
  currentSectionNumber: number | null;
  changeLogs: CharacterChangeLog[];
  allLogs: CharacterChangeLog[];
  loading?: boolean;
  onOpenRelationshipGraph: () => void;
  onOpenGrowthPath: () => void;
  onGenerateDialogue: (log: CharacterChangeLog) => void;
}

const CharacterStatusSidebar: React.FC<CharacterStatusSidebarProps> = ({
  currentChapterNumber,
  currentSectionNumber,
  changeLogs,
  allLogs,
  loading = false,
  onOpenRelationshipGraph,
  onOpenGrowthPath,
  onGenerateDialogue,
}) => {
  const currentLogs = useMemo(() => {
    if (currentChapterNumber == null || currentSectionNumber == null) return [];
    return changeLogs.filter(
      (log) => log.chapterNumber === currentChapterNumber && log.sectionNumber === currentSectionNumber
    );
  }, [changeLogs, currentChapterNumber, currentSectionNumber]);

  const turningPointCount = useMemo(
    () => allLogs.filter((log) => log.isTurningPoint).length,
    [allLogs]
  );

  const renderRelationshipChanges = (log: CharacterChangeLog) => {
    if (!log.relationshipChanges || log.relationshipChanges.length === 0) {
      return <Text type="secondary">本节没有关系变化</Text>;
    }
    return (
      <List
        size="small"
        dataSource={log.relationshipChanges}
        renderItem={(item) => (
          <List.Item>
            <div>
              <Text strong>目标角色 ID：</Text>
              <Text>{item.targetCharacterId}</Text>
              <br />
              <Text type="secondary">
                {`${item.previousRelationship || '未知'} → ${item.currentRelationship || '未知'}`}
              </Text>
              {item.changeReason && <div>{item.changeReason}</div>}
            </div>
          </List.Item>
        )}
      />
    );
  };

  const renderLogCard = (log: CharacterChangeLog) => (
    <Card
      key={log.id}
      title={
        <Space align="center">
          <Text strong>{log.characterName}</Text>
          {log.isTurningPoint ? (
            <Tooltip title="关键转折点">
              <StarFilled style={{ color: '#faad14' }} />
            </Tooltip>
          ) : (
            <Tooltip title="普通更新">
              <StarOutlined style={{ color: '#d9d9d9' }} />
            </Tooltip>
          )}
        </Space>
      }
      size="small"
      extra={
        <Button
          size="small"
          type="link"
          icon={<CommentOutlined />}
          onClick={() => onGenerateDialogue(log)}
        >
          生成对话
        </Button>
      }
      style={{ marginBottom: 12 }}
    >
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <div>
          <Text type="secondary">新知信息</Text>
          <Paragraph style={{ marginBottom: 0 }}>{log.newlyKnownInfo || '无'}</Paragraph>
        </div>
        <div>
          <Text type="secondary">状态变化</Text>
          <Paragraph style={{ marginBottom: 0 }}>{log.characterChanges || '无'}</Paragraph>
        </div>
        <div>
          <Text type="secondary">最新角色详情</Text>
          <Paragraph ellipsis={{ rows: 4, expandable: true, symbol: '展开全部' }}>
            {log.characterDetailsAfter}
          </Paragraph>
        </div>
        <div>
          <Text type="secondary">关系变化</Text>
          {renderRelationshipChanges(log)}
        </div>
        <Space>
          <Tag color="blue">第{log.chapterNumber}章</Tag>
          <Tag color="purple">第{log.sectionNumber}节</Tag>
          {log.isAutoCopied && <Tag>自动继承</Tag>}
        </Space>
      </Space>
    </Card>
  );

  return (
    <Card
      title={<Title level={5} style={{ margin: 0 }}>角色状态追踪</Title>}
      extra={
        <Space>
          <Button icon={<NodeIndexOutlined />} size="small" onClick={onOpenRelationshipGraph}>
            关系图谱
          </Button>
          <Button icon={<HistoryOutlined />} size="small" onClick={onOpenGrowthPath}>
            成长轨迹
            {turningPointCount > 0 && (
              <Tag color="gold" style={{ marginLeft: 8 }}>{turningPointCount}</Tag>
            )}
          </Button>
        </Space>
      }
      loading={loading}
      style={{ height: '100%', overflow: 'auto' }}
      bodyStyle={{ maxHeight: '100%', overflowY: 'auto' }}
    >
      {currentChapterNumber == null || currentSectionNumber == null ? (
        <Empty description="请选择章节场景以查看角色变化" />
      ) : currentLogs.length === 0 ? (
        <Empty description="本节暂时没有角色变化记录" />
      ) : (
        currentLogs
          .sort((a, b) => a.characterName.localeCompare(b.characterName, 'zh-CN'))
          .map((log) => renderLogCard(log))
      )}
    </Card>
  );
};

export default CharacterStatusSidebar;
