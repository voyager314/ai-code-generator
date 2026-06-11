import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { userApi } from '@/api';
import { useUserStore } from '@/store/user';

export default function Login() {
  const navigate = useNavigate();
  const setUser = useUserStore((s) => s.setUser);
  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ userAccount: '', userPassword: '', checkPassword: '' });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isLogin) {
        const res = await userApi.login({ userAccount: form.userAccount, userPassword: form.userPassword });
        setUser(res.data);
        navigate('/apps');
      } else {
        if (form.userPassword !== form.checkPassword) {
          alert('两次密码不一致');
          return;
        }
        await userApi.register(form);
        alert('注册成功，请登录');
        setIsLogin(true);
      }
    } catch (err: any) {
      alert(err.message || '操作失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>{isLogin ? '登录' : '注册'}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              placeholder="账号"
              value={form.userAccount}
              onChange={(e) => setForm({ ...form, userAccount: e.target.value })}
              required
            />
            <Input
              type="password"
              placeholder="密码"
              value={form.userPassword}
              onChange={(e) => setForm({ ...form, userPassword: e.target.value })}
              required
            />
            {!isLogin && (
              <Input
                type="password"
                placeholder="确认密码"
                value={form.checkPassword}
                onChange={(e) => setForm({ ...form, checkPassword: e.target.value })}
                required
              />
            )}
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? '处理中...' : isLogin ? '登录' : '注册'}
            </Button>
            <div className="text-center text-sm">
              <button
                type="button"
                onClick={() => setIsLogin(!isLogin)}
                className="text-primary hover:underline"
              >
                {isLogin ? '没有账号？去注册' : '已有账号？去登录'}
              </button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
