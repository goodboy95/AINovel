import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";

const Pricing = () => {
  return (
    <div className="container mx-auto py-12">
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold mb-4">简单透明的定价</h1>
        <p className="text-muted-foreground">选择适合你创作旅程的方案。</p>
      </div>

      <div className="grid md:grid-cols-3 gap-8 max-w-5xl mx-auto">
        <div className="border rounded-xl p-6 flex flex-col">
          <h3 className="text-xl font-bold">新手版</h3>
          <div className="text-3xl font-bold my-4">
            ¥0<span className="text-sm font-normal text-muted-foreground">/月</span>
          </div>
          <ul className="space-y-2 mb-6 flex-1">
            <li>✓ 每日基础额度</li>
            <li>✓ 基础工作台</li>
            <li>✓ 素材管理</li>
          </ul>
          <Button variant="outline" className="w-full">
            开始使用
          </Button>
        </div>

        <div className="border-2 border-primary rounded-xl p-6 flex flex-col relative shadow-lg">
          <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-primary text-primary-foreground px-3 py-1 rounded-full text-xs">推荐</div>
          <h3 className="text-xl font-bold">专业版</h3>
          <div className="text-3xl font-bold my-4">
            ¥39<span className="text-sm font-normal text-muted-foreground">/月</span>
          </div>
          <ul className="space-y-2 mb-6 flex-1">
            <li>✓ 更高积分额度</li>
            <li>✓ 更完整的 AI 辅助</li>
            <li>✓ 后台管理与配置</li>
          </ul>
          <Button className="w-full">立即订阅</Button>
        </div>

        <div className="border rounded-xl p-6 flex flex-col">
          <h3 className="text-xl font-bold">畅享版</h3>
          <div className="text-3xl font-bold my-4">
            ¥99<span className="text-sm font-normal text-muted-foreground">/月</span>
          </div>
          <ul className="space-y-2 mb-6 flex-1">
            <li>✓ 更高并发与额度</li>
            <li>✓ 优先支持</li>
            <li>✓ 新功能抢先体验</li>
          </ul>
          <Button variant="outline" className="w-full">
            联系销售
          </Button>
        </div>
      </div>

      <div className="text-center mt-8">
        <Link to="/" className="text-sm text-muted-foreground hover:underline">
          返回首页
        </Link>
      </div>
    </div>
  );
};

export default Pricing;

