import { useEffect, useState, useMemo } from 'react';
import { Card, Typography, Spin, Alert, Table, List, Space, Button } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { fetchPromptTemplateMetadata } from '../services/api';
import type { PromptTemplateMetadata, PromptTypeMetadata, PromptFunctionMetadata, PromptVariableMetadata } from '../types';

const { Title, Paragraph, Text } = Typography;

type FunctionRow = PromptFunctionMetadata & { key: string };
type VariableRow = PromptVariableMetadata & { key: string };

type TableColumns<T> = ColumnsType<T>;

const PromptHelpPage = () => {
    const navigate = useNavigate();
    const [metadata, setMetadata] = useState<PromptTemplateMetadata | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const loadMetadata = async () => {
            setLoading(true);
            setError(null);
            try {
                const data = await fetchPromptTemplateMetadata();
                setMetadata(data);
            } catch (err) {
                console.error('Failed to load prompt template metadata:', err);
                const errorMessage = err instanceof Error ? err.message : '未知错误';
                setError(`获取提示词帮助数据失败：${errorMessage}`);
            } finally {
                setLoading(false);
            }
        };
        loadMetadata();
    }, []);

    const functionColumns: TableColumns<FunctionRow> = useMemo(() => [
        { title: '函数', dataIndex: 'name', key: 'name', width: 220 },
        { title: '作用', dataIndex: 'description', key: 'description' },
        {
            title: '使用示例',
            dataIndex: 'usage',
            key: 'usage',
            render: (value: string) => <Text code>{value}</Text>,
        },
    ], []);

    const variableColumns: TableColumns<VariableRow> = useMemo(() => [
        { title: '变量名', dataIndex: 'name', key: 'name', width: 220 },
        { title: '类型', dataIndex: 'valueType', key: 'valueType', width: 120 },
        { title: '说明', dataIndex: 'description', key: 'description' },
    ], []);

    const buildVariableRows = (template: PromptTypeMetadata): VariableRow[] => {
        return template.variables.map((variable, index) => ({
            key: `${template.type}-${index}`,
            ...variable,
        }));
    };

    const functionRows: FunctionRow[] = metadata
        ? metadata.functions.map((func, index) => ({ key: `${func.name}-${index}`, ...func }))
        : [];

    return (
        <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
            <Card style={{ maxWidth: 1100, margin: '0 auto' }}>
                <Space direction="vertical" size={24} style={{ width: '100%' }}>
                    <Space align="center" style={{ justifyContent: 'space-between', width: '100%', flexWrap: 'wrap', gap: 12 }}>
                        <Title level={2} style={{ margin: 0 }}>提示词编写帮助</Title>
                        <Button onClick={() => navigate('/settings')} type="primary">
                            返回设置
                        </Button>
                    </Space>
                    <Paragraph>
                        模板支持使用 <Text code>{'${变量}'}</Text> 语法插入上下文数据，并可通过管道函数调整输出格式。本页整理了全部可用的变量、函数与常见示例，帮助你更高效地编写提示词。
                    </Paragraph>
                    {error && <Alert type="error" showIcon message={error} />}
                    <Spin spinning={loading}>
                        <Space direction="vertical" size={16} style={{ width: '100%' }}>
                            {metadata && (
                                <>
                                    <Card type="inner" title="插值语法速览">
                                        <List
                                            dataSource={metadata.syntaxTips}
                                            renderItem={(item, index) => (
                                                <List.Item key={`${index}-${item}`}>{item}</List.Item>
                                            )}
                                            bordered
                                            split
                                        />
                                    </Card>
                                    <Card type="inner" title="支持的函数">
                                        <Table
                                            columns={functionColumns}
                                            dataSource={functionRows}
                                            pagination={false}
                                            size="middle"
                                        />
                                    </Card>
                                    {metadata.templates.map(template => (
                                        <Card
                                            type="inner"
                                            key={template.type}
                                            title={`${template.label}（${template.type}）可用变量`}
                                        >
                                            <Table
                                                columns={variableColumns}
                                                dataSource={buildVariableRows(template)}
                                                pagination={false}
                                                size="middle"
                                            />
                                        </Card>
                                    ))}
                                    {metadata.examples.length > 0 && (
                                        <Card type="inner" title="常用示例">
                                            <List
                                                dataSource={metadata.examples}
                                                renderItem={(example, index) => (
                                                    <List.Item key={`${index}-${example}`}>
                                                        <Text code>{example}</Text>
                                                    </List.Item>
                                                )}
                                                bordered
                                                split
                                            />
                                        </Card>
                                    )}
                                </>
                            )}
                        </Space>
                    </Spin>
                    <Paragraph type="secondary">
                        提示：想快速恢复系统模板，可以在设置页点击“恢复默认”。修改后的模板仅对当前账号生效。
                    </Paragraph>
                </Space>
            </Card>
        </div>
    );
};

export default PromptHelpPage;
