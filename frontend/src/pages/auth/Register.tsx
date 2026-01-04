import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { api } from "@/lib/mock-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/components/ui/use-toast";
import { Loader2, Mail, ArrowRight, CheckCircle2 } from "lucide-react";
import CapJS from "@/components/auth/CapJS";

const Register = () => {
  const [step, setStep] = useState<1 | 2>(1);
  const [email, setEmail] = useState("");
  const [captchaToken, setCaptchaToken] = useState("");
  const [code, setCode] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  
  const navigate = useNavigate();
  const { toast } = useToast();

  const handleSendCode = async () => {
    if (!email) {
      toast({ variant: "destructive", title: "请输入邮箱" });
      return;
    }
    if (!captchaToken) {
      toast({ variant: "destructive", title: "请先完成人机验证" });
      return;
    }

    setIsSendingCode(true);
    try {
      await api.auth.sendCode(email, captchaToken);
      toast({ title: "验证码已发送", description: "请查收邮件" });
      setStep(2);
    } catch (error: any) {
      toast({ variant: "destructive", title: "发送失败", description: error.message });
    } finally {
      setIsSendingCode(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      toast({ variant: "destructive", title: "两次密码不一致" });
      return;
    }

    setIsRegistering(true);
    try {
      await api.auth.registerV2({ email, code, username, password });
      toast({ title: "注册成功", description: "请登录" });
      navigate("/login");
    } catch (error: any) {
      toast({ variant: "destructive", title: "注册失败", description: error.message });
    } finally {
      setIsRegistering(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/30 px-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold text-center">创建账号</CardTitle>
          <CardDescription className="text-center">
            {step === 1 ? "验证您的邮箱以开始" : "设置您的账户信息"}
          </CardDescription>
        </CardHeader>
        
        <CardContent className="space-y-4">
          {step === 1 && (
            <div className="space-y-4 animate-in slide-in-from-right-4 duration-300">
              <div className="space-y-2">
                <Label htmlFor="email">邮箱地址</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="writer@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              
              <CapJS onVerify={setCaptchaToken} />
              
              <Button 
                className="w-full" 
                onClick={handleSendCode} 
                disabled={isSendingCode || !captchaToken || !email}
              >
                {isSendingCode ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Mail className="mr-2 h-4 w-4" />}
                发送验证码
              </Button>
            </div>
          )}

          {step === 2 && (
            <form onSubmit={handleRegister} className="space-y-4 animate-in slide-in-from-right-4 duration-300">
              <div className="p-3 bg-primary/5 rounded-md flex items-center gap-2 text-sm text-muted-foreground mb-4">
                <CheckCircle2 className="h-4 w-4 text-green-600" />
                验证码已发送至 {email}
                <Button variant="link" className="h-auto p-0 ml-auto text-xs" onClick={() => setStep(1)}>修改</Button>
              </div>

              <div className="space-y-2">
                <Label htmlFor="code">验证码</Label>
                <Input
                  id="code"
                  placeholder="6位数字"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="username">用户名</Label>
                <Input
                  id="username"
                  placeholder="WriterName"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">密码</Label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirmPassword">确认密码</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>

              <Button type="submit" className="w-full" disabled={isRegistering}>
                {isRegistering ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                完成注册
              </Button>
            </form>
          )}
        </CardContent>
        
        <CardFooter>
          <div className="w-full text-center text-sm text-muted-foreground">
            已有账号?{" "}
            <Link to="/login" className="text-primary hover:underline font-medium">
              直接登录
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
};

export default Register;
