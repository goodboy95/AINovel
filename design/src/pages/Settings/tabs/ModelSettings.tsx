import { useEffect, useState } from "react";
import { api } from "@/lib/mock-api";
import { SystemSettings } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/components/ui/use-toast";
import { Loader2, CheckCircle2, AlertCircle } from "lucide-react";

const ModelSettings = () => {
  const [settings, setSettings] = useState<SystemSettings | null>(null);
  const [apiKey, setApiKey] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isTesting, setIsTesting] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    api.settings.get().then(setSettings);
  }, []);

  const handleSave = async () => {
    if (!settings) return;
    setIsLoading(true);
    try {
      await api.settings.update({
        ...settings,
        // Only update apiKey if user entered a new one
        ...(apiKey ? { apiKeyIsSet: true } : {}) 
      });
      toast({ title: "设置已保存" });
      setApiKey(""); // Clear input after save
    } catch (error) {
      toast({ variant: "destructive", title: "保存失败" });
    } finally {
      setIsLoading(false);
    }
  };

  const handleTestConnection = async () => {
    setIsTesting(true);
    try {
      await api.settings.testConnection();
      toast({ 
        title: "连接成功", 
        description: "模型接口响应正常",
        className: "bg-green-50 border-green-200 text-green-800"
      });
    } catch (error) {
      toast({ 
        variant: "destructive", 
        title: "连接失败", 
        description: "请检查 Base URL 和 API Key" 
      });
    } finally {
      setIsTesting(false);
    }
  };

  if (!settings) return <div>Loading...</div>;

  return (
    <Card className="max-w-2xl">
      <CardHeader>
        <CardTitle>模型接入</CardTitle>
        <CardDescription>配置 LLM 服务的连接参数，支持 OpenAI 兼容接口。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label>Base URL</Label>
          <Input 
            value={settings.baseUrl} 
            onChange={(e) => setSettings({ ...settings, baseUrl: e.target.value })}
            placeholder="https://api.openai.com/v1"
          />
        </div>
        <div className="space-y-2">
          <Label>Model Name</Label>
          <Input 
            value={settings.modelName} 
            onChange={(e) => setSettings({ ...settings, modelName: e.target.value })}
            placeholder="gpt-4-turbo"
          />
        </div>
        <div className="space-y-2">
          <Label>API Key</Label>
          <div className="flex gap-2">
            <Input 
              type="password" 
              value={apiKey} 
              onChange={(e) => setApiKey(e.target.value)}
              placeholder={settings.apiKeyIsSet ? "已设置 (输入新 Key 以覆盖)" : "请输入 API Key"}
            />
          </div>
          {settings.apiKeyIsSet && (
            <p className="text-xs text-green-600 flex items-center gap-1">
              <CheckCircle2 className="h-3 w-3" /> 当前已配置有效 Key
            </p>
          )}
        </div>
      </CardContent>
      <CardFooter className="flex justify-between">
        <Button variant="outline" onClick={handleTestConnection} disabled={isTesting}>
          {isTesting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
          测试连接
        </Button>
        <Button onClick={handleSave} disabled={isLoading}>
          {isLoading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
          保存配置
        </Button>
      </CardFooter>
    </Card>
  );
};

export default ModelSettings;