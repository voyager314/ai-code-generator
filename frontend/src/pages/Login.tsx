import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api';
import { useUserStore } from '@/store/user';

// ─── Icons ────────────────────────────────────────────────────────────────────

function BrandMark() {
  return (
    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-cyan-500 text-sm font-black text-white shadow-lg shadow-cyan-500/25">
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

// ─── Image Panel ──────────────────────────────────────────────────────────────

const HERO_IMAGE =
  'https://images.pexels.com/photos/27913444/pexels-photo-27913444.jpeg?auto=compress&cs=tinysrgb&fit=crop&h=1200&w=900';

function ImagePanel() {
  return (
    <aside className="hidden min-h-dvh flex-1 lg:block relative overflow-hidden bg-slate-950">
      {/* Hero image */}
      <img
        src={HERO_IMAGE}
        alt=""
        className="absolute inset-0 h-full w-full object-cover"
        loading="eager"
      />

      {/* Layered gradient overlay for depth and text readability */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-950/70 via-slate-950/40 to-cyan-950/60" />
      <div className="absolute inset-0 bg-gradient-to-t from-slate-950/80 via-transparent to-transparent" />

      {/* Content */}
      <div className="relative z-10 flex h-full flex-col justify-between p-10">
        {/* Top: brand identity */}
        <div className="flex items-center gap-3">
          <BrandMark />
          <div>
            <div className="text-lg font-bold leading-none text-white">Zeno</div>
            <div className="mt-1 text-xs text-cyan-200/70">AI Code Studio</div>
          </div>
        </div>

        {/* Middle: hero messaging */}
        <div className="flex-1 flex items-center">
          <div className="max-w-md">
            <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs text-cyan-200 mb-8">
              AI 应用生成工作台
            </div>
            <h2 className="text-4xl font-semibold leading-tight tracking-normal text-white">
              从一句需求
              <br />
              到可运行的应用
            </h2>
            <p className="mt-4 max-w-sm text-sm leading-6 text-slate-300">
              描述你想要的应用，AI 将自动完成需求分析、代码生成与质量检查，几分钟内交付可预览、可部署的前端项目。
            </p>
          </div>
        </div>

        {/* Bottom: attribution + stats */}
        <div className="flex items-center justify-between text-xs text-white/40">
          <a
            href="https://www.pexels.com/photo/27913444/"
            target="_blank"
            rel="noopener noreferrer"
            className="underline-offset-4 hover:text-white/60 hover:underline transition"
          >
            Photo by James L on Pexels
          </a>
          <span className="flex items-center gap-2">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
            服务运行中
          </span>
        </div>
      </div>
    </aside>
  );
}

// ─── Form ─────────────────────────────────────────────────────────────────────

const inputClass =
  'mt-2 h-12 w-full rounded-lg border border-slate-200 bg-white px-3 text-base text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100 disabled:cursor-not-allowed disabled:bg-slate-50';

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
    <main className="min-h-dvh bg-white lg:flex">
      {/* ── Left: Hero image panel ── */}
      <ImagePanel />

      {/* ── Right: Form ── */}
      <section className="flex min-h-dvh w-full items-center justify-center px-5 py-8 lg:w-1/2 lg:px-14">
        <div className="w-full max-w-[400px]">
          {/* Mobile-only brand (hidden on desktop — brand is on the image panel) */}
          <div className="mb-10 flex items-center gap-3 lg:hidden">
            <BrandMark />
            <div>
              <div className="text-lg font-bold leading-none">Zeno</div>
              <div className="mt-1 text-xs text-slate-500">AI Code Studio</div>
            </div>
          </div>

          {/* Heading */}
          <div className="mb-8">
            <h1 className="text-2xl font-semibold tracking-normal text-slate-950">
              {isLogin ? '登录到工作台' : '创建账号'}
            </h1>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              {isLogin
                ? '继续生成应用、管理作品和查看历史对话。'
                : '创建账号后即可保存作品并继续生成应用。'}
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {/* Account */}
            <div>
              <label htmlFor="userAccount" className="text-sm font-medium text-slate-700">
                账号
              </label>
              <input
                id="userAccount"
                type="text"
                placeholder="请输入账号"
                value={form.userAccount}
                onChange={(e) => setForm({ ...form, userAccount: e.target.value })}
                required
                autoComplete="username"
                disabled={loading}
                className={inputClass}
              />
            </div>

            {/* Password */}
            <div>
              <label htmlFor="userPassword" className="text-sm font-medium text-slate-700">
                密码
              </label>
              <div className="relative">
                <input
                  id="userPassword"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="请输入密码"
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
                  className="absolute right-1 top-3 flex h-10 w-10 items-center justify-center rounded-md text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-500"
                  aria-label={showPassword ? '隐藏密码' : '显示密码'}
                >
                  <EyeIcon open={showPassword} />
                </button>
              </div>
            </div>

            {/* Confirm password (register only) */}
            {!isLogin && (
              <div>
                <label htmlFor="checkPassword" className="text-sm font-medium text-slate-700">
                  确认密码
                </label>
                <input
                  id="checkPassword"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="请再次输入密码"
                  value={form.checkPassword}
                  onChange={(e) => setForm({ ...form, checkPassword: e.target.value })}
                  required
                  autoComplete="new-password"
                  disabled={loading}
                  className={inputClass}
                />
              </div>
            )}

            {/* Error / success message */}
            <div aria-live="polite" className="min-h-6 text-sm">
              {error && (
                <p className={error.includes('成功') ? 'text-emerald-600' : 'text-red-600'}>
                  {error}
                </p>
              )}
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading}
              className="flex h-12 w-full items-center justify-center rounded-lg bg-slate-950 px-4 text-sm font-semibold text-white transition hover:bg-slate-800 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-cyan-200 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? '处理中...' : isLogin ? '登录' : '注册'}
            </button>
          </form>

          {/* Toggle login / register */}
          <p className="mt-8 text-sm text-slate-500">
            {isLogin ? '还没有账号？' : '已有账号？'}
            <button
              type="button"
              onClick={switchMode}
              className="ml-2 min-h-11 rounded-md font-semibold text-cyan-600 underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-500"
            >
              {isLogin ? '创建账号' : '去登录'}
            </button>
          </p>
        </div>
      </section>
    </main>
  );
}
