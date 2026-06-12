import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api';
import { useUserStore } from '@/store/user';

function BrandMark() {
  return (
    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-950 text-sm font-black text-white">
      Z
    </div>
  );
}

function EyeIcon({ open }: { open: boolean }) {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" aria-hidden="true">
      <path
        d="M3 12s3.2-5 9-5 9 5 9 5-3.2 5-9 5-9-5-9-5Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {open ? (
        <circle cx="12" cy="12" r="2.6" stroke="currentColor" strokeWidth="1.8" />
      ) : (
        <path d="M5 5l14 14" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      )}
    </svg>
  );
}

function ProductPanel() {
  return (
    <aside className="hidden min-h-dvh flex-1 bg-slate-950 text-white lg:block">
      <div className="flex h-full flex-col justify-between p-10">
        <div>
          <div className="mb-10 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs text-slate-300">
            AI 应用生成工作台
          </div>
          <h2 className="max-w-xl text-4xl font-semibold leading-tight tracking-normal">
            从一句需求到可预览、可部署的应用。
          </h2>
          <p className="mt-4 max-w-lg text-sm leading-6 text-slate-300">
            登录后继续管理作品、查看生成记录，并在同一个工作台里调试代码与预览结果。
          </p>
        </div>

        <div className="rounded-lg border border-white/10 bg-white/[0.04] p-5 shadow-2xl shadow-black/30">
          <div className="mb-4 flex items-center justify-between border-b border-white/10 pb-3">
            <span className="text-sm font-medium text-slate-200">生成流水线</span>
            <span className="rounded-full bg-emerald-400/15 px-2.5 py-1 text-xs font-medium text-emerald-200">
              在线
            </span>
          </div>
          <div className="space-y-3">
            {[
              ['需求理解', '解析页面目标与交互结构'],
              ['代码生成', '生成前端文件与业务骨架'],
              ['质量检查', '检查可运行性与基础体验'],
            ].map(([title, desc], index) => (
              <div key={title} className="flex gap-3 rounded-md bg-white/[0.03] p-3">
                <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-cyan-300 text-xs font-bold text-slate-950">
                  {index + 1}
                </div>
                <div>
                  <div className="text-sm font-medium text-white">{title}</div>
                  <div className="mt-0.5 text-xs text-slate-400">{desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </aside>
  );
}

const inputClass =
  'mt-2 h-12 w-full rounded-lg border border-slate-200 bg-white px-3 text-base text-slate-950 outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100 disabled:cursor-not-allowed disabled:bg-slate-50';

export default function Login() {
  const navigate = useNavigate();
  const setUser = useUserStore((s) => s.setUser);
  const user = useUserStore((s) => s.user);

  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ userAccount: '', userPassword: '', checkPassword: '' });

  useEffect(() => {
    if (user) {
      navigate('/apps', { replace: true });
    }
  }, [user, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!isLogin && form.userPassword !== form.checkPassword) {
      setError('两次输入的密码不一致。');
      return;
    }

    setLoading(true);
    try {
      if (isLogin) {
        const res = await userApi.login({
          userAccount: form.userAccount,
          userPassword: form.userPassword,
        });
        setUser(res.data);
        navigate('/apps', { replace: true });
      } else {
        await userApi.register(form);
        setIsLogin(true);
        setForm((f) => ({ ...f, userPassword: '', checkPassword: '' }));
        setError('注册成功，请使用新账号登录。');
      }
    } catch (err: any) {
      setError(err.message || '操作失败，请稍后重试。');
    } finally {
      setLoading(false);
    }
  };

  const switchMode = () => {
    setError('');
    setIsLogin((v) => !v);
    setForm({ userAccount: '', userPassword: '', checkPassword: '' });
  };

  return (
    <main className="min-h-dvh bg-slate-50 text-slate-950 lg:flex">
      <section className="flex min-h-dvh w-full items-center justify-center px-5 py-8 lg:w-[46%] lg:px-10">
        <div className="w-full max-w-[420px]">
          <div className="mb-10 flex items-center gap-3">
            <BrandMark />
            <div>
              <div className="text-lg font-bold leading-none">Zeno</div>
              <div className="mt-1 text-xs text-slate-500">AI Code Studio</div>
            </div>
          </div>

          <div className="mb-8">
            <h1 className="text-3xl font-semibold tracking-normal text-slate-950">
              {isLogin ? '登录到工作台' : '创建账号'}
            </h1>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              {isLogin
                ? '继续生成应用、管理作品和查看历史对话。'
                : '创建账号后即可保存作品并继续生成应用。'}
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            <div>
              <label htmlFor="userAccount" className="text-sm font-medium text-slate-800">
                账号
              </label>
              <input
                id="userAccount"
                type="text"
                value={form.userAccount}
                onChange={(e) => setForm({ ...form, userAccount: e.target.value })}
                required
                autoComplete="username"
                disabled={loading}
                className={inputClass}
              />
            </div>

            <div>
              <label htmlFor="userPassword" className="text-sm font-medium text-slate-800">
                密码
              </label>
              <div className="relative">
                <input
                  id="userPassword"
                  type={showPassword ? 'text' : 'password'}
                  value={form.userPassword}
                  onChange={(e) => setForm({ ...form, userPassword: e.target.value })}
                  required
                  autoComplete={isLogin ? 'current-password' : 'new-password'}
                  disabled={loading}
                  className={`${inputClass} pr-12`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-1 top-3 flex h-10 w-10 items-center justify-center rounded-md text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-500"
                  aria-label={showPassword ? '隐藏密码' : '显示密码'}
                >
                  <EyeIcon open={showPassword} />
                </button>
              </div>
            </div>

            {!isLogin && (
              <div>
                <label htmlFor="checkPassword" className="text-sm font-medium text-slate-800">
                  确认密码
                </label>
                <input
                  id="checkPassword"
                  type={showPassword ? 'text' : 'password'}
                  value={form.checkPassword}
                  onChange={(e) => setForm({ ...form, checkPassword: e.target.value })}
                  required
                  autoComplete="new-password"
                  disabled={loading}
                  className={inputClass}
                />
              </div>
            )}

            <div aria-live="polite" className="min-h-6 text-sm">
              {error && (
                <p className={error.includes('成功') ? 'text-emerald-700' : 'text-red-600'}>
                  {error}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="flex h-12 w-full items-center justify-center rounded-lg bg-slate-950 px-4 text-sm font-semibold text-white transition hover:bg-slate-800 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-cyan-200 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? '处理中...' : isLogin ? '登录' : '注册'}
            </button>
          </form>

          <p className="mt-8 text-sm text-slate-600">
            {isLogin ? '还没有账号？' : '已有账号？'}
            <button
              type="button"
              onClick={switchMode}
              className="ml-2 min-h-11 rounded-md font-semibold text-cyan-700 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-500"
            >
              {isLogin ? '创建账号' : '去登录'}
            </button>
          </p>
        </div>
      </section>

      <ProductPanel />
    </main>
  );
}
