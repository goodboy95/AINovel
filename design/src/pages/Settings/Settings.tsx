import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Settings2, Terminal, Globe } from "lucide-react";

import ModelSettings from "./tabs/ModelSettings";
import WorkspacePrompts from "./tabs/WorkspacePrompts";
import WorldPrompts from "./tabs/WorldPrompts";

const Settings = () => {
  const [activeTab, setActiveTab] = useState("model");

  return (
    <div className="h-full flex flex-col space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">系统设置</h1>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 flex flex-col">
        <TabsList className="grid w-full grid-cols-3 lg:w-[600px]">
          <TabsTrigger value="model" className="gap-2">
            <Settings2 className="h-4 w-4" /> 模型接入
          </TabsTrigger>
          <TabsTrigger value="workspace" className="gap-2">
            <Terminal className="h-4 w-4" /> 工作区提示词
          </TabsTrigger>
          <TabsTrigger value="world" className="gap-2">
            <Globe className="h-4 w-4" /> 世界观提示词
          </TabsTrigger>
        </TabsList>

        <div className="flex-1 mt-6">
          <TabsContent value="model" className="m-0">
            <ModelSettings />
          </TabsContent>
          <TabsContent value="workspace" className="m-0">
            <WorkspacePrompts />
          </TabsContent>
          <TabsContent value="world" className="m-0">
            <WorldPrompts />
          </TabsContent>
        </div>
      </Tabs>
    </div>
  );
};

export default Settings;