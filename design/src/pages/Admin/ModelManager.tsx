import { useEffect, useState } from "react";
import { api } from "@/lib/mock-api";
import { ModelConfig } from "@/types";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Edit, Plus } from "lucide-react";

const ModelManager = () => {
  const [models, setModels] = useState<ModelConfig[]>([]);

  useEffect(() => {
    api.admin.getModels().then(setModels);
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">模型与 API 池管理</h1>
        <Button className="bg-red-600 hover:bg-red-700"><Plus className="mr-2 h-4 w-4" /> 添加模型</Button>
      </div>

      <div className="border border-zinc-800 rounded-md bg-zinc-900">
        <Table>
          <TableHeader>
            <TableRow className="border-zinc-800 hover:bg-zinc-900">
              <TableHead className="text-zinc-400">显示名称</TableHead>
              <TableHead className="text-zinc-400">内部 ID</TableHead>
              <TableHead className="text-zinc-400">输入倍率</TableHead>
              <TableHead className="text-zinc-400">输出倍率</TableHead>
              <TableHead className="text-zinc-400">API 池</TableHead>
              <TableHead className="text-zinc-400">状态</TableHead>
              <TableHead className="text-right text-zinc-400">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {models.map((model) => (
              <TableRow key={model.id} className="border-zinc-800 hover:bg-zinc-800/50">
                <TableCell className="font-medium text-zinc-200">{model.displayName}</TableCell>
                <TableCell className="text-zinc-500 font-mono text-xs">{model.name}</TableCell>
                <TableCell className="text-zinc-300">x{model.inputMultiplier}</TableCell>
                <TableCell className="text-zinc-300">x{model.outputMultiplier}</TableCell>
                <TableCell>
                  <Badge variant="outline" className="border-zinc-700 text-zinc-400">{model.poolId}</Badge>
                </TableCell>
                <TableCell>
                  <Switch checked={model.isEnabled} />
                </TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="icon" className="text-zinc-400 hover:text-zinc-100">
                    <Edit className="h-4 w-4" />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

export default ModelManager;