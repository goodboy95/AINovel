import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { World, WorldDetail, WorldModuleDefinition } from "@/types";
import WorldSelectorPanel from "./components/WorldSelectorPanel";
import WorldMetadataForm from "./components/WorldMetadataForm";
import WorldModuleEditor from "./components/WorldModuleEditor";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/use-toast";
import { Save, UploadCloud, Loader2 } from "lucide-react";

const WorldBuilderPage = () => {
  const [worlds, setWorlds] = useState<World[]>([]);
  const [selectedWorldId, setSelectedWorldId] = useState<string | null>(null);
  const [worldDetail, setWorldDetail] = useState<WorldDetail | null>(null);
  const [definitions, setDefinitions] = useState<WorldModuleDefinition[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const { toast } = useToast();

  // Load initial data
  useEffect(() => {
    const init = async () => {
      const [worldsData, defsData] = await Promise.all([
        api.worlds.list(),
        api.worlds.getDefinitions()
      ]);
      setWorlds(worldsData);
      setDefinitions(defsData);
      if (worldsData.length > 0) {
        handleSelectWorld(worldsData[0].id);
      }
    };
    init();
  }, []);

  const handleSelectWorld = async (id: string) => {
    setSelectedWorldId(id);
    setIsLoading(true);
    try {
      const detail = await api.worlds.getDetail(id);
      setWorldDetail(detail);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateWorld = async () => {
    const newWorld = await api.worlds.create({
      name: "新世界草稿",
      tagline: "待编辑",
    });
    setWorlds([...worlds, newWorld]);
    handleSelectWorld(newWorld.id);
    toast({ title: "已创建新世界草稿" });
  };

  const handleUpdateMetadata = (field: keyof WorldDetail, value: any) => {
    if (!worldDetail) return;
    setWorldDetail({ ...worldDetail, [field]: value });
  };

  const handleUpdateModule = (moduleKey: string, fieldKey: string, value: string) => {
    if (!worldDetail) return;
    
    const newModules = [...(worldDetail.modules || [])];
    const moduleIndex = newModules.findIndex(m => m.key === moduleKey);
    
    if (moduleIndex >= 0) {
      newModules[moduleIndex] = {
        ...newModules[moduleIndex],
        fields: {
          ...newModules[moduleIndex].fields,
          [fieldKey]: value
        }
      };
    } else {
      newModules.push({
        key: moduleKey,
        fields: { [fieldKey]: value }
      });
    }
    
    setWorldDetail({ ...worldDetail, modules: newModules });
  };

  const handleSave = async () => {
    if (!worldDetail) return;
    setIsSaving(true);
    try {
      await api.worlds.update(worldDetail.id, worldDetail);
      
      // Update list if name changed
      setWorlds(worlds.map(w => w.id === worldDetail.id ? { ...w, name: worldDetail.name, tagline: worldDetail.tagline } : w));
      
      toast({ title: "保存成功" });
    } finally {
      setIsSaving(false);
    }
  };

  const handlePublish = () => {
    toast({ title: "发布流程模拟", description: "正在检查完整性并生成最终文档..." });
  };

  return (
    <div className="flex h-[calc(100vh-64px)] bg-background">
      <WorldSelectorPanel 
        worlds={worlds} 
        selectedWorldId={selectedWorldId} 
        onSelect={handleSelectWorld}
        onCreate={handleCreateWorld}
      />
      
      <div className="flex-1 flex flex-col h-full overflow-hidden relative">
        {isLoading ? (
          <div className="flex items-center justify-center h-full">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : worldDetail ? (
          <>
            <div className="flex-1 overflow-y-auto p-6 pb-24 space-y-6">
              <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">{worldDetail.name}</h1>
                <div className="flex gap-2">
                  <Button variant="outline" onClick={handleSave} disabled={isSaving}>
                    {isSaving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
                    保存草稿
                  </Button>
                  <Button onClick={handlePublish}>
                    <UploadCloud className="mr-2 h-4 w-4" />
                    发布版本
                  </Button>
                </div>
              </div>

              <WorldMetadataForm 
                data={worldDetail} 
                onChange={handleUpdateMetadata} 
              />
              
              <WorldModuleEditor 
                definitions={definitions}
                moduleData={worldDetail.modules || []}
                onUpdateModule={handleUpdateModule}
                onAutoGenerate={(key) => toast({ title: `AI 正在生成模块: ${key}` })}
              />
            </div>
          </>
        ) : (
          <div className="flex items-center justify-center h-full text-muted-foreground">
            请选择或创建一个世界
          </div>
        )}
      </div>
    </div>
  );
};

export default WorldBuilderPage;