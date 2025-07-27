import { Card, Collapse, Typography, Tag, Divider, Space } from 'antd';
import type { Outline, Chapter, Scene, RefineHandler } from '../types';

const { Title, Text, Paragraph } = Typography;
const { Panel } = Collapse;

const SceneView = ({ scene, onSelect }: { scene: Scene, onSelect?: (scene: Scene) => void }) => {
    const characters = scene.presentCharacters?.split(/[,，、]/).map(name => name.trim()).filter(Boolean) || [];

    return (
        <Card
            size="small"
            className="mb-4 shadow-sm hover:shadow-md transition-shadow"
            onClick={() => onSelect && onSelect(scene)}
            hoverable={!!onSelect}
        >
            <div className="flex justify-between items-start">
                <Title level={5} className="!mb-1">第 {scene.sceneNumber} 节</Title>
                <Text type="secondary">约 {scene.expectedWords} 字</Text>
            </div>
            <Divider className="my-2" />
            <Paragraph ellipsis={{ rows: 3, expandable: true, symbol: '更多' }}>
                {scene.synopsis}
            </Paragraph>
            
            {characters.length > 0 && (
                <>
                    <Divider className="my-2" />
                    <div className="mb-2">
                        <Text strong>出场人物: </Text>
                        <Space wrap>
                            {characters.map((char, index) => <Tag key={index} color="blue">{char}</Tag>)}
                        </Space>
                    </div>
                </>
            )}

            {scene.characterStates && (
                <Collapse ghost size="small">
                    <Panel header="人物状态与行动" key="1">
                        <Paragraph pre-wrap className="text-xs bg-gray-50 p-2 rounded">
                            {scene.characterStates}
                        </Paragraph>
                    </Panel>
                </Collapse>
            )}
        </Card>
    );
};

const ChapterView = ({ chapter, onSelectScene }: { chapter: Chapter, onSelectScene?: (scene: Scene) => void }) => {
    return (
        <Card className="mb-4">
            <Title level={4}>第 {chapter.chapterNumber} 章: {chapter.title}</Title>
            <Paragraph type="secondary">{chapter.synopsis}</Paragraph>
            <Divider />
            <div>
                {chapter.scenes.map((scene) => (
                    <SceneView key={scene.id} scene={scene} onSelect={onSelectScene} />
                ))}
            </div>
        </Card>
    );
};

interface OutlineTreeViewProps {
    outline: Outline | null;
    onSelectScene?: (scene: Scene) => void;
    onUpdate?: (updatedOutline: Outline) => void;
    handleOpenRefineModal?: RefineHandler;
}

const OutlineTreeView = ({ outline, onSelectScene }: OutlineTreeViewProps) => {
    if (!outline) return null;

    return (
        <div>
            <Title level={2} className="text-center mb-2">{outline.title}</Title>
            <Text type="secondary" className="block text-center mb-6">叙事视角: {outline.pointOfView}</Text>
            <div>
                {outline.chapters.map((chapter) => (
                    <ChapterView key={chapter.id} chapter={chapter} onSelectScene={onSelectScene} />
                ))}
            </div>
        </div>
    );
};

export default OutlineTreeView;
