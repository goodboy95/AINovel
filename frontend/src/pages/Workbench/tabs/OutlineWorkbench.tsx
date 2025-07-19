import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Story, Outline } from "@/types";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { ChevronRight, ChevronDown, FileText, Plus } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";

const OutlineWorkbench = () => {
  const [stories, setStories] = useState<Story[]>([]);
  const [selectedStoryId, setSelectedStoryId] = useState<string>("");
  const [outlines, setOutlines] = useState<Outline[]>([]);
  const [selectedOutline, setSelectedOutline] = useState<Outline | null>(null);
  
  // Mock selection state
  const [selectedNode, setSelectedNode] = useState<{type: 'chapter'|'scene', id: string} | null>(null);

  useEffect(() => {
    api.stories.list().then(data => {
      setStories(data);
      if (data.length > 0) setSelectedStoryId(data[0].id);
    });
  }, []);

  useEffect(() => {
    if (selectedStoryId) {
      api.outlines.listByStory(selectedStoryId).then(data => {
        setOutlines(data);
        if (data.length > 0) setSelectedOutline(data[0]);
      });
    }
  }, [selectedStoryId]);

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
          <Button size="sm" variant="ghost" className="h-6 w-6 p-0"><Plus className="h-4 w-4" /></Button>
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
              <Input defaultValue={selectedNode.type === 'chapter' ? "第一章：觉醒" : "垃圾场苏醒"} />
            </div>
            <div className="space-y-2">
              <Label>梗概 / 摘要</Label>
              <Textarea 
                className="min-h-[200px]" 
                defaultValue={selectedNode.type === 'chapter' ? "主角在垃圾堆中醒来..." : "雨夜，霓虹灯闪烁，主角睁开眼。"} 
              />
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline">AI 润色</Button>
              <Button>保存修改</Button>
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