import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Empty, Layout, Modal, Space, Spin, Typography, message } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import WorldSelectorPanel from './components/WorldSelectorPanel';
import MetadataForm from './components/MetadataForm';
import type { MetadataFormValue } from './components/MetadataForm';
import ModuleTabs from './components/ModuleTabs';
import StickyFooterBar from './components/StickyFooterBar';
import type { SaveState } from './components/StickyFooterBar';
import GenerationProgressModal from './components/GenerationProgressModal';
import RefineModal from '../../components/modals/RefineModal';
import {
    createWorld,
    deleteWorld,
    fetchWorldBuildingDefinitions,
    fetchWorldDetail,
    fetchWorldGenerationStatus,
    fetchWorlds,
    generateWorldModule,
    previewWorldPublish,
    publishWorld,
    refineWorldField,
    runWorldGenerationModule,
    retryWorldGeneration,
    updateWorld,
    updateWorldModules,
    type WorldUpsertPayload,
} from '../../services/api';
import type {
    WorldBuildingDefinitionsResponse,
    WorldDetail,
    WorldGenerationStatus,
    WorldModule,
    WorldModuleDefinition,
    WorldSummary,
} from '../../types';

const { Content } = Layout;
const { Title, Paragraph } = Typography;

const AUTO_SAVE_DELAY = 2000;

const cloneWorldDetail = (detail: WorldDetail): WorldDetail => {
    return JSON.parse(JSON.stringify(detail)) as WorldDetail;
};

const mergeModules = (current: WorldModule[], updated: WorldModule[]): WorldModule[] => {
    if (!updated.length) return current;
    const updatedMap = new Map(updated.map(module => [module.key, module]));
    const merged = current.map(module => updatedMap.get(module.key) ?? module);
    updated.forEach(module => {
        if (!current.find(item => item.key === module.key)) {
            merged.push(module);
        }
    });
    return merged;
};

const initialCreativeIntent = '（占位文案）请在此处写下你的创作意图、灵感来源、预期主题以及需要遵守的世界观边界。建议至少 150 字，说明故事想聚焦的冲突、角色成长方向和需要避免的情节。该文本仅为占位，请尽快替换为真实内容，以帮助 AI 在生成时保持一致性和合理性。同时补充时代背景、关键势力、禁忌设定与长期发展目标，让生成的内容更贴近你的设计。';

const buildBasicInfoPayload = (detail: WorldDetail): WorldUpsertPayload => ({
    name: detail.world.name,
    tagline: detail.world.tagline,
    themes: detail.world.themes ?? [],
    creativeIntent: detail.world.creativeIntent,
    notes: detail.world.notes ?? '',
});

