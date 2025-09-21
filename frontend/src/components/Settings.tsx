import { useState, useEffect, useMemo, useCallback } from 'react';
import { Form, Input, Button, Card, Typography, message, Spin, Tabs, Space, Tag, Alert } from 'antd';
import type { TabsProps } from 'antd';
import { Link } from 'react-router-dom';
import { fetchPromptTemplates, updatePromptTemplates, resetPromptTemplates } from '../services/api';
import type { PromptTemplatesResponse, PromptTemplatesUpdatePayload } from '../types';

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;

interface SettingsValues {
    baseUrl?: string;
    modelName?: string;
    apiKey?: string;
}

type PromptFieldKey = 'storyCreation' | 'outlineChapter' | 'manuscriptSection' | 'refineWithInstruction' | 'refineWithoutInstruction';
type PromptFormValues = Record<PromptFieldKey, string>;

const promptFieldOrder: PromptFieldKey[] = [
    'storyCreation',
    'outlineChapter',
    'manuscriptSection',
    'refineWithInstruction',
    'refineWithoutInstruction',
];

const promptResetKeyMap: Record<PromptFieldKey, string> = {
    storyCreation: 'storyCreation',
    outlineChapter: 'outlineChapter',
    manuscriptSection: 'manuscriptSection',
    refineWithInstruction: 'refine.withInstruction',
    refineWithoutInstruction: 'refine.withoutInstruction',
};

const promptFieldConfigs: Record<PromptFieldKey, { label: string; description: string }> = {
    storyCreation: {
        label: '故事构思提示词',
        description: '用于根据用户想法生成故事卡片和主角设定。',
    },
    outlineChapter: {
        label: '大纲章节提示词',
        description: '用于编写章节级大纲与场景结构。',
    },
    manuscriptSection: {
        label: '正文创作提示词',
        description: '用于生成具体小节的小说正文。',
    },
    refineWithInstruction: {
        label: '文本润色（带指令）',
        description: '当你附带修改意见时使用此模板进行润色。',
    },
    refineWithoutInstruction: {
        label: '文本润色（无指令）',
        description: '仅需润色文本、无额外指令时使用。',
    },
};

