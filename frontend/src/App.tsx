import { BrowserRouter as Router, Route, Routes, Link, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Register from './components/Register';
import Settings from './components/Settings';
import Workbench from './components/Workbench';

function Home() {
  return (
    <div className="flex flex-col items-center justify-center h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white">
      <h1 className="text-5xl font-extrabold mb-4">欢迎来到 AI 小说家</h1>
      <p className="text-xl text-gray-300 mb-8">您的创意写作伙伴。</p>
      <nav className="flex flex-wrap justify-center gap-4">
        <Link to="/login" className="px-6 py-3 font-bold text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-all">登录</Link>
        <Link to="/register" className="px-6 py-3 font-bold text-white bg-green-600 rounded-lg hover:bg-green-700 transition-all">注册</Link>
        <Link to="/settings" className="px-6 py-3 font-bold text-white bg-gray-600 rounded-lg hover:bg-gray-700 transition-all">设置</Link>
        <Link to="/workbench" className="px-6 py-3 font-bold text-white bg-purple-600 rounded-lg hover:bg-purple-700 transition-all">进入工作台</Link>
      </nav>
    </div>
  );
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="/workbench" element={<Workbench />} />
        <Route path="/workbench/:tab" element={<Workbench />} />
        <Route path="/story-conception" element={<Navigate to="/workbench/story-conception" replace />} />
      </Routes>
    </Router>
  )
}

export default App
