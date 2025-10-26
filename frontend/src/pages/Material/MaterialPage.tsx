import { Card, Tabs } from 'antd';
import MaterialCreateForm from '../../components/MaterialCreateForm';
import MaterialUpload from '../../components/MaterialUpload';
import ReviewDashboard from './ReviewDashboard';
import MaterialList from '../../components/MaterialList';
import Can from '../../components/Can';

const { TabPane } = Tabs;

const MaterialPage = () => (
    <div style={{ padding: 24 }}>
        <Card title="素材库" bordered={false}>
            <Tabs defaultActiveKey="create">
                <Can perform="workspace:write">
                    <TabPane tab="创建素材" key="create">
                        <MaterialCreateForm />
                    </TabPane>
                </Can>
                <Can perform="workspace:write">
                    <TabPane tab="上传文件" key="upload">
                        <MaterialUpload />
                    </TabPane>
                </Can>
                <Can perform="workspace:write">
                    <TabPane tab="素材审核" key="review">
                        <ReviewDashboard />
                    </TabPane>
                </Can>
                <TabPane tab="素材列表" key="list">
                    <MaterialList />
                </TabPane>
            </Tabs>
        </Card>
    </div>
);

export default MaterialPage;
