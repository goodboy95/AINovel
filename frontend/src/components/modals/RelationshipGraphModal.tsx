import React, { useMemo } from 'react';
import { Modal, Tabs, Timeline, Empty, Typography } from 'antd';
import type { CharacterChangeLog } from '../../types';

interface RelationshipGraphModalProps {
  open: boolean;
  onClose: () => void;
  logs: CharacterChangeLog[];
}

const RelationshipGraphModal: React.FC<RelationshipGraphModalProps> = ({ open, onClose, logs }) => {
  const groupedByCharacter = useMemo(() => {
    const result = new Map<number, { name: string; entries: Array<{ key: string; label: string; description: string; order: number }> }>();
    logs.forEach((log) => {
      if (!log.relationshipChanges || log.relationshipChanges.length === 0) return;
      const existing = result.get(log.characterId) ?? { name: log.characterName, entries: [] };
      log.relationshipChanges.forEach((change, idx) => {
        const label = `第${log.chapterNumber}章第${log.sectionNumber}节`;
        const description = `${change.previousRelationship || '未知'} → ${change.currentRelationship || '未知'} | 原因：${change.changeReason || '未说明'} | 对象角色ID：${change.targetCharacterId}`;
        existing.entries.push({
          key: `${log.id}-${idx}`,
          label,
          description,
          order: (log.chapterNumber ?? 0) * 1000 + (log.sectionNumber ?? 0),
        });
      });
      result.set(log.characterId, existing);
    });
    return Array.from(result.entries()).map(([characterId, data]) => ({
      characterId,
      name: data.name,
      entries: data.entries.sort((a, b) => a.order - b.order),
    }));
  }, [logs]);

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title="角色关系图谱"
      width={720}
      footer={null}
      destroyOnClose
    >
      {groupedByCharacter.length === 0 ? (
        <Empty description="暂无关系变化记录" />
      ) : (
        <Tabs
          items={groupedByCharacter.map((item) => ({
            key: item.characterId.toString(),
            label: item.name,
            children: (
              <Timeline>
                {item.entries.map((entry) => (
                  <Timeline.Item key={entry.key}>
                    <Typography.Text strong>{entry.label}</Typography.Text>
                    <div>{entry.description}</div>
                  </Timeline.Item>
                ))}
              </Timeline>
            ),
          }))}
        />
      )}
    </Modal>
  );
};

export default RelationshipGraphModal;

