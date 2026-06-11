import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { appApi } from '@/api';
import type { AppVO } from '@/types';

export default function Home() {
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [apps, setApps] = useState<AppVO[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    loadApps();
  }, []);

  const loadApps = async () => {
    try {
      const res = await appApi.getMyList({ pageNum: 1, pageSize: 8 });
      setApps(res.data.records);
    } catch (err) {
      console.error('加载应用列表失败:', err);
    }
  };

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    setLoading(true);
    try {
      const res = await appApi.create({
        initPrompt: input.trim(),
        appName: `应用_${Date.now()}`,
      });
      navigate(`/app/${res.data}`);
    } catch (err: any) {
      alert(err.message || '创建失败');
      setLoading(false);
    }
  };

  const examples = [
    '装修风电商页面',
    '企业网站',
    '电商运营后台',
    '编辑器验证比',
  ];

  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'linear-gradient(135deg, #e0f7fa 0%, #b3e5fc 50%, #81d4fa 100%)' }}>
      <header className="px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 bg-teal-600 rounded-lg" />
          <span className="text-xl font-bold">NoCode</span>
        </div>
        <nav className="flex gap-6 text-sm">
          <a href="#" className="hover:underline">使用文档</a>
          <a href="#" className="hover:underline">交流社区</a>
          <a href="#" className="hover:underline">更多产品</a>
        </nav>
        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-orange-400 to-pink-500" />
      </header>

      <div className="flex-1 flex flex-col items-center justify-center px-6 -mt-20">
        <h1 className="text-5xl font-bold mb-2 flex items-center gap-2">
          一句话
          <span className="w-10 h-10 bg-teal-600 rounded-lg inline-flex items-center justify-center text-white">AI</span>
          呈所想
        </h1>
        <p className="text-gray-600 mb-12">与 AI 对话轻松创建应用和网站</p>

        <div className="w-full max-w-2xl bg-white rounded-2xl shadow-lg p-6 mb-6">
          <Input
            placeholder="使用 NoCode 创建一个有趣的小游戏、游戏玩"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            disabled={loading}
            className="mb-4 text-base"
          />
          <div className="flex items-center justify-between">
            <div className="flex gap-2 text-sm text-gray-500">
              <button className="hover:text-gray-700">上传</button>
              <button className="hover:text-gray-700">识图</button>
            </div>
            <Button
              onClick={handleSend}
              disabled={loading || !input.trim()}
              className="rounded-full w-10 h-10 p-0"
            >
              ↑
            </Button>
          </div>
        </div>

        <div className="flex gap-3">
          {examples.map((text) => (
            <button
              key={text}
              onClick={() => setInput(text)}
              className="px-4 py-2 text-sm bg-white/60 backdrop-blur-sm rounded-full hover:bg-white transition"
            >
              {text}
            </button>
          ))}
        </div>
      </div>

      <div className="px-6 pb-12">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-2xl font-bold mb-6">我的作品</h2>
          <div className="grid grid-cols-4 gap-4">
            {apps.length === 0 ? (
              <div className="col-span-4 text-center text-gray-500 py-8">暂无应用</div>
            ) : (
              apps.map((app) => (
                <div
                  key={app.id}
                  onClick={() => navigate(`/app/${app.id}`)}
                  className="bg-white rounded-lg shadow p-4 aspect-[4/3] cursor-pointer hover:shadow-lg transition-shadow"
                >
                  <div className="w-full h-32 bg-gray-200 rounded mb-2 flex items-center justify-center text-gray-400">
                    {app.codeGenType || 'APP'}
                  </div>
                  <div className="text-sm font-medium truncate">{app.appName || `应用 ${app.id}`}</div>
                  <div className="text-xs text-gray-500 mt-1">{app.createTime?.split('T')[0]}</div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
