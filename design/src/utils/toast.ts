import { toast } from "@/components/ui/use-toast";

export const showSuccess = (message: string) =>
  toast({ title: "提示", description: message });

export const showError = (message: string) =>
  toast({ title: "错误", description: message, variant: "destructive" });
