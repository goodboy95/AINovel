import { useState, useEffect } from 'react';
import { Form, Input, Button, Select, Card, Typography, message, Spin } from 'antd';

const { Title } = Typography;
const { Option } = Select;

interface SettingsValues {
    llmProvider: string;
    modelName: string;
    apiKey: string;
}

const Settings = () => {
    const [form] = Form.useForm();
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [isTesting, setIsTesting] = useState(false);

    useEffect(() => {
        const fetchSettings = async () => {
            setIsLoading(true);
            try {
                const response = await fetch('/api/v1/settings', {
                    headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
                });
                if (response.ok) {
                    const data = await response.json();
                    form.setFieldsValue({
                        llmProvider: data.llmProvider || 'openai',
                        modelName: data.modelName || '',
                    });
                } else {
                    message.error('获取设置失败。');
                }
            } catch (error: unknown) {
                if (error instanceof Error) {
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

    const onFinish = async (values: SettingsValues) => {
        setIsSaving(true);
        try {
            const response = await fetch('/api/v1/settings', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify(values),
            });
            if (response.ok) {
                message.success('设置已成功保存！');
                form.setFieldsValue({ apiKey: '' }); // Clear API key field after save
            } else {
                const data = await response.json();
                message.error(data.message || '保存设置失败。');
            }
        } catch (error: unknown) {
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
            const response = await fetch('/api/v1/settings/test', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify(values),
            });
            const data = await response.json();
            if (response.ok) {
                message.success(data.message || '连接成功！');
            } else {
                message.error(data.message || '连接测试失败。');
            }
        } catch (error: unknown) {
            if (error instanceof Error) {
                message.error(`测试期间发生错误： ${error.message}`);
            } else {
                message.error('测试期间发生未知错误。');
            }
        } finally {
            setIsTesting(false);
        }
    };

    return (
        <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
            <Card style={{ maxWidth: 600, margin: 'auto' }}>
                <Title level={3}>AI 模型设置</Title>
                <Spin spinning={isLoading}>
                    <Form form={form} layout="vertical" onFinish={onFinish}>
                        <Form.Item name="llmProvider" label="LLM 提供商" rules={[{ required: true }]}>
                            <Select>
                                <Option value="openai">OpenAI</Option>
                                <Option value="claude">Claude</Option>
                                <Option value="gemini">Gemini</Option>
                            </Select>
                        </Form.Item>
                        <Form.Item name="modelName" label="模型名称" help="例如 gpt-4, claude-3-opus-20240229">
                            <Input placeholder="输入模型名称" />
                        </Form.Item>
                        <Form.Item
                            name="apiKey"
                            label="API 密钥"
                            help="您的 API 密钥仅用于当前会话，不会存储在服务器上。"
                        >
                            <Input.Password placeholder="输入您的 API 密钥" />
                        </Form.Item>
                        <Form.Item>
                            <div style={{ display: 'flex', gap: '16px' }}>
                                <Button type="primary" htmlType="submit" loading={isSaving} style={{ flex: 1 }}>
                                    保存设置
                                </Button>
                                <Button onClick={handleTestConnection} loading={isTesting} style={{ flex: 1 }}>
                                    测试连接
                                </Button>
                            </div>
                        </Form.Item>
                    </Form>
                </Spin>
            </Card>
        </div>
    );
};

export default Settings;
