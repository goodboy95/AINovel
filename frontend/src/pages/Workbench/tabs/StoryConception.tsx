import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Sparkles, Loader2 } from "lucide-react";
import { useToast } from "@/components/ui/use-toast";
import { api } from "@/lib/api";

const StoryConception = () => {
  const [idea, setIdea] = useState("");
  const [genre, setGenre] = useState("");
  const [tone, setTone] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const { toast } = useToast();

  const handleGenerate = async () => {
    if (!idea) {
      toast({ variant: "destructive", title: "请输入核心创意" });
      return;
    }
    setIsGenerating(true);
    try {
      // Mock generation
      await api.stories.create({
        title: "AI 生成的故事: " + idea.slice(0, 10) + "...",
        synopsis: idea,
        genre: genre || "未分类",
        tone: tone || "默认",
        status: "draft"
      });
      toast({ title: "故事生成成功", description: "已添加到故事列表" });
      setIdea("");
    } catch (error) {
      toast({ variant: "destructive", title: "生成失败" });
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-full">
      <Card className="h-fit">
        <CardHeader>
          <CardTitle>灵感输入</CardTitle>
          <CardDescription>告诉 AI 你的想法，我们将为你生成完整的故事大纲与角色设定。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label>核心创意 / 一句话梗概</Label>
            <Textarea 
              placeholder="例如：一个在末日废土上送快递的机器人，意外发现了一个冷冻的人类婴儿..." 
              className="min-h-[120px]"
              value={idea}
              onChange={(e) => setIdea(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>类型流派</Label>
              <Select value={genre} onValueChange={setGenre}>
                <SelectTrigger>
                  <SelectValue placeholder="选择类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="scifi">科幻</SelectItem>
                  <SelectItem value="fantasy">奇幻</SelectItem>
                  <SelectItem value="mystery">悬疑</SelectItem>
                  <SelectItem value="romance">言情</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>基调风格</Label>
              <Select value={tone} onValueChange={setTone}>
                <SelectTrigger>
                  <SelectValue placeholder="选择基调" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="dark">暗黑/压抑</SelectItem>
                  <SelectItem value="humorous">幽默/轻松</SelectItem>
                  <SelectItem value="epic">史诗/宏大</SelectItem>
                  <SelectItem value="warm">治愈/温馨</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
        <CardFooter>
          <Button className="w-full" onClick={handleGenerate} disabled={isGenerating}>
            {isGenerating ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Sparkles className="mr-2 h-4 w-4" />}
            生成故事卡与角色
          </Button>
        </CardFooter>
      </Card>

      <div className="space-y-6">
        <div className="border-2 border-dashed rounded-lg p-8 flex flex-col items-center justify-center text-muted-foreground h-[400px] bg-muted/20">
          <Sparkles className="h-12 w-12 mb-4 opacity-20" />
          <p>生成结果将显示在这里</p>
          <p className="text-sm opacity-70">包含：故事标题、详细大纲、主要角色卡</p>
        </div>
      </div>
    </div>
  );
};

export default StoryConception;