import { useState } from 'react';
import type { Outline, Chapter, Scene } from '../types';

type SelectedNode =
  | { type: 'chapter'; data: Chapter }
  | { type: 'scene'; data: Scene }
  | null;

// Scene Card Component
interface SceneCardProps {
    scene: Scene;
    onNodeSelect: (node: SelectedNode) => void;
}

const SceneCard = ({ scene, onNodeSelect }: SceneCardProps) => {
    const [isStatesExpanded, setIsStatesExpanded] = useState(false);
    const characters = scene.presentCharacters?.split(/[,，、]/).map(name => name.trim()).filter(Boolean) || [];

    return (
        <div
            className="border border-gray-200 rounded-lg p-4 mb-3 bg-white shadow-sm hover:shadow-lg transition-shadow cursor-pointer"
            onClick={() => onNodeSelect({ type: 'scene', data: scene })}
        >
            {/* Scene Header */}
            <div className="flex justify-between items-center mb-2">
                <h4 className="text-md font-bold text-gray-800">第 {scene.sceneNumber} 节</h4>
                <span className="text-sm text-gray-500">约 {scene.expectedWords} 字</span>
            </div>

            {/* Synopsis */}
            <p className="text-sm text-gray-600 mb-3">{scene.synopsis}</p>

            <div className="space-y-3">
                {/* Present Characters */}
                {characters.length > 0 && (
                    <div className="flex items-center">
                        <div>
                            <h5 className="text-sm font-semibold text-gray-700">核心人物</h5>
                            <div className="flex flex-wrap gap-2 mt-1">
                                {characters.map((char, index) => (
                                    <span key={index} className="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded-full">{char}</span>
                                ))}
                            </div>
                        </div>
                    </div>
                )}

                {/* Temporary Characters */}
                {scene.temporaryCharacters && scene.temporaryCharacters.length > 0 && (
                    <div>
                        <div>
                            <h5 className="text-sm font-semibold text-gray-700">临时人物</h5>
                            <ul className="list-disc list-inside mt-1 space-y-1">
                                {scene.temporaryCharacters.map(tc => (
                                    <li key={tc.id} className="text-sm text-gray-600">
                                        <span className="font-semibold">{tc.name}:</span> {tc.summary}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </div>
                )}

                {/* Character States */}
                {scene.characterStates && (
                    <div>
                        <button
                            onClick={(e) => {
                                e.stopPropagation(); // Prevent card click event
                                setIsStatesExpanded(!isStatesExpanded);
                            }}
                            className="flex items-center justify-between w-full text-left text-sm font-semibold text-gray-700"
                        >
                            <span>人物状态与行动</span>
                        </button>
                        {isStatesExpanded && (
                            <div className="mt-2 p-3 bg-gray-50 rounded-md whitespace-pre-wrap text-sm text-gray-800">
                                {scene.characterStates}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

// Chapter Card Component
interface ChapterCardProps {
    chapter: Chapter;
    onNodeSelect: (node: SelectedNode) => void;
}

const ChapterCard = ({ chapter, onNodeSelect }: ChapterCardProps) => {
    return (
        <div className="bg-gray-50 rounded-xl p-4 mb-6 shadow">
            <div onClick={() => onNodeSelect({ type: 'chapter', data: chapter })} className="cursor-pointer">
                <h3 className="text-xl font-bold text-gray-900 mb-1">第 {chapter.chapterNumber} 章: {chapter.title}</h3>
                <p className="text-sm text-gray-600 mb-4">{chapter.synopsis}</p>
            </div>
            <div>
                {chapter.scenes.sort((a, b) => a.sceneNumber - b.sceneNumber).map((scene) => (
                    <SceneCard key={scene.id} scene={scene} onNodeSelect={onNodeSelect} />
                ))}
            </div>
        </div>
    );
};

// Main Tree View Component
interface OutlineTreeViewProps {
    outline: Outline | null;
    onNodeSelect: (node: SelectedNode) => void;
}

const OutlineTreeView = ({ outline, onNodeSelect }: OutlineTreeViewProps) => {
    if (!outline) return null;

    return (
        <div className="h-full overflow-y-auto">
            {outline.chapters.sort((a, b) => a.chapterNumber - b.chapterNumber).map((chapter) => (
                <ChapterCard key={chapter.id} chapter={chapter} onNodeSelect={onNodeSelect} />
            ))}
        </div>
    );
};

export default OutlineTreeView;
