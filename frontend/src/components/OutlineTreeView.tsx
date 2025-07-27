import React, { useState } from 'react';
import { Input, Button } from 'antd';
import type { Outline, Chapter, Scene } from '../types';
import Icon from '@ant-design/icons';

const SparklesSvg = () => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" fill="currentColor" width="1em" height="1em">
        <path d="M512 900.7c-26.5 0-48-21.5-48-48V640c0-26.5 21.5-48 48-48s48 21.5 48 48v212.7c0 26.5-21.5 48-48 48zM221.3 550.7c-18.7 18.7-49.1 18.7-67.9 0s-18.7-49.1 0-67.9l150.4-150.4c18.7-18.7 49.1-18.7 67.9 0s18.7 49.1 0 67.9L221.3 550.7zM802.7 550.7L652.3 400.3c-18.7-18.7-18.7-49.1 0-67.9s49.1-18.7 67.9 0l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0zM512 432c-26.5 0-48-21.5-48-48V171.3c0-26.5 21.5-48 48-48s48 21.5 48 48V384c0 26.5-21.5 48-48 48zM123.3 221.3c-18.7-18.7-18.7-49.1 0-67.9s49.1-18.7 67.9 0l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0L123.3 221.3zM868.6 221.3l-150.4-150.4c-18.7-18.7-49.1-18.7-67.9 0s-18.7 49.1 0 67.9l150.4 150.4c18.7 18.7 18.7 49.1 0 67.9s-49.1 18.7-67.9 0z" />
    </svg>
);
const SparklesIcon = (props: React.ComponentProps<typeof Icon>) => <Icon component={SparklesSvg} {...props} />;

const { TextArea } = Input;

// Define a callback type for updates
type UpdateHandler<T> = (updatedData: T) => void;
export type RefineHandler = (text: string, context: { endpoint: string; fieldName: string; cardId: number; onSuccess: (newText: string) => void; }) => void;

const SceneView = ({ scene, onUpdate, onSelect, handleOpenRefineModal }: { scene: Scene, onUpdate?: UpdateHandler<Scene>, onSelect?: (scene: Scene) => void, handleOpenRefineModal?: RefineHandler }) => {
    const handleSynopsisChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        onUpdate?.({ ...scene, synopsis: e.target.value });
    };

    return (
        <div className="ml-8 p-2 border-l-2 cursor-pointer hover:bg-gray-100" onClick={() => onSelect && onSelect(scene)}>
            <div style={{ position: 'relative' }}>
                <p><strong>场景 {scene.sceneNumber}:</strong> (预期字数: {scene.expectedWords})</p>
                <TextArea
                    value={scene.synopsis}
                    onChange={handleSynopsisChange}
                    placeholder="场景梗概"
                    autoSize={{ minRows: 2, maxRows: 6 }}
                    className="text-sm text-gray-600"
                    disabled={!onUpdate}
                />
                {onUpdate && handleOpenRefineModal && (
                    <Button icon={<SparklesIcon />} size="small" style={{ position: 'absolute', top: 0, right: 0 }} onClick={(e) => { e.stopPropagation(); handleOpenRefineModal(scene.synopsis, { cardId: scene.id, endpoint: `/api/v1/outlines/scenes/${scene.id}/refine`, fieldName: 'synopsis', onSuccess: (newText) => onUpdate({ ...scene, synopsis: newText }) })}} />
                )}
            </div>
        </div>
    );
};

const ChapterView = ({ chapter, onUpdate, onSelectScene, handleOpenRefineModal }: { chapter: Chapter, onUpdate?: UpdateHandler<Chapter>, onSelectScene?: (scene: Scene) => void, handleOpenRefineModal?: RefineHandler }) => {
    const [isOpen, setIsOpen] = useState(true);

    const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        onUpdate?.({ ...chapter, title: e.target.value });
    };

    const handleSynopsisChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        onUpdate?.({ ...chapter, synopsis: e.target.value });
    };

    const handleSceneUpdate = (updatedScene: Scene) => {
        const updatedScenes = chapter.scenes.map(s => s.id === updatedScene.id ? updatedScene : s);
        onUpdate?.({ ...chapter, scenes: updatedScenes });
    };

    return (
        <div className="mb-2 border rounded">
            <div className="p-2 bg-gray-100 cursor-pointer" onClick={() => setIsOpen(!isOpen)}>
                <div className="flex items-center">
                    <span className="font-semibold mr-2">第 {chapter.chapterNumber} 章:</span>
                    <Input
                        value={chapter.title}
                        onChange={handleTitleChange}
                        placeholder="章节标题"
                        className="font-semibold"
                        disabled={!onUpdate}
                    />
                </div>
                <TextArea
                    value={chapter.synopsis}
                    onChange={handleSynopsisChange}
                    placeholder="章节梗概"
                    autoSize={{ minRows: 2, maxRows: 4 }}
                    className="mt-2 text-sm text-gray-700"
                    disabled={!onUpdate}
                />
            </div>
            {isOpen && (
                <div>
                    {chapter.scenes.map((scene) => (
                        <SceneView key={scene.id} scene={scene} onUpdate={handleSceneUpdate} onSelect={onSelectScene} handleOpenRefineModal={handleOpenRefineModal} />
                    ))}
                </div>
            )}
        </div>
    );
};

const OutlineTreeView = ({ outline, onUpdate, onSelectScene, handleOpenRefineModal }: { outline: Outline | null, onUpdate?: (updatedOutline: Outline) => void, onSelectScene?: (scene: Scene) => void, handleOpenRefineModal?: RefineHandler }) => {
    if (!outline) return null;

    const handleChapterUpdate = (updatedChapter: Chapter) => {
        if (!onUpdate) return;
        const updatedChapters = outline.chapters.map(c => c.id === updatedChapter.id ? updatedChapter : c);
        onUpdate({ ...outline, chapters: updatedChapters });
    };

    return (
        <div>
            {/* Title and POV are edited in the parent modal, so we just display them here */}
            <h2 className="text-xl font-bold mb-4">{outline.title}</h2>
            <p className="mb-2"><strong>视角:</strong> {outline.pointOfView}</p>
            <div>
                {outline.chapters.map((chapter) => (
                    <ChapterView key={chapter.id} chapter={chapter} onUpdate={handleChapterUpdate} onSelectScene={onSelectScene} handleOpenRefineModal={handleOpenRefineModal} />
                ))}
            </div>
        </div>
    );
};

export default OutlineTreeView;
