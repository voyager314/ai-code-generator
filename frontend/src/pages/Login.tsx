import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api';
import { useUserStore } from '@/store/user';

// ─── Right-panel illustration ─────────────────────────────────────────────────
// Defined outside Login to avoid rerender-no-inline-components violation.

function PhoneIllustration() {
  return (
    <svg
      viewBox="0 0 480 560"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="w-full max-w-[420px] select-none pointer-events-none"
      aria-hidden="true"
    >
      {/* ── Clouds ── */}
      {/* Top-right cluster */}
      <ellipse cx="448" cy="42" rx="76" ry="38" fill="white" fillOpacity="0.24" />
      <ellipse cx="490" cy="30" rx="52" ry="29" fill="white" fillOpacity="0.18" />
      <ellipse cx="406" cy="60" rx="42" ry="23" fill="white" fillOpacity="0.15" />

      {/* Bottom-left cluster */}
      <ellipse cx="52" cy="524" rx="82" ry="38" fill="white" fillOpacity="0.26" />
      <ellipse cx="10" cy="516" rx="50" ry="27" fill="white" fillOpacity="0.19" />

      {/* Bottom-right cluster */}
      <ellipse cx="438" cy="532" rx="74" ry="36" fill="white" fillOpacity="0.24" />
      <ellipse cx="484" cy="524" rx="48" ry="26" fill="white" fillOpacity="0.18" />

      {/* ── Phone body ── */}
      <rect x="140" y="88" width="200" height="376" rx="28" fill="#1E1B4B" />
      {/* Screen */}
      <rect x="155" y="110" width="170" height="332" rx="18" fill="url(#screenGrad)" />
      {/* Notch */}
      <rect x="218" y="98" width="64" height="9" rx="4.5" fill="#0F0D2A" />

      {/* ── Fingerprint scanner ── */}
      <circle cx="240" cy="266" r="66" stroke="white" strokeWidth="2.4" strokeOpacity="0.38" />
      <circle cx="240" cy="266" r="49" stroke="white" strokeWidth="2.4" strokeOpacity="0.35" />
      <circle cx="240" cy="266" r="33" stroke="white" strokeWidth="2.4" strokeOpacity="0.32" />
      <circle cx="240" cy="266" r="17" stroke="white" strokeWidth="2" strokeOpacity="0.28"
        fill="white" fillOpacity="0.08" />
      {/* Fingerprint arcs */}
      <path d="M218 264 Q232 247 246 264 Q260 281 274 264"
        stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeOpacity="0.35" fill="none" />
      <path d="M214 275 Q232 254 250 275 Q265 288 280 275"
        stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeOpacity="0.28" fill="none" />

      {/* Progress bar at screen bottom */}
      <rect x="178" y="416" width="124" height="6" rx="3" fill="white" fillOpacity="0.16" />
      <rect x="178" y="416" width="70" height="6" rx="3" fill="url(#progressGrad)" />

      {/* Screen header hint strips */}
      <rect x="175" y="152" width="88" height="7" rx="3.5" fill="white" fillOpacity="0.28" />
      <rect x="175" y="165" width="118" height="5" rx="2.5" fill="white" fillOpacity="0.18" />

      {/* ── Speech bubble with checkmark ── */}
      <rect x="26" y="148" width="94" height="74" rx="14" fill="white" fillOpacity="0.93" />
      {/* Tail */}
      <polygon points="58,222 76,222 67,240" fill="white" fillOpacity="0.93" />
      {/* Checkmark */}
      <path d="M48 185 L63 200 L98 164"
        stroke="#7C3AED" strokeWidth="5.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />

      {/* ── Lock ── */}
      <rect x="354" y="214" width="82" height="74" rx="13" fill="white" fillOpacity="0.93" />
      {/* Shackle */}
      <path d="M371 214 V191 Q395 170 419 191 V214"
        stroke="#374151" strokeWidth="5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      {/* Keyhole */}
      <circle cx="395" cy="248" r="11" fill="#7C3AED" fillOpacity="0.75" />
      <rect x="391" y="252" width="8" height="15" rx="2" fill="#7C3AED" fillOpacity="0.75" />

      {/* ── Decorative dots ── */}
      <circle cx="104" cy="376" r="6" fill="white" fillOpacity="0.27" />
      <circle cx="90" cy="396" r="4" fill="white" fillOpacity="0.20" />
      <circle cx="390" cy="104" r="6" fill="white" fillOpacity="0.27" />
      <circle cx="408" cy="122" r="4" fill="white" fillOpacity="0.20" />

      <defs>
        <linearGradient id="screenGrad" x1="155" y1="110" x2="325" y2="442"
          gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#F472B6" />
          <stop offset="50%" stopColor="#A855F7" />
          <stop offset="100%" stopColor="#6D28D9" />
        </linearGradient>
        <linearGradient id="progressGrad" x1="178" y1="419" x2="248" y2="419"
          gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#C4B5FD" />
          <stop offset="100%" stopColor="#93C5FD" />
        </linearGradient>
      </defs>
    </svg>
  );
}

