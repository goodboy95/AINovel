import { Modal, Form, Input, Select, Row, Col, Divider } from 'antd';
import { useEffect, useState } from 'react';
import type { Outline } from '../../types';
import OutlineTreeView from '../OutlineTreeView'; // Assuming this component is reusable

const { Option } = Select;

interface EditOutlineModalProps {
    open: boolean;
    onOk: (outline: Outline) => void;
    onCancel: () => void;
    confirmLoading: boolean;
    outline: Outline | null;
}

const EditOutlineModal = ({ open, onOk, onCancel, confirmLoading, outline }: EditOutlineModalProps) => {
    const [editableOutline, setEditableOutline] = useState<Outline | null>(outline);

    useEffect(() => {
        setEditableOutline(outline);
    }, [outline]);

    const handleOk = () => {
        if (editableOutline) {
            onOk(editableOutline);
        }
    };

    if (!editableOutline) return null;

    return (
        <Modal
            title="编辑大纲"
            open={open}
            onOk={handleOk}
            onCancel={onCancel}
            confirmLoading={confirmLoading}
            width="80vw"
            style={{ top: 20 }}
            destroyOnHidden
        >
            <Form layout="vertical">
                <Row gutter={16}>
                    <Col span={12}>
                        <Form.Item label="大纲标题">
                            <Input 
                                value={editableOutline.title} 
                                onChange={e => setEditableOutline({...editableOutline, title: e.target.value})} 
                            />
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item label="叙事视角">
                            <Select 
                                value={editableOutline.pointOfView} 
                                onChange={value => setEditableOutline({...editableOutline, pointOfView: value})}
                            >
                                <Option value="第一人称">第一人称</Option>
                                <Option value="第三人称">第三人称</Option>
                            </Select>
                        </Form.Item>
                    </Col>
                </Row>
                <Divider>内容编辑</Divider>
                <OutlineTreeView
                    outline={editableOutline}
                    onNodeSelect={() => {}}
                />
            </Form>
        </Modal>
    );
};

export default EditOutlineModal;
