import React from 'react';
import { Button, Space, Typography } from 'antd';
import type { WorldStatus } from '../../../types';

const { Text } = Typography;

export type SaveState = 'saved' | 'saving' | 'pending' | 'error';

type StickyFooterBarProps = {
    worldStatus?: WorldStatus;
    version?: number | null;
    saveState: SaveState;
    hasUnsavedChanges: boolean;
    onSave: () => void;
    onPublish: () => void;
    savingDisabled?: boolean;
    publishing: boolean;
    canPublish: boolean;
};

const worldStatusLabel: Record<WorldStatus, string> = {
    DRAFT: '草稿',
    GENERATING: '生成中',
    ACTIVE: '已发布',
    ARCHIVED: '已归档',
};

const saveStateLabel: Record<SaveState, { text: string; color: string }> = {
    saved: { text: '已保存', color: '#52c41a' },
    saving: { text: '保存中…', color: '#1677ff' },
    pending: { text: '存在未保存的修改', color: '#faad14' },
    error: { text: '保存失败，请重试', color: '#ff4d4f' },
};

const StickyFooterBar: React.FC<StickyFooterBarProps> = ({
    worldStatus,
    version,
    saveState,
    hasUnsavedChanges,
    onSave,
    onPublish,
    savingDisabled,
    publishing,
    canPublish,
}) => {
    const statusText = worldStatus ? worldStatusLabel[worldStatus] : '未选择世界';
    const saveMeta = saveStateLabel[saveState];

    return (
        <div
            style={{
                position: 'sticky',
                bottom: 0,
                background: '#fff',
                borderTop: '1px solid #f0f0f0',
                padding: '12px 24px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                zIndex: 10,
            }}
        >
            <Space direction="vertical" size={0}>
                <Text strong>{statusText}{typeof version === 'number' ? ` · 版本 ${version}` : ''}</Text>
                <Text style={{ color: saveMeta.color }}>{saveMeta.text}</Text>
            </Space>
            <Space size={12}>
                <Button
                    onClick={onSave}
                    disabled={savingDisabled || (!hasUnsavedChanges && saveState !== 'error')}
                    loading={saveState === 'saving'}
                >
                    保存草稿
                </Button>
                <Button
                    type="primary"
                    onClick={onPublish}
                    disabled={!canPublish}
                    loading={publishing}
                >
                    正式创建世界
                </Button>
            </Space>
        </div>
    );
};

export default StickyFooterBar;
