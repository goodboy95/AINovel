import { useState } from 'react';
import { Empty, Input, List, message, Space, Spin, Tag, Typography } from 'antd';
import { searchMaterials } from '../services/api';
import type { MaterialSearchResult } from '../types';

const { Text } = Typography;

const MaterialSearchPanel = () => {
    const [query, setQuery] = useState('');
    const [loading, setLoading] = useState(false);
    const [results, setResults] = useState<MaterialSearchResult[]>([]);
    const [searched, setSearched] = useState(false);

    const handleSearch = async (value?: string) => {
        const keyword = (value ?? query).trim();
        if (!keyword) {
            message.warning('请输入检索关键词');
            return;
        }
        setLoading(true);
        try {
            const data = await searchMaterials(keyword, 10);
            setResults(data);
            setSearched(true);
        } catch (error) {
            const msg = error instanceof Error ? error.message : '素材检索失败';
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Input.Search
                placeholder="输入关键词（如角色名、场景描述）"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                enterButton="搜索"
                loading={loading}
                onSearch={handleSearch}
            />
            {loading && <Spin tip="检索中..." />}
            {!loading && results.length === 0 && searched && (
                <Empty description="未找到相关素材" />
            )}
            {!loading && results.length > 0 && (
                <List
                    dataSource={results}
                    renderItem={(item) => (
                        <List.Item>
                            <Space direction="vertical" size="small" style={{ width: '100%' }}>
                                <Space align="baseline">
                                    <Text strong>{item.title || '未命名素材'}</Text>
                                    {typeof item.score === 'number' && (
                                        <Tag color="blue">Score: {item.score.toFixed(2)}</Tag>
                                    )}
                                    {typeof item.chunkSeq === 'number' && (
                                        <Tag>片段 #{item.chunkSeq}</Tag>
                                    )}
                                </Space>
                                <Text>{item.snippet}</Text>
                            </Space>
                        </List.Item>
                    )}
                />
            )}
        </Space>
    );
};

export default MaterialSearchPanel;

