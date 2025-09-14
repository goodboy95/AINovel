import { Modal, Form, Input } from 'antd';
import { useEffect } from 'react';
import type { StoryCard } from '../../types';

interface EditStoryCardModalProps {
    open: boolean;
    onOk: (storyCard: StoryCard) => void;
    onCancel: () => void;
    confirmLoading: boolean;
    storyCard: StoryCard | null;
}

const EditStoryCardModal = ({ open, onOk, onCancel, confirmLoading, storyCard }: EditStoryCardModalProps) => {
    const [form] = Form.useForm();

    useEffect(() => {
        if (storyCard) {
            form.setFieldsValue(storyCard);
        }
    }, [storyCard, form]);

    const handleOk = () => {
        form.validateFields()
            .then(values => {
                if (storyCard) {
                    onOk({ ...storyCard, ...values });
                }
            })
            .catch(info => {
                console.log('Validate Failed:', info);
            });
    };

    return (
        <Modal
            title="编辑故事卡"
            open={open}
            onOk={handleOk}
            onCancel={onCancel}
            confirmLoading={confirmLoading}
            destroyOnHidden
        >
            <Form form={form} layout="vertical" name="edit_story_card_form">
                <Form.Item
                    name="title"
                    label="标题"
                    rules={[{ required: true, message: '请输入标题!' }]}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name="synopsis"
                    label="概要"
                >
                    <Input.TextArea autoSize />
                </Form.Item>
                <Form.Item
                    name="storyArc"
                    label="故事弧线"
                >
                    <Input.TextArea autoSize />
                </Form.Item>
                <Form.Item
                    name="genre"
                    label="类型"
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name="tone"
                    label="基调"
                >
                    <Input />
                </Form.Item>
            </Form>
        </Modal>
    );
};

export default EditStoryCardModal;
