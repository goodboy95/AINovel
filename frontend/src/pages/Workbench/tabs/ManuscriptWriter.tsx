import { useEffect, useMemo, useRef, useState } from "react";
import TiptapEditor from "@/components/editor/TiptapEditor";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Save, History, PanelRightOpen, PanelRightClose, Sparkles, Loader2 } from "lucide-react";
import CopilotSidebar from "@/components/ai/CopilotSidebar";
import { cn } from "@/lib/utils";
import { api } from "@/lib/mock-api";
import { Manuscript, Outline, Story } from "@/types";
import { useToast } from "@/components/ui/use-toast";

interface ManuscriptWriterProps {
  initialStoryId?: string;
}

const ManuscriptWriter = ({ initialStoryId }: ManuscriptWriterProps) => {
  const { toast } = useToast();
  const [stories, setStories] = useState<Story[]>([]);
  const [selectedStoryId, setSelectedStoryId] = useState<string>("");
  const [outlines, setOutlines] = useState<Outline[]>([]);
  const [selectedOutlineId, setSelectedOutlineId] = useState<string>("");
  const [manuscripts, setManuscripts] = useState<Manuscript[]>([]);
  const [selectedManuscriptId, setSelectedManuscriptId] = useState<string>("");

  const [selectedSceneId, setSelectedSceneId] = useState<string>("");
  const [content, setContent] = useState<string>("");
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [lastSavedAt, setLastSavedAt] = useState<string>("");
  const [isGenerating, setIsGenerating] = useState(false);
  const saveTimer = useRef<number | null>(null);

  const selectedStory = useMemo(() => stories.find((s) => s.id === selectedStoryId) || null, [stories, selectedStoryId]);
  const selectedOutline = useMemo(() => outlines.find((o) => o.id === selectedOutlineId) || null, [outlines, selectedOutlineId]);
  const selectedManuscript = useMemo(() => manuscripts.find((m) => m.id === selectedManuscriptId) || null, [manuscripts, selectedManuscriptId]);

  useEffect(() => {
    api.stories
      .list()
      .then((list) => {
        setStories(list);
        if (initialStoryId && list.some((s) => s.id === initialStoryId)) setSelectedStoryId(initialStoryId);
        else if (list.length > 0) setSelectedStoryId(list[0].id);
      })
      .catch((e: any) => toast({ variant: "destructive", title: "加载故事失败", description: e.message }));
  }, []);

  useEffect(() => {
    if (!selectedStoryId) return;
    api.outlines
      .listByStory(selectedStoryId)
      .then(async (list) => {
        let next = list;
        if (next.length === 0) {
          const created = await api.outlines.create(selectedStoryId, { title: "主线大纲" });
          next = [created];
        }
        setOutlines(next);
        setSelectedOutlineId((prev) => (prev && next.some((o) => o.id === prev) ? prev : next[0].id));
      })
      .catch((e: any) => toast({ variant: "destructive", title: "加载大纲失败", description: e.message }));
  }, [selectedStoryId]);

  useEffect(() => {
    if (!selectedOutlineId) return;
    api.manuscripts
      .listByOutline(selectedOutlineId)
      .then(async (list) => {
        let next = list;
        if (next.length === 0) {
          const created = await api.manuscripts.create(selectedOutlineId, { title: "正文稿" });
          next = [created];
        }
        setManuscripts(next);
        setSelectedManuscriptId((prev) => (prev && next.some((m) => m.id === prev) ? prev : next[0].id));
      })
      .catch((e: any) => toast({ variant: "destructive", title: "加载稿件失败", description: e.message }));
  }, [selectedOutlineId]);

  // pick default scene when outline changes
  useEffect(() => {
    const firstScene = selectedOutline?.chapters?.[0]?.scenes?.[0]?.id || "";
    setSelectedSceneId((prev) => {
      if (!prev) return firstScene;
      const exists = selectedOutline?.chapters?.some((c) => c.scenes.some((s) => s.id === prev));
      return exists ? prev : firstScene;
    });
  }, [selectedOutlineId]);

  // load content for selected scene
  useEffect(() => {
    if (!selectedManuscript || !selectedSceneId) {
      setContent("");
      return;
    }
    setContent(selectedManuscript.sections?.[selectedSceneId] || "");
  }, [selectedManuscriptId, selectedSceneId]);

  const scheduleSave = (html: string) => {
    if (!selectedManuscriptId || !selectedSceneId) return;
    if (saveTimer.current) window.clearTimeout(saveTimer.current);
    saveTimer.current = window.setTimeout(async () => {
      setIsSaving(true);
      try {
        const saved = await api.manuscripts.saveSection(selectedManuscriptId, selectedSceneId, html);
        setManuscripts((prev) => prev.map((m) => (m.id === saved.id ? saved : m)));
        setLastSavedAt(new Date().toLocaleTimeString());
      } catch (e: any) {
        toast({ variant: "destructive", title: "自动保存失败", description: e.message });
      } finally {
        setIsSaving(false);
      }
    }, 1200);
  };

  // Mock context data for AI
  const contextData = useMemo(() => {
    const currentChapter = selectedOutline?.chapters?.find((c) => c.scenes.some((s) => s.id === selectedSceneId));
    const currentScene = currentChapter?.scenes?.find((s) => s.id === selectedSceneId);
    return {
      storyTitle: selectedStory?.title,
      outlineTitle: selectedOutline?.title,
      currentChapter: currentChapter?.title,
      currentScene: currentScene?.title,
      sceneSummary: currentScene?.summary,
      currentContent: content,
    };
  }, [selectedStory, selectedOutline, selectedSceneId, content]);

  return (
    <div className="flex h-[calc(100vh-180px)] gap-4 relative">
      {/* Left: Navigation (Simplified) */}
      <div className="w-64 flex flex-col gap-2 border-r pr-4 hidden lg:flex">
        <Select value={selectedStoryId} onValueChange={setSelectedStoryId}>
          <SelectTrigger>
            <SelectValue placeholder="选择故事" />
          </SelectTrigger>
          <SelectContent>
            {stories.map((s) => (
              <SelectItem key={s.id} value={s.id}>
                {s.title}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={selectedOutlineId} onValueChange={setSelectedOutlineId} disabled={!selectedStoryId}>
          <SelectTrigger>
            <SelectValue placeholder="选择大纲" />
          </SelectTrigger>
          <SelectContent>
            {outlines.map((o) => (
              <SelectItem key={o.id} value={o.id}>
                {o.title}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={selectedManuscriptId} onValueChange={setSelectedManuscriptId} disabled={!selectedOutlineId}>
          <SelectTrigger>
            <SelectValue placeholder="选择稿件" />
          </SelectTrigger>
          <SelectContent>
            {manuscripts.map((m) => (
              <SelectItem key={m.id} value={m.id}>
                {m.title}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        
        <ScrollArea className="flex-1 mt-2">
          {selectedOutline ? (
            <div className="space-y-2">
              {selectedOutline.chapters.map((c, ci) => (
                <div key={c.id} className="space-y-1">
                  <div className="text-xs font-semibold text-muted-foreground px-2 pt-2">{`第${ci + 1}章  ${c.title}`}</div>
                  <div className="space-y-1">
                    {c.scenes.map((s, si) => (
                      <Button
                        key={s.id}
                        variant={selectedSceneId === s.id ? "secondary" : "ghost"}
                        className="w-full justify-start text-sm"
                        onClick={() => setSelectedSceneId(s.id)}
                      >
                        {`Sc.${si + 1} ${s.title}`}
                      </Button>
                    ))}
                    {c.scenes.length === 0 && <div className="text-xs text-muted-foreground px-2 pb-2">暂无场景，请先在“大纲编排”添加</div>}
                  </div>
                </div>
              ))}
              {selectedOutline.chapters.length === 0 && <div className="text-xs text-muted-foreground px-2">暂无章节，请先在“大纲编排”添加</div>}
            </div>
          ) : (
            <div className="text-xs text-muted-foreground px-2">暂无大纲</div>
          )}
        </ScrollArea>
      </div>

      {/* Center: Editor */}
      <div className="flex-1 flex flex-col min-w-0 transition-all duration-300">
        <div className="flex items-center justify-between mb-2 px-2">
          <div className="text-sm text-muted-foreground">
            {isSaving ? "正在保存..." : lastSavedAt ? `上次保存: ${lastSavedAt}` : "未保存"}
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm"><History className="mr-2 h-4 w-4" /> 历史版本</Button>
            <Button
              size="sm"
              onClick={async () => {
                if (!selectedManuscriptId || !selectedSceneId) return;
                setIsSaving(true);
                try {
                  const saved = await api.manuscripts.saveSection(selectedManuscriptId, selectedSceneId, content);
                  setManuscripts((prev) => prev.map((m) => (m.id === saved.id ? saved : m)));
                  setLastSavedAt(new Date().toLocaleTimeString());
                } finally {
                  setIsSaving(false);
                }
              }}
              disabled={!selectedManuscriptId || !selectedSceneId}
            >
              <Save className="mr-2 h-4 w-4" /> 保存
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={async () => {
                if (!selectedManuscriptId || !selectedSceneId) return;
                setIsGenerating(true);
                try {
                  const saved = await api.manuscripts.generateScene(selectedManuscriptId, selectedSceneId);
                  setManuscripts((prev) => prev.map((m) => (m.id === saved.id ? saved : m)));
                  setContent(saved.sections?.[selectedSceneId] || "");
                  toast({ title: "已生成场景正文" });
                } catch (e: any) {
                  toast({ variant: "destructive", title: "生成失败", description: e.message });
                } finally {
                  setIsGenerating(false);
                }
              }}
              disabled={isGenerating || !selectedSceneId || !selectedManuscriptId}
            >
              {isGenerating ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Sparkles className="mr-2 h-4 w-4" />}
              生成本场景
            </Button>
            <Button 
              variant="ghost" 
              size="icon" 
              onClick={() => setIsSidebarOpen(!isSidebarOpen)}
              className="ml-2"
              title={isSidebarOpen ? "收起助手" : "展开助手"}
            >
              {isSidebarOpen ? <PanelRightClose className="h-4 w-4" /> : <PanelRightOpen className="h-4 w-4" />}
            </Button>
          </div>
        </div>
        <div className="flex-1 border rounded-lg overflow-hidden bg-background shadow-sm">
          <TiptapEditor 
            content={content} 
            onChange={(html) => {
              setContent(html);
              scheduleSave(html);
            }} 
            className="h-full"
            editable={!!selectedSceneId}
          />
        </div>
      </div>

      {/* Right: AI Copilot Sidebar */}
      <div className={cn(
        "border-l transition-all duration-300 overflow-hidden",
        isSidebarOpen ? "w-80 opacity-100" : "w-0 opacity-0 border-l-0"
      )}>
        <CopilotSidebar context={contextData} className="h-full border-none" />
      </div>
    </div>
  );
};

export default ManuscriptWriter;
