import React, { useState, useEffect, useMemo } from 'react';
import { Card, Input, Button, message, Empty, Layout, Spin, Row, Col } from 'antd';
import OutlineTreeView, { type RefineHandler } from './OutlineTreeView';
import type { Outline, ManuscriptSection } from '../types';

const { Content, Sider } = Layout;
const { TextArea } = Input;

export interface ManuscriptWriterProps {
    selectedOutline: Outline | null;
    selectedSceneId: number | null;
    onSelectScene: (sceneId: number | null) => void;
    onUpdateOutline: (updatedOutline: Outline) => void;
    handleOpenRefineModal: RefineHandler;
}

const ManuscriptWriter: React.FC<ManuscriptWriterProps> = ({
    selectedOutline,
    selectedSceneId,
    onSelectScene,
    onUpdateOutline,
    handleOpenRefineModal,
}) => {
    const [manuscriptMap, setManuscriptMap] = useState<Record<number, ManuscriptSection>>({});
    const [manuscriptContent, setManuscriptContent] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [isFetchingManuscript, setIsFetchingManuscript] = useState(false);

    const selectedScene = useMemo(() => {
        if (!selectedOutline || !selectedSceneId) return null;
        for (const chapter of selectedOutline.chapters) {
            const scene = chapter.scenes.find(s => s.id === selectedSceneId);
            if (scene) return scene;
        }
        return null;
    }, [selectedOutline, selectedSceneId]);

    useEffect(() => {
        if (selectedOutline) {
            setIsFetchingManuscript(true);
            const fetchManuscript = async () => {
                try {
                    const token = localStorage.getItem('token');
                    const response = await fetch(`/api/v1/manuscript/outlines/${selectedOutline.id}`, {
                        headers: { 'Authorization': `Bearer ${token}` }
                    });
                    if (!response.ok) {
                        console.error('Failed to fetch manuscript: Response not OK', response.status);
                        throw new Error('Failed to fetch manuscript');
                    }
                    const data = await response.json();
                    setManuscriptMap(data);
                } catch (error) {
                    console.error("Failed to fetch manuscript:", error);
                    message.error('获取正文内容失败');
                } finally {
                    setIsFetchingManuscript(false);
                }
            };
            fetchManuscript();
        }
    }, [selectedOutline]);

    useEffect(() => {
        if (selectedSceneId && manuscriptMap[selectedSceneId]) {
            const newContent = manuscriptMap[selectedSceneId].content;
            setManuscriptContent(newContent);
        } else {
            setManuscriptContent('');
        }
    }, [selectedSceneId, manuscriptMap]);


    const handleGenerateContent = async () => {
        if (!selectedSceneId) {
            message.warning('请先从左侧选择一个场景！');
            return;
        }
        setIsLoading(true);
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`/api/v1/manuscript/scenes/${selectedSceneId}/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || '生成内容失败。');
            }

            const newSection = await response.json();
            setManuscriptMap(prev => {
                const newMap = { ...prev, [newSection.scene.id]: newSection };
                return newMap;
            });
            message.success('内容已生成！现在您可以继续编辑。');
        } catch (error) {
            message.error(error instanceof Error ? error.message : '内容生成失败，请检查后台服务。');
        } finally {
            setIsLoading(false);
        }
    };
    
    const handleSaveContent = async () => {
        if (!selectedSceneId) return;
        
        const sectionToUpdate = manuscriptMap[selectedSceneId];
        if (!sectionToUpdate) {
            message.warning("没有可保存的内容。请先生成内容。");
            return;
        }

        setIsLoading(true);
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`/api/v1/manuscript/sections/${sectionToUpdate.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                body: JSON.stringify({ content: manuscriptContent }),
            });

            if (!response.ok) {
                console.error('Failed to save content: Response not OK', response.status);
                throw new Error('保存失败');
            }

            const updatedSection = await response.json();
            setManuscriptMap(prev => ({ ...prev, [updatedSection.sceneId]: updatedSection }));
            message.success('内容已保存！');
        } catch (error) {
            console.error("Failed to save content:", error);
            message.error(error instanceof Error ? error.message : '保存失败');
        } finally {
            setIsLoading(false);
        }
    };


    if (!selectedOutline) {
        return (
            <div className="flex justify-center items-center h-full">
                <Empty description="请从“大纲管理”页面选择一个大纲开始创作。" />
            </div>
        );
    }

    return (
        <Layout style={{ height: 'calc(100vh - 150px)', background: '#fff' }}>
            <Sider width={300} style={{ background: '#fff', padding: '16px', borderRight: '1px solid #f0f0f0', overflow: 'auto' }}>
                <h2 className="text-lg font-bold mb-4">{selectedOutline.title}</h2>
                <OutlineTreeView
                    outline={selectedOutline}
                    onSelectScene={(scene) => onSelectScene(scene ? scene.id : null)}
                    onUpdate={onUpdateOutline}
                    handleOpenRefineModal={handleOpenRefineModal}
                />
            </Sider>
            <Content style={{ padding: '24px' }}>
                <Spin spinning={isFetchingManuscript} tip="加载正文...">
                    <Row gutter={24} style={{ height: '100%' }}>
                        <Col span={24} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                            <Card title="场景内容" style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                                <Card.Meta
                                    title="当前场景概要"
                                    description={selectedScene?.synopsis || '请从左侧大纲树中选择一个场景。'}
                                    style={{ marginBottom: '16px' }}
                                />
                                <TextArea
                                    value={manuscriptContent}
                                    onChange={(e) => setManuscriptContent(e.target.value)}
                                    placeholder="这里是您创作的故事内容..."
                                    rows={20}
                                    style={{ resize: 'none' }}
                                    disabled={!selectedSceneId}
                                />
                                <div className="mt-4 flex items-center justify-between">
                                    <div>
                                        <Button
                                            type="primary"
                                            loading={isLoading}
                                            onClick={handleGenerateContent}
                                            disabled={!selectedSceneId}
                                        >
                                            ✨ 生成内容
                                        </Button>
                                        <Button
                                            style={{ marginLeft: 8 }}
                                            onClick={handleSaveContent}
                                            disabled={!selectedSceneId || isLoading}
                                        >
                                            保存
                                        </Button>
                                    </div>
                                    <span className="text-sm text-gray-500">
                                        预期字数: {selectedScene?.expectedWords || 'N/A'}
                                    </span>
                                </div>
                            </Card>
                        </Col>
                    </Row>
                </Spin>
            </Content>
        </Layout>
    );
};

export default ManuscriptWriter;