const WorldBuilderPage: React.FC = () => {
    const [definitions, setDefinitions] = useState<WorldBuildingDefinitionsResponse | null>(null);
    const [loadingDefinitions, setLoadingDefinitions] = useState(true);
    const [worlds, setWorlds] = useState<WorldSummary[]>([]);
    const [loadingWorlds, setLoadingWorlds] = useState(false);
    const [selectedWorldId, setSelectedWorldId] = useState<number | null>(null);
    const [worldDetail, setWorldDetail] = useState<WorldDetail | null>(null);
    const [baselineDetail, setBaselineDetail] = useState<WorldDetail | null>(null);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [dirtyBasicInfo, setDirtyBasicInfo] = useState<Partial<MetadataFormValue>>({});
    const [dirtyModules, setDirtyModules] = useState<Record<string, Record<string, string>>>({});
    const [saveState, setSaveState] = useState<SaveState>('saved');
    const autoSaveTimer = useRef<number | undefined>(undefined);
    const [creating, setCreating] = useState(false);
    const [moduleLoadingKeys, setModuleLoadingKeys] = useState<Set<string>>(new Set());
    const [publishing, setPublishing] = useState(false);
    const [progressVisible, setProgressVisible] = useState(false);
    const [progressStatus, setProgressStatus] = useState<WorldGenerationStatus | null>(null);
    const [progressLoading, setProgressLoading] = useState(false);
    const [progressWorldName, setProgressWorldName] = useState('');
    const generationWorldRef = useRef<number | null>(null);
    const generationRunningRef = useRef(false);
    const [refineState, setRefineState] = useState<{
        moduleKey: string;
        fieldKey: string;
        moduleLabel: string;
        fieldLabel: string;
        text: string;
    } | null>(null);
    const hasDirtyBasicInfo = useMemo(() => Object.keys(dirtyBasicInfo).length > 0, [dirtyBasicInfo]);
    const hasDirtyModules = useMemo(() => Object.keys(dirtyModules).length > 0, [dirtyModules]);
    const hasUnsavedChanges = hasDirtyBasicInfo || hasDirtyModules;
    const isEditingDisabled = worldDetail?.world.status === 'GENERATING';

    const draftWorlds = useMemo(() => worlds.filter(world => world.status === 'DRAFT'), [worlds]);
    const publishedWorlds = useMemo(() => worlds.filter(world => world.status !== 'DRAFT'), [worlds]);
    const selectedWorldSummary = useMemo(
        () => (selectedWorldId ? worlds.find(world => world.id === selectedWorldId) : undefined),
        [selectedWorldId, worlds]
    );

    const clearAutoSaveTimer = useCallback(() => {
        if (autoSaveTimer.current) {
            window.clearTimeout(autoSaveTimer.current);
            autoSaveTimer.current = undefined;
        }
    }, []);

    const stopGeneration = useCallback(() => {
        generationWorldRef.current = null;
        generationRunningRef.current = false;
        setProgressLoading(false);
    }, []);

    const resolveWorldName = useCallback((worldId: number) => {
        if (worldDetail?.world.id === worldId) {
            return worldDetail.world.name ?? '';
        }
        const summary = worlds.find(world => world.id === worldId);
        return summary?.name ?? '';
    }, [worldDetail, worlds]);

    const loadWorldSummaries = useCallback(async () => {
        setLoadingWorlds(true);
        try {
            const data = await fetchWorlds();
            setWorlds(data);
            if (selectedWorldId != null && !data.some(world => world.id === selectedWorldId)) {
                setSelectedWorldId(data.length ? data[0].id : null);
            }
        } catch (err) {
            message.error(err instanceof Error ? err.message : '加载世界列表失败');
        } finally {
            setLoadingWorlds(false);
        }
    }, [selectedWorldId]);

    const handleGenerationCompletion = useCallback(async (worldId: number) => {
        stopGeneration();
        setProgressStatus(null);
        setProgressVisible(false);
        setProgressWorldName('');
        message.success('世界完整信息生成完成');
        await loadWorldSummaries();
        try {
            const detail = await fetchWorldDetail(worldId);
            setWorldDetail(detail);
            setBaselineDetail(cloneWorldDetail(detail));
            setDirtyBasicInfo({});
            setDirtyModules({});
            setSaveState('saved');
        } catch (err) {
            message.error(err instanceof Error ? err.message : '刷新世界详情失败');
        }
    }, [loadWorldSummaries, stopGeneration]);

    const runGenerationPipeline = useCallback(async (worldId: number, initialStatus?: WorldGenerationStatus) => {
        if (generationRunningRef.current) {
            return;
        }
        generationRunningRef.current = true;
        try {
            let status = initialStatus;
            if (!status) {
                status = await fetchWorldGenerationStatus(worldId);
                if (generationWorldRef.current !== worldId) {
                    return;
                }
                setProgressStatus(status);
            }
            while (generationWorldRef.current === worldId) {
                const queue = status?.queue ?? [];
                const nextJob = queue.find(job => job.status === 'WAITING');
                if (!nextJob) {
                    if (status && status.status !== 'GENERATING') {
                        await handleGenerationCompletion(worldId);
                    }
                    break;
                }
                setProgressStatus(prev => {
                    if (!prev) return prev;
                    return {
                        ...prev,
                        queue: prev.queue.map(job => job.moduleKey === nextJob.moduleKey
                            ? { ...job, status: 'RUNNING' }
                            : job),
                    };
                });
                try {
                    const updated = await runWorldGenerationModule(worldId, nextJob.moduleKey);
                    if (generationWorldRef.current !== worldId) {
                        break;
                    }
                    setProgressStatus(updated);
                    status = updated;
                    if (updated.status !== 'GENERATING') {
                        await handleGenerationCompletion(worldId);
                        break;
                    }
                } catch (err) {
                    if (generationWorldRef.current === worldId) {
                        message.error(err instanceof Error ? err.message : '生成模块失败');
                        try {
                            const fallback = await fetchWorldGenerationStatus(worldId);
                            if (generationWorldRef.current === worldId) {
                                setProgressStatus(fallback);
                                status = fallback;
                            }
                        } catch {
                            // ignore secondary failure
                        }
                    }
                    break;
                }
            }
        } finally {
            generationRunningRef.current = false;
        }
    }, [handleGenerationCompletion]);

    const startGeneration = useCallback(async (
        worldId: number,
        options?: { initialStatus?: WorldGenerationStatus; force?: boolean; worldName?: string }
    ) => {
        const { initialStatus, force = false, worldName } = options ?? {};
        const resolvedName = worldName ?? resolveWorldName(worldId);
        if (resolvedName) {
            setProgressWorldName(resolvedName);
        }
        setProgressVisible(true);
        if (!force && generationWorldRef.current === worldId) {
            if (initialStatus) {
                setProgressStatus(initialStatus);
                if (initialStatus.status === 'GENERATING') {
                    await runGenerationPipeline(worldId, initialStatus);
                } else {
                    await handleGenerationCompletion(worldId);
                }
            }
            return;
        }
        generationWorldRef.current = worldId;
        if (initialStatus) {
            setProgressStatus(initialStatus);
            setProgressLoading(false);
        } else {
            setProgressLoading(true);
        }
        try {
            const status = initialStatus ?? await fetchWorldGenerationStatus(worldId);
            if (generationWorldRef.current !== worldId) {
                return;
            }
            setProgressStatus(status);
            setProgressLoading(false);
            if (status.status !== 'GENERATING') {
                await handleGenerationCompletion(worldId);
                return;
            }
            await runGenerationPipeline(worldId, status);
        } catch (err) {
            setProgressLoading(false);
            generationWorldRef.current = null;
            generationRunningRef.current = false;
            message.error(err instanceof Error ? err.message : '获取生成进度失败');
        }
    }, [handleGenerationCompletion, resolveWorldName, runGenerationPipeline]);

    const loadWorldDetail = useCallback(async (worldId: number) => {
        setLoadingDetail(true);
        try {
            const detail = await fetchWorldDetail(worldId);
            setWorldDetail(detail);
            setBaselineDetail(cloneWorldDetail(detail));
            setDirtyBasicInfo({});
            setDirtyModules({});
            setSaveState('saved');
            if (detail.world.status === 'GENERATING') {
                void startGeneration(worldId, {
                    force: true,
                    worldName: detail.world.name ?? undefined,
                });
            } else if (generationWorldRef.current === worldId) {
                stopGeneration();
                setProgressVisible(false);
                setProgressStatus(null);
                setProgressWorldName('');
            }
        } catch (err) {
            message.error(err instanceof Error ? err.message : '加载世界详情失败');
        } finally {
            setLoadingDetail(false);
        }
    }, [startGeneration, stopGeneration]);

    useEffect(() => {
        const fetchDefinitions = async () => {
            setLoadingDefinitions(true);
            try {
                const data = await fetchWorldBuildingDefinitions();
                setDefinitions(data);
            } catch (err) {
                message.error(err instanceof Error ? err.message : '加载模块定义失败');
            } finally {
                setLoadingDefinitions(false);
            }
        };
        fetchDefinitions();
    }, []);

    useEffect(() => {
        loadWorldSummaries();
    }, [loadWorldSummaries]);

    useEffect(() => {
        if (selectedWorldId == null) {
            setWorldDetail(null);
            setBaselineDetail(null);
            return;
        }
        loadWorldDetail(selectedWorldId);
    }, [selectedWorldId, loadWorldDetail]);

    useEffect(() => {
        return () => {
            clearAutoSaveTimer();
            stopGeneration();
        };
    }, [clearAutoSaveTimer, stopGeneration]);

    useEffect(() => {
        const handleBeforeUnload = (event: BeforeUnloadEvent) => {
            if (hasUnsavedChanges || saveState === 'saving') {
                event.preventDefault();
                event.returnValue = '';
            }
        };
        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [hasUnsavedChanges, saveState]);

    const saveDraft = useCallback(async ({ silent = false }: { silent?: boolean } = {}): Promise<boolean> => {
        if (!worldDetail || !baselineDetail) {
            return true;
        }
        if (!hasUnsavedChanges) {
            return true;
        }
        clearAutoSaveTimer();
        setSaveState('saving');
        try {
            let updatedDetail = worldDetail;
            if (hasDirtyBasicInfo) {
                const payload = buildBasicInfoPayload(updatedDetail);
                const response = await updateWorld(updatedDetail.world.id!, payload);
                updatedDetail = { ...updatedDetail, world: response.world };
                setWorldDetail(prev => (prev ? { ...prev, world: response.world } : response));
                setBaselineDetail(prev => (prev ? { ...prev, world: response.world } : cloneWorldDetail(response)));
            }
            if (hasDirtyModules) {
                const modulesPayload = Object.entries(dirtyModules).map(([key, fields]) => ({ key, fields }));
                if (modulesPayload.length > 0) {
                    const modules = await updateWorldModules(updatedDetail.world.id!, modulesPayload);
                    const merged = mergeModules(updatedDetail.modules, modules);
                    updatedDetail = { ...updatedDetail, modules: merged };
                    setWorldDetail(updatedDetail);
                    setBaselineDetail(prev => (prev ? { ...prev, modules: merged } : cloneWorldDetail(updatedDetail)));
                }
            }
            setDirtyBasicInfo({});
            setDirtyModules({});
            setSaveState('saved');
            if (!silent) {
                message.success('草稿已保存');
            }
            await loadWorldSummaries();
            return true;
        } catch (err) {
            const msg = err instanceof Error ? err.message : '保存失败';
            if (!silent) {
                message.error(msg);
            }
            setSaveState('error');
            return false;
        }
    }, [baselineDetail, dirtyModules, hasDirtyBasicInfo, hasDirtyModules, hasUnsavedChanges, loadWorldSummaries, worldDetail, clearAutoSaveTimer]);

    const scheduleAutoSave = useCallback(() => {
        clearAutoSaveTimer();
        autoSaveTimer.current = window.setTimeout(() => {
            void saveDraft({ silent: true });
        }, AUTO_SAVE_DELAY);
    }, [clearAutoSaveTimer, saveDraft]);

    const handleMetadataChange = useCallback(<K extends keyof MetadataFormValue>(field: K, value: MetadataFormValue[K]) => {
        if (!worldDetail || !baselineDetail) return;
        setWorldDetail(prev => (prev ? { ...prev, world: { ...prev.world, [field]: value } } : prev));
        setDirtyBasicInfo(prev => {
            const next = { ...prev };
            const baselineValue = baselineDetail.world[field];
            const isEqual = Array.isArray(value)
                ? JSON.stringify(value) === JSON.stringify(baselineValue ?? [])
                : (baselineValue ?? '') === value;
            if (isEqual) {
                delete (next as Record<string, unknown>)[field as string];
            } else {
                (next as Record<string, unknown>)[field as string] = value as unknown as string;
            }
            return next;
        });
        setSaveState('pending');
        scheduleAutoSave();
    }, [baselineDetail, scheduleAutoSave, worldDetail]);

    const handleFieldChange = useCallback((moduleKey: string, fieldKey: string, value: string) => {
        if (!worldDetail || !baselineDetail) return;
        setWorldDetail(prev => {
            if (!prev) return prev;
            const modules = prev.modules.map(module =>
                module.key === moduleKey
                    ? { ...module, fields: { ...module.fields, [fieldKey]: value } }
                    : module
            );
            return { ...prev, modules };
        });
        setDirtyModules(prev => {
            const next = { ...prev };
            const baselineModule = baselineDetail.modules.find(module => module.key === moduleKey);
            const baselineValue = baselineModule?.fields?.[fieldKey] ?? '';
            if (value === baselineValue) {
                if (next[moduleKey]) {
                    const fields = { ...next[moduleKey] };
                    delete fields[fieldKey];
                    if (Object.keys(fields).length === 0) {
                        delete next[moduleKey];
                    } else {
                        next[moduleKey] = fields;
                    }
                }
            } else {
                next[moduleKey] = { ...(next[moduleKey] ?? {}), [fieldKey]: value };
            }
            return next;
        });
        setSaveState('pending');
        scheduleAutoSave();
    }, [baselineDetail, scheduleAutoSave, worldDetail]);
    const handleOpenRefine = useCallback((moduleKey: string, fieldKey: string, fieldLabel: string, originalText: string) => {
        const moduleDefinition = definitions?.modules.find(def => def.key === moduleKey);
        setRefineState({
            moduleKey,
            fieldKey,
            fieldLabel,
            moduleLabel: moduleDefinition?.label ?? moduleKey,
            text: originalText,
        });
    }, [definitions?.modules]);

    const handleApplyRefine = useCallback((newText: string) => {
        if (!refineState) return;
        handleFieldChange(refineState.moduleKey, refineState.fieldKey, newText);
        setRefineState(null);
    }, [handleFieldChange, refineState]);

    const handleRefineRequest = useCallback(async (text: string, instruction: string) => {
        if (!worldDetail || !refineState) return text;
        try {
            const response = await refineWorldField(worldDetail.world.id!, refineState.moduleKey, refineState.fieldKey, {
                text,
                instruction,
            });
            if (!response || typeof response.result !== 'string') {
                throw new Error('AI 返回结果为空，请稍后重试');
            }
            return response.result;
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : '字段优化失败，请稍后重试';
            message.error(errorMessage);
            throw (error instanceof Error ? error : new Error(errorMessage));
        }
    }, [refineState, worldDetail]);

    const handleGenerateModule = useCallback(async (moduleKey: string) => {
        if (!worldDetail) return;
        setModuleLoadingKeys(prev => new Set(prev).add(moduleKey));
        try {
            const module = await generateWorldModule(worldDetail.world.id!, moduleKey);
            setWorldDetail(prev => (prev ? { ...prev, modules: mergeModules(prev.modules, [module]) } : prev));
            setBaselineDetail(prev => {
                const base = prev ?? (worldDetail ? cloneWorldDetail(worldDetail) : null);
                return base ? { ...base, modules: mergeModules(base.modules, [module]) } : base;
            });
            setDirtyModules(prev => {
                if (!prev[moduleKey]) return prev;
                const next = { ...prev };
                delete next[moduleKey];
                return next;
            });
            message.success(`${module.label} 模块已生成`);
        } catch (err) {
            message.error(err instanceof Error ? err.message : '模块生成失败');
        } finally {
            setModuleLoadingKeys(prev => {
                const next = new Set(prev);
                next.delete(moduleKey);
                return next;
            });
        }
    }, [worldDetail]);

    const handleViewFullContent = useCallback((_: string, label: string, content: string) => {
        if (!content) {
            message.info('该模块尚未生成完整信息');
            return;
        }
        Modal.info({
            title: `${label} · 完整信息`,
            width: 760,
            content: (
                <Paragraph style={{ whiteSpace: 'pre-wrap' }}>
                    {content}
                </Paragraph>
            ),
        });
    }, []);
    const handleCreateWorld = useCallback(async () => {
        setCreating(true);
        try {
            const detail = await createWorld({
                name: '未命名世界',
                tagline: '请填写一句话世界概述，突出设定亮点与核心冲突。',
                themes: ['待补充主题'],
                creativeIntent: initialCreativeIntent,
                notes: '',
            });
            message.success('已创建新世界，请完善基础信息');
            setWorldDetail(detail);
            setBaselineDetail(cloneWorldDetail(detail));
            setDirtyBasicInfo({});
            setDirtyModules({});
            setSaveState('saved');
            await loadWorldSummaries();
            setSelectedWorldId(detail.world.id ?? null);
        } catch (err) {
            message.error(err instanceof Error ? err.message : '新建世界失败');
        } finally {
            setCreating(false);
        }
    }, [loadWorldSummaries]);

    const handleSelectWorld = useCallback((worldId: number) => {
        if (worldId === selectedWorldId) return;
        const proceed = () => setSelectedWorldId(worldId);
        if (hasUnsavedChanges) {
            Modal.confirm({
                title: '切换世界',
                icon: <ExclamationCircleOutlined />,
                content: '当前世界存在未保存的修改，切换前需要先保存。',
                okText: '保存并切换',
                cancelText: '取消',
                onOk: async () => {
                    const success = await saveDraft({ silent: false });
                    if (success) {
                        proceed();
                    }
                },
            });
        } else {
            proceed();
        }
    }, [hasUnsavedChanges, saveDraft, selectedWorldId]);

    const handleRenameDraft = useCallback(async (worldId: number, newName: string) => {
        try {
            const detail = worldId === worldDetail?.world.id ? worldDetail : await fetchWorldDetail(worldId);
            if (!detail) return;
            const payload = buildBasicInfoPayload(detail);
            payload.name = newName;
            const response = await updateWorld(worldId, payload);
            if (worldId === worldDetail?.world.id) {
                setWorldDetail(prev => (prev ? { ...prev, world: response.world } : response));
                setBaselineDetail(prev => (prev ? { ...prev, world: response.world } : cloneWorldDetail(response)));
                setDirtyBasicInfo(prev => {
                    const next = { ...prev };
                    delete next.name;
                    return next;
                });
            }
            await loadWorldSummaries();
            message.success('重命名成功');
        } catch (err) {
            message.error(err instanceof Error ? err.message : '重命名失败');
        }
    }, [loadWorldSummaries, worldDetail]);

    const handleDeleteDraft = useCallback(async (worldId: number) => {
        try {
            await deleteWorld(worldId);
            message.success('草稿已删除');
            await loadWorldSummaries();
            if (worldId === selectedWorldId) {
                setSelectedWorldId(null);
                setWorldDetail(null);
                setBaselineDetail(null);
            }
        } catch (err) {
            message.error(err instanceof Error ? err.message : '删除失败');
        }
    }, [loadWorldSummaries, selectedWorldId]);
    const handlePublish = useCallback(async () => {
        if (!worldDetail) return;
        if (worldDetail.world.status === 'GENERATING') {
            message.info('世界正在生成中，请稍后再试');
            return;
        }
        const saved = await saveDraft({ silent: false });
        if (!saved) return;
        setPublishing(true);
        try {
            const preview = await previewWorldPublish(worldDetail.world.id!);
            if (!preview.ready) {
                Modal.warning({
                    title: '仍有未完成的字段',
                    content: preview.missingFields.length ? (
                        <div>
                            {preview.missingFields.map(field => (
                                <div key={`${field.moduleKey}-${field.fieldKey}`}>
                                    {field.moduleLabel} · {field.fieldLabel}
                                </div>
                            ))}
                        </div>
                    ) : '请检查各模块状态，确保内容完整。',
                });
                return;
            }
            const response = await publishWorld(worldDetail.world.id!);
            message.success('已提交生成任务，开始生成完整信息');
            const toGenerateKeys = new Set(response.modulesToGenerate.map(module => module.key));
            setWorldDetail(prev => prev ? {
                ...prev,
                world: { ...prev.world, status: 'GENERATING' },
                modules: prev.modules.map(module =>
                    toGenerateKeys.has(module.key)
                        ? { ...module, status: 'AWAITING_GENERATION' }
                        : module
                ),
            } : prev);
            setBaselineDetail(prev => prev ? {
                ...prev,
                world: { ...prev.world, status: 'GENERATING' },
                modules: prev.modules.map(module =>
                    toGenerateKeys.has(module.key)
                        ? { ...module, status: 'AWAITING_GENERATION' }
                        : module
                ),
            } : prev);
            await loadWorldSummaries();
            void startGeneration(worldDetail.world.id!, {
                force: true,
                worldName: worldDetail.world.name ?? undefined,
            });
        } catch (err) {
            message.error(err instanceof Error ? err.message : '正式创建失败');
        } finally {
            setPublishing(false);
        }
    }, [loadWorldSummaries, saveDraft, startGeneration, worldDetail]);

    const handleRetryModule = useCallback(async (moduleKey: string) => {
        if (!worldDetail) return;
        try {
            await retryWorldGeneration(worldDetail.world.id!, moduleKey);
            message.success('已重新排队');
            const status = await fetchWorldGenerationStatus(worldDetail.world.id!);
            setProgressStatus(status);
            void startGeneration(worldDetail.world.id!, {
                initialStatus: status,
                force: true,
                worldName: worldDetail.world.name ?? undefined,
            });
        } catch (err) {
            message.error(err instanceof Error ? err.message : '重新排队失败');
        }
    }, [startGeneration, worldDetail]);
    useEffect(() => {
        if (selectedWorldId == null && worlds.length > 0) {
            setSelectedWorldId(worlds[0].id);
        }
    }, [selectedWorldId, worlds]);
    const metadataValue: MetadataFormValue | undefined = worldDetail
        ? {
            name: worldDetail.world.name ?? '',
            tagline: worldDetail.world.tagline ?? '',
            themes: worldDetail.world.themes ?? [],
            creativeIntent: worldDetail.world.creativeIntent ?? '',
            notes: worldDetail.world.notes ?? '',
        }
        : undefined;
    const moduleDefinitions: WorldModuleDefinition[] = definitions?.modules ?? [];
    const canPublish = worldDetail ? worldDetail.world.status !== 'GENERATING' && !publishing : false;

    return (
        <Layout style={{ minHeight: '100vh', background: '#f5f6fa' }}>
            <Content style={{ padding: '24px 48px 80px' }}>
                <Spin spinning={loadingDefinitions || loadingDetail} tip="加载中...">
                    <Space direction="vertical" size={16} style={{ width: '100%' }}>
                        <Title level={2}>世界构建</Title>
                        <Paragraph type="secondary">
                            管理世界观设定、调用 AI 生成模块内容，并跟踪正式创建的生成进度。
                        </Paragraph>
                        <WorldSelectorPanel
                            worlds={publishedWorlds}
                            drafts={draftWorlds}
                            selectedWorldId={selectedWorldId}
                            selectedWorld={selectedWorldSummary}
                            loading={loadingWorlds}
                            creating={creating}
                            onSelectWorld={handleSelectWorld}
                            onCreateWorld={handleCreateWorld}
                            onRenameDraft={handleRenameDraft}
                            onDeleteDraft={handleDeleteDraft}
                        />
                        {worldDetail && metadataValue ? (
                            <>
                                <MetadataForm
                                    value={metadataValue}
                                    definition={definitions?.basicInfo}
                                    disabled={isEditingDisabled}
                                    onChange={handleMetadataChange}
                                />
                                {isEditingDisabled && (
                                    <Alert
                                        type="info"
                                        showIcon
                                        message="世界正在生成完整信息"
                                        description="生成完成前暂不可编辑字段，进度将在下方实时更新。"
                                        style={{ marginBottom: 16 }}
                                    />
                                )}
                                <ModuleTabs
                                    modules={worldDetail.modules}
                                    definitions={moduleDefinitions}
                                    disabled={isEditingDisabled}
                                    generatingModules={moduleLoadingKeys}
                                    onFieldChange={handleFieldChange}
                                    onOpenRefine={handleOpenRefine}
                                    onGenerateModule={handleGenerateModule}
                                    onViewFullContent={handleViewFullContent}
                                />
                            </>
                        ) : (
                            <Empty description={loadingDetail ? '正在加载世界详情' : '请选择一个世界或新建草稿'} />
                        )}
                    </Space>
                </Spin>
            </Content>
            <StickyFooterBar
                worldStatus={worldDetail?.world.status}
                version={worldDetail?.world.version}
                saveState={saveState}
                hasUnsavedChanges={hasUnsavedChanges}
                onSave={() => { void saveDraft({ silent: false }); }}
                onPublish={handlePublish}
                savingDisabled={!worldDetail || isEditingDisabled}
                publishing={publishing}
                canPublish={canPublish}
            />
            <GenerationProgressModal
                open={progressVisible}
                status={progressStatus}
                worldName={progressWorldName || worldDetail?.world.name}
                loading={progressLoading}
                onClose={() => {
                    setProgressVisible(false);
                    stopGeneration();
                    setProgressStatus(null);
                    setProgressWorldName('');
                }}
                onRetry={handleRetryModule}
            />
            {refineState && (
                <RefineModal
                    open={!!refineState}
                    onCancel={() => setRefineState(null)}
                    originalText={refineState.text}
                    contextType={`世界构建 · ${refineState.moduleLabel} / ${refineState.fieldLabel}`}
                    onRefined={handleApplyRefine}
                    onRequest={handleRefineRequest}
                />
            )}
        </Layout>
    );
};

export default WorldBuilderPage;
