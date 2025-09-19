import React, { useEffect, useMemo, useState } from 'react';
import { Form, Input, Button, List, Popconfirm, Select, InputNumber, Card } from 'antd';
import type { Scene, SceneCharacter, TemporaryCharacter } from '../types';
import AiRefineButton from './AiRefineButton';
import RefineModal from './modals/RefineModal';
import TemporaryCharacterEditModal from './modals/TemporaryCharacterEditModal';
import { fetchCharactersForStory } from '../services/api';

const { TextArea } = Input;

interface SceneEditFormProps {
  scene: Scene;
  onUpdate: (updatedScene: Scene) => void;
  storyId?: number; // 用于拉取可选角色列表
}

type RefineTarget = {
  field: keyof Scene;
  context: string;
};

const SceneEditForm: React.FC<SceneEditFormProps> = ({ scene, onUpdate, storyId }) => {
  const [form] = Form.useForm();
  const [isRefineModalOpen, setIsRefineModalOpen] = useState(false);
  const [refineTarget, setRefineTarget] = useState<RefineTarget | null>(null);
  const [isTempCharModalOpen, setIsTempCharModalOpen] = useState(false);
  const [editingTempChar, setEditingTempChar] = useState<TemporaryCharacter | null>(null);
  const [characterOptions, setCharacterOptions] = useState<{ label: string; value: number }[]>([]);

  const idToNameMap = useMemo(() => new Map(characterOptions.map(opt => [opt.value, opt.label])), [characterOptions]);
  const nameToIdMap = useMemo(() => new Map(characterOptions.map(opt => [opt.label.trim(), opt.value])), [characterOptions]);

  // 拉取当前故事下的角色作为多选下拉选项
  useEffect(() => {
    const loadCharacters = async () => {
      if (!storyId) {
        setCharacterOptions([]);
        return;
      }
      try {
        const chars = await fetchCharactersForStory(storyId);
        setCharacterOptions(chars.map(c => ({ label: c.name, value: c.id })));
      } catch {
        setCharacterOptions([]);
      }
    };
    loadCharacters();
  }, [storyId]);

  // 在选中树节点变更时，同步表单为受控显示当前场景数据
  useEffect(() => {
    const normalizedSceneCharacters = (scene.sceneCharacters || []).map((sc) => {
      const mappedId = sc.characterCardId ?? (sc.characterName ? nameToIdMap.get(sc.characterName.trim()) : undefined);
      const resolvedName = sc.characterName || (mappedId ? idToNameMap.get(mappedId) : '');
      return {
        id: sc.id,
        characterCardId: mappedId,
        characterName: resolvedName || sc.characterName || '',
        status: sc.status || '',
        thought: sc.thought || '',
        action: sc.action || '',
      } as SceneCharacter;
    });

    const derivedPresentIds = (scene.presentCharacterIds && scene.presentCharacterIds.length > 0)
      ? scene.presentCharacterIds
      : normalizedSceneCharacters
          .map((sc) => sc.characterCardId)
          .filter((id): id is number => typeof id === 'number');

    form.setFieldsValue({
      ...scene,
      presentCharacterIds: derivedPresentIds.length > 0 ? derivedPresentIds : scene.presentCharacterIds,
      sceneCharacters: normalizedSceneCharacters,
    });
  }, [scene, form, idToNameMap, nameToIdMap]);

  const handleValuesChange = (changedValues: Partial<Scene>, allValues: Scene) => {
    let updatedScene: Scene = { ...scene, ...allValues };

    if (Object.prototype.hasOwnProperty.call(changedValues, 'presentCharacterIds')) {
      const selectedIds = allValues.presentCharacterIds || [];
      const existing = allValues.sceneCharacters || [];
      const mapped: SceneCharacter[] = selectedIds.map((id) => {
        const matched = existing.find((sc) => sc.characterCardId === id)
          || (scene.sceneCharacters || []).find((sc) => sc.characterCardId === id
            || (sc.characterName && idToNameMap.get(id)?.trim() === sc.characterName.trim()));
        return {
          id: matched?.id,
          characterCardId: id,
          characterName: idToNameMap.get(id) || matched?.characterName || '',
          status: matched?.status || '',
          thought: matched?.thought || '',
          action: matched?.action || '',
        };
      });
      form.setFieldsValue({ sceneCharacters: mapped });
      updatedScene = { ...updatedScene, sceneCharacters: mapped };
      const names = mapped.map((sc) => sc.characterName).filter((name) => name && name.trim().length > 0);
      updatedScene.presentCharacters = names.length > 0 ? names.join(', ') : updatedScene.presentCharacters;
    } else if (Object.prototype.hasOwnProperty.call(changedValues, 'sceneCharacters')) {
      const mapped = (allValues.sceneCharacters || []).map((sc) => ({
        ...sc,
        characterName: sc.characterName || (sc.characterCardId ? idToNameMap.get(sc.characterCardId) || '' : ''),
      }));
      updatedScene = { ...updatedScene, sceneCharacters: mapped };
      const names = mapped.map((sc) => sc.characterName).filter((name) => name && name.trim().length > 0);
      if (names.length > 0) {
        updatedScene.presentCharacters = names.join(', ');
      }
    }

    onUpdate(updatedScene);
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
    form.setFieldsValue({ temporaryCharacters: updatedChars });
    onUpdate({ ...scene, temporaryCharacters: updatedChars });
  };

  const handleDeleteTempChar = (id: number) => {
    const updatedChars = (scene.temporaryCharacters || []).filter(c => c.id !== id);
    form.setFieldsValue({ temporaryCharacters: updatedChars });
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

  const temporaryCharacters: TemporaryCharacter[] =
    (Form.useWatch('temporaryCharacters', form) as TemporaryCharacter[] | undefined)
      ?? scene.temporaryCharacters
      ?? [];

  return (
    <>
      <Form form={form} layout="vertical" initialValues={scene} onValuesChange={handleValuesChange}>
        {renderTextAreaWithRefine('synopsis', '场景梗概', '场景梗概')}

        <Form.Item name="expectedWords" label="预定字数">
          <InputNumber min={0} step={100} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item name="presentCharacterIds" label="核心出场人物">
          <Select
            mode="multiple"
            options={characterOptions}
            placeholder="选择出场人物"
            allowClear
          />
        </Form.Item>

        <Form.List name="sceneCharacters">
          {(fields) => (
            <div style={{ marginBottom: 24 }}>
              <div style={{ fontWeight: 600, marginBottom: 12 }}>核心人物状态卡</div>
              {fields.length === 0 ? (
                <div style={{ color: '#999', marginBottom: 16 }}>请选择核心出场人物以生成对应的状态卡。</div>
              ) : (
                fields.map((field) => {
                  const fieldValue: SceneCharacter | undefined = form.getFieldValue(['sceneCharacters', field.name]);
                  const displayName = fieldValue?.characterName || '未命名角色';
                  return (
                    <Card key={field.key} title={displayName} style={{ marginBottom: 16 }}>
                      <Form.Item name={[field.name, 'id']} hidden>
                        <Input />
                      </Form.Item>
                      <Form.Item name={[field.name, 'characterCardId']} hidden>
                        <Input />
                      </Form.Item>
                      <Form.Item name={[field.name, 'characterName']} hidden>
                        <Input />
                      </Form.Item>
                      <Form.Item label="状态" name={[field.name, 'status']}>
                        <TextArea rows={3} placeholder="此角色在本节的状态或处境" />
                      </Form.Item>
                      <Form.Item label="想法" name={[field.name, 'thought']}>
                        <TextArea rows={3} placeholder="此角色在本节的核心想法或心理活动" />
                      </Form.Item>
                      <Form.Item label="行动" name={[field.name, 'action']}>
                        <TextArea rows={3} placeholder="此角色在本节的关键行动" />
                      </Form.Item>
                    </Card>
                  );
                })
              )}
            </div>
          )}
        </Form.List>

        <List<TemporaryCharacter>
          header={<div>临时人物</div>}
          bordered
          dataSource={temporaryCharacters}
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
        <Button
          type="dashed"
          onClick={() => { setEditingTempChar(null); setIsTempCharModalOpen(true); }}
          style={{ marginTop: 16 }}
        >
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
