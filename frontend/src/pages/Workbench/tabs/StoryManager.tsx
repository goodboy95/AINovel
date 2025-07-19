import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Story } from "@/types";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Edit, Trash2, BookOpen } from "lucide-react";

const StoryManager = () => {
  const [stories, setStories] = useState<Story[]>([]);
  const [selectedStory, setSelectedStory] = useState<Story | null>(null);

  useEffect(() => {
    api.stories.list().then(setStories);
  }, []);

  return (
    <div className="flex h-[calc(100vh-200px)] gap-6">
      {/* Story List */}
      <div className="w-1/3 flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-lg">我的故事</h3>
          <Button size="sm" variant="outline">新建</Button>
        </div>
        <ScrollArea className="flex-1 pr-4">
          <div className="space-y-3">
            {stories.map((story) => (
              <Card 
                key={story.id} 
                className={`cursor-pointer transition-all hover:border-primary/50 ${selectedStory?.id === story.id ? 'border-primary bg-primary/5' : ''}`}
                onClick={() => setSelectedStory(story)}
              >
                <CardHeader className="p-4 pb-2">
                  <div className="flex justify-between items-start">
                    <CardTitle className="text-base">{story.title}</CardTitle>
                    <Badge variant={story.status === 'published' ? 'default' : 'secondary'}>
                      {story.status}
                    </Badge>
                  </div>
                  <CardDescription className="line-clamp-2 text-xs mt-1">
                    {story.synopsis}
                  </CardDescription>
                </CardHeader>
                <CardContent className="p-4 pt-2 flex gap-2 text-xs text-muted-foreground">
                  <span>{story.genre}</span>
                  <span>•</span>
                  <span>{story.tone}</span>
                </CardContent>
              </Card>
            ))}
          </div>
        </ScrollArea>
      </div>

      {/* Story Detail */}
      <div className="flex-1">
        {selectedStory ? (
          <Card className="h-full flex flex-col">
            <CardHeader>
              <div className="flex justify-between items-start">
                <div>
                  <CardTitle className="text-2xl">{selectedStory.title}</CardTitle>
                  <CardDescription className="mt-2 text-base">
                    {selectedStory.synopsis}
                  </CardDescription>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" size="icon"><Edit className="h-4 w-4" /></Button>
                  <Button variant="destructive" size="icon"><Trash2 className="h-4 w-4" /></Button>
                </div>
              </div>
              <div className="flex gap-2 mt-4">
                <Badge variant="outline">{selectedStory.genre}</Badge>
                <Badge variant="outline">{selectedStory.tone}</Badge>
                <Badge variant="outline">更新于 {new Date(selectedStory.updatedAt).toLocaleDateString()}</Badge>
              </div>
            </CardHeader>
            <CardContent className="flex-1 overflow-y-auto">
              <div className="space-y-6">
                <div>
                  <h4 className="font-semibold mb-3 flex items-center gap-2">
                    <BookOpen className="h-4 w-4" /> 角色列表
                  </h4>
                  <div className="grid grid-cols-2 gap-4">
                    {/* Mock Characters */}
                    <div className="border rounded p-3 bg-muted/30">
                      <div className="font-medium">主角</div>
                      <div className="text-sm text-muted-foreground">性格坚毅，背负着沉重的过去...</div>
                    </div>
                    <div className="border rounded p-3 bg-muted/30 flex items-center justify-center text-muted-foreground border-dashed">
                      + 添加角色
                    </div>
                  </div>
                </div>
                
                <div>
                  <h4 className="font-semibold mb-3">世界观设定</h4>
                  <p className="text-sm text-muted-foreground">暂未关联世界观</p>
                </div>
              </div>
            </CardContent>
          </Card>
        ) : (
          <div className="h-full flex items-center justify-center text-muted-foreground border-2 border-dashed rounded-lg">
            请选择一个故事查看详情
          </div>
        )}
      </div>
    </div>
  );
};

export default StoryManager;