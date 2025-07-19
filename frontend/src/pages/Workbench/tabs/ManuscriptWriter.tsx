import { useState } from "react";
import TiptapEditor from "@/components/editor/TiptapEditor";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Sparkles, Save, History, User, Lightbulb } from "lucide-react";

const ManuscriptWriter = () => {
  const [content, setContent] = useState("<h1>第一章：觉醒</h1><p>雨水顺着生锈的铁皮屋顶滴落，在积水的地面上激起一圈圈油腻的涟漪。杰克睁开眼，视网膜显示屏上闪烁着红色的警告标志。</p><p>“系统重启中... 义体完整度 42%...”</p>");

  return (
    <div className="flex h-[calc(100vh-180px)] gap-4">
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
      <div className="flex-1 flex flex-col min-w-0">
        <div className="flex items-center justify-between mb-2 px-2">
          <div className="text-sm text-muted-foreground">上次保存: 刚刚</div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm"><History className="mr-2 h-4 w-4" /> 历史版本</Button>
            <Button size="sm"><Save className="mr-2 h-4 w-4" /> 保存</Button>
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

      {/* Right: Assistant Sidebar */}
      <div className="w-72 border-l pl-4 hidden xl:flex flex-col">
        <Tabs defaultValue="status" className="h-full flex flex-col">
          <TabsList className="w-full">
            <TabsTrigger value="status" className="flex-1"><User className="h-4 w-4 mr-2"/>状态</TabsTrigger>
            <TabsTrigger value="materials" className="flex-1"><Lightbulb className="h-4 w-4 mr-2"/>素材</TabsTrigger>
          </TabsList>
          
          <TabsContent value="status" className="flex-1 overflow-hidden">
            <ScrollArea className="h-full pr-2">
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <h4 className="font-medium text-sm">当前场景角色</h4>
                  <div className="p-3 bg-muted/50 rounded-lg text-sm">
                    <div className="font-medium">杰克 (主角)</div>
                    <div className="text-xs text-muted-foreground mt-1">状态：受伤 (义体损坏)</div>
                    <div className="text-xs text-muted-foreground">心理：迷茫、警惕</div>
                  </div>
                </div>
                <div className="space-y-2">
                  <h4 className="font-medium text-sm">AI 建议</h4>
                  <div className="p-3 border border-primary/20 bg-primary/5 rounded-lg text-sm">
                    <div className="flex items-center gap-2 text-primary mb-1">
                      <Sparkles className="h-3 w-3" /> 剧情张力
                    </div>
                    建议增加环境描写来烘托主角苏醒时的无助感，例如描述雨水的冰冷和周围金属垃圾的压迫感。
                  </div>
                </div>
              </div>
            </ScrollArea>
          </TabsContent>
          
          <TabsContent value="materials" className="flex-1 overflow-hidden">
            <ScrollArea className="h-full pr-2">
              <div className="space-y-3 py-4">
                <div className="text-xs text-muted-foreground mb-2">根据正文自动推荐</div>
                <div className="p-2 border rounded hover:bg-accent cursor-pointer transition-colors">
                  <div className="font-medium text-sm">赛博义肢型号大全</div>
                  <div className="text-xs text-muted-foreground line-clamp-2 mt-1">...军用型义体通常配备神经阻断器...</div>
                </div>
                <div className="p-2 border rounded hover:bg-accent cursor-pointer transition-colors">
                  <div className="font-medium text-sm">新东京天气设定</div>
                  <div className="text-xs text-muted-foreground line-clamp-2 mt-1">酸雨是常态，底层区域终年不见阳光...</div>
                </div>
              </div>
            </ScrollArea>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};

export default ManuscriptWriter;