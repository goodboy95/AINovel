import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Select, Tag, Typography } from 'antd';
import type { SelectProps } from 'antd';
import { fetchWorlds } from '../services/api';
import type { WorldStatus, WorldSummary } from '../types';

const { Text } = Typography;

type OptionType = {
  value: number;
  label: React.ReactNode;
  world: WorldSummary;
};

export interface WorldSelectProps {
  value?: number | null;
  onChange?: (worldId: number | undefined, world?: WorldSummary) => void;
  onWorldResolved?: (world?: WorldSummary) => void;
  allowClear?: boolean;
  placeholder?: string;
  size?: SelectProps['size'];
  style?: React.CSSProperties;
  disabled?: boolean;
  className?: string;
}

const WorldSelect: React.FC<WorldSelectProps> = ({
  value,
  onChange,
  onWorldResolved,
  allowClear = true,
  placeholder = '选择世界',
  size = 'middle',
  style,
  disabled,
  className,
}) => {
  const [worlds, setWorlds] = useState<WorldSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const list = await fetchWorlds('active');
        if (mounted) {
          setWorlds(list ?? []);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : '获取世界列表失败');
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      mounted = false;
    };
  }, []);

  const options: OptionType[] = useMemo(() => {
    return (worlds || []).map((world) => ({
      value: world.id,
      world,
      label: (
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <Text strong ellipsis style={{ maxWidth: 240 }}>
            {world.name}
          </Text>
          <Text type="secondary" ellipsis style={{ maxWidth: 240 }}>
            {world.tagline}
          </Text>
          {world.themes && world.themes.length > 0 && (
            <div style={{ marginTop: 4 }}>
              {world.themes.slice(0, 3).map((theme) => (
                <Tag key={theme} color="blue" style={{ marginBottom: 4 }}>
                  {theme}
                </Tag>
              ))}
              {world.themes.length > 3 && <Tag style={{ marginBottom: 4 }}>+{world.themes.length - 3}</Tag>}
            </div>
          )}
        </div>
      ),
    }));
  }, [worlds]);

  const selectedWorld = value == null ? undefined : worlds.find((w) => w.id === value);

  const mergedOptions: OptionType[] = useMemo(() => {
    if (value == null || selectedWorld) {
      return options;
    }
    const fallbackWorld: WorldSummary = {
      id: value,
      name: `世界 #${value}`,
      tagline: '当前世界不可用或尚未发布',
      themes: [],
      status: 'DRAFT' as WorldStatus,
      version: null,
      updatedAt: null,
      publishedAt: null,
      moduleProgress: {},
    };
    return [
      ...options,
      {
        value,
        world: fallbackWorld,
        label: (
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <Text strong ellipsis style={{ maxWidth: 240 }}>
              {fallbackWorld.name}
            </Text>
            <Text type="secondary" ellipsis style={{ maxWidth: 240 }}>
              {fallbackWorld.tagline}
            </Text>
          </div>
        ),
      },
    ];
  }, [options, selectedWorld, value]);

  const resolvedOption = useMemo(() => {
    if (value == null) {
      return undefined;
    }
    return mergedOptions.find((option) => option.value === value);
  }, [mergedOptions, value]);

  const lastResolvedSignatureRef = useRef<string | null>(null);

  useEffect(() => {
    if (!onWorldResolved) {
      return;
    }
    if (value == null) {
      if (lastResolvedSignatureRef.current !== null) {
        lastResolvedSignatureRef.current = null;
        onWorldResolved(undefined);
      }
      return;
    }
    const resolvedWorld = resolvedOption?.world;
    if (!resolvedWorld) {
      return;
    }
    const signature = [
      resolvedWorld.id,
      resolvedWorld.name ?? '',
      resolvedWorld.tagline ?? '',
      (resolvedWorld.themes ?? []).join('|'),
    ].join('::');
    if (lastResolvedSignatureRef.current !== signature) {
      lastResolvedSignatureRef.current = signature;
      onWorldResolved(resolvedWorld);
    }
  }, [value, resolvedOption, onWorldResolved]);

  const handleChange: SelectProps<number>['onChange'] = (nextValue, option) => {
    if (Array.isArray(option)) {
      return;
    }
    const world = option ? (option as OptionType).world : undefined;
    const normalized = typeof nextValue === 'number' ? nextValue : undefined;
    onChange?.(normalized, world);
  };

  const filterOption: SelectProps<number>['filterOption'] = (input, option) => {
    if (!option) return false;
    const data = option as unknown as OptionType;
    const haystack = [
      data.world.name ?? '',
      data.world.tagline ?? '',
      ...(data.world.themes ?? []),
    ]
      .join(' ')
      .toLowerCase();
    return haystack.includes(input.toLowerCase());
  };

  return (
    <Select<number>
      className={className}
      showSearch
      allowClear={allowClear}
      placeholder={placeholder}
      size={size}
      style={style}
      disabled={disabled}
      value={value == null ? undefined : value}
      loading={loading}
      options={mergedOptions}
      onChange={handleChange}
      onClear={() => onChange?.(undefined, undefined)}
      filterOption={filterOption}
      optionLabelProp="label"
      notFoundContent={loading ? '加载中...' : error || '暂无可用世界'}
    />
  );
};

export default WorldSelect;
