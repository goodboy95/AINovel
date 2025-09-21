import React from 'react';
import { Layout, Dropdown, Typography, Space, Button } from 'antd';
import { Link } from 'react-router-dom';
import { UserOutlined, SettingOutlined, LogoutOutlined } from '@ant-design/icons';

const { Header } = Layout;
const { Title, Text } = Typography;

type WorkbenchHeaderProps = {
  username?: string;
  onOpenSettings: () => void;
  onLogout: () => void;
};

const WorkbenchHeader: React.FC<WorkbenchHeaderProps> = ({ username, onOpenSettings, onLogout }) => {
  const items = [
    {
      key: 'settings',
      label: '设置',
      icon: <SettingOutlined />,
      onClick: onOpenSettings,
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      label: '退出',
      icon: <LogoutOutlined />,
      onClick: onLogout,
    },
  ];

  return (
    <Header
      style={{
        background: '#fff',
        padding: '0 24px',
        borderBottom: '1px solid #f0f0f0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        height: 64,
      }}
    >
      <Space align="center">
        <img src="/vite.svg" alt="AI 小说家" style={{ width: 28, height: 28 }} />
        <Title level={3} style={{ margin: 0 }}>AI 小说家工作台</Title>
        <Link to="/worlds">
          <Button type="link">世界构建</Button>
        </Link>
      </Space>

      <Dropdown menu={{ items }} trigger={['click']}>
        <Button type="text" icon={<UserOutlined />}>
          <Space>
            <Text>{username || '未登录'}</Text>
          </Space>
        </Button>
      </Dropdown>
    </Header>
  );
};

export default WorkbenchHeader;
