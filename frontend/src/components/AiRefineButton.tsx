import React from 'react';
import { Button } from 'antd';
import aiIcon from '../../assets/img/ai.png';

interface AiRefineButtonProps {
  onClick: () => void;
  className?: string;
}

const buttonStyle: React.CSSProperties = {
  position: 'absolute',
  top: 0,
  right: 0,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: 0,
};

const iconStyle: React.CSSProperties = {
  width: 16,
  height: 16,
  display: 'block',
};

const AiRefineButton: React.FC<AiRefineButtonProps> = ({ onClick, className }) => {
  return (
    <Button
      type="default"
      size="small"
      shape="circle"
      onClick={onClick}
      title="AI 优化"
      className={className}
      style={buttonStyle}
    >
      <img src={aiIcon} alt="AI Icon" style={iconStyle} />
    </Button>
  );
};

export default AiRefineButton;
