import { useEffect, useMemo, useState } from 'react';
import { Card, Typography, Spin, Alert, Table, List, Space, Button, Anchor, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { fetchWorldPromptMetadata } from '../services/api';
import type {
    WorldPromptMetadata,
    WorldPromptVariableMetadata,
    WorldPromptFunctionMetadata,
    WorldPromptModuleMetadata,
} from '../types';

const { Title, Paragraph, Text } = Typography;

type VariableRow = WorldPromptVariableMetadata & { key: string };
type FunctionRow = WorldPromptFunctionMetadata & { key: string };

interface ModuleFieldRow {
    key: string;
    fieldKey: string;
    label: string;
    recommendedLength?: string;
}

const overviewTips = [
    '模块自动生成模板必须返回合法 JSON，键名需与模块字段 key 完全一致。',
    '正式创建模板建议输出结构化段落文本，可包含列表或加粗，但请避免 Markdown 标题，以便直接注入工作台。',
    '字段优化模板只需维护整体骨架，系统会注入字段原文与 focus note，帮助模型聚焦修改重点。',
    '可以通过 ${helper.fieldDefinitions[*]|map(f -> "- " + f.label + "：" + f.recommendedLength)|join("\\n")} 提醒模型字数范围。',
    '若需要引用其他模块，可配合 map()/join() 函数拼接相关摘要，确保跨模块信息一致。',
];

const faqItems = [
    {
        question: '如何引用其他模块的信息？',
        answer:
            '使用 relatedModules 列表与 map()/join() 函数。例如：${relatedModules[*]|map(m -> "- " + m.label + "摘要：" + m.summary)|join("\\n")}。',
    },
    {
        question: '如何避免 JSON 解析失败？',
        answer:
            '请确保模板最后只输出 JSON 对象，不要追加额外说明。可在调试阶段加入 ${module|json()} 检查上下文结构，完成后记得删除。',
    },
    {
        question: 'headline() 与 truncate() 有什么用途？',
        answer:
            'headline() 会将文本转为醒目的标题格式，truncate(n) 可以限制摘要长度，便于在提示词中引用精炼信息。',
    },
];

const WorldPromptHelpPage = () => {
    const navigate = useNavigate();
    const [metadata, setMetadata] = useState<WorldPromptMetadata | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const loadMetadata = async () => {
            setLoading(true);
            setError(null);
            try {
                const data = await fetchWorldPromptMetadata();
                setMetadata(data);
            } catch (err) {
                console.error('Failed to load world prompt metadata:', err);
                const message = err instanceof Error ? err.message : '未知错误';
                setError(`获取世界提示词帮助数据失败：${message}`);
            } finally {
                setLoading(false);
            }
        };
        loadMetadata();
    }, []);

    const anchorItems = useMemo(
        () => [
            { key: 'overview', href: '#world-prompt-overview', title: '概述' },
            { key: 'variables', href: '#world-prompt-variables', title: '可用变量' },
            { key: 'functions', href: '#world-prompt-functions', title: '模板函数' },
            { key: 'modules', href: '#world-prompt-modules', title: '模块字段' },
            { key: 'examples', href: '#world-prompt-examples', title: '示例片段' },
            { key: 'faq', href: '#world-prompt-faq', title: '常见问题' },
        ],
        [],
    );

    const variableColumns: ColumnsType<VariableRow> = useMemo(
        () => [
            { title: '变量名', dataIndex: 'name', key: 'name', width: 240 },
            { title: '类型', dataIndex: 'valueType', key: 'valueType', width: 120 },
            { title: '说明', dataIndex: 'description', key: 'description' },
        ],
        [],
    );

    const functionColumns: ColumnsType<FunctionRow> = useMemo(
        () => [
            { title: '函数', dataIndex: 'name', key: 'name', width: 200 },
            { title: '作用', dataIndex: 'description', key: 'description' },
            {
                title: '使用示例',
                dataIndex: 'usage',
                key: 'usage',
                render: (value: string) => <Text code>{value}</Text>,
            },
        ],
        [],
    );

    const moduleFieldColumns: ColumnsType<ModuleFieldRow> = useMemo(
        () => [
            { title: '字段 Key', dataIndex: 'fieldKey', key: 'fieldKey', width: 180 },
            { title: '字段名称', dataIndex: 'label', key: 'label', width: 200 },
            { title: '推荐字数', dataIndex: 'recommendedLength', key: 'recommendedLength' },
        ],
        [],
    );

    const variableRows: VariableRow[] = useMemo(() => {
        if (!metadata) {
            return [];
        }
        return metadata.variables.map((variable, index) => ({ key: `${variable.name}-${index}`, ...variable }));
    }, [metadata]);

    const functionRows: FunctionRow[] = useMemo(() => {
        if (!metadata) {
            return [];
        }
        return metadata.functions.map((func, index) => ({ key: `${func.name}-${index}`, ...func }));
    }, [metadata]);

    const buildModuleFieldRows = (module: WorldPromptModuleMetadata): ModuleFieldRow[] => {
        if (!module.fields || module.fields.length === 0) {
            return [];
        }
        return module.fields.map(field => ({
            key: field.key,
            fieldKey: field.key,
            label: field.label,
            recommendedLength: field.recommendedLength || '建议 200-400 字',
        }));
    };

    return (
        <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
            <Card style={{ maxWidth: 1100, margin: '0 auto' }}>
                <Space direction="vertical" size={24} style={{ width: '100%' }}>
                    <Space align="start" style={{ justifyContent: 'space-between', width: '100%', flexWrap: 'wrap', gap: 16 }}>
                        <Space direction="vertical" size={8} style={{ flex: 1, minWidth: 260 }}>
                            <Title level={2} style={{ margin: 0 }}>世界构建提示词编写帮助</Title>
                            <Paragraph type="secondary" style={{ margin: 0 }}>
                                本页面汇总世界构建模板可用的变量、函数与字段说明，帮助你自定义模块自动生成、正式创建与字段优化的提示词。
                            </Paragraph>
                        </Space>
                        <Space direction="vertical" align="end" size={16} style={{ minWidth: 200 }}>
                            <Button onClick={() => navigate('/settings')} type="primary">
                                返回设置
                            </Button>
                            <Anchor items={anchorItems} affix={false} />
                        </Space>
                    </Space>
                    {error && <Alert type="error" showIcon message={error} />}
                    <Spin spinning={loading}>
                        <Space direction="vertical" size={24} style={{ width: '100%' }}>
                            <section id="world-prompt-overview">
                                <Card type="inner" title="编写要点">
                                    <List
                                        dataSource={overviewTips}
                                        renderItem={(item, index) => <List.Item key={`${index}-${item}`}>{item}</List.Item>}
                                        bordered
                                        split
                                    />
                                </Card>
                            </section>
                            <section id="world-prompt-variables">
                                <Card type="inner" title="可用变量">
                                    {metadata ? (
                                        <Table
                                            columns={variableColumns}
                                            dataSource={variableRows}
                                            pagination={false}
                                            size="middle"
                                        />
                                    ) : (
                                        <Paragraph type="secondary">正在加载变量列表……</Paragraph>
                                    )}
                                </Card>
                            </section>
                            <section id="world-prompt-functions">
                                <Card type="inner" title="模板函数">
                                    {metadata ? (
                                        <Table
                                            columns={functionColumns}
                                            dataSource={functionRows}
                                            pagination={false}
                                            size="middle"
                                        />
                                    ) : (
                                        <Paragraph type="secondary">正在加载函数说明……</Paragraph>
                                    )}
                                </Card>
                            </section>
                            <section id="world-prompt-modules">
                                <Card type="inner" title="模块字段详情">
                                    {metadata ? (
                                        <Space direction="vertical" size={16} style={{ width: '100%' }}>
                                            {metadata.modules.map(module => (
                                                <Card
                                                    key={module.key}
                                                    type="inner"
                                                    title={
                                                        <Space align="center" size={8}>
                                                            <Text strong>{module.label}</Text>
                                                            <Tag color="blue">{module.key}</Tag>
                                                        </Space>
                                                    }
                                                >
                                                    {module.fields.length > 0 ? (
                                                        <Table
                                                            columns={moduleFieldColumns}
                                                            dataSource={buildModuleFieldRows(module)}
                                                            pagination={false}
                                                            size="small"
                                                        />
                                                    ) : (
                                                        <Paragraph type="secondary">该模块暂未定义字段。</Paragraph>
                                                    )}
                                                </Card>
                                            ))}
                                        </Space>
                                    ) : (
                                        <Paragraph type="secondary">正在加载模块字段……</Paragraph>
                                    )}
                                </Card>
                            </section>
                            <section id="world-prompt-examples">
                                <Card type="inner" title="示例片段">
                                    {metadata ? (
                                        <List
                                            dataSource={metadata.examples}
                                            bordered
                                            split
                                            renderItem={(example, index) => (
                                                <List.Item key={`${index}-${example}`}>
                                                    <Text code>{example}</Text>
                                                </List.Item>
                                            )}
                                        />
                                    ) : (
                                        <Paragraph type="secondary">正在整理示例……</Paragraph>
                                    )}
                                </Card>
                            </section>
                            <section id="world-prompt-faq">
                                <Card type="inner" title="常见问题">
                                    <List
                                        dataSource={faqItems}
                                        renderItem={item => (
                                            <List.Item key={item.question}>
                                                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                                                    <Text strong>{item.question}</Text>
                                                    <Paragraph style={{ marginBottom: 0 }}>{item.answer}</Paragraph>
                                                </Space>
                                            </List.Item>
                                        )}
                                        bordered
                                        split
                                    />
                                </Card>
                            </section>
                            <Paragraph type="secondary">
                                提示：如需恢复默认模板，可在设置页选择“世界构建提示词配置”并点击“恢复默认”。修改后的模板仅对当前账号生效。
                            </Paragraph>
                        </Space>
                    </Spin>
                </Space>
            </Card>
        </div>
    );
};

export default WorldPromptHelpPage;
