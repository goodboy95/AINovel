import React, { useState } from 'react';
import { Form, Input, Button, List, Popconfirm } from 'antd';
import type { Scene, TemporaryCharacter } from '../types';
import AiRefineButton from './AiRefineButton';
import RefineModal from './modals/RefineModal';
import TemporaryCharacterEditModal from './modals/TemporaryCharacterEditModal';

const { TextArea } = Input;

interface SceneEditFormProps {
  scene: Scene;
  onUpdate: (updatedScene: Scene) => void;
}

type RefineTarget = {
  field: keyof Scene;
  context: string;
};

const SceneEditForm: React.FC<SceneEditFormProps> = ({ scene, onUpdate }) => {
  const [form] = Form.useForm();
  const [isRefineModalOpen, setIsRefineModalOpen] = useState(false);
  const [refineTarget, setRefineTarget] = useState<RefineTarget | null>(null);
  const [isTempCharModalOpen, setIsTempCharModalOpen] = useState(false);
  const [editingTempChar, setEditingTempChar] = useState<TemporaryCharacter | null>(null);

  const handleValuesChange = (_: Partial<Scene>, allValues: Scene) => {
    onUpdate({ ...scene, ...allValues });
  };

  const handleOpenRefineModal = (field: keyof Scene, context: string) => {
    setRefineTarget({ field, context });
    setIsRefineModalOpen(true);
  };

  const handleRefinedText = (newText: string) => {
    if (refineTarget) {
      form.setFieldsValue({ [refineTarget.field]: newText });
      onUpdate({ ...scene, [refineTarget.field]: newText });
    }
  };

  const handleSaveTempChar = (character: TemporaryCharacter) => {
    const existingIndex = scene.temporaryCharacters?.findIndex(c => c.id === character.id);
    let updatedChars: TemporaryCharacter[];

    if (existingIndex !== undefined && existingIndex > -1) {
      updatedChars = [...(scene.temporaryCharacters || [])];
      updatedChars[existingIndex] = character;
    } else {
      updatedChars = [...(scene.temporaryCharacters || []), character];
    }
    onUpdate({ ...scene, temporaryCharacters: updatedChars });
  };

  const handleDeleteTempChar = (id: number) => {
    const updatedChars = scene.temporaryCharacters?.filter(c => c.id !== id);
    onUpdate({ ...scene, temporaryCharacters: updatedChars });
  };

  const renderTextAreaWithRefine = (name: keyof Scene, label: string, context: string) => (
    <div style={{ position: 'relative' }}>
      <Form.Item name={name} label={label}>
        <TextArea rows={4} />
      </Form.Item>
      <AiRefineButton onClick={() => handleOpenRefineModal(name, context)} />
    </div>
  );

  return (
    <>
      <Form form={form} layout="vertical" initialValues={scene} onValuesChange={handleValuesChange}>
        {renderTextAreaWithRefine('synopsis', '场景梗概', '场景梗概')}
        <Form.Item name="presentCharacters" label="核心出场人物">
          <Input placeholder="多个角色请用逗号分隔" />
        </Form.Item>
        {renderTextAreaWithRefine('characterStates', '核心人物状态', '核心人物状态')}
        
        <List
          header={<div>临时人物</div>}
          bordered
          dataSource={scene.temporaryCharacters || []}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button type="link" onClick={() => { setEditingTempChar(item); setIsTempCharModalOpen(true); }}>编辑</Button>,
                <Popconfirm title="确定删除吗？" onConfirm={() => handleDeleteTempChar(item.id)}>
                  <Button type="link" danger>删除</Button>
                </Popconfirm>
              ]}
            >
              {item.name}
            </List.Item>
          )}
        />
        <Button type="dashed" onClick={() => { setEditingTempChar(null); setIsTempCharModalOpen(true); }} style={{ marginTop: 16 }}>
          + 新增临时人物
        </Button>
      </Form>

      {refineTarget && (
        <RefineModal
          open={isRefineModalOpen}
          onCancel={() => setIsRefineModalOpen(false)}
          originalText={form.getFieldValue(refineTarget.field) || ''}
          contextType={refineTarget.context}
          onRefined={handleRefinedText}
        />
      )}

      <TemporaryCharacterEditModal
        open={isTempCharModalOpen}
        onCancel={() => setIsTempCharModalOpen(false)}
        onSave={handleSaveTempChar}
        character={editingTempChar}
      />
    </>
  );
};

export default SceneEditForm;