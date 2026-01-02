import { useEffect, useState } from "react";
import { api } from "@/lib/mock-api";
import { User } from "@/types";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Ban, Coins, MoreHorizontal, CheckCircle } from "lucide-react";
import { useToast } from "@/components/ui/use-toast";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";

const UserManager = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [creditAmount, setCreditAmount] = useState("");
  const [isCreditDialogOpen, setIsCreditDialogOpen] = useState(false);
  const { toast } = useToast();

  const fetchUsers = () => {
    api.admin.getUsers().then(setUsers);
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleGrantCredits = async () => {
    if (!selectedUser || !creditAmount) return;
    const amount = parseInt(creditAmount);
    if (isNaN(amount)) return;

    await api.admin.grantCredits(selectedUser.id, amount);
    toast({ title: "积分调整成功", description: `${amount > 0 ? '发放' : '扣除'} ${Math.abs(amount)} 积分` });
    setIsCreditDialogOpen(false);
    setCreditAmount("");
    fetchUsers();
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">用户管理</h1>
      </div>

      <div className="border border-zinc-800 rounded-md bg-zinc-900">
        <Table>
          <TableHeader>
            <TableRow className="border-zinc-800 hover:bg-zinc-900">
              <TableHead className="text-zinc-400">用户</TableHead>
              <TableHead className="text-zinc-400">角色</TableHead>
              <TableHead className="text-zinc-400">积分余额</TableHead>
              <TableHead className="text-zinc-400">状态</TableHead>
              <TableHead className="text-right text-zinc-400">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.map((user) => (
              <TableRow key={user.id} className="border-zinc-800 hover:bg-zinc-800/50">
                <TableCell className="font-medium text-zinc-200">
                  <div className="flex flex-col">
                    <span>{user.username}</span>
                    <span className="text-xs text-zinc-500">{user.email}</span>
                  </div>
                </TableCell>
                <TableCell>
                  <Badge variant="outline" className={user.role === 'admin' ? "border-red-500 text-red-500" : "border-zinc-700 text-zinc-400"}>
                    {user.role}
                  </Badge>
                </TableCell>
                <TableCell className="text-zinc-300 font-mono">{user.credits.toLocaleString()}</TableCell>
                <TableCell>
                  {user.isBanned ? (
                    <Badge variant="destructive">已封禁</Badge>
                  ) : (
                    <Badge variant="outline" className="border-green-900 text-green-500 bg-green-900/20">正常</Badge>
                  )}
                </TableCell>
                <TableCell className="text-right">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon" className="text-zinc-400 hover:text-zinc-100">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="bg-zinc-900 border-zinc-800 text-zinc-200">
                      <DropdownMenuItem 
                        onClick={() => { setSelectedUser(user); setIsCreditDialogOpen(true); }}
                        className="focus:bg-zinc-800 focus:text-zinc-100"
                      >
                        <Coins className="mr-2 h-4 w-4" /> 调整积分
                      </DropdownMenuItem>
                      <DropdownMenuItem className="text-red-500 focus:bg-red-900/20 focus:text-red-400">
                        <Ban className="mr-2 h-4 w-4" /> 封禁账号
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={isCreditDialogOpen} onOpenChange={setIsCreditDialogOpen}>
        <DialogContent className="bg-zinc-900 border-zinc-800 text-zinc-100">
          <DialogHeader>
            <DialogTitle>调整积分 - {selectedUser?.username}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>变动数量 (正数增加，负数扣除)</Label>
              <Input 
                type="number" 
                value={creditAmount} 
                onChange={(e) => setCreditAmount(e.target.value)}
                className="bg-zinc-950 border-zinc-800"
                placeholder="例如: 1000 或 -500"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCreditDialogOpen(false)} className="border-zinc-700 hover:bg-zinc-800 text-zinc-300">取消</Button>
            <Button onClick={handleGrantCredits} className="bg-zinc-100 text-zinc-900 hover:bg-zinc-200">确认调整</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default UserManager;