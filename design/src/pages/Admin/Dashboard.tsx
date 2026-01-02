import { useEffect, useState } from "react";
import { api } from "@/lib/mock-api";
import { AdminDashboardStats } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Users, Zap, AlertTriangle, FileClock } from "lucide-react";

const Dashboard = () => {
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);

  useEffect(() => {
    api.admin.getDashboardStats().then(setStats);
  }, []);

  if (!stats) return <div className="text-zinc-500">Loading stats...</div>;

  return (
    <div className="space-y-8">
      <h1 className="text-3xl font-bold">系统概览</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="bg-zinc-900 border-zinc-800 text-zinc-100">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-zinc-400">总用户数</CardTitle>
            <Users className="h-4 w-4 text-zinc-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.totalUsers}</div>
            <p className="text-xs text-zinc-500">今日新增 +{stats.todayNewUsers}</p>
          </CardContent>
        </Card>

        <Card className="bg-zinc-900 border-zinc-800 text-zinc-100">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-zinc-400">今日积分消耗</CardTitle>
            <Zap className="h-4 w-4 text-yellow-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.todayCreditsConsumed.toLocaleString()}</div>
            <p className="text-xs text-zinc-500">总消耗 {stats.totalCreditsConsumed.toLocaleString()}</p>
          </CardContent>
        </Card>

        <Card className="bg-zinc-900 border-zinc-800 text-zinc-100">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-zinc-400">API 错误率</CardTitle>
            <AlertTriangle className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{(stats.apiErrorRate * 100).toFixed(1)}%</div>
            <p className="text-xs text-zinc-500">健康状态: {stats.apiErrorRate < 0.05 ? '良好' : '警告'}</p>
          </CardContent>
        </Card>

        <Card className="bg-zinc-900 border-zinc-800 text-zinc-100">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-zinc-400">待审素材</CardTitle>
            <FileClock className="h-4 w-4 text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.pendingReviews}</div>
            <p className="text-xs text-zinc-500">需尽快处理</p>
          </CardContent>
        </Card>
      </div>

      {/* Placeholder for Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <Card className="bg-zinc-900 border-zinc-800 text-zinc-100 h-[300px]">
          <CardHeader>
            <CardTitle>流量趋势 (Mock)</CardTitle>
          </CardHeader>
          <CardContent className="flex items-center justify-center h-[200px] text-zinc-600 border-2 border-dashed border-zinc-800 rounded">
            Chart Component Here
          </CardContent>
        </Card>
        <Card className="bg-zinc-900 border-zinc-800 text-zinc-100 h-[300px]">
          <CardHeader>
            <CardTitle>模型调用分布 (Mock)</CardTitle>
          </CardHeader>
          <CardContent className="flex items-center justify-center h-[200px] text-zinc-600 border-2 border-dashed border-zinc-800 rounded">
            Pie Chart Here
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Dashboard;