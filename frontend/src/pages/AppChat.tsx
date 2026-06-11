import { useEffect, useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import CodeViewer from '@/components/CodeViewer';
import { appApi, chatApi } from '@/api';
import type { AppDetailVO } from '@/types';

export default function AppChat() {
  const { id } = useParams<{ id: string }>();
  const [app, setApp] = useState<AppDetailVO | null>(null);
  const [messages, setMessages] = useState<{ role: string; content: string }[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [view, setView] = useState<'code' | 'preview'>('code');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    loadApp();
    loadHistory();
  }, [id]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadApp = async () => {
    try {
      const res = await appApi.getDetail(Number(id));
      setApp(res.data);
    } catch (err: any) {
      alert(err.message);
    }
  };

  const loadHistory = async () => {
    try {
      const res = await chatApi.getHistory({ appId: Number(id), pageNum: 1, pageSize: 50 });
      const msgs = res.data.records.map((h) => ({ role: h.role, content: h.content }));
      setMessages(msgs);
    } catch (err: any) {
      console.error(err);
    }
  };

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMsg = input.trim();
    setInput('');
    setMessages((prev) => [...prev, { role: 'user', content: userMsg }]);
    setLoading(true);

    try {
      const es = new EventSource(
        `/api/app/chat/gen/code?appId=${id}&msg=${encodeURIComponent(userMsg)}&agent=false`
      );

      let aiContent = '';

      es.addEventListener('message', (e) => {
        try {
          const data = JSON.parse(e.data);
          if (data.d) {
            aiContent += data.d;
            setMessages((prev) => {
              const copy = [...prev];
              if (copy[copy.length - 1]?.role === 'ai') {
                copy[copy.length - 1].content = aiContent;
              } else {
                copy.push({ role: 'ai', content: aiContent });
              }
              return copy;
            });
          }
        } catch (err) {
          console.error(err);
        }
      });

      es.addEventListener('done', () => {
        es.close();
        setLoading(false);
      });

      es.onerror = () => {
        es.close();
        setLoading(false);
        alert('连接失败');
      };
    } catch (err: any) {
      setLoading(false);
      alert(err.message);
    }
  };

  const handleDeploy = async () => {
    try {
      const res = await appApi.deploy(Number(id));
      alert(`部署成功：${res.data}`);
      loadApp();
    } catch (err: any) {
      alert(err.message);
    }
  };

  return (
    <div className="h-screen flex flex-col bg-white">
      <header className="border-b px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-6 h-6 bg-teal-600 rounded" />
          <span className="text-sm">{app?.appName || '加载中...'}</span>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => appApi.download(Number(id))}>
            下载
          </Button>
          <Button variant="outline" size="sm" onClick={handleDeploy}>
            部署
          </Button>
        </div>
      </header>

      <div className="flex-1 flex overflow-hidden">
        <div className="w-[40%] border-r flex flex-col">
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {messages.map((msg, i) => (
              <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[85%] px-3 py-2 rounded text-sm ${
                    msg.role === 'user'
                      ? 'bg-blue-500 text-white'
                      : 'bg-gray-100 text-gray-900'
                  }`}
                >
                  <pre className="whitespace-pre-wrap font-sans">{msg.content}</pre>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>

          <div className="border-t p-4">
            <div className="flex gap-2 mb-2 text-xs text-gray-500">
              <button className="hover:text-gray-700">上传</button>
              <button className="hover:text-gray-700">识图</button>
              <button className="hover:text-gray-700">编辑</button>
            </div>
            <div className="flex gap-2">
              <Input
                placeholder="描述你的想法..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                disabled={loading}
                className="text-sm"
              />
              <Button
                onClick={handleSend}
                disabled={loading || !input.trim()}
                className="rounded-full w-9 h-9 p-0 shrink-0"
              >
                ↑
              </Button>
            </div>
          </div>
        </div>

        <div className="flex-1 flex flex-col">
          <div className="border-b px-4 py-2 flex gap-4">
            <button
              onClick={() => setView('code')}
              className={`text-sm pb-2 border-b-2 transition ${
                view === 'code' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-600'
              }`}
            >
              代码
            </button>
            <button
              onClick={() => setView('preview')}
              className={`text-sm pb-2 border-b-2 transition ${
                view === 'preview' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-600'
              }`}
            >
              预览
            </button>
          </div>

          <div className="flex-1 overflow-hidden">
            {view === 'code' ? (
              <CodeViewer appId={Number(id)} />
            ) : (
              <div className="h-full flex items-center justify-center bg-gray-50 text-gray-400 text-sm">
                预览功能暂未实现
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
