import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { WorldPromptTemplates } from "@/types";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/components/ui/use-toast";
import { Loader2, HelpCircle } from "lucide-react";
import { Link } from "react-router-dom";

const WorldPrompts = () => {
  const [prompts, setPrompts] = useState<WorldPromptTemplates | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    api.prompts.getWorld().then(setPrompts);
  }, []);

  const handleSave = async () => {
    if (!prompts) return;
    setIsSaving(true);
    try {
      await api.prompts.updateWorld(prompts);
      toast({ title: "世界观模板已更新" });
    } finally {
      setIsSaving(false);
    }
  };

  if (!prompts) return <div>Loading...</div>;

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex justify-end">
        <Button variant="outline" size="sm" asChild>
          <Link to="/settings/world-prompts/help">
            <HelpCircle className="mr-2 h-4 w-4" />
            查看世界观变量说明
          </Link>
        </Button>
      </div>

      <Tabs defaultValue="modules">
        <TabsList>
          <TabsTrigger value="modules">模块生成模板</TabsTrigger>
          <TabsTrigger value="refine">字段精修模板</TabsTrigger>
        </TabsList>
        
        <TabsContent value="modules" className="space-y-4 mt-4">
          {Object.entries(prompts.modules).map(([key, template]) => (
            <Card key={key}>
              <CardHeader>
                <CardTitle className="capitalize">{key} Module</CardTitle>
              </CardHeader>
              <CardContent>
                <Textarea 
                  value={template}
                  onChange={(e) => setPrompts({
                    ...prompts,
                    modules: { ...prompts.modules, [key]: e.target.value }
                  })}
                  className="min-h-[150px] font-mono text-sm"
                />
              </CardContent>
            </Card>
          ))}
        </TabsContent>

        <TabsContent value="refine" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>字段精修</CardTitle>
              <CardDescription>用于优化单个设定字段的提示词。</CardDescription>
            </CardHeader>
            <CardContent>
              <Textarea 
                value={prompts.fieldRefine}
                onChange={(e) => setPrompts({ ...prompts, fieldRefine: e.target.value })}
                className="min-h-[150px] font-mono text-sm"
              />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <div className="sticky bottom-6 flex justify-end bg-background/80 backdrop-blur p-4 border rounded-lg shadow-lg">
        <Button onClick={handleSave} disabled={isSaving} size="lg">
          {isSaving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
          保存所有修改
        </Button>
      </div>
    </div>
  );
};

export default WorldPrompts;