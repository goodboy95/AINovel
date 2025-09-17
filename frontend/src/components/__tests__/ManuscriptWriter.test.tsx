import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ManuscriptWriter from '../ManuscriptWriter';

const analyzeCharacterChangesMock = vi.fn();
const fetchCharacterChangeLogsMock = vi.fn();

vi.mock('../../services/api', () => ({
  fetchStories: vi.fn().mockResolvedValue([{ id: 1, title: '故事一', synopsis: '', storyArc: '', genre: '', tone: '' }]),
  fetchStoryDetails: vi.fn().mockResolvedValue({
    storyCard: { id: 1, title: '故事一', synopsis: '', storyArc: '', genre: '', tone: '' },
    characterCards: [
      { id: 1001, name: '林渊', synopsis: '', details: '', relationships: '' },
    ],
  }),
  fetchManuscriptsForOutline: vi.fn().mockResolvedValue([
    { id: 200, title: '手稿A', outlineId: 1, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
  ]),
  fetchManuscriptWithSections: vi.fn().mockResolvedValue({
    manuscript: { id: 200, title: '手稿A', outlineId: 1, createdAt: '', updatedAt: '' },
    sections: {
      101: { id: 300, content: '原始正文', version: 1, sceneId: 101 },
    },
  }),
  createManuscript: vi.fn(),
  deleteManuscript: vi.fn(),
  analyzeCharacterChanges: analyzeCharacterChangesMock,
  fetchCharacterChangeLogs: fetchCharacterChangeLogsMock,
}));

const outlineDetail = {
  id: 1,
  title: '大纲一',
  pointOfView: '',
  chapters: [
    {
      id: 10,
      chapterNumber: 2,
      title: '第二章',
      synopsis: '',
      scenes: [
        {
          id: 101,
          sceneNumber: 4,
          synopsis: '场景概要',
          expectedWords: 800,
          presentCharacterIds: [1001],
          presentCharacters: '',
          characterStates: '',
          temporaryCharacters: [],
        },
      ],
    },
  ],
};

vi.mock('../../hooks/useOutlineData', () => ({
  useOutlineData: () => ({
    outlines: [{ id: 1, title: '大纲一', pointOfView: '', chapters: [] }],
    selectedOutline: { id: 1, title: '大纲一', pointOfView: '', chapters: [] },
    selectOutline: vi.fn(),
    getOutlineForWriting: vi.fn().mockResolvedValue(outlineDetail),
    loadOutlines: vi.fn(),
  }),
}));

describe('ManuscriptWriter runCharacterAnalysis', () => {
  beforeEach(() => {
    analyzeCharacterChangesMock.mockClear();
    fetchCharacterChangeLogsMock.mockClear();
    analyzeCharacterChangesMock.mockResolvedValue([
      {
        id: 900,
        characterId: 1001,
        chapterNumber: 2,
        sectionNumber: 4,
        newlyKnownInfo: '新信息',
        characterChanges: '状态改变',
        characterDetailsAfter: '更新后的详情',
        isAutoCopied: false,
        createdAt: new Date().toISOString(),
      },
    ]);
    fetchCharacterChangeLogsMock.mockResolvedValue([]);
  });

  it('triggers character analysis and renders results', async () => {
    render(<ManuscriptWriter storyId="1" selectedSceneId={101} onSelectScene={vi.fn()} />);

    await waitFor(() => expect(screen.getByText('手稿A')).toBeInTheDocument());

    fireEvent.click(screen.getByText('手稿A'));

    await waitFor(() => expect(screen.getByText('原始正文')).toBeInTheDocument());

    const analyzeButton = await screen.findByRole('button', { name: '分析角色变化' });
    fireEvent.click(analyzeButton);

    await waitFor(() => expect(analyzeCharacterChangesMock).toHaveBeenCalled());

    expect(analyzeCharacterChangesMock).toHaveBeenCalledWith(200, expect.objectContaining({
      sceneId: 101,
      characterIds: [1001],
      sectionContent: '原始正文',
    }));

    await waitFor(() => expect(screen.getByText('状态改变')).toBeInTheDocument());
  });
});

