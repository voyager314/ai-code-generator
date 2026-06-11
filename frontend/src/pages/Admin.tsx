import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import request from '@/utils/request';
import type { BaseResponse } from '@/types';

interface User {
  id: number;
  userAccount: string;
  userName: string;
  userRole: string;
  createTime: string;
}

export default function Admin() {
  const navigate = useNavigate();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const res = await request.get<any, BaseResponse<User[]>>('/user/list');
      setUsers(res.data);
    } catch (err: any) {
      alert(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确认删除该用户？')) return;
    try {
      await request.delete(`/user/remove/${id}`);
      loadUsers();
    } catch (err: any) {
      alert(err.message);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-6 py-4 flex justify-between items-center">
        <h1 className="text-xl font-bold">管理后台</h1>
        <Button variant="ghost" size="sm" onClick={() => navigate('/apps')}>
          返回应用列表
        </Button>
      </header>

      <div className="max-w-6xl mx-auto p-6">
        <Card>
          <CardHeader>
            <CardTitle>用户管理</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="text-center py-8">加载中...</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="border-b">
                    <tr>
                      <th className="text-left py-3 px-4">ID</th>
                      <th className="text-left py-3 px-4">账号</th>
                      <th className="text-left py-3 px-4">昵称</th>
                      <th className="text-left py-3 px-4">角色</th>
                      <th className="text-left py-3 px-4">创建时间</th>
                      <th className="text-left py-3 px-4">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((user) => (
                      <tr key={user.id} className="border-b">
                        <td className="py-3 px-4">{user.id}</td>
                        <td className="py-3 px-4">{user.userAccount}</td>
                        <td className="py-3 px-4">{user.userName || '-'}</td>
                        <td className="py-3 px-4">
                          <span
                            className={`px-2 py-1 text-xs rounded ${
                              user.userRole === 'admin'
                                ? 'bg-red-100 text-red-700'
                                : 'bg-gray-100 text-gray-700'
                            }`}
                          >
                            {user.userRole}
                          </span>
                        </td>
                        <td className="py-3 px-4">{user.createTime?.split('T')[0]}</td>
                        <td className="py-3 px-4">
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleDelete(user.id)}
                          >
                            删除
                          </Button>
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
