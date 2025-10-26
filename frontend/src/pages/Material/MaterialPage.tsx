import { Card, Tabs, Alert } from 'antd';
import MaterialCreateForm from '../../components/MaterialCreateForm';
import MaterialUpload from '../../components/MaterialUpload';

const { TabPane } = Tabs;

const MaterialPage = () => (
    <div style={{ padding: 24 }}>
        <Card title="素材库" bordered={false}>
            <Tabs defaultActiveKey="create">
                <TabPane tab="创建素材" key="create">
                    <MaterialCreateForm />
                </TabPane>
                <TabPane tab="上传文件" key="upload">
                    <MaterialUpload />
                </TabPane>
                <TabPane tab="素材列表" key="list">
                    <Alert
                        message="素材列表功能将在后续版本中提供"
                        type="info"
                        showIcon
                    />
                </TabPane>
            </Tabs>
        </Card>
    </div>
);

export default MaterialPage;

