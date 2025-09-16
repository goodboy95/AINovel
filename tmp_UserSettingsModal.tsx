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
        message.error('鑾峰彇璁剧疆澶辫触銆?);
      }
    } catch (e) {
      if (e instanceof SyntaxError) {
        message.error('瑙ｆ瀽璁剧疆澶辫触锛氳繑鍥炴暟鎹牸寮忔棤鏁堛€?);
      } else if (e instanceof Error) {
        message.error(`鑾峰彇璁剧疆鏃跺嚭閿欙細${e.message}`);
      } else {
        message.error('鑾峰彇璁剧疆鏃跺彂鐢熸湭鐭ラ敊璇€?);
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
        message.success('璁剧疆宸叉垚鍔熶繚瀛橈紒');
        form.setFieldsValue({ apiKey: '' });
        onClose();
      } else {
        const data = (await response.json().catch(() => ({}))) as { message?: string };
        message.error(data.message || '淇濆瓨璁剧疆澶辫触銆?);
      }
    } catch (e) {
      if (e instanceof Error) {
        message.error(`淇濆瓨鏃跺彂鐢熼敊璇細${e.message}`);
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
        message.success(data.message || '杩炴帴鎴愬姛锛?);
      } else {
        message.error(data.message || '杩炴帴娴嬭瘯澶辫触銆?);
      }
    } catch (e) {
      if (e instanceof Error) {
        message.error(`娴嬭瘯鏈熼棿鍙戠敓閿欒锛?{e.message}`);
      } else {
        message.error('娴嬭瘯鏈熼棿鍙戠敓鏈煡閿欒銆?);
      }
    } finally {
      setIsTesting(false);
    }
  };

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title="AI 妯″瀷璁剧疆"
      footer={
        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <Button onClick={onClose}>鍙栨秷</Button>
          <Button onClick={handleTest} loading={isTesting}>娴嬭瘯杩炴帴</Button>
          <Button type="primary" onClick={handleSave} loading={isSaving}>淇濆瓨璁剧疆</Button>
        </div>
      }
      destroyOnHidden
      maskClosable={false}
    >
      <Spin spinning={isLoading}>
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary">閰嶇疆鐢ㄤ簬 AI 鐢熸垚鐨勬湇鍔′俊鎭€侫PI 瀵嗛挜浠呯敤浜庤皟鐢紝涓嶄細鏄庢枃鎸佷箙鍖栧埌鍚庣銆?/Text>
        </div>
        <Form form={form} layout="vertical">
          <Form.Item name="baseUrl" label="Base URL" tooltip="鍙€夛細鑷畾涔?OpenAI 鍏煎鏈嶅姟鐨勫熀纭€鍦板潃">
            <Input placeholder="渚嬪锛歨ttps://api.openai.com/v1 鎴栬嚜寤虹綉鍏冲湴鍧€" />
          </Form.Item>
          <Form.Item name="modelName" label="妯″瀷鍚嶇О" tooltip="渚嬪 gpt-4o-mini, gpt-4-turbo">
            <Input placeholder="杈撳叆妯″瀷鍚嶇О" />
          </Form.Item>
          <Form.Item
            name="apiKey"
            label="API 瀵嗛挜"
            tooltip="鎮ㄧ殑 API 瀵嗛挜浠呯敤浜庡綋鍓嶄細璇濓紝涓嶄細瀛樺偍鍦ㄦ湇鍔″櫒涓娿€?
            rules={[{ required: false }]}
          >
            <Input.Password placeholder="杈撳叆鎮ㄧ殑 API 瀵嗛挜" />
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  );
};

export default UserSettingsModal;
