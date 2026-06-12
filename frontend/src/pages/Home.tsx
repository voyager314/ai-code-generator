import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { appApi, userApi } from '@/api';
import { useUserStore } from '@/store/user';
import type { AppVO } from '@/types';

function SidebarIcon({ name }: { name: 'collapse' | 'expand' | 'search' | 'plus' | 'chat' | 'logout' | 'admin' | 'delete' }) {
  const d: Record<string, string> = {
    collapse: 'M11 19V5M4 12h14',
    expand: 'M4 6h16M4 12h16M4 18h16',
    search: 'M21 21l-4.35-4.35M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16Z',
    plus: 'M12 5v14M5 12h14',
    chat: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10Z',
    logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
    admin: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z',
    delete: 'M3 6h18M8 6V4h8v2M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6',
  };

  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4 shrink-0" fill="none" aria-hidden="true">
      <path d={d[name]} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function Home() {
  const navigate = useNavigate();
  const user = useUserStore((s) => s.user);
  const isAdmin = useUserStore((s) => s.isAdmin());
  const setUser = useUserStore((s) => s.setUser);

  const [apps, setApps] = useState<AppVO[]>([]);
  const [search, setSearch] = useState('');
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [agentMode, setAgentMode] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    loadApps();
  }, []);

  const loadApps = async () => {
    try {
      const res = await appApi.getMyList({ pageNum: 1, pageSize: 50 });
      setApps(res.data.records);
    } catch (err) {
      console.error('Failed to load apps:', err);
    }
  };

  const filteredApps = apps.filter((app) =>
    (app.appName || '').toLowerCase().includes(search.toLowerCase())
  );

  const handleSend = async () => {
    if (!input.trim() || loading) return;
    setLoading(true);
    try {
      const res = await appApi.create({
        initPrompt: input.trim(),
        appName: `应用_${Date.now()}`,
      });
      navigate(`/app/${res.data}`, { state: { initMsg: input.trim(), agentMode } });
    } catch (err: any) {
      alert(err.message || '创建失败');
      setLoading(false);
    }
  };

  const handleDeleteApp = async (e: React.MouseEvent, appId: number) => {
    e.stopPropagation();
    if (deletingId) return;
    setDeletingId(appId);
    try {
      await appApi.delete(appId);
      setApps((prev) => prev.filter((a) => a.id !== appId));
    } catch (err: any) {
      alert(err.message || '删除失败');
    } finally {
      setDeletingId(null);
    }
  };

  const handleLogout = async () => {
    await userApi.logout();
    setUser(null);
    navigate('/login');
  };

  const handleNewChat = () => {
    setInput('');
    setMobileOpen(false);
    inputRef.current?.focus();
  };

  const suggestions = [
    { title: '电商落地页', desc: '设计一个时尚的商品展示页面', icon: '🛍️' },
    { title: '企业官网', desc: '创建一个现代化的企业品牌网站', icon: '🏢' },
    { title: '后台管理系统', desc: '搭建一个功能完善的数据管理面板', icon: '📊' },
  ];

  const userName = user?.userName || user?.userAccount || '用户';

  const sidebar = (
    <aside
      className={`flex h-full flex-col bg-white border-r border-[#ebebeb] transition-all duration-300 ${
        collapsed ? 'w-[68px]' : 'w-[260px]'
      }`}
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 pt-5 pb-3">
        {!collapsed && (
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#18181b]">
              <span className="text-xs font-bold text-white">Z</span>
            </div>
            <span className="text-[15px] font-semibold tracking-tight text-[#1a1a1a]">Zeno</span>
          </div>
        )}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="hidden lg:flex h-8 w-8 items-center justify-center rounded-lg text-[#8e8e93] hover:bg-[#f5f5f5] hover:text-[#1a1a1a] transition-colors"
          aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}
        >
          <SidebarIcon name={collapsed ? 'expand' : 'collapse'} />
        </button>
      </div>

      {/* New Chat Button */}
      <div className="px-3 pb-2">
        <button
          onClick={handleNewChat}
          className={`flex w-full items-center gap-2.5 rounded-lg border border-[#e5e5e5] px-3 py-2.5 text-sm font-medium text-[#1a1a1a] transition-colors hover:bg-[#f5f5f5] ${
            collapsed ? 'justify-center' : ''
          }`}
        >
          <SidebarIcon name="plus" />
          {!collapsed && <span>新建应用</span>}
        </button>
      </div>

      {/* Search */}
      {!collapsed && (
        <div className="px-3 pb-3">
          <div className="relative">
            <div className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[#c5c5c7]">
              <SidebarIcon name="search" />
            </div>
            <input
              type="text"
              placeholder="搜索应用..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="h-9 w-full rounded-lg border border-[#e5e5e5] bg-[#fafafa] pl-9 pr-3 text-sm text-[#1a1a1a] placeholder:text-[#c5c5c7] outline-none transition-colors focus:border-[#c5c5c7] focus:bg-white"
            />
          </div>
        </div>
      )}

      {/* App List */}
      <div className="flex-1 overflow-y-auto px-2">
        {!collapsed && (
          <div className="px-2 pb-2 pt-1 text-[11px] font-medium uppercase tracking-wider text-[#b4b4b8]">
            我的应用
          </div>
        )}
        <div className="space-y-0.5">
          {filteredApps.length === 0 ? (
            !collapsed && (
              <div className="px-2 py-6 text-center text-xs text-[#b4b4b8]">
                {search ? '没有匹配的应用' : '暂无应用，开始创建吧'}
              </div>
            )
          ) : (
            filteredApps.map((app) => (
              <div
                key={app.id}
                onClick={() => navigate(`/app/${app.id}`)}
                className={`group flex items-center gap-2.5 rounded-lg px-2.5 py-2 cursor-pointer transition-colors hover:bg-[#f5f5f5] ${
                  collapsed ? 'justify-center' : ''
                }`}
              >
                <div className="flex h-5 w-5 shrink-0 items-center justify-center text-[#8e8e93]">
                  <SidebarIcon name="chat" />
                </div>
                {!collapsed && (
                  <>
                    <span className="flex-1 truncate text-sm text-[#3c3c43]">
                      {app.appName || `应用 ${app.id}`}
                    </span>
                    <button
                      onClick={(e) => handleDeleteApp(e, app.id)}
                      disabled={deletingId === app.id}
                      className="hidden shrink-0 rounded p-1 text-[#c5c5c7] transition-colors hover:bg-[#ebebeb] hover:text-[#8e8e93] group-hover:flex disabled:opacity-40"
                      aria-label="删除应用"
                    >
                      <SidebarIcon name="delete" />
                    </button>
                  </>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      {/* Bottom: User Info */}
      <div className="border-t border-[#ebebeb] p-3">
        {isAdmin && (
          <button
            onClick={() => navigate('/admin')}
            className={`mb-1 flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm text-[#8e8e93] transition-colors hover:bg-[#f5f5f5] hover:text-[#1a1a1a] ${
              collapsed ? 'justify-center' : ''
            }`}
          >
            <SidebarIcon name="admin" />
            {!collapsed && <span>管理后台</span>}
          </button>
        )}
        <div
          className={`flex items-center gap-2.5 rounded-lg px-2.5 py-2 ${
            collapsed ? 'justify-center' : ''
          }`}
        >
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-violet-400 to-indigo-500 text-xs font-semibold text-white">
            {userName.charAt(0).toUpperCase()}
          </div>
          {!collapsed && (
            <div className="flex flex-1 items-center justify-between min-w-0">
              <span className="truncate text-sm font-medium text-[#1a1a1a]">{userName}</span>
              <button
                onClick={handleLogout}
                className="shrink-0 rounded-lg p-1.5 text-[#c5c5c7] transition-colors hover:bg-[#f5f5f5] hover:text-[#8e8e93]"
                aria-label="退出登录"
              >
                <SidebarIcon name="logout" />
              </button>
            </div>
          )}
        </div>
      </div>
    </aside>
  );

  return (
    <div className="flex h-dvh bg-white">
      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/20 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Sidebar — desktop */}
      <div className="hidden lg:flex h-full">{sidebar}</div>

      {/* Sidebar — mobile drawer */}
      <div
        className={`fixed inset-y-0 left-0 z-50 lg:hidden transition-transform duration-300 ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {sidebar}
      </div>

      {/* Main Content */}
      <main className="flex flex-1 flex-col min-w-0">
        {/* Top Bar */}
        <header className="flex items-center justify-between px-6 py-4">
          {/* Mobile hamburger */}
          <button
            onClick={() => setMobileOpen(true)}
            className="flex h-10 w-10 items-center justify-center rounded-lg text-[#8e8e93] hover:bg-[#f5f5f5] lg:hidden"
            aria-label="打开菜单"
          >
            <SidebarIcon name="expand" />
          </button>

          <div className="hidden lg:block" />

          {/* User info */}
          <div className="flex items-center gap-3">
            <span className="text-sm text-[#8e8e93]">{userName}</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-violet-400 to-indigo-500 text-xs font-semibold text-white">
              {userName.charAt(0).toUpperCase()}
            </div>
          </div>
        </header>

        {/* Center Content */}
        <div className="flex flex-1 flex-col items-center justify-center px-6 pb-8">
          <div className="w-full max-w-[720px]">
            {/* Greeting */}
            <div className="mb-10 text-center">
              <h1 className="text-3xl font-semibold text-[#1a1a1a] sm:text-4xl">
                你好，{userName}
              </h1>
              <p className="mt-3 text-base text-[#8e8e93] sm:text-lg">
                今天想创建什么？
              </p>
            </div>

            {/* Suggestion Cards */}
            <div className="mb-10 grid grid-cols-1 gap-3 sm:grid-cols-3">
              {suggestions.map((s) => (
                <button
                  key={s.title}
                  onClick={() => {
                    setInput(s.title);
                    inputRef.current?.focus();
                  }}
                  className="group flex flex-col rounded-xl border border-[#ebebeb] bg-[#fafafa] p-4 text-left transition-all hover:border-[#d4d4d4] hover:bg-[#f5f5f5] hover:shadow-sm"
                >
                  <span className="mb-2 text-xl">{s.icon}</span>
                  <span className="text-sm font-medium text-[#1a1a1a]">{s.title}</span>
                  <span className="mt-1 text-xs text-[#8e8e93] leading-relaxed">{s.desc}</span>
                </button>
              ))}
            </div>

            {/* Input Bar */}
            <div className="rounded-2xl border border-[#e5e5e5] bg-[#fafafa] shadow-sm transition-colors focus-within:border-[#c5c5c7] focus-within:bg-white focus-within:shadow-md">
              <textarea
                ref={inputRef}
                placeholder="描述你想创建的应用..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSend();
                  }
                }}
                disabled={loading}
                rows={1}
                className="block w-full resize-none bg-transparent px-5 pt-4 pb-2 text-[15px] text-[#1a1a1a] placeholder:text-[#c5c5c7] outline-none disabled:opacity-50"
                style={{ minHeight: '44px', maxHeight: '200px' }}
                onInput={(e) => {
                  const target = e.target as HTMLTextAreaElement;
                  target.style.height = 'auto';
                  target.style.height = Math.min(target.scrollHeight, 200) + 'px';
                }}
              />
              <div className="flex items-center justify-between px-3 pb-3 pt-1">
                <button
                  type="button"
                  onClick={() => setAgentMode((v) => !v)}
                  disabled={loading}
                  className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 ${
                    agentMode
                      ? 'border-indigo-400 bg-indigo-50 text-indigo-600'
                      : 'border-[#d4d4d4] bg-white text-[#8e8e93] hover:border-[#b4b4b8] hover:text-[#636366]'
                  }`}
                >
                  <svg viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" aria-hidden="true">
                    <path d="M12 2a7 7 0 0 1 7 7c0 2.38-1.19 4.47-3 5.74V17a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-2.26C6.19 13.47 5 11.38 5 9a7 7 0 0 1 7-7Z" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M9 21h6M10 17v4M14 17v4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                  </svg>
                  深度思考
                </button>
                <button
                  onClick={handleSend}
                  disabled={loading || !input.trim()}
                  className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#18181b] text-white transition-all hover:bg-[#333] disabled:opacity-30 disabled:cursor-not-allowed"
                  aria-label="发送"
                >
                  {loading ? (
                    <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2.5" opacity="0.3" />
                      <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
                    </svg>
                  ) : (
                    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none">
                      <path d="M12 19V5M5 12l7-7 7 7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
