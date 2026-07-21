import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api';
import { useUserStore } from '@/store/user';

/* ------------------------------------------------------------------ */
/*  Shared input style                                                 */
/* ------------------------------------------------------------------ */
const inputClass =
  'block w-full h-11 rounded-lg border border-border bg-card px-4 text-[14px] text-foreground placeholder:text-muted-foreground cursor-text transition-colors duration-200 focus:border-ring focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed';

/* ------------------------------------------------------------------ */
/*  Eye icon — shown / hidden                                          */
/* ------------------------------------------------------------------ */
function EyeOpen() {
  return (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" aria-hidden="true">
      <path
        d="M3 12s3.2-5 9-5 9 5 9 5-3.2 5-9 5-9-5-9-5Z"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.5" />
    </svg>
  );
}

function EyeClosed() {
  return (
    <svg viewBox="0 0 24 24" className="h-[18px] w-[18px]" fill="none" aria-hidden="true">
      <path
        d="M3 12s3.2-5 9-5 9 5 9 5-3.2 5-9 5-9-5-9-5Z"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M2 2l20 20" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

/* ================================================================== */
/*  Login Page                                                         */
/* ================================================================== */
export default function Login() {
  const navigate = useNavigate();
  const setUser = useUserStore((s) => s.setUser);
  const user = useUserStore((s) => s.user);

  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    userAccount: '',
    userPassword: '',
    checkPassword: '',
  });

  /* already logged in → redirect */
  useEffect(() => {
    if (user) {
      navigate('/', { replace: true });
    }
  }, [user, navigate]);

  /* ---------- submit ---------- */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!isLogin && form.userPassword !== form.checkPassword) {
      setError('两次输入的密码不一致');
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
        navigate('/', { replace: true });
      } else {
        await userApi.register(form);
        setIsLogin(true);
        setForm((f) => ({ ...f, userPassword: '', checkPassword: '' }));
        setError('注册成功，请登录');
      }
    } catch (err: any) {
      setError(err.message || '操作失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  /* ---------- helpers ---------- */
  const reset = () => {
    setError('');
    setForm({ userAccount: '', userPassword: '', checkPassword: '' });
  };

  const switchMode = (toLogin: boolean) => {
    reset();
    setIsLogin(toLogin);
  };

  /* ============================ */
  /*  Render                       */
  /* ============================ */
  return (
    <main className="flex min-h-screen items-center justify-center bg-background font-sans antialiased">
      <div className="w-full max-w-[400px] px-6">

        {/* Logo */}
        <div className="text-center mb-10">
          <h1 className="text-3xl font-bold tracking-tight text-white select-none">
            Zeno
          </h1>
        </div>

        {/* Header */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-white">
            {isLogin ? '登录账户' : '创建一个账户'}
          </h2>
          <p className="mt-2 text-[14px] text-[#888]">
            {isLogin ? '没有账户？' : '已有账户？'}
            <button
              type="button"
              onClick={() => switchMode(!isLogin)}
              className="ml-1 text-primary hover:text-primary/80 underline underline-offset-4 transition-colors"
            >
              {isLogin ? '注册.' : '登录.'}
            </button>
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-5" noValidate>

          {/* Account */}
          <div className="space-y-1.5">
            <label
              htmlFor="userAccount"
              className="block text-[13px] font-medium text-[#ccc]"
            >
              用户名
            </label>
            <input
              id="userAccount"
              type="text"
              value={form.userAccount}
              onChange={(e) =>
                setForm({ ...form, userAccount: e.target.value })
              }
              required
              autoFocus
              autoComplete="username"
              placeholder="输入您的用户名"
              disabled={loading}
              className={inputClass}
            />
          </div>

          {/* Password */}
          <div className="space-y-1.5">
            <label
              htmlFor="userPassword"
              className="block text-[13px] font-medium text-[#ccc]"
            >
              密码
            </label>
            <div className="relative">
              <input
                id="userPassword"
                type={showPassword ? 'text' : 'password'}
                value={form.userPassword}
                onChange={(e) =>
                  setForm({ ...form, userPassword: e.target.value })
                }
                required
                autoComplete={
                  isLogin ? 'current-password' : 'new-password'
                }
                placeholder="输入密码 (8-20 个字符)"
                disabled={loading}
                className={`${inputClass} pr-10`}
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#888] hover:text-[#ccc] transition-colors"
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
                tabIndex={-1}
              >
                {showPassword ? <EyeOpen /> : <EyeClosed />}
              </button>
            </div>
          </div>

          {/* Confirm password — register only */}
          {!isLogin && (
            <div className="space-y-1.5">
              <label
                htmlFor="checkPassword"
                className="block text-[13px] font-medium text-[#ccc]"
              >
                确认密码
              </label>
              <div className="relative">
                <input
                  id="checkPassword"
                  type={showPassword ? 'text' : 'password'}
                  value={form.checkPassword}
                  onChange={(e) =>
                    setForm({ ...form, checkPassword: e.target.value })
                  }
                  required
                  autoComplete="new-password"
                  placeholder="确认密码"
                  disabled={loading}
                  className={`${inputClass} pr-10`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#888] hover:text-[#ccc] transition-colors"
                  aria-label={showPassword ? '隐藏密码' : '显示密码'}
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOpen /> : <EyeClosed />}
                </button>
              </div>
            </div>
          )}

          {/* Message */}
          <div aria-live="polite" className="min-h-0">
            {error && (
              <p
                className={`text-[13px] ${
                  error.includes('成功')
                    ? 'text-green-500'
                    : 'text-red-500'
                }`}
              >
                {error}
              </p>
            )}
          </div>

          {/* Submit */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={loading}
              className="flex h-11 w-full items-center justify-center rounded-full bg-primary text-[14px] font-medium text-primary-foreground transition-all hover:bg-primary/80 focus:outline-none focus:ring-2 focus:ring-ring/20 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '请稍候…' : isLogin ? '登录' : '创建账户'}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}
