import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Button, Spin, Alert, Row, Col, Typography, Card, Divider } from 'antd';
import * as api from '../../services/api';

const { Title, Paragraph } = Typography;

interface RefineModalProps {
    open: boolean;
    onCancel: () => void;
    originalText: string;
    contextType: string;
    onRefined: (newText: string) => void;
}

const RefineModal: React.FC<RefineModalProps> = ({
    open,
    onCancel,
    originalText,
    contextType,
    onRefined,
}) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [refinedText, setRefinedText] = useState('');

    useEffect(() => {
        if (open) {
            // Reset state when modal opens
            setRefinedText('');
            setError(null);
            form.resetFields();
        }
    }, [open, form]);

    const handleFinish = async (values: { instruction: string }) => {
        setLoading(true);
        setError(null);
        try {
            const response = await api.refineText({
                text: originalText,
                instruction: values.instruction,
                contextType: contextType,
            });
            setRefinedText(response.refinedText);
        } catch (err) {
            if (err instanceof Error) {
                setError(err.message);
            } else {
                setError('An unknown error occurred.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleAccept = () => {
        onRefined(refinedText);
        onCancel(); // Close modal after accepting
    };

    return (
        <Modal
            title="AI 文本优化"
            open={open}
            onCancel={onCancel}
            width="70vw"
            footer={[
                <Button key="back" onClick={onCancel}>
                    取消
                </Button>,
                <Button key="submit" type="primary" onClick={handleAccept} disabled={!refinedText || loading}>
                    应用修改
                </Button>,
            ]}
        >
            <Spin spinning={loading} tip="AI 正在思考...">
                {error && <Alert message="优化出错" description={error} type="error" showIcon style={{ marginBottom: 16 }} />}
                <Row gutter={16}>
                    <Col span={12}>
                        <Title level={5}>原始文本 ({contextType})</Title>
                        <Card style={{ minHeight: 200, maxHeight: 400, overflowY: 'auto', background: '#f5f5f5' }}>
                            <Paragraph>{originalText}</Paragraph>
                        </Card>
                    </Col>
                    <Col span={12}>
                        <Title level={5}>优化结果</Title>
                        <Card style={{ minHeight: 200, maxHeight: 400, overflowY: 'auto' }}>
                            <Paragraph>{refinedText || "点击下方按钮开始优化..."}</Paragraph>
                        </Card>
                    </Col>
                </Row>
                <Divider />
                <Form form={form} layout="vertical" onFinish={handleFinish}>
                    <Form.Item name="instruction" label="优化方向 (可选)">
                        <Input.TextArea rows={2} placeholder="例如：让这段描述更黑暗一点，或者，用更简洁的语言表达。" />
                    </Form.Item>
                    <Form.Item>
                        <Button htmlType="submit" type="primary" loading={loading}>
                            开始优化
                        </Button>
                    </Form.Item>
                </Form>
            </Spin>
        </Modal>
    );
};

export default RefineModal;