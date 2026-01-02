import { WorldDetail, WorldModuleDefinition, WorldModuleData } from "@/types";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Sparkles } from "lucide-react";

interface WorldModuleEditorProps {
  definitions: WorldModuleDefinition[];
  moduleData: WorldModuleData[];
  onUpdateModule: (moduleKey: string, fieldKey: string, value: string) => void;
  onAutoGenerate: (moduleKey: string) => void;
}

const WorldModuleEditor = ({ definitions, moduleData, onUpdateModule, onAutoGenerate }: WorldModuleEditorProps) => {
  const getFieldValue = (moduleKey: string, fieldKey: string) => {
    const module = moduleData.find(m => m.key === moduleKey);
    return module?.fields[fieldKey] || "";
  };

  return (
    <Tabs defaultValue={definitions[0]?.key} className="w-full">
      <TabsList className="w-full justify-start h-auto flex-wrap gap-2 bg-transparent p-0 mb-4">
        {definitions.map(def => (
          <TabsTrigger 
            key={def.key} 
            value={def.key}
            className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground border bg-card"
          >
            {def.label}
          </TabsTrigger>
        ))}
      </TabsList>

      {definitions.map(def => (
        <TabsContent key={def.key} value={def.key}>
          <Card>
            <CardHeader>
              <div className="flex justify-between items-start">
                <div>
                  <CardTitle>{def.label}</CardTitle>
                  <CardDescription>{def.description}</CardDescription>
                </div>
                <Button variant="outline" size="sm" onClick={() => onAutoGenerate(def.key)}>
                  <Sparkles className="mr-2 h-4 w-4" />
                  AI 生成此模块
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-6">
              {def.fields.map(field => (
                <div key={field.key} className="space-y-2">
                  <div className="flex justify-between">
                    <Label>{field.label}</Label>
                    <Button variant="ghost" size="sm" className="h-6 text-xs text-muted-foreground">
                      <Sparkles className="mr-1 h-3 w-3" /> 润色
                    </Button>
                  </div>
                  {field.type === 'textarea' ? (
                    <Textarea 
                      value={getFieldValue(def.key, field.key)}
                      onChange={(e) => onUpdateModule(def.key, field.key, e.target.value)}
                      placeholder={field.placeholder}
                      className="min-h-[100px]"
                    />
                  ) : (
                    <Input 
                      value={getFieldValue(def.key, field.key)}
                      onChange={(e) => onUpdateModule(def.key, field.key, e.target.value)}
                      placeholder={field.placeholder}
                    />
                  )}
                  {field.description && (
                    <p className="text-xs text-muted-foreground">{field.description}</p>
                  )}
                </div>
              ))}
            </CardContent>
          </Card>
        </TabsContent>
      ))}
    </Tabs>
  );
};

export default WorldModuleEditor;