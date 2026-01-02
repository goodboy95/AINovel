import { useState } from "react";
import TiptapEditor from "@/components/editor/TiptapEditor";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Save, History, PanelRightOpen, PanelRightClose } from "lucide-react";
import CopilotSidebar from "@/components/ai/CopilotSidebar";
import { cn } from "@/lib/utils";

const ManuscriptWriter = () => {
  const [content, setContent] = useState("<h1>第一章：觉醒</h1><p>雨水顺着生锈的铁皮屋顶滴落，在积水的地面上激起一圈圈油腻的涟漪。杰克睁开眼，视网膜显示屏上闪烁着红色的警告标志。</p><p>“系统重启中... 义体完整度 42%...”</p>");
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  // Mock context data for AI
  const contextData = {
    storyTitle: "赛博侦探：霓虹雨",
    currentChapter: "第一章：觉醒",
    characters: ["杰克", "神秘医生"],
    currentContent: content
  };

  return (
    <div className="flex h-[calc(100vh-180px)] gap-4 relative">
      {/* Left: Navigation (Simplified) */}
      <div className="w-64 flex flex-col gap-2 border-r pr-4 hidden lg:flex">
        <Select>
          <SelectTrigger><SelectValue placeholder="选择故事" /></SelectTrigger>
          <SelectContent><SelectItem value="s1">赛博侦探：霓虹雨</SelectItem></SelectContent>
        </Select>
        <Select>
          <SelectTrigger><SelectValue placeholder="选择章节" /></SelectTrigger>
          <SelectContent><SelectItem value="c1">第一章：觉醒</SelectItem></SelectContent>
        </Select>
        
        <ScrollArea className="flex-1 mt-2">
          <div className="space-y-1">
            <Button variant="secondary" className="w-full justify-start text-sm">Sc.1 垃圾场苏醒</Button>
            <Button variant="ghost" className="w-full justify-start text-sm">Sc.2 遭遇拾荒者</Button>
          </div>
        </ScrollArea>
      </div>

      {/* Center: Editor */}
      <div className="flex-1 flex flex-col min-w-0 transition-all duration-300">
        <div className="flex items-center justify-between mb-2 px-2">
          <div className="text-sm text-muted-foreground">上次保存: 刚刚</div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm"><History className="mr-2 h-4 w-4" /> 历史版本</Button>
            <Button size="sm"><Save className="mr-2 h-4 w-4" /> 保存</Button>
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
            onChange={setContent} 
            className="h-full"
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