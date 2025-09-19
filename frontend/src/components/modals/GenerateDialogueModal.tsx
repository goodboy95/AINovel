import React, { useEffect } from 'react';
import { Modal, Form, Input } from 'antd';

interface GenerateDialogueModalProps {
  open: boolean;
  characterName: string;
  submitting: boolean;
  defaultSceneDescription?: string;
  onCancel: () => void;
  onSubmit: (values: { dialogueTopic: string; currentSceneDescription: string }) => void;
}

const GenerateDialogueModal: React.FC<GenerateDialogueModalProps> = ({
  open,
  characterName,
  submitting,
  defaultSceneDescription,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      form.setFieldsValue({
        dialogueTopic: '',
        currentSceneDescription: defaultSceneDescription || '',
      });
    }
  }, [open, defaultSceneDescription, form]);

  return (
    <Modal
      title={`为 ${characterName} 生成对话`}
      open={open}
      onCancel={onCancel}
      onOk={() => {
        form
          .validateFields()
          .then(onSubmit)
          .catch(() => undefined);
      }}
      okText="生成"
      confirmLoading={submitting}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          label="对话主题"
          name="dialogueTopic"
          rules={[{ required: true, message: '请填写对话主题' }]}
        >
          <Input placeholder="例如：关于信任、关于计划的分歧" />
        </Form.Item>
        <Form.Item label="当前场景描述" name="currentSceneDescription">
          <Input.TextArea rows={4} placeholder="简要描述当前场景，帮助AI把握语境" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default GenerateDialogueModal;

