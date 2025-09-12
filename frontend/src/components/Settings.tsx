import { useState, useEffect } from 'react';
import { Form, Input, Button, Card, Typography, message, Spin } from 'antd';

const { Title } = Typography;

interface SettingsValues {
    baseUrl?: string;
    modelName?: string;
    apiKey?: string;
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
                    const text = await response.text();
                    if (text) {
                        const data = JSON.parse(text);
                        form.setFieldsValue({
                            baseUrl: data.baseUrl || '',
                            modelName: data.modelName || '',
                        });
                    } else {
                        form.setFieldsValue({
                            baseUrl: '',
                            modelName: '',
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
                form.setFieldsValue({ apiKey: '' }); // 保存后清空密钥字段
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

    return (
        <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
            <Card style={{ maxWidth: 600, margin: 'auto' }}>
                <Title level={3}>AI 模型设置</Title>
                <Spin spinning={isLoading}>
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
                            tooltip="您的 API 密钥仅用于当前会话，不会存储在服务器上。"
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
