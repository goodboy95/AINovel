import { Modal, Form, Input } from 'antd';
import { useEffect } from 'react';
import type { CharacterCard } from '../../types';

interface EditCharacterCardModalProps {
    open: boolean;
    onOk: (characterCard: CharacterCard) => void;
    onCancel: () => void;
    confirmLoading: boolean;
    characterCard: CharacterCard | null;
}

const EditCharacterCardModal = ({ open, onOk, onCancel, confirmLoading, characterCard }: EditCharacterCardModalProps) => {
    const [form] = Form.useForm();

    useEffect(() => {
        if (characterCard) {
            form.setFieldsValue(characterCard);
        }
    }, [characterCard, form]);

    const handleOk = () => {
        form.validateFields()
            .then(values => {
                if (characterCard) {
                    onOk({ ...characterCard, ...values });
                }
            })
            .catch(info => {
                console.log('Validate Failed:', info);
            });
    };

    return (
        <Modal
            title="编辑角色卡"
            open={open}
            onOk={handleOk}
            onCancel={onCancel}
            confirmLoading={confirmLoading}
            destroyOnHidden
        >
            <Form form={form} layout="vertical" name="edit_character_card_form">
                <Form.Item
                    name="name"
                    label="姓名"
                    rules={[{ required: true, message: '请输入角色姓名!' }]}
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
                    name="details"
                    label="详情"
                >
                    <Input.TextArea autoSize />
                </Form.Item>
                <Form.Item
                    name="relationships"
                    label="人物关系"
                >
                    <Input.TextArea autoSize />
                </Form.Item>
            </Form>
        </Modal>
    );
};

export default EditCharacterCardModal;