const Settings = () => {
    const [form] = Form.useForm<SettingsValues>();
    const [activeTab, setActiveTab] = useState<'model' | 'prompts'>('model');

    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [isTesting, setIsTesting] = useState(false);
    const [apiKeyEditable, setApiKeyEditable] = useState(false);
    const [apiKeyIsSet, setApiKeyIsSet] = useState(false);

    const [promptValues, setPromptValues] = useState<PromptFormValues>({
        storyCreation: '',
        outlineChapter: '',
        manuscriptSection: '',
        refineWithInstruction: '',
        refineWithoutInstruction: '',
    });
    const [promptTemplatesData, setPromptTemplatesData] = useState<PromptTemplatesResponse | null>(null);
    const [isPromptLoading, setIsPromptLoading] = useState(false);
    const [isPromptSaving, setIsPromptSaving] = useState(false);
    const [promptError, setPromptError] = useState<string | null>(null);
    const [resettingKey, setResettingKey] = useState<PromptFieldKey | null>(null);

    useEffect(() => {
        const fetchSettings = async () => {
            setIsLoading(true);
            try {
                const response = await fetch('/api/v1/settings', {
                    headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` },
                });
                if (response.ok) {
                    const text = await response.text();
                    if (text) {
                        const data = JSON.parse(text) as SettingsValues & { apiKeyIsSet?: boolean };
                        setApiKeyIsSet(!!data.apiKeyIsSet);
                        setApiKeyEditable(!data.apiKeyIsSet);
                        form.setFieldsValue({
                            baseUrl: data.baseUrl || '',
                            modelName: data.modelName || '',
                            apiKey: data.apiKeyIsSet ? '********' : '',
                        });
                    } else {
                        setApiKeyIsSet(false);
                        setApiKeyEditable(true);
                        form.setFieldsValue({
                            baseUrl: '',
                            modelName: '',
                            apiKey: '',
                        });
                    }
                } else {
                    console.error('Failed to fetch settings: Response not OK', response.status);
                    message.error('获取设置失败。');
                }
            } catch (error: unknown) {
                console.error('Failed to fetch settings:', error);
                if (error instanceof SyntaxError) {
                    message.error('解析设置失败：从服务器返回的数据格式无效。');
                } else if (error instanceof Error) {
                    message.error(`获取设置时出错： ${error.message}`);
                } else {
                    message.error('获取设置时发生未知错误。');
                }
            } finally {
                setIsLoading(false);
            }
        };
        fetchSettings();
    }, [form]);

    const loadPromptTemplates = useCallback(async () => {
        setIsPromptLoading(true);
        setPromptError(null);
        try {
            const data = await fetchPromptTemplates();
            setPromptTemplatesData(data);
            const newValues: PromptFormValues = {
                storyCreation: data.storyCreation.content,
                outlineChapter: data.outlineChapter.content,
                manuscriptSection: data.manuscriptSection.content,
                refineWithInstruction: data.refine.withInstruction.content,
                refineWithoutInstruction: data.refine.withoutInstruction.content,
            };
            setPromptValues(newValues);
        } catch (error: unknown) {
            console.error('Failed to load prompt templates:', error);
            const errorMessage = error instanceof Error ? error.message : '未知错误';
            setPromptError(`加载提示词模板失败：${errorMessage}`);
            message.error(`加载提示词模板失败：${errorMessage}`);
        } finally {
            setIsPromptLoading(false);
        }
    }, []);

    useEffect(() => {
        loadPromptTemplates();
    }, [loadPromptTemplates]);

    const onFinish = async (values: SettingsValues) => {
        setIsSaving(true);
        try {
            const payload: Record<string, unknown> = {
                baseUrl: values.baseUrl ?? '',
                modelName: values.modelName ?? '',
            };
            if (apiKeyEditable && values.apiKey && values.apiKey.trim() !== '' && values.apiKey !== '********') {
                payload.apiKey = values.apiKey.trim();
            }

            const response = await fetch('/api/v1/settings', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`,
                },
                body: JSON.stringify(payload),
            });
            if (response.ok) {
                message.success('设置已成功保存！');
                if ('apiKey' in payload) {
                    setApiKeyIsSet(true);
                    setApiKeyEditable(false);
                    form.setFieldsValue({ apiKey: '********' });
                }
            } else {
                const data = await response.json();
                console.error('Failed to save settings: Response not OK', data);
                message.error(data.message || '保存设置失败。');
            }
        } catch (error: unknown) {
            console.error('Failed to save settings:', error);
            if (error instanceof Error) {
                message.error(`保存时发生错误： ${error.message}`);
            } else {
                message.error('保存时发生未知错误。');
            }
        } finally {
            setIsSaving(false);
        }
    };

    const handleTestConnection = async () => {
        try {
            const values = await form.validateFields();
            setIsTesting(true);

            const testPayload: Record<string, unknown> = {
                baseUrl: values.baseUrl ?? '',
                modelName: values.modelName ?? '',
            };
            if (apiKeyEditable && values.apiKey && values.apiKey.trim() !== '' && values.apiKey !== '********') {
                testPayload.apiKey = values.apiKey.trim();
            }

            const response = await fetch('/api/v1/settings/test', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`,
                },
                body: JSON.stringify(testPayload),
            });
            const data = await response.json();
            if (response.ok) {
                message.success(data.message || '连接成功！');
            } else {
                console.error('Connection test failed: Response not OK', data);
                message.error(data.message || '连接测试失败。');
            }
        } catch (error: unknown) {
            console.error('Failed to test connection:', error);
            if (error instanceof Error) {
                message.error(`测试期间发生错误： ${error.message}`);
            } else {
                message.error('测试期间发生未知错误。');
            }
        } finally {
            setIsTesting(false);
        }
    };

    const handlePromptChange = (key: PromptFieldKey, value: string) => {
        setPromptValues(prev => ({
            ...prev,
            [key]: value,
        } as PromptFormValues));
    };

    const getSavedContent = useCallback((key: PromptFieldKey): string => {
        if (!promptTemplatesData) {
            return '';
        }
        switch (key) {
            case 'storyCreation':
                return promptTemplatesData.storyCreation.content;
            case 'outlineChapter':
                return promptTemplatesData.outlineChapter.content;
            case 'manuscriptSection':
                return promptTemplatesData.manuscriptSection.content;
            case 'refineWithInstruction':
                return promptTemplatesData.refine.withInstruction.content;
            case 'refineWithoutInstruction':
                return promptTemplatesData.refine.withoutInstruction.content;
            default:
                return '';
        }
    }, [promptTemplatesData]);

    const getSavedIsDefault = useCallback((key: PromptFieldKey): boolean => {
        if (!promptTemplatesData) {
            return true;
        }
        switch (key) {
            case 'storyCreation':
                return promptTemplatesData.storyCreation.default;
            case 'outlineChapter':
                return promptTemplatesData.outlineChapter.default;
            case 'manuscriptSection':
                return promptTemplatesData.manuscriptSection.default;
            case 'refineWithInstruction':
                return promptTemplatesData.refine.withInstruction.default;
            case 'refineWithoutInstruction':
                return promptTemplatesData.refine.withoutInstruction.default;
            default:
                return true;
        }
    }, [promptTemplatesData]);

    const isPromptDirty = useMemo(() => {
        if (!promptTemplatesData) {
            return false;
        }
        return promptFieldOrder.some(key => promptValues[key] !== getSavedContent(key));
    }, [promptTemplatesData, promptValues, getSavedContent]);

    const handleSavePrompts = async () => {
        const payload: PromptTemplatesUpdatePayload = {
            storyCreation: promptValues.storyCreation,
            outlineChapter: promptValues.outlineChapter,
            manuscriptSection: promptValues.manuscriptSection,
            refine: {
                withInstruction: promptValues.refineWithInstruction,
                withoutInstruction: promptValues.refineWithoutInstruction,
            },
        };

        setIsPromptSaving(true);
        setPromptError(null);
        try {
            await updatePromptTemplates(payload);
            message.success('提示词模板已保存。');
            await loadPromptTemplates();
        } catch (error: unknown) {
            console.error('Failed to save prompt templates:', error);
            const errorMessage = error instanceof Error ? error.message : '未知错误';
            setPromptError(`保存提示词失败：${errorMessage}`);
            message.error(`保存提示词失败：${errorMessage}`);
        } finally {
            setIsPromptSaving(false);
        }
    };

    const handleResetPrompt = async (key: PromptFieldKey) => {
        setResettingKey(key);
        setPromptError(null);
        try {
            await resetPromptTemplates([promptResetKeyMap[key]]);
            message.success('已恢复系统默认模板。');
            await loadPromptTemplates();
        } catch (error: unknown) {
            console.error('Failed to reset prompt template:', error);
            const errorMessage = error instanceof Error ? error.message : '未知错误';
            setPromptError(`恢复默认模板失败：${errorMessage}`);
            message.error(`恢复默认模板失败：${errorMessage}`);
        } finally {
            setResettingKey(null);
        }
    };

    const getFieldStatus = (key: PromptFieldKey): { label: string; color: string } => {
        if (!promptTemplatesData) {
            return { label: '加载中', color: 'default' };
        }
        if (promptValues[key] !== getSavedContent(key)) {
            return { label: '已修改（未保存）', color: 'processing' };
        }
        if (getSavedIsDefault(key)) {
            return { label: '系统默认模板', color: 'default' };
        }
        return { label: '自定义模板', color: 'success' };
    };

    const renderModelSettings = () => (
        <Spin spinning={isLoading}>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Paragraph type="secondary">
                    配置访问大语言模型所需的基础信息。密钥仅用于调用，不会以明文存储。
                </Paragraph>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="baseUrl" label="Base URL" tooltip="可选：自定义 OpenAI 兼容服务的基础地址">
                        <Input placeholder="例如：https://api.openai.com/v1 或自建网关地址" />
                    </Form.Item>
                    <Form.Item name="modelName" label="模型名称" tooltip="例如 gpt-4o-mini, gpt-4-turbo">
                        <Input placeholder="输入模型名称" />
                    </Form.Item>
                    <Form.Item
                        name="apiKey"
                        label="API 密钥"
                        tooltip="后端不回显密钥原文。若显示“********”，表示已设置。点击“修改”可重新输入。"
                    >
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                            <Input.Password
                                placeholder={apiKeyIsSet ? '********' : '输入您的 API 密钥'}
                                disabled={!apiKeyEditable}
                            />
                            {apiKeyIsSet && (
                                <Button
                                    onClick={() => {
                                        if (apiKeyEditable) {
                                            form.setFieldsValue({ apiKey: '********' });
                                            setApiKeyEditable(false);
                                        } else {
                                            form.setFieldsValue({ apiKey: '' });
                                            setApiKeyEditable(true);
                                        }
                                    }}
                                >
                                    {apiKeyEditable ? '取消' : '修改'}
                                </Button>
                            )}
                        </div>
                        <div style={{ marginTop: 8, color: apiKeyIsSet ? '#52c41a' : '#faad14' }}>
                            {apiKeyIsSet ? '密钥状态：已设置' : '密钥状态：未设置'}
                        </div>
                    </Form.Item>
                    <Form.Item>
                        <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                            <Button type="primary" htmlType="submit" loading={isSaving} style={{ flex: 1, minWidth: 120 }}>
                                保存设置
                            </Button>
                            <Button onClick={handleTestConnection} loading={isTesting} style={{ flex: 1, minWidth: 120 }}>
                                测试连接
                            </Button>
                        </div>
                    </Form.Item>
                </Form>
            </Space>
        </Spin>
    );

    const renderPromptSettings = () => (
        <Spin spinning={isPromptLoading}>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Paragraph>
                    你可以为不同场景编写独立的提示词模板，使用 <Text code>{'${变量}'}</Text> 形式插入上下文数据。查看{' '}
                    <Link to="/settings/prompt-guide">提示词编写帮助</Link>，了解所有可用变量与函数。
                </Paragraph>
                {promptError && <Alert type="error" showIcon message={promptError} />}
                {isPromptDirty && !promptError && (
                    <Alert type="warning" showIcon message="提示词内容已修改但尚未保存。" />
                )}
                {promptFieldOrder.map(key => {
                    const config = promptFieldConfigs[key];
                    const status = getFieldStatus(key);
                    return (
                        <Card key={key} size="small">
                            <Space direction="vertical" size={12} style={{ width: '100%' }}>
                                <Space style={{ justifyContent: 'space-between', alignItems: 'center', width: '100%', flexWrap: 'wrap', gap: 12 }}>
                                    <div>
                                        <Title level={5} style={{ marginBottom: 4 }}>{config.label}</Title>
                                        <Text type="secondary">{config.description}</Text>
                                    </div>
                                    <Tag color={status.color}>{status.label}</Tag>
                                </Space>
                                <TextArea
                                    value={promptValues[key]}
                                    onChange={event => handlePromptChange(key, event.target.value)}
                                    autoSize={{ minRows: 8, maxRows: 40 }}
                                    showCount
                                    spellCheck={false}
                                />
                                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                                    <Button
                                        onClick={() => handleResetPrompt(key)}
                                        loading={resettingKey === key}
                                        disabled={isPromptSaving}
                                    >
                                        恢复默认
                                    </Button>
                                </div>
                            </Space>
                        </Card>
                    );
                })}
                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                        type="primary"
                        onClick={handleSavePrompts}
                        loading={isPromptSaving}
                        disabled={!isPromptDirty}
                    >
                        保存提示词
                    </Button>
                </div>
            </Space>
        </Spin>
    );

    const items: TabsProps['items'] = [
        { key: 'model', label: '模型设置', children: renderModelSettings() },
        { key: 'prompts', label: '提示词配置', children: renderPromptSettings() },
    ];

    return (
        <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
            <Card style={{ maxWidth: 1000, margin: '0 auto' }}>
                <Space direction="vertical" size={24} style={{ width: '100%' }}>
                    <Title level={3} style={{ margin: 0 }}>用户设置</Title>
                    <Tabs items={items} activeKey={activeTab} onChange={key => setActiveTab(key as 'model' | 'prompts')} />
                </Space>
            </Card>
        </div>
    );
};

export default Settings;
