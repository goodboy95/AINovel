import React, { useState } from 'react';
import { Button, Card, Col, Row, Spin, Alert, Empty } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import type { Outline, Chapter, Scene } from '../types';
import OutlineTreeView from './OutlineTreeView';
import ChapterEditForm from './ChapterEditForm';
import SceneEditForm from './SceneEditForm';

interface OutlineManagementPageProps {
  outline: Outline | null;
  onUpdateLocal: (updatedData: Chapter | Scene) => void;
  onSaveRemote: () => Promise<void>;
  isSaving: boolean;
  error: string | null;
}

type SelectedNode = 
  | { type: 'chapter'; data: Chapter }
  | { type: 'scene'; data: Scene }
  | null;

const OutlineManagementPage: React.FC<OutlineManagementPageProps> = ({ outline, onUpdateLocal, onSaveRemote, isSaving, error }) => {
  const [selectedNode, setSelectedNode] = useState<SelectedNode>(null);

  const handleNodeSelect = (node: SelectedNode) => {
    setSelectedNode(node);
  };

  const renderEditForm = () => {
    if (!selectedNode) {
      return <Empty description="请在左侧选择一个章节或场景进行编辑" />;
    }
    switch (selectedNode.type) {
      case 'chapter':
        return <ChapterEditForm chapter={selectedNode.data} onUpdate={onUpdateLocal} />;
      case 'scene':
        return <SceneEditForm scene={selectedNode.data} onUpdate={onUpdateLocal} />;
      default:
        return null;
    }
  };

  if (!outline) {
    return <Card><Empty description="没有加载任何大纲。请先在“大纲设计”页面选择或创建一个大纲。" /></Card>;
  }

  return (
    <Spin spinning={isSaving} tip="正在保存大纲...">
      {error && <Alert message="保存失败" description={error} type="error" showIcon style={{ marginBottom: 16 }} />}
      <Row gutter={16}>
        <Col span={10}>
          <Card title="大纲结构" extra={<Button icon={<SaveOutlined />} type="primary" onClick={onSaveRemote}>保存大纲</Button>}>
            <OutlineTreeView outline={outline} onNodeSelect={handleNodeSelect} />
          </Card>
        </Col>
        <Col span={14}>
          <Card title="编辑区域">
            {renderEditForm()}
          </Card>
        </Col>
      </Row>
    </Spin>
  );
};

export default OutlineManagementPage;