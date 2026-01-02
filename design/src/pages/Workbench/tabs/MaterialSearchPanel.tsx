import { useState } from "react";
import { api } from "@/lib/mock-api";
import { Material } from "@/types";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Search, Loader2 } from "lucide-react";

const MaterialSearchPanel = () => {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Material[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  const handleSearch = async () => {
    if (!query) return;
    setIsSearching(true);
    try {
      const data = await api.materials.search(query);
      setResults(data);
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex gap-2">
        <Input 
          placeholder="搜索素材库..." 
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          className="h-12 text-lg"
        />
        <Button size="lg" className="h-12 px-8" onClick={handleSearch} disabled={isSearching}>
          {isSearching ? <Loader2 className="h-5 w-5 animate-spin" /> : <Search className="h-5 w-5" />}
        </Button>
      </div>

      <div className="space-y-4">
        {results.length > 0 ? (
          results.map(material => (
            <Card key={material.id} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-2">
                <div className="flex justify-between items-start">
                  <CardTitle className="text-lg">{material.title}</CardTitle>
                  <Badge variant="outline">{material.type}</Badge>
                </div>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground line-clamp-2 mb-3">
                  {material.content}
                </p>
                <div className="flex gap-2">
                  {material.tags.map(tag => (
                    <Badge key={tag} variant="secondary" className="text-xs">{tag}</Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          ))
        ) : (
          <div className="text-center py-12 text-muted-foreground">
            {query ? "未找到相关素材" : "输入关键词开始搜索"}
          </div>
        )}
      </div>
    </div>
  );
};

export default MaterialSearchPanel;