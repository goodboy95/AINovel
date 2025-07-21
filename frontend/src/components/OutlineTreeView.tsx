import React, { useState } from 'react';
import type { Outline, Chapter, Scene } from '../types';

const SceneView = ({ scene }: { scene: Scene }) => {
    return (
        <div className="ml-8 p-2 border-l-2">
            <p><strong>场景 {scene.sceneNumber}:</strong></p>
            <p className="text-sm text-gray-600">{scene.synopsis}</p>
            <p className="text-xs text-gray-500">预期字数: {scene.expectedWords}</p>
        </div>
    );
};

const ChapterView = ({ chapter }: { chapter: Chapter }) => {
    const [isOpen, setIsOpen] = useState(true);

    return (
        <div className="mb-2 border rounded">
            <div className="p-2 bg-gray-100 cursor-pointer" onClick={() => setIsOpen(!isOpen)}>
                <h3 className="font-semibold">第 {chapter.chapterNumber} 章: {chapter.title}</h3>
                <p className="text-sm text-gray-700">{chapter.synopsis}</p>
            </div>
            {isOpen && (
                <div>
                    {chapter.scenes.map((scene) => (
                        <SceneView key={scene.id} scene={scene} />
                    ))}
                </div>
            )}
        </div>
    );
};

const OutlineTreeView = ({ outline }: { outline: Outline | null }) => {
    if (!outline) return null;

    return (
        <div>
            <h2 className="text-xl font-bold mb-4">{outline.title}</h2>
            <p className="mb-2"><strong>视角:</strong> {outline.pointOfView}</p>
            <div>
                {outline.chapters.map((chapter) => (
                    <ChapterView key={chapter.id} chapter={chapter} />
                ))}
            </div>
        </div>
    );
};

export default OutlineTreeView;
