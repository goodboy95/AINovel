import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import CharacterStatusSidebar from '../CharacterStatusSidebar';
import type { CharacterChangeLog, CharacterCard } from '../../types';

const characterMap: Record<number, CharacterCard> = {
  1: { id: 1, name: '林渊', synopsis: '', details: '', relationships: '' },
};

const logs: CharacterChangeLog[] = [
  {
    id: 10,
    characterId: 1,
    chapterNumber: 1,
    sectionNumber: 2,
    newlyKnownInfo: '得知真相',
    characterChanges: '情绪变化',
    characterDetailsAfter: '新的角色画像',
    isAutoCopied: false,
    createdAt: new Date(2024, 4, 1, 10, 30).toISOString(),
  },
];

describe('CharacterStatusSidebar', () => {
  it('renders character logs and opens history modal', () => {
    render(
      <CharacterStatusSidebar
        characterMap={characterMap}
        logs={logs}
        chapterNumber={2}
        sectionNumber={3}
        isAnalyzing={false}
      />
    );

    expect(screen.getByText('角色状态追踪（第2章·第3节）')).toBeInTheDocument();
    expect(screen.getByText('林渊')).toBeInTheDocument();
    expect(screen.getByText('得知真相')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '历史记录' }));

    expect(screen.getByText(/角色变化：/)).toBeInTheDocument();
    expect(screen.getByText('新的角色画像')).toBeInTheDocument();
  });

  it('shows empty state when no logs', () => {
    render(
      <CharacterStatusSidebar
        characterMap={{}}
        logs={[]}
        isAnalyzing={false}
      />
    );

    expect(screen.getByText('暂无分析记录')).toBeInTheDocument();
  });
});

