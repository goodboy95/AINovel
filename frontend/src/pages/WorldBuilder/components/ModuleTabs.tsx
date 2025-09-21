import React, { useMemo } from 'react';
import { Alert, Button, Card, Empty, Space, Tabs, Tag, Typography } from 'antd';
import type { TabsProps } from 'antd';
import FieldCard from './FieldCard';
import type { WorldModule, WorldModuleDefinition, WorldModuleStatus } from '../../../types';

const { Text, Paragraph } = Typography;

type ModuleTabsProps = {
    modules: WorldModule[];
    definitions: WorldModuleDefinition[];
    disabled?: boolean;
    generatingModules: Set<string>;
    onFieldChange: (moduleKey: string, fieldKey: string, value: string) => void;
    onOpenRefine: (moduleKey: string, fieldKey: string, fieldLabel: string, originalText: string) => void;
    onGenerateModule: (moduleKey: string) => void;
    onViewFullContent: (moduleKey: string, label: string, content: string) => void;
};

const moduleStatusMeta: Record<WorldModuleStatus, { color: string; text: string }> = {
    EMPTY: { color: 'default', text: '未填写' },
    IN_PROGRESS: { color: 'processing', text: '填写中' },
    READY: { color: 'warning', text: '待发布' },
    AWAITING_GENERATION: { color: 'purple', text: '待生成' },
    GENERATING: { color: 'processing', text: '生成中' },
    COMPLETED: { color: 'success', text: '已完成' },
    FAILED: { color: 'error', text: '生成失败' },
};

const ModuleTabs: React.FC<ModuleTabsProps> = ({
    modules,
    definitions,
    disabled,
    generatingModules,
    onFieldChange,
    onOpenRefine,
    onGenerateModule,
    onViewFullContent,
}) => {
    const moduleMap = useMemo(() => new Map(modules.map(module => [module.key, module])), [modules]);

    if (!definitions.length) {
        return <Empty description="暂无模块定义" />;
    }

    const items: TabsProps['items'] = definitions.map(definition => {
        const module = moduleMap.get(definition.key);
        const status = module?.status ?? 'EMPTY';
        const statusMeta = moduleStatusMeta[status];
        const isGenerating = generatingModules.has(definition.key);
        const fieldDisabled = disabled || status === 'GENERATING';
        const canViewFullContent = Boolean(module?.fullContent);

        return {
            key: definition.key,
            label: (
                <Space size={8}>
                    <span>{definition.label}</span>
                    <Tag color={statusMeta.color}>{statusMeta.text}</Tag>
                </Space>
            ),
            children: (
                <div>
                    <Card size="small" bordered={false} style={{ background: '#f6f8ff', marginBottom: 16 }}>
                        <Space style={{ width: '100%', justifyContent: 'space-between', alignItems: 'flex-start' }} wrap>
                            <Space direction="vertical" size={4} style={{ maxWidth: '70%' }}>
                                <Text strong>{definition.label}</Text>
                                {definition.description && (
                                    <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                                        {definition.description}
                                    </Paragraph>
                                )}
                            </Space>
                            <Space size={8} wrap>
                                {canViewFullContent && (
                                    <Button
                                        size="small"
                                        onClick={() => onViewFullContent(definition.key, definition.label, module?.fullContent ?? '')}
                                    >
                                        查看完整信息
                                    </Button>
                                )}
                                <Button
                                    size="small"
                                    type="primary"
                                    ghost
                                    onClick={() => onGenerateModule(definition.key)}
                                    loading={isGenerating}
                                    disabled={disabled}
                                >
                                    AI 自动生成本模块
                                </Button>
                            </Space>
                        </Space>
                    </Card>

                    {status === 'FAILED' && (
                        <Alert
                            type="error"
                            showIcon
                            message={`${definition.label} 生成失败`}
                            description="请检查字段内容后重新排队或修改文本。"
                            style={{ marginBottom: 16 }}
                        />
                    )}

                    {definition.fields.map(field => (
                        <FieldCard
                            key={field.key}
                            label={field.label}
                            tooltip={field.tooltip}
                            required
                            recommendedLength={field.recommendedLength}
                            value={module?.fields?.[field.key] ?? ''}
                            disabled={fieldDisabled || disabled}
                            onChange={text => onFieldChange(definition.key, field.key, text)}
                            onOptimize={() => onOpenRefine(definition.key, field.key, field.label, module?.fields?.[field.key] ?? '')}
                        />
                    ))}
                </div>
            ),
        };
    });

    return (
        <Card style={{ marginTop: 16 }}>
            <Tabs items={items} destroyInactiveTabPane={false} />
        </Card>
    );
};

export default ModuleTabs;
