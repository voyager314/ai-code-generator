import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api';
import { useUserStore } from '@/store/user';

// Replace with your preferred image from src/images/
import loginHero from '@/images/pexels-dorota-semla-1929451-37695994.jpg';

const inputClass =
  'block w-full h-12 rounded-lg border border-slate-200 bg-white px-4 text-sm text-slate-900 placeholder:text-slate-400 transition-colors hover:border-slate-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 focus:outline-none disabled:bg-slate-50 disabled:opacity-60';

export default function Login() {
  const navigate = useNavigate();
  const setUser = useUserStore((s) => s.setUser);
  const user = useUserStore((s) => s.user);

  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
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
      setError('Passwords do not match.');
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
        setError('Account created. Please log in.');
      }
    } catch (err: any) {
      setError(err.message || 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="flex h-dvh bg-white">
      {/* ── Left: image card ── */}
      <div className="hidden h-full w-1/2 items-center justify-center bg-slate-50 p-10 lg:flex">
        <div className="h-full w-full overflow-hidden rounded-2xl shadow-2xl shadow-slate-300/50 ring-1 ring-slate-200/50">
          <img
            src={loginHero}
            alt=""
            className="h-full w-full object-cover"
          />
        </div>
      </div>

      {/* ── Right: form ── */}
      <div className="flex h-full w-full items-center justify-center px-6 lg:w-1/2">
        <div className="w-full max-w-[360px]">
          {/* Heading */}
          <h1 className="text-[28px] font-normal leading-tight tracking-normal text-slate-900">
            {isLogin ? 'Login' : 'Create an account'}
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            {isLogin ? (
              <>
                Don&rsquo;t have an account?{' '}
                <button
                  type="button"
                  onClick={() => { setError(''); setIsLogin(false); setForm({ userAccount: '', userPassword: '', checkPassword: '' }); }}
                  className="font-medium text-blue-600 hover:text-blue-800 transition-colors"
                >
                  Sign up
                </button>
              </>
            ) : (
              <>
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => { setError(''); setIsLogin(true); setForm({ userAccount: '', userPassword: '', checkPassword: '' }); }}
                  className="font-medium text-blue-600 hover:text-blue-800 transition-colors"
                >
                  Log in
                </button>
              </>
            )}
          </p>

          {/* Form */}
          <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
            <div>
              <label htmlFor="userAccount" className="block text-sm font-medium text-slate-700 mb-1.5">
                Account
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
              <label htmlFor="userPassword" className="block text-sm font-medium text-slate-700 mb-1.5">
                Password
              </label>
              <input
                id="userPassword"
                type="password"
                value={form.userPassword}
                onChange={(e) => setForm({ ...form, userPassword: e.target.value })}
                required
                autoComplete={isLogin ? 'current-password' : 'new-password'}
                disabled={loading}
                className={inputClass}
              />
            </div>

            {!isLogin && (
              <div>
                <label htmlFor="checkPassword" className="block text-sm font-medium text-slate-700 mb-1.5">
                  Confirm password
                </label>
                <input
                  id="checkPassword"
                  type="password"
                  value={form.checkPassword}
                  onChange={(e) => setForm({ ...form, checkPassword: e.target.value })}
                  required
                  autoComplete="new-password"
                  disabled={loading}
                  className={inputClass}
                />
              </div>
            )}

            {/* Message */}
            <div aria-live="polite" className="min-h-[20px]">
              {error && (
                <p className={`text-xs ${error.includes('created') ? 'text-emerald-600' : 'text-red-600'}`}>
                  {error}
                </p>
              )}
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading}
              className="flex h-11 w-full items-center justify-center rounded-md bg-slate-900 px-4 text-sm font-medium text-white transition-colors hover:bg-slate-800 focus:outline-none focus:ring-4 focus:ring-blue-100 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Please wait…' : isLogin ? 'Login' : 'Create account'}
            </button>
          </form>
        </div>
      </div>
    </main>
  );
}
