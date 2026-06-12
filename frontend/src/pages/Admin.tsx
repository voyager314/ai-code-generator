import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { userApi } from '@/api';
import type { UserVO, UserAddRequest } from '@/types';

// ─── AddUserDialog ────────────────────────────────────────────────────────────

function AddUserDialog({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: () => void;
}) {
  const [form, setForm] = useState<UserAddRequest>({
    userAccount: '',
    userName: '',
    userRole: 'user',
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.userAccount.trim()) return;
    setLoading(true);
    try {
      await userApi.add(form);
      onCreated();
      onClose();
    } catch (err: any) {
      alert(err.message || '创建失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <Card className="w-full max-w-sm mx-4">
        <CardHeader>
          <CardTitle>新增用户</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-3">
            <Input
              placeholder="账号（必填）"
              value={form.userAccount}
              onChange={(e) => setForm({ ...form, userAccount: e.target.value })}
              required
            />
            <Input
              placeholder="昵称（可选）"
              value={form.userName ?? ''}
              onChange={(e) => setForm({ ...form, userName: e.target.value })}
            />
            <select
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={form.userRole}
              onChange={(e) =>
                setForm({ ...form, userRole: e.target.value as 'user' | 'admin' })
              }
            >
              <option value="user">普通用户</option>
              <option value="admin">管理员</option>
            </select>
            <p className="text-xs text-gray-400">默认密码：123456</p>
            <div className="flex gap-2 pt-1">
              <Button type="submit" disabled={loading}>
                {loading ? '创建中...' : '创建'}
              </Button>
              <Button type="button" variant="outline" onClick={onClose}>
                取消
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

// ─── RoleBadge ────────────────────────────────────────────────────────────────

function RoleBadge({ role }: { role: string }) {
  const isAdmin = role === 'admin';
  return (
    <span
      className={`inline-block px-2 py-0.5 text-xs rounded font-medium ${
        isAdmin ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600'
      }`}
    >
      {isAdmin ? '管理员' : '普通用户'}
    </span>
  );
}

// ─── Admin page ───────────────────────────────────────────────────────────────

export default function Admin() {
  const navigate = useNavigate();
  const [users, setUsers] = useState<UserVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [showAdd, setShowAdd] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      // GET /user/list — admin only, returns full user list
      const res = await userApi.getList();
      setUsers(res.data);
    } catch (err: any) {
      alert(err.message || '加载失败');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleRole = async (user: UserVO) => {
    const newRole = user.userRole === 'admin' ? 'user' : 'admin';
    if (!confirm(`将「${user.userAccount}」的角色改为 ${newRole === 'admin' ? '管理员' : '普通用户'}？`)) return;
    try {
      await userApi.update({ id: user.id, userRole: newRole });
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, userRole: newRole } : u))
      );
    } catch (err: any) {
      alert(err.message || '操作失败');
    }
  };

  const handleDelete = async (user: UserVO) => {
    if (!confirm(`确认删除用户「${user.userAccount}」？此操作不可撤销。`)) return;
    try {
      // DELETE /user/remove/{id}
      await userApi.remove(user.id);
      setUsers((prev) => prev.filter((u) => u.id !== user.id));
    } catch (err: any) {
      alert(err.message || '删除失败');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {showAdd && (
        <AddUserDialog onClose={() => setShowAdd(false)} onCreated={loadUsers} />
      )}

      <header className="bg-white border-b px-6 py-4 flex justify-between items-center">
        <h1 className="text-xl font-bold">管理后台</h1>
        <div className="flex gap-2">
          <Button size="sm" onClick={() => setShowAdd(true)}>
            新增用户
          </Button>
          <Button variant="ghost" size="sm" onClick={() => navigate('/')}>
            返回
          </Button>
        </div>
      </header>

      <div className="max-w-6xl mx-auto p-6">
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>用户管理</CardTitle>
              <span className="text-sm text-gray-400">共 {users.length} 人</span>
            </div>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="text-center py-10 text-gray-400">加载中...</div>
            ) : users.length === 0 ? (
              <div className="text-center py-10 text-gray-400">暂无用户</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left text-gray-500">
                      <th className="py-3 px-4 font-medium">ID</th>
                      <th className="py-3 px-4 font-medium">账号</th>
                      <th className="py-3 px-4 font-medium">昵称</th>
                      <th className="py-3 px-4 font-medium">角色</th>
                      <th className="py-3 px-4 font-medium">注册时间</th>
                      <th className="py-3 px-4 font-medium">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((user) => (
                      <tr key={user.id} className="border-b hover:bg-gray-50 transition-colors">
                        <td className="py-3 px-4 text-gray-400">{user.id}</td>
                        <td className="py-3 px-4 font-medium">{user.userAccount}</td>
                        <td className="py-3 px-4 text-gray-500">{user.userName || '—'}</td>
                        <td className="py-3 px-4">
                          <RoleBadge role={user.userRole} />
                        </td>
                        <td className="py-3 px-4 text-gray-400">
                          {user.createTime?.split('T')[0] ?? '—'}
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleToggleRole(user)}
                            >
                              切换角色
                            </Button>
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleDelete(user)}
                            >
                              删除
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
