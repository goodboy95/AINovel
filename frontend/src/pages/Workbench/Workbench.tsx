import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Sparkles, Book, FileText, PenTool, Search } from "lucide-react";

// Tabs Components
import StoryConception from "./tabs/StoryConception";
import StoryManager from "./tabs/StoryManager";
import OutlineWorkbench from "./tabs/OutlineWorkbench";
import ManuscriptWriter from "./tabs/ManuscriptWriter";
import MaterialSearchPanel from "./tabs/MaterialSearchPanel";

const Workbench = () => {
  const [activeTab, setActiveTab] = useState("conception");

  return (
    <div className="h-full flex flex-col space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">创作工作台</h1>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 flex flex-col">
        <TabsList className="grid w-full grid-cols-5 lg:w-[800px]">
          <TabsTrigger value="conception" className="gap-2">
            <Sparkles className="h-4 w-4" /> 故事构思
          </TabsTrigger>
          <TabsTrigger value="stories" className="gap-2">
            <Book className="h-4 w-4" /> 故事管理
          </TabsTrigger>
          <TabsTrigger value="outline" className="gap-2">
            <FileText className="h-4 w-4" /> 大纲编排
          </TabsTrigger>
          <TabsTrigger value="writing" className="gap-2">
            <PenTool className="h-4 w-4" /> 小说创作
          </TabsTrigger>
          <TabsTrigger value="search" className="gap-2">
            <Search className="h-4 w-4" /> 素材检索
          </TabsTrigger>
        </TabsList>

        <div className="flex-1 mt-6 bg-card rounded-xl border shadow-sm p-6 min-h-[500px]">
          <TabsContent value="conception" className="h-full m-0 border-0 p-0">
            <StoryConception />
          </TabsContent>
          <TabsContent value="stories" className="h-full m-0 border-0 p-0">
            <StoryManager />
          </TabsContent>
          <TabsContent value="outline" className="h-full m-0 border-0 p-0">
            <OutlineWorkbench />
          </TabsContent>
          <TabsContent value="writing" className="h-full m-0 border-0 p-0">
            <ManuscriptWriter />
          </TabsContent>
          <TabsContent value="search" className="h-full m-0 border-0 p-0">
            <MaterialSearchPanel />
          </TabsContent>
        </div>
      </Tabs>
    </div>
  );
};

export default Workbench;