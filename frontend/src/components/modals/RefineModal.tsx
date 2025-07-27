import { Modal, Form, Input, Button, Spin, Alert, Row, Col, Typography, Card, Divider } from 'antd';

const { Title, Paragraph } = Typography;

interface RefineModalProps {
    open: boolean;
    onCancel: () => void;
    onRefine: (userFeedback: string) => void;
    onAccept: () => void;
    loading: boolean;
    error: string | null;
    originalText: string;
    refinedText: string;
}

const RefineModal = ({
    open,
    onCancel,
    onRefine,
    onAccept,
    loading,
    error,
    originalText,
    refinedText,
}: RefineModalProps) => {
    const [form] = Form.useForm();

    const handleFinish = (values: { userFeedback: string }) => {
        onRefine(values.userFeedback);
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
                <Button key="submit" type="primary" loading={loading} onClick={onAccept} disabled={!refinedText}>
                    接受修改
                </Button>,
            ]}
        >
            <Spin spinning={loading} tip="AI 正在思考...">
                {error && <Alert message="优化出错" description={error} type="error" showIcon style={{ marginBottom: 16 }} />}
                <Row gutter={16}>
                    <Col span={12}>
                        <Title level={5}>原始文本</Title>
                        <Card style={{ minHeight: 200, background: '#f5f5f5' }}>
                            <Paragraph>{originalText}</Paragraph>
                        </Card>
                    </Col>
                    <Col span={12}>
                        <Title level={5}>优化结果</Title>
                        <Card style={{ minHeight: 200 }}>
                            <Paragraph>{refinedText || "点击下方按钮开始优化..."}</Paragraph>
                        </Card>
                    </Col>
                </Row>
                <Divider />
                <Form form={form} layout="vertical" onFinish={handleFinish}>
                    <Form.Item name="userFeedback" label="优化建议 (可选)">
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