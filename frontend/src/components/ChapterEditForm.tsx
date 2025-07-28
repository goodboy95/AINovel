import React, { useState } from 'react';
import { Form, Input } from 'antd';
import type { Chapter } from '../types';
import AiRefineButton from './AiRefineButton';
import RefineModal from './modals/RefineModal';

const { TextArea } = Input;

interface ChapterEditFormProps {
  chapter: Chapter;
  onUpdate: (updatedChapter: Chapter) => void;
}

type RefineTarget = {
  field: keyof Chapter;
  context: string;
};

const ChapterEditForm: React.FC<ChapterEditFormProps> = ({ chapter, onUpdate }) => {
  const [form] = Form.useForm();
  const [isRefineModalOpen, setIsRefineModalOpen] = useState(false);
  const [refineTarget, setRefineTarget] = useState<RefineTarget | null>(null);

  const handleValuesChange = (_: Partial<Chapter>, allValues: Chapter) => {
    onUpdate({ ...chapter, ...allValues });
  };

  const handleOpenRefineModal = (field: keyof Chapter, context: string) => {
    setRefineTarget({ field, context });
    setIsRefineModalOpen(true);
  };

  const handleRefinedText = (newText: string) => {
    if (refineTarget) {
      form.setFieldsValue({ [refineTarget.field]: newText });
      onUpdate({ ...chapter, [refineTarget.field]: newText });
    }
  };

  const renderTextAreaWithRefine = (name: keyof Chapter, label: string, context: string) => (
    <div style={{ position: 'relative' }}>
      <Form.Item name={name} label={label}>
        <TextArea rows={4} />
      </Form.Item>
      <AiRefineButton onClick={() => handleOpenRefineModal(name, context)} />
    </div>
  );

  return (
    <>
      <Form form={form} layout="vertical" initialValues={chapter} onValuesChange={handleValuesChange}>
        <Form.Item name="title" label="章节标题">
          <Input />
        </Form.Item>
        {renderTextAreaWithRefine('synopsis', '章节梗概', '章节梗概')}
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
    </>
  );
};

export default ChapterEditForm;