import { useEditor, EditorContent, BubbleMenu } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Placeholder from "@tiptap/extension-placeholder";
import BubbleMenuExtension from "@tiptap/extension-bubble-menu";
import EditorToolbar from "./EditorToolbar";
import { cn } from "@/lib/utils";
import { FontType, WidthType, ThemeType } from "./AppearanceSettings";
import { SlashCommand, suggestionOptions } from "./slash-command";
import { Button } from "@/components/ui/button";
import { 
  Bold, 
  Italic, 
  Strikethrough, 
  Sparkles, 
  Wand2,
  Highlighter
} from "lucide-react";
import { Separator } from "@/components/ui/separator";
import { showSuccess, showError } from "@/utils/toast";
import { useState } from "react";
import AiRefineDialog from "@/components/ai/AiRefineDialog";
import { api } from "@/lib/mock-api";
import { useAuth } from "@/contexts/AuthContext";

interface TiptapEditorProps {
  content: string;
  onChange: (content: string) => void;
  editable?: boolean;
  className?: string;
  zenMode?: boolean;
  font?: FontType;
  width?: WidthType;
  theme?: ThemeType;
}

const TiptapEditor = ({ 
  content, 
  onChange, 
  editable = true, 
  className,
  zenMode = false,
  font = "serif",
  width = "medium",
  theme = "light"
}: TiptapEditorProps) => {
  const { refreshProfile, user } = useAuth();
  
  // AI Refine State
  const [isRefineOpen, setIsRefineOpen] = useState(false);
  const [originalText, setOriginalText] = useState("");
  const [refinedText, setRefinedText] = useState("");
  const [refineCost, setRefineCost] = useState(0);

  const editor = useEditor({
    extensions: [
      StarterKit,
      Placeholder.configure({
        placeholder: "开始你的故事... (输入 '/' 唤起 AI 指令)",
      }),
      SlashCommand.configure({
        suggestion: suggestionOptions,
      }),
      BubbleMenuExtension.configure({
        pluginKey: 'bubbleMenu',
      }),
    ],
    content,
    editable,
    onUpdate: ({ editor }) => {
      onChange(editor.getHTML());
    },
    editorProps: {
      attributes: {
        class: cn(
          "prose dark:prose-invert max-w-none focus:outline-none min-h-[500px] px-8 py-6 transition-all duration-300",
          font === "sans" && "font-sans prose-p:font-sans",
          font === "serif" && "font-serif prose-p:font-serif",
          font === "mono" && "font-mono prose-p:font-mono",
          "prose-headings:font-bold prose-h1:text-3xl prose-h2:text-2xl",
          "prose-p:leading-relaxed prose-p:mb-6",
          className
        ),
      },
    },
  });

  const handleAIPolish = async () => {
    if (!editor) return;
    if (user && user.credits < 0) {
      showError("积分不足（已透支），请先充值/兑换后再使用 AI");
      return;
    }
    const selection = editor.state.selection;
    const text = editor.state.doc.textBetween(selection.from, selection.to);
    
    if (text.length < 2) {
      showError("请选择更多文本");
      return;
    }

    setOriginalText(text);
    showSuccess("AI 正在润色，请稍候...");
    
    try {
      // Use default model for quick refine
      const models = await api.ai.getModels();
      const modelId = models[0]?.id || "m1";
      
      const res = await api.ai.refine(text, "使其更具文学性", modelId);
      setRefinedText(res.result);
      setRefineCost(res.usage.cost);
      setIsRefineOpen(true);
      await refreshProfile();
    } catch (error) {
      showError("AI 请求失败");
    }
  };

  const confirmRefine = () => {
    if (editor) {
      editor.chain().focus().insertContent(refinedText).run();
    }
  };

  const getContainerStyle = () => {
    const styles = {
      maxWidth: "",
      background: "",
      color: "",
    };

    switch (width) {
      case "narrow": styles.maxWidth = "42rem"; break;
      case "medium": styles.maxWidth = "48rem"; break;
      case "wide": styles.maxWidth = "64rem"; break;
    }

    if (theme === "parchment") {
      styles.background = "#f5e6c8";
      styles.color = "#4a3b2a";
    } else if (theme === "hacker") {
      styles.background = "#000000";
      styles.color = "#00ff00";
    }

    return styles;
  };

  const containerStyles = getContainerStyle();

  return (
    <div className="flex flex-col w-full h-full relative">
      {!zenMode && <EditorToolbar editor={editor} />}
      
      {/* AI Refine Dialog */}
      <AiRefineDialog 
        open={isRefineOpen} 
        onOpenChange={setIsRefineOpen}
        originalText={originalText}
        refinedText={refinedText}
        onConfirm={confirmRefine}
        cost={refineCost}
      />

      {/* Bubble Menu */}
      {editor && (
        <BubbleMenu 
          editor={editor} 
          tippyOptions={{ duration: 100 }}
          className="flex items-center gap-1 p-1 rounded-lg border bg-popover shadow-md animate-in fade-in zoom-in-95"
        >
          <Button
            variant="ghost"
            size="sm"
            className="h-8 w-8 p-0"
            onClick={() => editor.chain().focus().toggleBold().run()}
            data-state={editor.isActive('bold') ? 'on' : 'off'}
          >
            <Bold className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-8 w-8 p-0"
            onClick={() => editor.chain().focus().toggleItalic().run()}
            data-state={editor.isActive('italic') ? 'on' : 'off'}
          >
            <Italic className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-8 w-8 p-0"
            onClick={() => editor.chain().focus().toggleStrike().run()}
            data-state={editor.isActive('strike') ? 'on' : 'off'}
          >
            <Strikethrough className="h-4 w-4" />
          </Button>
          
          <Separator orientation="vertical" className="h-6 mx-1" />
          
          <Button
            variant="ghost"
            size="sm"
            className="h-8 px-2 text-purple-600 hover:text-purple-700 hover:bg-purple-50"
            onClick={handleAIPolish}
          >
            <Sparkles className="h-3 w-3 mr-1" />
            <span className="text-xs font-medium">润色</span>
          </Button>
        </BubbleMenu>
      )}

      <div 
        className={cn(
          "flex-1 overflow-y-auto flex justify-center transition-colors duration-300",
          theme === "parchment" && "bg-[#f5e6c8]",
          theme === "hacker" && "bg-black"
        )} 
        onClick={() => editor?.chain().focus().run()}
      >
        <div 
          className={cn(
            "w-full py-8 transition-all duration-300",
            theme === "parchment" && "prose-p:text-[#4a3b2a] prose-headings:text-[#2c241b]",
            theme === "hacker" && "prose-p:text-[#00ff00] prose-headings:text-[#00ff00] prose-strong:text-[#00ff00]"
          )}
          style={{ maxWidth: containerStyles.maxWidth }}
        >
          <EditorContent editor={editor} />
        </div>
      </div>
    </div>
  );
};

export default TiptapEditor;
