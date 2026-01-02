import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { ShieldCheck, Loader2, AlertTriangle } from "lucide-react";
import { cn } from "@/lib/utils";

interface CapJSProps {
  onVerify: (token: string) => void;
  className?: string;
}

const CapJS = ({ onVerify, className }: CapJSProps) => {
  const [status, setStatus] = useState<'idle' | 'computing' | 'verified'>('idle');

  const startVerification = () => {
    setStatus('computing');
    // Simulate PoW calculation delay (1.5s)
    setTimeout(() => {
      setStatus('verified');
      const mockToken = `capjs_token_${Date.now()}`;
      onVerify(mockToken);
    }, 1500);
  };

  return (
    <div className={cn("border rounded-md p-3 bg-muted/20 flex items-center justify-between", className)}>
      <div className="flex items-center gap-3">
        {status === 'idle' && (
          <div className="h-6 w-6 rounded-full border-2 border-muted-foreground/30" />
        )}
        {status === 'computing' && (
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
        )}
        {status === 'verified' && (
          <ShieldCheck className="h-6 w-6 text-green-600 animate-in zoom-in" />
        )}
        <div className="flex flex-col">
          <span className="text-sm font-medium">
            {status === 'idle' ? "点击进行人机验证" : status === 'computing' ? "正在计算安全哈希..." : "验证通过"}
          </span>
          <span className="text-[10px] text-muted-foreground">Protected by CapJS PoW</span>
        </div>
      </div>
      
      {status === 'idle' && (
        <Button type="button" variant="outline" size="sm" onClick={startVerification}>
          点击验证
        </Button>
      )}
    </div>
  );
};

export default CapJS;