import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate, Outlet } from "react-router-dom";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";

// Layouts
import AppLayout from "@/components/layout/AppLayout";
import AdminLayout from "@/components/layout/AdminLayout";

// Pages
import Index from "./pages/Index";
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import NotFound from "./pages/NotFound";
import Workbench from "./pages/Workbench/Workbench";
import WorldBuilderPage from "./pages/WorldBuilder/WorldBuilderPage";
import MaterialPage from "./pages/Material/MaterialPage";
import Settings from "./pages/Settings/Settings";
import PromptHelpPage from "./pages/Settings/PromptHelpPage";
import WorldPromptHelpPage from "./pages/Settings/WorldPromptHelpPage";
import ProfilePage from "./pages/Profile/ProfilePage";

// Admin Pages
import Dashboard from "./pages/Admin/Dashboard";
import ModelManager from "./pages/Admin/ModelManager";
import UserManager from "./pages/Admin/UserManager";
import CreditLogs from "./pages/Admin/CreditLogs";
import SystemSettingsPage from "./pages/Admin/SystemSettings";
import RedeemCodes from "./pages/Admin/RedeemCodes";
import EmailSettings from "./pages/Admin/EmailSettings";

// Placeholder Pages
const NovelManager = () => <div className="p-4"><h1>小说管理 (开发中)</h1></div>;

const queryClient = new QueryClient();

// Protected Route Wrapper
const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return <div className="flex items-center justify-center h-screen">Loading...</div>;
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

// Admin Route Wrapper
const AdminRoute = () => {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();
  if (isLoading) return <div className="flex items-center justify-center h-screen bg-zinc-950 text-zinc-500">Loading...</div>;
  return isAuthenticated && isAdmin ? <Outlet /> : <Navigate to="/workbench" replace />;
};

const App = () => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <TooltipProvider>
        <Toaster />
        <Sonner />
        <BrowserRouter>
          <Routes>
            {/* Public Routes */}
            <Route path="/" element={<Index />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* User Protected Routes */}
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/workbench" element={<Workbench />} />
                <Route path="/novels" element={<NovelManager />} />
                <Route path="/worlds" element={<WorldBuilderPage />} />
                <Route path="/materials" element={<MaterialPage />} />
                <Route path="/settings" element={<Settings />} />
                <Route path="/settings/prompt-guide" element={<PromptHelpPage />} />
                <Route path="/settings/world-prompts/help" element={<WorldPromptHelpPage />} />
                <Route path="/profile" element={<ProfilePage />} />
              </Route>
            </Route>

            {/* Admin Routes */}
            <Route path="/admin" element={<AdminRoute />}>
              <Route element={<AdminLayout />}>
                <Route index element={<Navigate to="/admin/dashboard" replace />} />
                <Route path="dashboard" element={<Dashboard />} />
                <Route path="models" element={<ModelManager />} />
                <Route path="users" element={<UserManager />} />
                <Route path="logs" element={<CreditLogs />} />
                <Route path="codes" element={<RedeemCodes />} />
                <Route path="email" element={<EmailSettings />} />
                <Route path="settings" element={<SystemSettingsPage />} />
              </Route>
            </Route>

            {/* Catch-all */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </TooltipProvider>
    </AuthProvider>
  </QueryClientProvider>
);

export default App;