// ─── Login / Register page ────────────────────────────────────────────────────

const INPUT_CLS =
  'w-full px-4 py-3.5 rounded-xl border border-gray-200 text-sm text-gray-900 ' +
  'placeholder:text-gray-400 outline-none transition ' +
  'focus:ring-2 focus:ring-violet-400/50 focus:border-violet-400';

export default function Login() {
  const navigate = useNavigate();
  const setUser = useUserStore((s) => s.setUser);
  const user = useUserStore((s) => s.user);

  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ userAccount: '', userPassword: '', checkPassword: '' });

  // Already authenticated → skip login page
  useEffect(() => {
    if (user) navigate('/apps', { replace: true });
  }, [user, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isLogin) {
        const res = await userApi.login({
          userAccount: form.userAccount,
          userPassword: form.userPassword,
        });
        setUser(res.data);
        navigate('/apps');
      } else {
        if (form.userPassword !== form.checkPassword) {
          alert('两次密码不一致');
          return;
        }
        await userApi.register(form);
        setIsLogin(true);
        setForm((f) => ({ ...f, userPassword: '', checkPassword: '' }));
      }
    } catch (err: any) {
      alert(err.message || '操作失败');
    } finally {
      setLoading(false);
    }
  };

  const switchMode = () => {
    setIsLogin((v) => !v);
    setForm({ userAccount: '', userPassword: '', checkPassword: '' });
  };

  return (
    <div className="min-h-screen flex">

      {/* ── Left: form panel ── */}
      <div className="w-full lg:w-[42%] flex flex-col justify-center px-12 py-16 bg-white">

        {/* Logo */}
        <div className="flex items-center gap-2.5 mb-14">
          <div className="w-7 h-7 bg-violet-600 rounded-md flex items-center justify-center shrink-0">
            <span className="text-white text-[11px] font-black leading-none">Z</span>
          </div>
          <span className="text-[17px] font-bold tracking-tight text-gray-900">Zeno</span>
        </div>

        {/* Heading */}
        <div className="mb-10">
          <h1 className="text-[2.45rem] font-extrabold text-gray-900 leading-[1.17]">
            {isLogin ? (
              <>嗨，<br />欢迎回来</>
            ) : (
              <>创建账号，<br />开始体验</>
            )}
          </h1>
          <p className="mt-3.5 text-sm text-gray-400 leading-relaxed">
            {isLogin
              ? '嘿，欢迎回到你的专属创作空间'
              : '填写以下信息，加入 Zeno 创作社区'}
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4 max-w-[320px] w-full">
          <input
            type="text"
            placeholder="账号"
            value={form.userAccount}
            onChange={(e) => setForm({ ...form, userAccount: e.target.value })}
            required
            autoComplete="username"
            className={INPUT_CLS}
          />
          <input
            type="password"
            placeholder="密码"
            value={form.userPassword}
            onChange={(e) => setForm({ ...form, userPassword: e.target.value })}
            required
            autoComplete={isLogin ? 'current-password' : 'new-password'}
            className={INPUT_CLS}
          />
          {!isLogin && (
            <input
              type="password"
              placeholder="确认密码"
              value={form.checkPassword}
              onChange={(e) => setForm({ ...form, checkPassword: e.target.value })}
              required
              autoComplete="new-password"
              className={INPUT_CLS}
            />
          )}

          {isLogin && (
            <label className="flex items-center gap-2 text-sm text-gray-400 cursor-pointer select-none -mt-1">
              <input type="checkbox" className="w-4 h-4 rounded accent-violet-600" />
              记住我
            </label>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 mt-1 rounded-xl bg-violet-600 text-white font-bold text-sm
                       tracking-widest hover:bg-violet-700 active:scale-[0.985]
                       disabled:opacity-60 transition-all"
          >
            {loading ? '处理中...' : isLogin ? 'Sign In' : 'Sign Up'}
          </button>
        </form>

        {/* Switch mode */}
        <p className="mt-10 text-sm text-gray-400">
          {isLogin ? "Don't have an account?" : '已有账号？'}
          <button
            type="button"
            onClick={switchMode}
            className="ml-1.5 text-violet-600 font-semibold hover:underline"
          >
            {isLogin ? 'Sign Up' : '去登录'}
          </button>
        </p>
      </div>

      {/* ── Right: gradient + illustration ── */}
      <div
        className="hidden lg:flex flex-1 items-center justify-center overflow-hidden relative"
        style={{
          background:
            'linear-gradient(148deg, #5B21B6 0%, #7C3AED 36%, #9D72FF 66%, #C4B5FD 100%)',
        }}
      >
        <PhoneIllustration />
      </div>

    </div>
  );
}
