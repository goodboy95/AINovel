import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Divider, Drawer, Space, Spin, Tag, Typography } from 'antd';
import { fetchWorldFull } from '../services/api';
import type { WorldFull } from '../types';

const { Title, Paragraph, Text } = Typography;

export interface WorldReferenceDrawerProps {
  worldId?: number | null;
  open: boolean;
  onClose: () => void;
}

const WorldReferenceDrawer: React.FC<WorldReferenceDrawerProps> = ({ worldId, open, onClose }) => {
  const [data, setData] = useState<WorldFull | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !worldId) {
      return;
    }
    let mounted = true;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchWorldFull(worldId);
        if (mounted) {
          setData(result);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : '获取世界详情失败');
          setData(null);
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      mounted = false;
    };
  }, [open, worldId]);

  const themeTags = useMemo(() => data?.world.themes ?? [], [data]);

  return (
    <Drawer
      title={data?.world.name ? `世界设定：${data.world.name}` : '世界设定'}
      width={520}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      {loading && (
        <div style={{ display: 'flex', justifyContent: 'center', marginTop: 40 }}>
          <Spin tip="加载世界设定中..." />
        </div>
      )}
      {!loading && error && <Alert type="error" message={error} showIcon />}
      {!loading && !error && data && (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div>
            <Title level={4} style={{ marginBottom: 8 }}>
              {data.world.name}
            </Title>
            <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
              {data.world.tagline}
            </Text>
            {themeTags.length > 0 && (
              <div>
                {themeTags.map((theme) => (
                  <Tag key={theme} color="geekblue" style={{ marginBottom: 6 }}>
                    {theme}
                  </Tag>
                ))}
              </div>
            )}
            {data.world.creativeIntent && (
              <Paragraph style={{ whiteSpace: 'pre-wrap', marginTop: 12 }}>
                <Text strong>创作意图：</Text>
                <br />
                {data.world.creativeIntent}
              </Paragraph>
            )}
          </div>
          <Divider plain>模块设定</Divider>
          {data.modules.length === 0 && <Text type="secondary">暂无完整模块信息。</Text>}
          {data.modules.map((module) => (
            <div key={module.key}>
              <Title level={5}>{module.label}</Title>
              {module.updatedAt && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  更新时间：{new Date(module.updatedAt).toLocaleString()}
                </Text>
              )}
              {module.excerpt && (
                <Paragraph type="secondary" style={{ marginTop: 8 }}>
                  {module.excerpt}
                </Paragraph>
              )}
              <Paragraph style={{ whiteSpace: 'pre-wrap' }}>{module.fullContent || '（暂无内容）'}</Paragraph>
              <Divider />
            </div>
          ))}
        </Space>
      )}
      {!loading && !error && !data && (
        <Text type="secondary">请选择一个世界以查看详细设定。</Text>
      )}
    </Drawer>
  );
};

export default WorldReferenceDrawer;
