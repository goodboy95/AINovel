import React, { useEffect, useState } from 'react';
import { Modal, Form, Input } from 'antd';
import type { TemporaryCharacter } from '../../types';
import AiRefineButton from '../AiRefineButton';
import RefineModal from './RefineModal';

const { TextArea } = Input;

interface TemporaryCharacterEditModalProps {
  open: boolean;
  onCancel: () => void;
  onSave: (character: TemporaryCharacter) => void;
  character: TemporaryCharacter | null;
}

type RefineTarget = {
  field: keyof TemporaryCharacter;
  context: string;
};

const TemporaryCharacterEditModal: React.FC<TemporaryCharacterEditModalProps> = ({ open, onCancel, onSave, character }) => {
  const [form] = Form.useForm();
  const [isRefineModalOpen, setIsRefineModalOpen] = useState(false);
  const [refineTarget, setRefineTarget] = useState<RefineTarget | null>(null);

  useEffect(() => {
    if (character) {
      form.setFieldsValue(character);
    } else {
      form.resetFields();
    }
  }, [character, form, open]);

  const handleSave = () => {
    form.validateFields().then(values => {
      const newCharacter = { ...character, ...values, id: character?.id || Date.now() };
      onSave(newCharacter);
      onCancel();
    });
  };

  const handleOpenRefineModal = (field: keyof TemporaryCharacter, context: string) => {
    setRefineTarget({ field, context });
    setIsRefineModalOpen(true);
  };

  const handleRefinedText = (newText: string) => {
    if (refineTarget) {
      form.setFieldsValue({ [refineTarget.field]: newText });
    }
  };

  const renderTextAreaWithRefine = (name: keyof TemporaryCharacter, label: string, context: string) => (
    <div style={{ position: 'relative' }}>
      <Form.Item name={name} label={label} rules={[{ required: true, message: `请输入${label}` }]}>
        <TextArea rows={3} />
      </Form.Item>
      <AiRefineButton onClick={() => handleOpenRefineModal(name, context)} />
    </div>
  );

  return (
    <>
      <Modal
        title={character ? '编辑临时人物' : '新增临时人物'}
        open={open}
        onCancel={onCancel}
        onOk={handleSave}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" name="temporaryCharacterForm">
          <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input />
          </Form.Item>
          {renderTextAreaWithRefine('summary', '概要', '临时人物概要')}
          {renderTextAreaWithRefine('details', '详情', '临时人物详情')}
          {renderTextAreaWithRefine('relationships', '与核心人物的关系', '临时人物关系')}
          {renderTextAreaWithRefine('statusInScene', '在本节中的状态', '临时人物状态')}
          {renderTextAreaWithRefine('moodInScene', '在本节中的心情', '临时人物心情')}
          {renderTextAreaWithRefine('actionsInScene', '在本节中的核心行动', '临时人物核心行动')}
        </Form>
      </Modal>
      {refineTarget && (
        <RefineModal
          open={isRefineModalOpen}
          onCancel={() => setIsRefineModalOpen(false)}
          originalText={form.getFieldValue(refineTarget.field) || ''}
          contextType={refineTarget.context}
          onRefined={handleRefinedText}
        />
      )}
    </>
  );
};

export default TemporaryCharacterEditModal;