import { useEffect, useState } from "react";
import { api } from "@/lib/mock-api";
import { CreditLog } from "@/types";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { format } from "date-fns";

const CreditLogs = () => {
  const [logs, setLogs] = useState<CreditLog[]>([]);

  useEffect(() => {
    api.admin.getLogs().then(setLogs);
  }, []);

  const getReasonBadge = (reason: string) => {
    switch (reason) {
      case 'generation': return <Badge variant="outline" className="border-blue-900 text-blue-400 bg-blue-900/10">AI 生成</Badge>;
      case 'check_in': return <Badge variant="outline" className="border-yellow-900 text-yellow-400 bg-yellow-900/10">签到奖励</Badge>;
      case 'redeem': return <Badge variant="outline" className="border-green-900 text-green-400 bg-green-900/10">兑换码</Badge>;
      case 'admin_grant': return <Badge variant="outline" className="border-purple-900 text-purple-400 bg-purple-900/10">管理员调整</Badge>;
      default: return <Badge variant="outline">{reason}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">积分流水日志</h1>

      <div className="border border-zinc-800 rounded-md bg-zinc-900">
        <Table>
          <TableHeader>
            <TableRow className="border-zinc-800 hover:bg-zinc-900">
              <TableHead className="text-zinc-400">时间</TableHead>
              <TableHead className="text-zinc-400">用户 ID</TableHead>
              <TableHead className="text-zinc-400">类型</TableHead>
              <TableHead className="text-zinc-400">变动金额</TableHead>
              <TableHead className="text-zinc-400">详情</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {logs.map((log) => (
              <TableRow key={log.id} className="border-zinc-800 hover:bg-zinc-800/50">
                <TableCell className="text-zinc-400 text-xs">
                  {format(new Date(log.createdAt), "yyyy-MM-dd HH:mm:ss")}
                </TableCell>
                <TableCell className="text-zinc-300 font-mono text-xs">{log.userId}</TableCell>
                <TableCell>{getReasonBadge(log.reason)}</TableCell>
                <TableCell className={`font-mono font-bold ${log.amount > 0 ? 'text-green-500' : 'text-red-500'}`}>
                  {log.amount > 0 ? '+' : ''}{log.amount}
                </TableCell>
                <TableCell className="text-zinc-400 text-sm">{log.details}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

export default CreditLogs;