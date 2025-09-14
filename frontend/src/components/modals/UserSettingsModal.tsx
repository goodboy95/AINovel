import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Button, Spin, Typography, message } from 'antd';

const { Text } = Typography;

type SettingsValues = {
  baseUrl?: string;
  modelName?: string;
  apiKey?: string;
};

type UserSettingsModalProps = {
  open: boolean;
  onClose: () => void;
};

const UserSettingsModal: React.FC<UserSettingsModalProps> = ({ open, onClose }) => {
  const [form] = Form.useForm<SettingsValues>();
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isTesting, setIsTesting] = useState(false);

  const fetchSettings = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('/api/v1/settings', {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      });
      if (response.ok) {
        const text = await response.text();
        if (text) {
          const data = JSON.parse(text) as SettingsValues;
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
        message.error('获取设置失败。');
      }
    } catch (e) {
      if (e instanceof SyntaxError) {
        message.error('解析设置失败：返回数据格式无效。');
      } else if (e instanceof Error) {
        message.error(`获取设置时出错：${e.message}`);
      } else {
        message.error('获取设置时发生未知错误。');
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      fetchSettings();
    } else {
      form.resetFields();
      setIsLoading(false);
      setIsSaving(false);
      setIsTesting(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setIsSaving(true);
      const response = await fetch('/api/v1/settings', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(values),
      });
      if (response.ok) {
        message.success('设置已成功保存！');
        form.setFieldsValue({ apiKey: '' });
        onClose();
      } else {
        const data = (await response.json().catch(() => ({}))) as { message?: string };
        message.error(data.message || '保存设置失败。');
      }
    } catch (e) {
      if (e instanceof Error) {
        message.error(`保存时发生错误：${e.message}`);
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleTest = async () => {
    try {
      const values = await form.validateFields();
      setIsTesting(true);
      const response = await fetch('/api/v1/settings/test', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(values),
      });
      const data = (await response.json().catch(() => ({}))) as { message?: string };
      if (response.ok) {
        message.success(data.message || '连接成功！');
      } else {
        message.error(data.message || '连接测试失败。');
      }
    } catch (e) {
      if (e instanceof Error) {
        message.error(`测试期间发生错误：${e.message}`);
      } else {
        message.error('测试期间发生未知错误。');
      }
    } finally {
      setIsTesting(false);
    }
  };

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title="AI 模型设置"
      footer={
        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <Button onClick={onClose}>取消</Button>
          <Button onClick={handleTest} loading={isTesting}>测试连接</Button>
          <Button type="primary" onClick={handleSave} loading={isSaving}>保存设置</Button>
        </div>
      }
      destroyOnHidden
      maskClosable={false}
    >
      <Spin spinning={isLoading}>
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary">配置用于 AI 生成的服务信息。API 密钥仅用于调用，不会明文持久化到后端。</Text>
        </div>
        <Form form={form} layout="vertical">
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
            rules={[{ required: false }]}
          >
            <Input.Password placeholder="输入您的 API 密钥" />
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  );
};

export default UserSettingsModal;
