import { Card, Tabs } from 'antd';
import type { TabsProps } from 'antd';
import { useMemo } from 'react';
import MaterialCreateForm from '../../components/MaterialCreateForm';
import MaterialUpload from '../../components/MaterialUpload';
import ReviewDashboard from './ReviewDashboard';
import MaterialList from '../../components/MaterialList';
import { useCanPerform } from '../../hooks/useCanPerform';

const MaterialPage = () => {
    const canWriteWorkspace = useCanPerform('workspace:write');

    const tabItems = useMemo(() => {
        const items: TabsProps['items'] = [];
        if (canWriteWorkspace) {
            items.push(
                {
                    key: 'create',
                    label: '创建素材',
                    children: <MaterialCreateForm />,
                },
                {
                    key: 'upload',
                    label: '上传文件',
                    children: <MaterialUpload />,
                },
                {
                    key: 'review',
                    label: '素材审核',
                    children: <ReviewDashboard />,
                },
            );
        }
        items.push({
            key: 'list',
            label: '素材列表',
            children: <MaterialList />,
        });
        return items;
    }, [canWriteWorkspace]);

    const defaultActiveKey = canWriteWorkspace ? 'create' : 'list';

    return (
        <div style={{ padding: 24 }}>
            <Card title="素材库" style={{ border: 'none' }}>
                <Tabs defaultActiveKey={defaultActiveKey} items={tabItems} />
            </Card>
        </div>
    );
};

export default MaterialPage;
