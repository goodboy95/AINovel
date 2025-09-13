import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from 'antd';
import { useAuth } from '../contexts/AuthContext';

const HomePage: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="flex flex-col items-center justify-center h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white">
      <h1 className="text-5xl font-extrabold mb-4">欢迎来到 AI 小说家</h1>
      <p className="text-xl text-gray-300 mb-8">您的创意写作伙伴。</p>
      <nav className="flex flex-wrap justify-center gap-4">
        {!isAuthenticated && (
          <>
            <Link to="/login">
              <Button type="primary" size="large">登录</Button>
            </Link>
            <Link to="/register">
              <Button size="large">注册</Button>
            </Link>
          </>
        )}
        {isAuthenticated && (
          <Link to="/workbench">
            <Button type="primary" size="large">进入工作台</Button>
          </Link>
        )}
      </nav>
    </div>
  );
};

export default HomePage;