import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ArrowLeft } from "lucide-react";
import { useNavigate } from "react-router-dom";

const PromptHelpPage = () => {
  const navigate = useNavigate();

  const variables = [
    { name: "{title}", desc: "故事标题" },
    { name: "{synopsis}", desc: "故事梗概" },
    { name: "{genre}", desc: "类型流派" },
    { name: "{tone}", desc: "基调风格" },
    { name: "{chapterNumber}", desc: "当前章节号" },
    { name: "{sceneDescription}", desc: "场景描述" },
    { name: "{instruction}", desc: "用户的修改指示" },
  ];

  return (
    <div className="container mx-auto py-8 max-w-4xl">
      <Button variant="ghost" onClick={() => navigate("/settings")} className="mb-4">
        <ArrowLeft className="mr-2 h-4 w-4" /> 返回设置
      </Button>
      
      <h1 className="text-3xl font-bold mb-6">提示词变量指南</h1>
      
      <Card className="mb-8">
        <CardHeader>
          <CardTitle>可用变量表</CardTitle>
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

      <Card>
        <CardHeader>
          <CardTitle>编写技巧</CardTitle>
        </CardHeader>
        <CardContent className="prose dark:prose-invert">
          <ul>
            <li>使用 <code>{`{variable}`}</code> 语法插入动态内容。</li>
            <li>明确指定 AI 的角色（如“你是一个科幻小说家”）。</li>
            <li>在提示词中包含具体的格式要求（如“请使用 JSON 格式返回”）。</li>
          </ul>
        </CardContent>
      </Card>
    </div>
  );
};

export default PromptHelpPage;