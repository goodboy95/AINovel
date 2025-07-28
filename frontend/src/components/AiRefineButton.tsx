import React from 'react';

interface AiRefineButtonProps {
  onClick: () => void;
  className?: string;
}

const AiRefineButton: React.FC<AiRefineButtonProps> = ({ onClick, className }) => {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`absolute bottom-2 right-2 text-gray-400 hover:text-blue-500 transition-colors ${className}`}
      title="AI 优化"
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
        <path fillRule="evenodd" d="M17.293 2.293a1 1 0 011.414 1.414l-1.414-1.414zM4 10a1 1 0 01-1-1V5a1 1 0 112 0v3h3a1 1 0 110 2H4zm13.707-5.707a1 1 0 00-1.414-1.414L4 14.586V16h1.414l12.293-12.293zM16 4.586L15.414 4 4 15.414V16h.586L16 4.586zM12 10a2 2 0 11-4 0 2 2 0 014 0z" clipRule="evenodd" />
        <path d="M12.293 15.707a1 1 0 010-1.414l4-4a1 1 0 011.414 1.414l-4 4a1 1 0 01-1.414 0z" />
      </svg>
    </button>
  );
};

export default AiRefineButton;