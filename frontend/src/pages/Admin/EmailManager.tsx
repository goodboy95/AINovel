import { useEffect, useState } from "react";
import { api } from "@/lib/mock-api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/use-toast";
import { RefreshCw, Mail } from "lucide-react";

const EmailManager = () => {
  const { toast } = useToast();
  const [smtp, setSmtp] = useState<any>(null);
  const [testEmail, setTestEmail] = useState("");
  const [isSending, setIsSending] = useState(false);

  const refresh = async () => {
    const smtpStatus = await api.admin.getSmtpStatus();
    setSmtp(smtpStatus);
  };

  useEffect(() => {
    refresh();
  }, []);

  const handleSendTest = async () => {
    if (!testEmail) return;
    setIsSending(true);
    try {
      await api.admin.sendTestEmail(testEmail);
      toast({ title: "测试邮件已发送", description: "请检查收件箱" });
    } catch (e: any) {
      toast({ variant: "destructive", title: "发送失败", description: e.message });
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">邮件验证与 SMTP</h1>
        <Button onClick={refresh} variant="outline" className="border-zinc-700 hover:bg-zinc-800 text-zinc-200">
          <RefreshCw className="mr-2 h-4 w-4" /> 刷新
        </Button>
      </div>

      <Card className="bg-zinc-900 border-zinc-800 text-zinc-100">
        <CardHeader>
          <CardTitle>SMTP 状态</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm text-zinc-300">
          <div>
            Host: <span className="font-mono text-zinc-100">{smtp?.host || "-"}</span>
          </div>
          <div>
            Port: <span className="font-mono text-zinc-100">{smtp?.port || "-"}</span>
          </div>
          <div>
            Username: <span className="font-mono text-zinc-100">{smtp?.username || "-"}</span>
          </div>
          <div>
            Password: <span className="font-mono text-zinc-100">{smtp?.passwordIsSet ? "已配置" : "未配置"}</span>
          </div>
          <div className="pt-3 flex gap-2">
            <Input value={testEmail} onChange={(e) => setTestEmail(e.target.value)} placeholder="输入收件邮箱发送测试邮件" className="bg-zinc-950 border-zinc-800" />
            <Button onClick={handleSendTest} disabled={!testEmail || isSending} className="bg-zinc-100 text-zinc-900 hover:bg-zinc-200">
              <Mail className="mr-2 h-4 w-4" /> {isSending ? "发送中..." : "发送测试"}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card className="bg-zinc-900 border-zinc-800 text-zinc-100">
        <CardHeader>
          <CardTitle>说明</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-zinc-300">
          本系统已启用统一登录，不再提供邮箱验证码注册/登录相关功能；此处仅保留 SMTP 配置状态与测试邮件。
        </CardContent>
      </Card>
    </div>
  );
};

export default EmailManager;
