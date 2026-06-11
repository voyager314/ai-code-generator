import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { appApi, userApi } from '@/api';
import { useUserStore } from '@/store/user';
import type { AppVO } from '@/types';

export default function AppList() {
  const navigate = useNavigate();
  const user = useUserStore((s) => s.user);
  const setUser = useUserStore((s) => s.setUser);
  const [tab, setTab] = useState<'my' | 'star'>('my');
  const [apps, setApps] = useState<AppVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ appName: '', initPrompt: '' });

  useEffect(() => {
    loadApps();
  }, [tab]);

  const loadApps = async () => {
    setLoading(true);
    try {
      const res = tab === 'my'
        ? await appApi.getMyList({ pageNum: 1, pageSize: 20 })
        : await appApi.getStarList({ pageNum: 1, pageSize: 20 });
      setApps(res.data.records);
    } catch (err: any) {
      alert(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await appApi.create(form);
      setShowCreate(false);
      setForm({ appName: '', initPrompt: '' });
      loadApps();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const handleLogout = async () => {
    await userApi.logout();
    setUser(null);
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-6 py-4 flex justify-between items-center">
        <h1 className="text-xl font-bold">Wise Code</h1>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{user?.userAccount}</span>
          {user?.userRole === 'admin' && (
            <Button variant="outline" size="sm" onClick={() => navigate('/admin')}>
              管理后台
            </Button>
          )}
          <Button variant="ghost" size="sm" onClick={handleLogout}>
            登出
          </Button>
        </div>
      </header>

      <div className="max-w-6xl mx-auto p-6">
        <div className="flex justify-between items-center mb-6">
          <div className="flex gap-2">
            <Button variant={tab === 'my' ? 'default' : 'outline'} onClick={() => setTab('my')}>
              我的应用
            </Button>
            <Button variant={tab === 'star' ? 'default' : 'outline'} onClick={() => setTab('star')}>
              精选应用
            </Button>
          </div>
          {tab === 'my' && (
            <Button onClick={() => setShowCreate(true)}>创建应用</Button>
          )}
        </div>

        {showCreate && (
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>创建应用</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreate} className="space-y-4">
                <Input
                  placeholder="应用名称（可选）"
                  value={form.appName}
                  onChange={(e) => setForm({ ...form, appName: e.target.value })}
                />
                <textarea
                  className="w-full min-h-32 rounded-md border border-input bg-background px-3 py-2 text-sm"
                  placeholder="初始化提示（必填）"
                  value={form.initPrompt}
                  onChange={(e) => setForm({ ...form, initPrompt: e.target.value })}
                  required
                />
                <div className="flex gap-2">
                  <Button type="submit">创建</Button>
                  <Button type="button" variant="outline" onClick={() => setShowCreate(false)}>
                    取消
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        )}

        {loading ? (
          <div className="text-center py-12">加载中...</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {apps.map((app) => (
              <Card key={app.id} className="cursor-pointer hover:shadow-md transition-shadow" onClick={() => navigate(`/app/${app.id}`)}>
                <CardContent className="p-6">
                  <h3 className="font-semibold mb-2">{app.appName || `应用 ${app.id}`}</h3>
                  <p className="text-sm text-gray-500">类型：{app.codeGenType || '未知'}</p>
                  <p className="text-sm text-gray-500">创建于：{app.createTime?.split('T')[0]}</p>
                  {app.deployKey && (
                    <span className="inline-block mt-2 px-2 py-1 bg-green-100 text-green-700 text-xs rounded">
                      已部署
                    </span>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
