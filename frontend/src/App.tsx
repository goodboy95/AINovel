import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Register from './components/Register';
import Workbench from './components/Workbench';
import HomePage from './components/HomePage';
import ProtectedRoute from './components/ProtectedRoute';
import Settings from './components/Settings';
import PromptHelpPage from './components/PromptHelpPage';
import WorldBuilderPage from './pages/WorldBuilder/WorldBuilderPage';
import { AuthProvider } from './contexts/AuthContext';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/workbench"
            element={
              <ProtectedRoute>
                <Workbench />
              </ProtectedRoute>
            }
          />
          <Route
            path="/workbench/:tab"
            element={
              <ProtectedRoute>
                <Workbench />
              </ProtectedRoute>
            }
          />
          <Route path="/workbench/outline-design" element={<Navigate to="/workbench/outline-workspace" replace />} />
          <Route path="/workbench/outline-management" element={<Navigate to="/workbench/outline-workspace" replace />} />
          <Route path="/story-conception" element={<Navigate to="/workbench/story-conception" replace />} />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <Settings />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings/prompt-guide"
            element={
              <ProtectedRoute>
                <PromptHelpPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/worlds"
            element={
              <ProtectedRoute>
                <WorldBuilderPage />
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </Router>
  )
}

export default App
