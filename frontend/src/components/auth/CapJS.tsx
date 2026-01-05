import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { ShieldCheck, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { sha256Hex } from "@/lib/sha256";

interface CapJSProps {
  email: string;
  onVerify: (token: string) => void;
  className?: string;
}

const DIFFICULTY = 2; // must match backend accepted range; keep small for UX

const CapJS = ({ email, onVerify, className }: CapJSProps) => {
  const [status, setStatus] = useState<'idle' | 'computing' | 'verified'>('idle');

  useEffect(() => {
    setStatus("idle");
    onVerify("");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [email]);

  const startVerification = () => {
    if (!email) return;
    setStatus("computing");
    (async () => {
      const ts = Date.now();
      let nonce = 0;
      const prefix = "0".repeat(DIFFICULTY);
      for (; nonce < 200000; nonce++) {
        const hash = await sha256Hex(`${email}|${ts}|${nonce}`);
        if (hash.startsWith(prefix)) {
          const payload = { email, ts, nonce, difficulty: DIFFICULTY, hash };
          const token = btoa(JSON.stringify(payload));
          setStatus("verified");
          onVerify(token);
          return;
        }
      }
      setStatus("idle");
      onVerify("");
    })();
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
