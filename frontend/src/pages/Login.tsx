import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api';
import { useUserStore } from '@/store/user';

/* ------------------------------------------------------------------ */
/*  Shared input style                                                 */
/* ------------------------------------------------------------------ */
const inputClass =
  'block w-full h-12 rounded-lg border border-[#e5e5e5] bg-[#f9f9f8] px-4 text-[15px] text-[#171717] placeholder:text-[#aaa] cursor-text transition-colors duration-200 hover:border-[#ccc] focus:border-[#171717] focus:ring-2 focus:ring-[#171717]/25 focus:outline-none focus:bg-white disabled:opacity-40 disabled:cursor-not-allowed';

/* ------------------------------------------------------------------ */
/*  Eye icon — shown / hidden                                          */
/* ------------------------------------------------------------------ */
function EyeOpen() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" aria-hidden="true">
      <path
        d="M3 12s3.2-5 9-5 9 5 9 5-3.2 5-9 5-9-5-9-5Z"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.75" />
    </svg>
  );
}

function EyeClosed() {
  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" aria-hidden="true">
      <path
        d="M3 12s3.2-5 9-5 9 5 9 5-3.2 5-9 5-9-5-9-5Z"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M2 2l20 20" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
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
    <main className="flex min-h-screen h-dvh bg-white">
      {/* ────────────────────── Left: typographic hero ────────────────────── */}
      <div className="hidden lg:flex relative z-0 h-full w-1/2 bg-[#0f1117] flex-col justify-center px-16 overflow-hidden">
        {/* subtle grain / texture overlay — purely atmospheric */}
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.03]"
          style={{
            backgroundImage:
              'radial-gradient(circle at 30% 40%, #c4a35a 1px, transparent 1px)',
            backgroundSize: '32px 32px',
          }}
        />

        <div className="relative">
          <p className="text-[#e6e2d8] text-[46px] leading-[1.18] font-light tracking-[-0.01em] select-none">
            描述你的想法
            <br />
            AI 为你
            <span className="text-[#c4a35a]"> 写代码</span>
          </p>
          <p className="text-[#7a766d] text-[15px] mt-5 font-light tracking-[0.02em] select-none">
            即刻生成，即刻部署
          </p>
        </div>

        {/* brand mark — quiet, bottom-left */}
        <p className="absolute bottom-8 left-16 text-[#3d3a34] text-xs tracking-[0.08em] font-medium select-none">
          ZENO
        </p>
      </div>

      {/* ────────────────────── Right: form ────────────────────── */}
      <div className="relative z-10 flex h-full w-full lg:w-1/2 items-center justify-center px-6">
        <div className="w-full max-w-[400px]">
          {/* Mobile-only brand */}
          <div className="lg:hidden mb-10">
            <p className="text-2xl font-bold tracking-tight text-[#171717] select-none">
              Zeno
            </p>
          </div>

          {/* Heading */}
          <h1 className="text-[26px] font-medium leading-tight tracking-[-0.01em] text-[#171717]">
            {isLogin ? '登录' : '创建账号'}
          </h1>
          <p className="mt-2 text-sm text-[#707070]">
            {isLogin ? (
              <>
                还没有账号？
                <button
                  type="button"
                  onClick={() => switchMode(false)}
                  className="font-medium text-[#171717] underline underline-offset-4 decoration-[#d4d4d4] hover:decoration-[#171717] transition-colors"
                >
                  注册
                </button>
              </>
            ) : (
              <>
                已有账号？
                <button
                  type="button"
                  onClick={() => switchMode(true)}
                  className="font-medium text-[#171717] underline underline-offset-4 decoration-[#d4d4d4] hover:decoration-[#171717] transition-colors"
                >
                  登录
                </button>
              </>
            )}
          </p>

          {/* Form */}
          <form onSubmit={handleSubmit} className="mt-8" noValidate>
            <div className="space-y-4">
              {/* Account */}
              <div>
                <label
                  htmlFor="userAccount"
                  className="block text-sm font-medium text-[#171717] mb-2"
                >
                  账号
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
                  disabled={loading}
                  className={inputClass}
                />
              </div>

              {/* Password */}
              <div>
                <label
                  htmlFor="userPassword"
                  className="block text-sm font-medium text-[#171717] mb-2"
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
                    disabled={loading}
                    className={`${inputClass} pr-10`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-1 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded text-[#aaa] hover:text-[#555] hover:bg-[#f0f0f0] transition-colors"
                    aria-label={showPassword ? '隐藏密码' : '显示密码'}
                    tabIndex={-1}
                  >
                    {showPassword ? <EyeOpen /> : <EyeClosed />}
                  </button>
                </div>
              </div>

              {/* Confirm password — register only */}
              {!isLogin && (
                <div>
                  <label
                    htmlFor="checkPassword"
                    className="block text-sm font-medium text-[#171717] mb-2"
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
                      disabled={loading}
                      className={`${inputClass} pr-10`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((v) => !v)}
                      className="absolute right-1 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded text-[#aaa] hover:text-[#555] hover:bg-[#f0f0f0] transition-colors"
                      aria-label={showPassword ? '隐藏密码' : '显示密码'}
                      tabIndex={-1}
                    >
                      {showPassword ? <EyeOpen /> : <EyeClosed />}
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Message */}
            <div aria-live="polite" className="min-h-0 mt-3">
              {error && (
                <p
                  className={`text-xs leading-relaxed ${
                    error.includes('成功')
                      ? 'text-emerald-600'
                      : 'text-red-600'
                  }`}
                >
                  {error}
                </p>
              )}
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading}
              className="mt-6 flex h-11 w-full items-center justify-center rounded-lg bg-[#0f1117] px-4 text-sm font-medium text-white transition-all duration-200 hover:bg-[#2a2d35] focus:outline-none focus:ring-2 focus:ring-[#0f1117]/20 focus:ring-offset-2 active:scale-[0.99] disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
            >
              {loading ? '请稍候…' : isLogin ? '登录' : '创建账号'}
            </button>
          </form>
        </div>
      </div>
    </main>
  );
}
