import { useEffect, useMemo, useRef, useState } from 'react';
import { Button, Card, Descriptions, message, Space, Typography, Upload, Alert } from 'antd';
import type { UploadProps } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { fetchMaterialUploadStatus, uploadMaterialFile } from '../services/api';
import type { FileImportJob } from '../types';

const { Paragraph, Text } = Typography;
const { Dragger } = Upload;

const MaterialUpload = () => {
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [isUploading, setIsUploading] = useState(false);
    const [job, setJob] = useState<FileImportJob | null>(null);
    const lastStatusRef = useRef<string | null>(null);

    const polling = useMemo(() => {
        if (!job) return null;
        const inProgress = job.status === 'PROCESSING' || job.status === 'PENDING';
        if (!inProgress) return null;
        return job.id;
    }, [job]);

    useEffect(() => {
        if (!polling) return;
        const timer = window.setInterval(async () => {
            try {
                const latest = await fetchMaterialUploadStatus(polling);
                setJob(latest);
                if (latest.status !== 'PROCESSING' && latest.status !== 'PENDING') {
                    window.clearInterval(timer);
                }
            } catch (error) {
                window.clearInterval(timer);
                const msg = error instanceof Error ? error.message : '查询任务状态失败';
                message.error(msg);
            }
        }, 2000);

        return () => window.clearInterval(timer);
    }, [polling]);

    useEffect(() => {
        if (job && job.status === 'COMPLETED' && lastStatusRef.current !== 'COMPLETED') {
            window.dispatchEvent(new CustomEvent('material:refresh'));
        }
        lastStatusRef.current = job ? job.status : null;
    }, [job]);

    const uploadProps: UploadProps = {
        multiple: false,
        maxCount: 1,
        accept: '.txt',
        beforeUpload: (file) => {
            setSelectedFile(file);
            return false;
        },
        onRemove: () => {
            setSelectedFile(null);
        },
        fileList: selectedFile ? [{
            uid: 'material-upload',
            name: selectedFile.name,
            status: 'done',
        }] : [],
    };

    const handleUpload = async () => {
        if (!selectedFile) {
            message.warning('请先选择要上传的 TXT 文件');
            return;
        }
        setIsUploading(true);
        try {
            const jobInfo = await uploadMaterialFile(selectedFile);
            setJob(jobInfo);
            message.success('上传成功，正在后台解析');
        } catch (error) {
            const msg = error instanceof Error ? error.message : '上传失败';
            message.error(msg);
        } finally {
            setIsUploading(false);
        }
    };

    const renderJobStatus = () => {
        if (!job) return null;
        const statusType = job.status === 'FAILED' ? 'error' : job.status === 'COMPLETED' ? 'success' : 'info';
        return (
            <Alert
                type={statusType}
                message={`当前状态：${job.status}`}
                description={job.errorMessage || (job.status === 'COMPLETED' ? '素材已入库，可在素材检索面板中使用。' : '文件正在解析中，请稍候...')}
                showIcon
            />
        );
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Paragraph>
                上传纯文本（.txt）素材文件，系统会自动提取文本、切片并写入向量库，后续可在写作时检索调用。
            </Paragraph>
            <Dragger {...uploadProps} style={{ padding: 16 }}>
                <p className="ant-upload-drag-icon">
                    <InboxOutlined />
                </p>
                <p className="ant-upload-text">点击或拖拽 TXT 文件到此区域上传</p>
                <p className="ant-upload-hint">
                    上传后系统会自动执行提取与向量化任务，仅支持纯文本文件。
                </p>
            </Dragger>
            <Space>
                <Button type="primary" onClick={handleUpload} loading={isUploading} disabled={!selectedFile}>
                    开始上传
                </Button>
                {selectedFile && <Text type="secondary">已选择文件：{selectedFile.name}</Text>}
            </Space>
            {renderJobStatus()}
            {job && (
                <Card size="small" title="最近一次上传详情">
                    <Descriptions column={1} size="small">
                        <Descriptions.Item label="任务 ID">{job.id}</Descriptions.Item>
                        <Descriptions.Item label="文件名">{job.fileName}</Descriptions.Item>
                        <Descriptions.Item label="创建时间">{job.createdAt}</Descriptions.Item>
                        {job.finishedAt && (
                            <Descriptions.Item label="完成时间">{job.finishedAt}</Descriptions.Item>
                        )}
                    </Descriptions>
                </Card>
            )}
        </Space>
    );
};

export default MaterialUpload;
