import React from 'react';
import { Button, Card, Space, Tooltip, Typography } from 'antd';
import TextArea from 'antd/es/input/TextArea';
import { InfoCircleOutlined } from '@ant-design/icons';

const { Text } = Typography;

const SparklesSvg = () => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="1em" height="1em">
        <path d="M11.5 2.75a.75.75 0 0 1 1.44 0l.862 2.649a3.25 3.25 0 0 0 2.046 2.046l2.649.862a.75.75 0 0 1 0 1.44l-2.649.862a3.25 3.25 0 0 0-2.046 2.046l-.862 2.649a.75.75 0 0 1-1.44 0l-.862-2.649a3.25 3.25 0 0 0-2.046-2.046l-2.649-.862a.75.75 0 0 1 0-1.44l2.649-.862a3.25 3.25 0 0 0 2.046-2.046Z" />
        <path d="M5 3.75a.75.75 0 0 1 .75.75v1a2.5 2.5 0 0 0 2.5 2.5h1a.75.75 0 0 1 0 1.5h-1A2.5 2.5 0 0 0 5.75 12v1a.75.75 0 0 1-1.5 0v-1A2.5 2.5 0 0 0 1.75 8.5h-1a.75.75 0 0 1 0-1.5h1A2.5 2.5 0 0 0 4.25 5.5v-1A.75.75 0 0 1 5 3.75Zm13.75 7.5a.75.75 0 0 1 .75.75v.5a1.75 1.75 0 0 0 1.75 1.75h.5a.75.75 0 0 1 0 1.5h-.5A1.75 1.75 0 0 0 19.5 17v.5a.75.75 0 0 1-1.5 0V17a1.75 1.75 0 0 0-1.75-1.75h-.5a.75.75 0 0 1 0-1.5h.5A1.75 1.75 0 0 0 18 12v-.5a.75.75 0 0 1 .75-.75Z" />
    </svg>
);

const SparklesIcon: React.FC = () => <SparklesSvg />;

type FieldCardProps = {
    label: string;
    tooltip?: string;
    required?: boolean;
    recommendedLength?: string;
    value: string;
    disabled?: boolean;
    onChange: (value: string) => void;
    onOptimize: () => void;
};

const FieldCard: React.FC<FieldCardProps> = ({
    label,
    tooltip,
    required,
    recommendedLength,
    value,
    disabled,
    onChange,
    onOptimize,
}) => {
    const length = value?.length ?? 0;
    return (
        <Card size="small" style={{ marginBottom: 16 }}>
            <Space style={{ width: '100%', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <Space size={6} align="center">
                    <Text strong>
                        {label}
                        {required ? ' *' : ''}
                    </Text>
                    {tooltip && (
                        <Tooltip title={tooltip}>
                            <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                        </Tooltip>
                    )}
                </Space>
                <Space size={12} align="center">
                    {recommendedLength && <Text type="secondary">{recommendedLength}</Text>}
                    <Button icon={<SparklesIcon />} size="small" onClick={onOptimize} disabled={disabled}>
                        AI 优化
                    </Button>
                </Space>
            </Space>
            <TextArea
                style={{ marginTop: 12 }}
                value={value}
                onChange={e => onChange(e.target.value)}
                disabled={disabled}
                autoSize={{ minRows: 4, maxRows: 10 }}
                placeholder="填写具体设定内容"
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
                <Text type="secondary">字数：{length}</Text>
                {disabled && <Text type="secondary">生成过程中暂不可编辑</Text>}
            </div>
        </Card>
    );
};

export default FieldCard;
