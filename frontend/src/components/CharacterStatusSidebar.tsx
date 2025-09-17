import React, { useMemo, useState } from 'react';
import { Card, Collapse, Empty, Button, Typography, Space, Tag, Modal, List, Spin } from 'antd';
import type { CharacterCard, CharacterChangeLog } from '../types';

const { Text, Paragraph } = Typography;

export interface CharacterStatusSidebarProps {
  characterMap: Record<number, CharacterCard>;
  logs: CharacterChangeLog[];
  chapterNumber?: number;
  sectionNumber?: number;
  isAnalyzing: boolean;
  onAnalyze?: () => void;
}

const CharacterStatusSidebar: React.FC<CharacterStatusSidebarProps> = ({
  characterMap,
  logs,
  chapterNumber,
  sectionNumber,
  isAnalyzing,
  onAnalyze,
}) => {
  const [historyVisible, setHistoryVisible] = useState(false);

  const locationLabel = useMemo(() => {
    if (chapterNumber && sectionNumber) {
      return `第${chapterNumber}章·第${sectionNumber}节`;
    }
    if (chapterNumber) {
      return `第${chapterNumber}章`;
    }
    return '当前场景';
  }, [chapterNumber, sectionNumber]);

  const sortedLogs = useMemo(() => {
    return [...logs].sort((a, b) => a.characterId - b.characterId);
  }, [logs]);

  const collapseItems = useMemo(
    () =>
      sortedLogs.map((log) => ({
        key: String(log.id),
        label: (
          <Space>
            <span>{characterMap[log.characterId]?.name || `角色 #${log.characterId}`}</span>
            {log.isAutoCopied && <Tag color="default">无变化</Tag>}
          </Space>
        ),
        children: (
          <Space direction="vertical" style={{ width: '100%' }}>
            <div>
              <Text strong>新增认知：</Text>
              <Paragraph type={log.newlyKnownInfo ? undefined : 'secondary'}>
                {log.newlyKnownInfo && log.newlyKnownInfo.trim() ? log.newlyKnownInfo : '无'}
              </Paragraph>
            </div>
            <div>
              <Text strong>角色变化：</Text>
              <Paragraph type={log.characterChanges ? undefined : 'secondary'}>
                {log.characterChanges && log.characterChanges.trim() ? log.characterChanges : '无'}
              </Paragraph>
            </div>
            <div>
              <Text strong>角色详情：</Text>
              <Paragraph ellipsis={{ rows: 4, expandable: true, symbol: '展开' }}>
                {log.characterDetailsAfter}
              </Paragraph>
            </div>
            <Text type="secondary">更新于：{new Date(log.createdAt).toLocaleString()}</Text>
          </Space>
        ),
      })),
    [sortedLogs, characterMap]
  );

  return (
    <Card
      title={`角色状态追踪（${locationLabel}）`}
      style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
      bodyStyle={{ flex: 1, overflowY: 'auto', paddingRight: 12 }}
      extra={
        onAnalyze ? (
          <Button type="primary" size="small" onClick={onAnalyze} loading={isAnalyzing}>
            重新分析
          </Button>
        ) : undefined
      }
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <Text type="secondary">基于最新正文的角色状态总结</Text>
          <Button type="link" size="small" onClick={() => setHistoryVisible(true)} disabled={sortedLogs.length === 0}>
            历史记录
          </Button>
        </div>
        <Spin spinning={isAnalyzing} tip="分析中...">
          {sortedLogs.length === 0 ? (
            <Empty description="暂无分析记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          ) : (
            <Collapse bordered={false} items={collapseItems} size="small" />
          )}
        </Spin>
      </Space>
      <Modal
        title="角色状态历史"
        open={historyVisible}
        onCancel={() => setHistoryVisible(false)}
        footer={null}
        width={560}
      >
        {sortedLogs.length === 0 ? (
          <Empty description="暂无记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          <List
            dataSource={sortedLogs}
            renderItem={(log) => (
              <List.Item key={log.id} style={{ alignItems: 'flex-start' }}>
                <List.Item.Meta
                  title={`${characterMap[log.characterId]?.name || `角色 #${log.characterId}`} · ${new Date(log.createdAt).toLocaleString()}`}
                  description={
                    <Space direction="vertical" style={{ width: '100%' }} size={4}>
                      <div>
                        <Text strong>角色变化：</Text>
                        <Text>{log.characterChanges && log.characterChanges.trim() ? log.characterChanges : '无'}</Text>
                      </div>
                      <div>
                        <Text strong>角色详情：</Text>
                        <Paragraph>{log.characterDetailsAfter}</Paragraph>
                      </div>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Modal>
    </Card>
  );
};

export default CharacterStatusSidebar;

