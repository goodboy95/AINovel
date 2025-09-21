import React, { useMemo } from 'react';
import { Card, Form, Input, Select, Space, Tooltip, Typography } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import type { WorldBasicInfoDefinition, WorldFieldDefinition } from '../../../types';

export type MetadataFormValue = {
    name: string;
    tagline: string;
    themes: string[];
    creativeIntent: string;
    notes?: string | null;
};

type MetadataFormProps = {
    value?: MetadataFormValue;
    definition?: WorldBasicInfoDefinition;
    disabled?: boolean;
    onChange: <K extends keyof MetadataFormValue>(field: K, value: MetadataFormValue[K]) => void;
};

const renderLabel = (definition?: WorldFieldDefinition) => {
    if (!definition) return null;
    return (
        <Space size={4} align="center">
            <span>{definition.label}</span>
            {definition.tooltip && (
                <Tooltip title={definition.tooltip} placement="top">
                    <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                </Tooltip>
            )}
        </Space>
    );
};

const MetadataForm: React.FC<MetadataFormProps> = ({ value, definition, disabled, onChange }) => {
    const definitionMap = useMemo(() => {
        const entries = definition?.fields?.map(field => [field.key, field] as const) ?? [];
        return Object.fromEntries(entries) as Record<string, WorldFieldDefinition>;
    }, [definition]);

    if (!value) {
        return null;
    }

    return (
        <Card title="基础信息" style={{ marginBottom: 16 }}>
            {definition?.description && (
                <Typography.Paragraph type="secondary" style={{ marginTop: 0 }}>
                    {definition.description}
                </Typography.Paragraph>
            )}
            <Form layout="vertical" style={{ maxWidth: 960 }}>
                <Form.Item label={renderLabel(definitionMap.name)} extra={definitionMap.name?.recommendedLength} required>
                    <Input
                        value={value.name}
                        disabled={disabled}
                        onChange={e => onChange('name', e.target.value)}
                        placeholder="请输入世界名称"
                        maxLength={80}
                    />
                </Form.Item>
                <Form.Item label={renderLabel(definitionMap.tagline)} extra={definitionMap.tagline?.recommendedLength} required>
                    <Input
                        value={value.tagline}
                        disabled={disabled}
                        onChange={e => onChange('tagline', e.target.value)}
                        placeholder="请输入一句话概述"
                        maxLength={180}
                    />
                </Form.Item>
                <Form.Item label={renderLabel(definitionMap.themes)} extra="可输入 1-5 个主题标签" required>
                    <Select
                        mode="tags"
                        disabled={disabled}
                        value={value.themes}
                        onChange={items => onChange('themes', items as string[])}
                        tokenSeparators={[',', ' ', '、']}
                        placeholder="添加主题标签"
                    />
                </Form.Item>
                <Form.Item
                    label={renderLabel(definitionMap.creativeIntent)}
                    extra={definitionMap.creativeIntent?.recommendedLength}
                    required
                >
                    <Input.TextArea
                        value={value.creativeIntent}
                        disabled={disabled}
                        onChange={e => onChange('creativeIntent', e.target.value)}
                        autoSize={{ minRows: 4, maxRows: 8 }}
                        placeholder="描述创作意图、灵感来源与创作边界"
                    />
                </Form.Item>
                <Form.Item
                    label={renderLabel(definitionMap.notes)}
                    extra={definitionMap.notes?.recommendedLength}
                >
                    <Input.TextArea
                        value={value.notes ?? ''}
                        disabled={disabled}
                        onChange={e => onChange('notes', e.target.value)}
                        autoSize={{ minRows: 2, maxRows: 6 }}
                        placeholder="（可选）补充开发者备注"
                    />
                </Form.Item>
            </Form>
        </Card>
    );
};

export default MetadataForm;
