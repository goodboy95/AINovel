import { Modal, Form, Input } from 'antd';
import type { CharacterCard } from '../../types';

interface AddCharacterModalProps {
    open: boolean;
    onOk: (characterData: Omit<CharacterCard, 'id'>) => void;
    onCancel: () => void;
    confirmLoading: boolean;
}

const AddCharacterModal = ({ open, onOk, onCancel, confirmLoading }: AddCharacterModalProps) => {
    const [form] = Form.useForm();

    const handleOk = () => {
        form.validateFields()
            .then(values => {
                onOk(values);
                form.resetFields();
            })
            .catch(info => {
                console.log('Validate Failed:', info);
            });
    };

    return (
        <Modal
            title="添加新角色"
            open={open}
            onOk={handleOk}
            onCancel={onCancel}
            confirmLoading={confirmLoading}
            destroyOnClose
        >
            <Form form={form} layout="vertical" name="add_character_form">
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

export default AddCharacterModal;