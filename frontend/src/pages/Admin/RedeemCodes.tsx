import { useEffect, useState } from "react";
import { api } from "@/lib/mock-api";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Plus, Copy } from "lucide-react";
import { useToast } from "@/components/ui/use-toast";

interface RedeemCode {
  id: string;
  code: string;
  amount: number;
  isUsed: boolean;
  usedBy?: string;
  expiresAt: string;
}

const RedeemCodes = () => {
  const [codes, setCodes] = useState<RedeemCode[]>([]);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newCode, setNewCode] = useState("");
  const [amount, setAmount] = useState("100");
  const { toast } = useToast();

  const fetchCodes = () => {
    api.admin.getCodes().then(setCodes);
  };

  useEffect(() => {
    fetchCodes();
  }, []);

  const handleCreate = async () => {
    if (!newCode || !amount) return;
    await api.admin.createCode({ code: newCode, amount: parseInt(amount) });
    toast({ title: "兑换码创建成功" });
    setIsCreateOpen(false);
    setNewCode("");
    fetchCodes();
  };

  const generateRandomCode = () => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';
    for (let i = 0; i < 8; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setNewCode(result);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">兑换码管理</h1>
        <Button onClick={() => setIsCreateOpen(true)} className="bg-zinc-100 text-zinc-900 hover:bg-zinc-200">
          <Plus className="mr-2 h-4 w-4" /> 生成兑换码
        </Button>
      </div>

      <div className="border border-zinc-800 rounded-md bg-zinc-900">
        <Table>
          <TableHeader>
            <TableRow className="border-zinc-800 hover:bg-zinc-900">
              <TableHead className="text-zinc-400">代码</TableHead>
              <TableHead className="text-zinc-400">面额</TableHead>
              <TableHead className="text-zinc-400">状态</TableHead>
              <TableHead className="text-zinc-400">使用者</TableHead>
              <TableHead className="text-zinc-400">过期时间</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {codes.map((code) => (
              <TableRow key={code.id} className="border-zinc-800 hover:bg-zinc-800/50">
                <TableCell className="font-mono text-zinc-200">{code.code}</TableCell>
                <TableCell className="text-zinc-300">{code.amount}</TableCell>
                <TableCell>
                  {code.isUsed ? (
                    <Badge variant="secondary" className="bg-zinc-800 text-zinc-500">已使用</Badge>
                  ) : (
                    <Badge variant="outline" className="border-green-900 text-green-500">未使用</Badge>
                  )}
                </TableCell>
                <TableCell className="text-zinc-500 text-xs">{code.usedBy || '-'}</TableCell>
                <TableCell className="text-zinc-500 text-xs">{code.expiresAt}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent className="bg-zinc-900 border-zinc-800 text-zinc-100">
          <DialogHeader>
            <DialogTitle>生成新兑换码</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>兑换码</Label>
              <div className="flex gap-2">
                <Input 
                  value={newCode} 
                  onChange={(e) => setNewCode(e.target.value.toUpperCase())}
                  className="bg-zinc-950 border-zinc-800 font-mono"
                  placeholder="输入或生成"
                />
                <Button variant="outline" onClick={generateRandomCode} className="border-zinc-700">随机</Button>
              </div>
            </div>
            <div className="space-y-2">
              <Label>积分面额</Label>
              <Input 
                type="number" 
                value={amount} 
                onChange={(e) => setAmount(e.target.value)}
                className="bg-zinc-950 border-zinc-800"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCreateOpen(false)} className="border-zinc-700 hover:bg-zinc-800 text-zinc-300">取消</Button>
            <Button onClick={handleCreate} className="bg-zinc-100 text-zinc-900 hover:bg-zinc-200">创建</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default RedeemCodes;