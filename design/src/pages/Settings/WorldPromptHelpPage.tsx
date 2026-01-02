import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ArrowLeft } from "lucide-react";
import { useNavigate } from "react-router-dom";

const WorldPromptHelpPage = () => {
  const navigate = useNavigate();

  const variables = [
    { name: "{worldName}", desc: "世界名称" },
    { name: "{themes}", desc: "主题标签列表" },
    { name: "{creativeIntent}", desc: "创作意图" },
    { name: "{moduleName}", desc: "当前模块名称" },
    { name: "{fieldLabel}", desc: "当前字段名称" },
  ];

  return (
    <div className="container mx-auto py-8 max-w-4xl">
      <Button variant="ghost" onClick={() => navigate("/settings")} className="mb-4">
        <ArrowLeft className="mr-2 h-4 w-4" /> 返回设置
      </Button>
      
      <h1 className="text-3xl font-bold mb-6">世界观构建变量指南</h1>
      
      <Card className="mb-8">
        <CardHeader>
          <CardTitle>世界上下文变量</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>变量名</TableHead>
                <TableHead>说明</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {variables.map((v) => (
                <TableRow key={v.name}>
                  <TableCell className="font-mono text-primary">{v.name}</TableCell>
                  <TableCell>{v.desc}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
};

export default WorldPromptHelpPage;