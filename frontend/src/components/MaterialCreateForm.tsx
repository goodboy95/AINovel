import { useState } from 'react';
import { Form, Input, Select, Button, message, Space } from 'antd';
import { createMaterial } from '../services/api';
import type { MaterialPayload } from '../types';

const { TextArea } = Input;

const MATERIAL_TYPES = [
    { label: '手动录入', value: 'manual' },
    { label: '设定资料', value: 'lore' },
    { label: '灵感碎片', value: 'idea' },
    { label: '上传文件', value: 'file' },
];

const MaterialCreateForm = () => {
    const [form] = Form.useForm<MaterialPayload>();
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (values: MaterialPayload) => {
        setIsSubmitting(true);
        try {
            await createMaterial({
                title: values.title.trim(),
                type: values.type || 'manual',
                summary: values.summary?.trim() || undefined,
                content: values.content,
                tags: values.tags?.trim() || undefined,
            });
            message.success('素材创建成功');
            window.dispatchEvent(new CustomEvent('material:refresh'));
            form.resetFields(['title', 'summary', 'content', 'tags']);
        } catch (error) {
            const msg = error instanceof Error ? error.message : '创建素材失败';
            message.error(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Form
            layout="vertical"
            form={form}
            initialValues={{ type: 'manual' }}
            onFinish={handleSubmit}
        >
            <Form.Item
                label="标题"
                name="title"
                rules={[{ required: true, message: '请输入素材标题' }]}
            >
                <Input placeholder="请输入素材标题" />
            </Form.Item>
            <Form.Item label="类型" name="type">
                <Select options={MATERIAL_TYPES} />
            </Form.Item>
            <Form.Item label="概要" name="summary">
                <TextArea rows={3} placeholder="简要描述素材内容，可选" />
            </Form.Item>
            <Form.Item
                label="正文内容"
                name="content"
                rules={[{ required: true, message: '请输入素材正文内容' }]}
            >
                <TextArea rows={10} placeholder="支持 Markdown 或纯文本" />
            </Form.Item>
            <Form.Item label="标签" name="tags" tooltip="使用英文逗号分隔多个标签">
                <Input placeholder="例如：主角, 转折, 战斗场景" />
            </Form.Item>
            <Form.Item>
                <Space>
                    <Button type="primary" htmlType="submit" loading={isSubmitting}>
                        创建素材
                    </Button>
                    <Button htmlType="button" onClick={() => form.resetFields()} disabled={isSubmitting}>
                        重置
                    </Button>
                </Space>
            </Form.Item>
        </Form>
    );
};

export default MaterialCreateForm;
