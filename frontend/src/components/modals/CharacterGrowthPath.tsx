import React, { useMemo, useState, useEffect } from 'react';
import { Drawer, Select, Timeline, Empty, Typography } from 'antd';
import type { CharacterChangeLog } from '../../types';

interface CharacterGrowthPathProps {
  open: boolean;
  onClose: () => void;
  logs: CharacterChangeLog[];
}

const CharacterGrowthPath: React.FC<CharacterGrowthPathProps> = ({ open, onClose, logs }) => {
  const characterOptions = useMemo(() => {
    const map = new Map<number, string>();
    logs.forEach((log) => {
      map.set(log.characterId, log.characterName);
    });
    return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
  }, [logs]);

  const [selectedCharacterId, setSelectedCharacterId] = useState<number | null>(null);

  useEffect(() => {
    if (!open) return;
    if (characterOptions.length === 0) {
      setSelectedCharacterId(null);
    } else if (!selectedCharacterId) {
      setSelectedCharacterId(characterOptions[0].value);
    }
  }, [open, characterOptions, selectedCharacterId]);

  const turningPoints = useMemo(() => {
    if (!selectedCharacterId) return [];
    return logs
      .filter((log) => log.characterId === selectedCharacterId && log.isTurningPoint)
      .sort((a, b) => {
        if (a.chapterNumber === b.chapterNumber) {
          return a.sectionNumber - b.sectionNumber;
        }
        return a.chapterNumber - b.chapterNumber;
      });
  }, [logs, selectedCharacterId]);

  const selectedCharacterName = characterOptions.find((opt) => opt.value === selectedCharacterId)?.label;

  return (
    <Drawer
      open={open}
      width={520}
      title="角色成长轨迹"
      onClose={onClose}
    >
      <Typography.Paragraph>仅显示被标记为关键转折点的记录，帮助你快速回顾角色成长。</Typography.Paragraph>
      <Select
        style={{ width: '100%', marginBottom: 16 }}
        placeholder="请选择角色"
        value={selectedCharacterId ?? undefined}
        options={characterOptions}
        onChange={(value) => setSelectedCharacterId(value)}
      />
      {turningPoints.length === 0 ? (
        <Empty description="暂无关键转折点" />
      ) : (
        <Timeline>
          {turningPoints.map((log) => (
            <Timeline.Item key={log.id}>
              <Typography.Text strong>
                {selectedCharacterName} · 第{log.chapterNumber}章第{log.sectionNumber}节
              </Typography.Text>
              <Typography.Paragraph style={{ marginBottom: 8 }}>
                {log.characterChanges || '变化描述缺失'}
              </Typography.Paragraph>
              <Typography.Paragraph type="secondary">
                {log.newlyKnownInfo || '无新知信息'}
              </Typography.Paragraph>
            </Timeline.Item>
          ))}
        </Timeline>
      )}
    </Drawer>
  );
};

export default CharacterGrowthPath;
