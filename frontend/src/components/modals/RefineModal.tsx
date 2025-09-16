import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Button, Spin, Alert, Row, Col, Typography, Card, Divider } from 'antd';
import * as api from '../../services/api';
import ChatMessage from '../ChatMessage';

const { Title } = Typography;

interface RefineModalProps {
  open: boolean;
  onCancel: () => void;
  originalText: string;
  contextType: string;
  onRefined: (newText: string) => void;
}

type ChatItem = { role: 'user' | 'assistant'; content: string };

const RefineModal: React.FC<RefineModalProps> = ({ open, onCancel, originalText, contextType, onRefined }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refinedText, setRefinedText] = useState('');
  const [chatHistory, setChatHistory] = useState<ChatItem[]>([]);

  useEffect(() => {
    if (open) {
      setRefinedText(originalText || '');
      setChatHistory([]);
      setError(null);
      form.resetFields();
    }
  }, [open, originalText, form]);

  const handleSend = async (values: { instruction?: string }) => {
    const instruction = (values.instruction || '').trim();
    if (!instruction) return;
    setLoading(true);
    setError(null);
    // Append user message
    setChatHistory(prev => [...prev, { role: 'user', content: instruction }]);
    form.resetFields(['instruction']);
    try {
      const response = await api.refineText({
        text: originalText,
        instruction,
        contextType,
      });
      setRefinedText(response.refinedText || '');
      // Append assistant ack
      setChatHistory(prev => [...prev, { role: 'assistant', content: '已根据你的指令优化了左侧结果。' }]);
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('未知错误');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAccept = () => {
    onRefined(refinedText);
    onCancel();
  };

  const handleReset = () => {
    setRefinedText(originalText || '');
  };

  return (
    <Modal
      title={`AI 文本优化 · ${contextType}`}
      open={open}
      onCancel={onCancel}
      width="80vw"
      footer={[
        <Button key="cancel" onClick={onCancel}>取消</Button>,
        <Button key="apply" type="primary" onClick={handleAccept} disabled={loading}>应用修改</Button>,
      ]}
    >
      <Spin spinning={loading} tip="AI 正在思考...">
        {error && (
          <Alert message="优化出错" description={error} type="error" showIcon style={{ marginBottom: 16 }} />
        )}
        <Row gutter={16}>
          <Col span={14}>
            <div>
              <Title level={5}>原始文本</Title>
              <Card style={{ minHeight: 160, maxHeight: 260, overflowY: 'auto', background: '#f5f5f5' }}>
                <pre style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{originalText}</pre>
              </Card>
            </div>
            <Divider style={{ margin: '12px 0' }} />
            <div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Title level={5} style={{ margin: 0 }}>优化结果</Title>
                <Button size="small" onClick={handleReset}>还原</Button>
              </div>
              <Card style={{ minHeight: 200 }}>
                <Input.TextArea
                  value={refinedText}
                  onChange={e => setRefinedText(e.target.value)}
                  rows={8}
                />
              </Card>
            </div>
          </Col>
          <Col span={10}>
            <Title level={5}>对话区</Title>
            <Card style={{ minHeight: 380, maxHeight: 460, display: 'flex', flexDirection: 'column' }}>
              <div style={{ flex: 1, overflowY: 'auto', paddingRight: 8 }}>
                {chatHistory.length === 0 && (
                  <div style={{ color: '#888', fontSize: 12 }}>给出你的优化指令，例如：让语言更凝练、更有节奏感。</div>
                )}
                {chatHistory.map((m, idx) => (
                  <ChatMessage key={idx} role={m.role} content={m.content} />
                ))}
              </div>
              <Divider style={{ margin: '8px 0' }} />
              <Form form={form} layout="vertical" onFinish={handleSend}>
                <Form.Item name="instruction" style={{ marginBottom: 8 }}>
                  <Input.TextArea rows={2} placeholder="输入优化指令后回车或点击发送" onPressEnter={(e) => { if (!e.shiftKey) { e.preventDefault(); form.submit(); } }} />
                </Form.Item>
                <div style={{ textAlign: 'right' }}>
                  <Button htmlType="submit" type="primary">发送</Button>
                </div>
              </Form>
            </Card>
          </Col>
        </Row>
      </Spin>
    </Modal>
  );
};

export default RefineModal;
