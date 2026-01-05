import { useEffect, useState } from "react";
import { api } from "@/lib/mock-api";
import { Story, Outline } from "@/types";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { ChevronDown, FileText, Plus, Save, Sparkles } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { useToast } from "@/components/ui/use-toast";

interface OutlineWorkbenchProps {
  initialStoryId?: string;
}

const OutlineWorkbench = ({ initialStoryId }: OutlineWorkbenchProps) => {
  const [stories, setStories] = useState<Story[]>([]);
  const [selectedStoryId, setSelectedStoryId] = useState<string>("");
  const [outlines, setOutlines] = useState<Outline[]>([]);
  const [selectedOutline, setSelectedOutline] = useState<Outline | null>(null);
  
  const [selectedNode, setSelectedNode] = useState<{type: 'chapter'|'scene', id: string} | null>(null);
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    api.stories.list().then(data => {
      setStories(data);
      if (initialStoryId && data.some((s) => s.id === initialStoryId)) {
        setSelectedStoryId(initialStoryId);
      } else if (data.length > 0) {
        setSelectedStoryId(data[0].id);
      }
    });
  }, []);

  useEffect(() => {
    if (selectedStoryId) {
      api.outlines.listByStory(selectedStoryId).then(data => {
        setOutlines(data);
        if (data.length > 0) setSelectedOutline(data[0]);
        else setSelectedOutline(null);
      });
    }
  }, [selectedStoryId]);

  useEffect(() => {
    if (!selectedOutline) return;
    if (!selectedNode) return;
    if (selectedNode.type === "chapter") {
      const c = selectedOutline.chapters.find((x) => x.id === selectedNode.id);
      setTitle(c?.title || "");
      setSummary(c?.summary || "");
    } else {
      for (const c of selectedOutline.chapters) {
        const s = c.scenes.find((x) => x.id === selectedNode.id);
        if (s) {
          setTitle(s.title || "");
          setSummary(s.summary || "");
          return;
        }
      }
      setTitle("");
      setSummary("");
    }
  }, [selectedNode, selectedOutline]);

  const ensureUuid = () => {
    if (crypto?.randomUUID) return crypto.randomUUID();
    // RFC4122-ish v4 uuid fallback (string shape must be parseable as UUID by backend)
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
      const r = Math.floor(Math.random() * 16);
      const v = c === "x" ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  };

  const handleCreateOutline = async () => {
    if (!selectedStoryId) return;
    try {
      const o = await api.outlines.create(selectedStoryId, { title: "新大纲" });
      setOutlines((prev) => [o, ...prev]);
      setSelectedOutline(o);
      setSelectedNode(null);
      toast({ title: "已创建大纲" });
    } catch (e: any) {
      toast({ variant: "destructive", title: "创建失败", description: e.message });
    }
  };

  const handleAddChapter = () => {
    if (!selectedOutline) return;
    const newChapter = { id: ensureUuid(), title: "新章节", summary: "", scenes: [] as any[] };
    setSelectedOutline({ ...selectedOutline, chapters: [...selectedOutline.chapters, newChapter as any] });
    setSelectedNode({ type: "chapter", id: newChapter.id });
  };

  const handleAddScene = () => {
    if (!selectedOutline || !selectedNode || selectedNode.type !== "chapter") return;
    const chapters = selectedOutline.chapters.map((c) => {
      if (c.id !== selectedNode.id) return c;
      const scenes = [...(c.scenes || []), { id: ensureUuid(), title: "新场景", summary: "", content: "" }];
      return { ...c, scenes };
    });
    setSelectedOutline({ ...selectedOutline, chapters });
  };

  const applyEditsToOutline = () => {
    if (!selectedOutline || !selectedNode) return selectedOutline;
    const chapters = selectedOutline.chapters.map((c) => {
      if (selectedNode.type === "chapter" && c.id === selectedNode.id) {
        return { ...c, title, summary };
      }
      if (selectedNode.type === "scene") {
        const scenes = (c.scenes || []).map((s) => (s.id === selectedNode.id ? { ...s, title, summary } : s));
        return { ...c, scenes };
      }
      return c;
    });
    return { ...selectedOutline, chapters };
  };

  const handleSave = async () => {
    if (!selectedOutline) return;
    const nextOutline = applyEditsToOutline();
    if (!nextOutline) return;
    setIsSaving(true);
    try {
      const saved = await api.outlines.save(selectedOutline.id, nextOutline as any);
      setSelectedOutline(saved);
      setOutlines((prev) => prev.map((o) => (o.id === saved.id ? saved : o)));
      toast({ title: "已保存" });
    } catch (e: any) {
      toast({ variant: "destructive", title: "保存失败", description: e.message });
    } finally {
      setIsSaving(false);
    }
  };

  const handleAiRefine = async () => {
    if (!summary.trim()) return;
    try {
      const models = await api.ai.getModels();
      const modelId = models[0]?.id;
      const res = await api.ai.refine(summary, "润色梗概，使其更清晰、更具叙事张力", modelId);
      setSummary(res.result);
    } catch (e: any) {
      toast({ variant: "destructive", title: "AI 失败", description: e.message });
    }
  };

  return (
    <div className="flex h-[calc(100vh-200px)] gap-6">
      {/* Left Sidebar: Selection & Tree */}
      <div className="w-80 flex flex-col gap-4 border-r pr-4">
        <div className="space-y-2">
          <Label>当前故事</Label>
          <Select value={selectedStoryId} onValueChange={setSelectedStoryId}>
            <SelectTrigger>
              <SelectValue placeholder="选择故事" />
            </SelectTrigger>
            <SelectContent>
              {stories.map(s => <SelectItem key={s.id} value={s.id}>{s.title}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>

        <div className="flex items-center justify-between mt-2">
          <span className="text-sm font-medium text-muted-foreground">大纲结构</span>
          <div className="flex gap-1">
            <Button size="sm" variant="ghost" className="h-6 px-2" onClick={handleCreateOutline}>
              <Plus className="mr-1 h-4 w-4" /> 新大纲
            </Button>
            <Button size="sm" variant="ghost" className="h-6 px-2" onClick={handleAddChapter} disabled={!selectedOutline}>
              <Plus className="mr-1 h-4 w-4" /> 章节
            </Button>
          </div>
        </div>

        <ScrollArea className="flex-1">
          {selectedOutline ? (
            <div className="space-y-1">
              {selectedOutline.chapters.map(chapter => (
                <div key={chapter.id} className="space-y-1">
                  <div 
                    className={`flex items-center gap-1 p-2 rounded-md cursor-pointer hover:bg-accent ${selectedNode?.id === chapter.id ? 'bg-accent' : ''}`}
                    onClick={() => setSelectedNode({type: 'chapter', id: chapter.id})}
                  >
                    <ChevronDown className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm font-medium truncate">{chapter.title}</span>
                  </div>
                  <div className="pl-6 space-y-1">
                    {chapter.scenes.map(scene => (
                      <div 
                        key={scene.id}
                        className={`flex items-center gap-2 p-2 rounded-md cursor-pointer hover:bg-accent text-sm ${selectedNode?.id === scene.id ? 'bg-accent text-accent-foreground' : 'text-muted-foreground'}`}
                        onClick={() => setSelectedNode({type: 'scene', id: scene.id})}
                      >
                        <FileText className="h-3 w-3" />
                        <span className="truncate">{scene.title}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-sm text-muted-foreground text-center py-4">暂无大纲</div>
          )}
        </ScrollArea>
      </div>

      {/* Main Content: Editor */}
      <div className="flex-1">
        {selectedNode ? (
          <div className="space-y-6 max-w-2xl">
            <div className="space-y-2">
              <Label>标题</Label>
              <Input value={title} onChange={(e) => setTitle(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>梗概 / 摘要</Label>
              <Textarea 
                className="min-h-[200px]" 
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-2">
              {selectedNode.type === "chapter" && (
                <Button variant="outline" onClick={handleAddScene}>
                  <Plus className="mr-2 h-4 w-4" /> 添加场景
                </Button>
              )}
              <Button variant="outline" onClick={handleAiRefine}>
                <Sparkles className="mr-2 h-4 w-4" /> AI 润色
              </Button>
              <Button onClick={handleSave} disabled={isSaving}>
                <Save className="mr-2 h-4 w-4" /> {isSaving ? "保存中..." : "保存修改"}
              </Button>
            </div>
          </div>
        ) : (
          <div className="h-full flex items-center justify-center text-muted-foreground">
            请在左侧选择章节或场景进行编辑
          </div>
        )}
      </div>
    </div>
  );
};

export default OutlineWorkbench;
