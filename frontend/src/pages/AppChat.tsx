import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { appApi, chatApi } from '@/api';
import type { AppDetailVO } from '@/types';

export default function AppChat() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [app, setApp] = useState<AppDetailVO | null>(null);
  const [messages, setMessages] = useState<{ role: string; content: string }[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [deepThink, setDeepThink] = useState(false);
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
        `/api/app/chat/gen/code?appId=${id}&msg=${encodeURIComponent(userMsg)}&agent=${deepThink}`
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
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <header className="bg-white border-b px-6 py-4 flex justify-between items-center">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="sm" onClick={() => navigate('/apps')}>
            ← 返回
          </Button>
          <h1 className="text-xl font-bold">{app?.appName || '应用详情'}</h1>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => appApi.download(Number(id))}>
            下载
          </Button>
          <Button size="sm" onClick={handleDeploy}>
            部署
          </Button>
        </div>
      </header>

      <div className="flex-1 max-w-4xl w-full mx-auto p-6 flex flex-col">
        <Card className="flex-1 flex flex-col mb-4">
          <CardContent className="flex-1 overflow-y-auto p-6 space-y-4">
            {messages.map((msg, i) => (
              <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[80%] px-4 py-2 rounded-lg ${
                    msg.role === 'user'
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-gray-100 text-gray-900'
                  }`}
                >
                  <pre className="whitespace-pre-wrap font-sans text-sm">{msg.content}</pre>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </CardContent>
        </Card>

        <div className="flex gap-2">
          <Button
            variant={deepThink ? 'default' : 'outline'}
            size="sm"
            onClick={() => setDeepThink(!deepThink)}
            className="shrink-0"
          >
            {deepThink ? '🧠 深度思考' : '💡 普通模式'}
          </Button>
          <Input
            placeholder="输入消息..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            disabled={loading}
          />
          <Button onClick={handleSend} disabled={loading || !input.trim()}>
            {loading ? '生成中...' : '发送'}
          </Button>
        </div>
      </div>
    </div>
  );
}
