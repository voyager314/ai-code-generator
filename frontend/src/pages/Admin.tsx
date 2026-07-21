import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api';
import type { UserVO, UserAddRequest } from '@/types';

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
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div className="w-full max-w-sm mx-4 rounded-xl border border-border bg-card p-6">
        <h3 className="text-lg font-semibold text-foreground mb-4">新增用户</h3>
        <form onSubmit={handleSubmit} className="space-y-3">
          <input
            placeholder="账号（必填）"
            value={form.userAccount}
            onChange={(e) => setForm({ ...form, userAccount: e.target.value })}
            required
            className="w-full rounded-lg border border-border bg-secondary px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />
          <input
            placeholder="昵称（可选）"
            value={form.userName ?? ''}
            onChange={(e) => setForm({ ...form, userName: e.target.value })}
            className="w-full rounded-lg border border-border bg-secondary px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />
          <select
            className="w-full rounded-lg border border-border bg-secondary px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            value={form.userRole}
            onChange={(e) =>
              setForm({ ...form, userRole: e.target.value as 'user' | 'admin' })
            }
          >
            <option value="user">普通用户</option>
            <option value="admin">管理员</option>
          </select>
          <p className="text-xs text-muted-foreground">默认密码：123456</p>
          <div className="flex gap-2 pt-1">
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/80 disabled:opacity-50"
            >
              {loading ? '创建中...' : '创建'}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-border bg-secondary px-4 py-2 text-sm font-medium text-foreground hover:bg-accent"
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function RoleBadge({ role }: { role: string }) {
  const isAdmin = role === 'admin';
  return (
    <span
      className={`inline-block px-2 py-0.5 text-xs rounded-md font-medium ${
        isAdmin ? 'bg-red-500/20 text-red-400' : 'bg-secondary text-muted-foreground'
      }`}
    >
      {isAdmin ? '管理员' : '普通用户'}
    </span>
  );
}

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
      await userApi.remove(user.id);
      setUsers((prev) => prev.filter((u) => u.id !== user.id));
    } catch (err: any) {
      alert(err.message || '删除失败');
    }
  };

  return (
    <div className="min-h-screen bg-background">
      {showAdd && (
        <AddUserDialog onClose={() => setShowAdd(false)} onCreated={loadUsers} />
      )}

      <header className="border-b border-border bg-card px-6 py-4 flex justify-between items-center">
        <h1 className="text-xl font-bold text-foreground">管理后台</h1>
        <div className="flex gap-2">
          <button
            onClick={() => setShowAdd(true)}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/80"
          >
            新增用户
          </button>
          <button
            onClick={() => navigate('/')}
            className="rounded-lg border border-border bg-secondary px-4 py-2 text-sm font-medium text-foreground hover:bg-accent"
          >
            返回
          </button>
        </div>
      </header>

      <div className="max-w-6xl mx-auto p-6">
        <div className="rounded-xl border border-border bg-card">
          <div className="flex items-center justify-between p-6 border-b border-border">
            <h3 className="text-lg font-semibold text-foreground">用户管理</h3>
            <span className="text-sm text-muted-foreground">共 {users.length} 人</span>
          </div>
          <div className="p-6">
            {loading ? (
              <div className="text-center py-10 text-muted-foreground">加载中...</div>
            ) : users.length === 0 ? (
              <div className="text-center py-10 text-muted-foreground">暂无用户</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-left text-muted-foreground">
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
                      <tr key={user.id} className="border-b border-border hover:bg-accent/50 transition-colors">
                        <td className="py-3 px-4 text-muted-foreground">{user.id}</td>
                        <td className="py-3 px-4 font-medium text-foreground">{user.userAccount}</td>
                        <td className="py-3 px-4 text-muted-foreground">{user.userName || '—'}</td>
                        <td className="py-3 px-4">
                          <RoleBadge role={user.userRole} />
                        </td>
                        <td className="py-3 px-4 text-muted-foreground">
                          {user.createTime?.split('T')[0] ?? '—'}
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleToggleRole(user)}
                              className="rounded-lg border border-border bg-secondary px-3 py-1.5 text-xs font-medium text-foreground hover:bg-accent"
                            >
                              切换角色
                            </button>
                            <button
                              onClick={() => handleDelete(user)}
                              className="rounded-lg bg-destructive px-3 py-1.5 text-xs font-medium text-destructive-foreground hover:bg-destructive/80"
                            >
                              删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
