import { useState, useEffect } from 'react';
import { Layout, Tree, Spin, Input, Button, Card, Typography, Empty } from 'antd';
import { useParams } from 'react-router-dom'; // Assuming you use React Router for navigation
import type { Outline, ManuscriptSection } from '../types';

const { Sider, Content, Header } = Layout;
const { Title, Paragraph } = Typography;

import React from 'react';

const ManuscriptWriter = () => {
    const { outlineId } = useParams<{ outlineId: string }>();
    const [outline, setOutline] = useState<Outline | null>(null);
    const [manuscript, setManuscript] = useState<ManuscriptSection[]>([]);
    const [selectedSceneId, setSelectedSceneId] = useState<number | null>(null);
    const [currentSection, setCurrentSection] = useState<ManuscriptSection | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isGenerating, setIsGenerating] = useState(false);

    useEffect(() => {
        if (outlineId) {
            fetchOutlineAndManuscript();
        }
    }, [outlineId]);

    const fetchOutlineAndManuscript = async () => {
        setIsLoading(true);
        const token = localStorage.getItem('token');
        const headers = { 'Authorization': `Bearer ${token}` };

        try {
            // Fetch outline details
            const outlineResponse = await fetch(`/api/v1/outlines/${outlineId}`, { headers });
            if (!outlineResponse.ok) throw new Error('Failed to fetch outline');
            const outlineData = await outlineResponse.json();
            setOutline(outlineData);

            // Fetch manuscript sections for this outline
            const manuscriptResponse = await fetch(`/api/v1/manuscript/outlines/${outlineId}`, { headers });
            if (!manuscriptResponse.ok) throw new Error('Failed to fetch manuscript');
            const manuscriptData = await manuscriptResponse.json();
            setManuscript(manuscriptData);

        } catch (error) {
            console.error(error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleGenerate = async () => {
        if (!selectedSceneId) return;
        setIsGenerating(true);
        const token = localStorage.getItem('token');
        try {
            const response = await fetch(`/api/v1/manuscript/scenes/${selectedSceneId}/generate`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) throw new Error('Failed to generate content');
            const newSection = await response.json();
            setManuscript(prev => [...prev.filter(s => s.scene.id !== selectedSceneId), newSection]);
            setCurrentSection(newSection);
        } catch (error) {
            console.error(error);
        } finally {
            setIsGenerating(false);
        }
    };
    
    const handleSave = async () => {
        if (!currentSection) return;
        const token = localStorage.getItem('token');
        try {
            await fetch(`/api/v1/manuscript/sections/${currentSection.id}`, {
                method: 'PUT',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ content: currentSection.content })
            });
        } catch (error) {
            console.error(error);
        }
    };

    const treeData = outline?.chapters.map(chapter => ({
        title: `Chapter ${chapter.chapterNumber}: ${chapter.title}`,
        key: `chapter-${chapter.id}`,
        children: chapter.scenes.map(scene => ({
            title: `Scene ${scene.sceneNumber}: ${scene.synopsis}`,
            key: scene.id,
            isLeaf: true,
        })),
    }));

    const onSelectNode = (selectedKeys: React.Key[]) => {
        const sceneId = selectedKeys[0];
        if (sceneId && typeof sceneId === 'number') {
            setSelectedSceneId(sceneId);
            const section = manuscript.find(s => s.scene.id === sceneId);
            setCurrentSection(section || { id: 0, scene: { id: sceneId }, content: '', version: 0, active: false });
        }
    };

    return (
        <Layout style={{ height: '100vh' }}>
            <Header>
                <Title style={{ color: 'white', lineHeight: '64px' }} level={3}>
                    {outline?.title || 'Writer Mode'}
                </Title>
            </Header>
            <Layout>
                <Sider width={350} theme="light">
                    <Spin spinning={isLoading}>
                        {treeData && <Tree treeData={treeData} onSelect={onSelectNode} />}
                    </Spin>
                </Sider>
                <Content style={{ padding: '24px' }}>
                    {selectedSceneId ? (
                        <Spin spinning={isGenerating} tip="AI is writing...">
                            <Input.TextArea
                                value={currentSection?.content}
                                onChange={(e) => setCurrentSection((prev: ManuscriptSection | null) => prev ? { ...prev, content: e.target.value } : null)}
                                autoSize={{ minRows: 20 }}
                            />
                            <Button onClick={handleGenerate} disabled={isGenerating} style={{ marginTop: 16 }}>
                                Generate
                            </Button>
                            <Button onClick={handleSave} style={{ marginLeft: 8 }}>
                                Save
                            </Button>
                        </Spin>
                    ) : (
                        <Empty description="Select a scene from the outline to start writing." />
                    )}
                </Content>
                <Sider width={300} theme="light" style={{ padding: '12px' }}>
                    <Title level={4}>Context</Title>
                    <Card title="Story Synopsis">
                        <Paragraph>{outline?.storyCard?.synopsis}</Paragraph>
                    </Card>
                </Sider>
            </Layout>
        </Layout>
    );
};

export default ManuscriptWriter;
