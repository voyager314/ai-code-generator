import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { appApi, userApi } from '@/api';
import { useUserStore } from '@/store/user';
import type { AppVO } from '@/types';

function SidebarIcon({ name }: { name: 'collapse' | 'expand' | 'search' | 'plus' | 'chat' | 'logout' | 'admin' | 'delete' | 'send' }) {
  const d: Record<string, string> = {
    collapse: 'M11 19V5M4 12h14',
    expand: 'M4 6h16M4 12h16M4 18h16',
    search: 'M21 21l-4.35-4.35M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16Z',
    plus: 'M12 5v14M5 12h14',
    chat: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10Z',
    logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
    admin: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z',
    delete: 'M3 6h18M8 6V4h8v2M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6',
    send: 'M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z'
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
    const prompt = input.trim();
    if (!prompt || loading) return;
    setLoading(true);
    try {
      const res = await appApi.create({
        initPrompt: prompt,
        appName: `应用_${Date.now()}`,
      });
      navigate(`/app/${res.data}`, { state: { initMsg: prompt, agentMode } });
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

  const userAccount = user?.userAccount || '用户';

  const sidebar = (
    <aside
      className={`flex h-full flex-col bg-[#171717] transition-all duration-300 ${
        collapsed ? 'w-[68px]' : 'w-[260px]'
      }`}
    >
      {/* Header - Collapse/Expand */}
      <div className="flex items-center justify-end px-3 pt-3 pb-2">
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="flex h-10 w-10 items-center justify-center rounded-lg text-[#b4b4b8] hover:bg-[#212121] hover:text-[#ececec] transition-colors"
          aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}
        >
          <SidebarIcon name={collapsed ? 'expand' : 'collapse'} />
        </button>
      </div>

      {/* Search */}
      {!collapsed && (
        <div className="px-3 pb-3">
          <div className="relative flex items-center h-10 rounded-lg hover:bg-[#212121] transition-colors cursor-pointer text-[#ececec]">
            <div className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[#b4b4b8]">
              <SidebarIcon name="search" />
            </div>
            <input
              type="text"
              placeholder="搜索聊天"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="h-full w-full bg-transparent pl-9 pr-3 text-sm text-[#ececec] placeholder:text-[#b4b4b8] outline-none"
            />
          </div>
        </div>
      )}

      {/* App List */}
      <div className="flex-1 overflow-y-auto px-2">
        {!collapsed && (
          <div className="px-2 pb-2 pt-2 text-[12px] font-semibold text-[#8e8e93]">
            最近
          </div>
        )}
        <div className="space-y-0.5">
          {filteredApps.length === 0 ? (
            !collapsed && (
              <div className="px-2 py-6 text-center text-xs text-[#8e8e93]">
                {search ? '没有匹配的应用' : '暂无应用'}
              </div>
            )
          ) : (
            filteredApps.map((app) => (
              <div
                key={app.id}
                onClick={() => navigate(`/app/${app.id}`)}
                className={`group flex h-10 items-center gap-2.5 rounded-lg px-2.5 cursor-pointer transition-colors hover:bg-[#212121] ${
                  collapsed ? 'justify-center' : ''
                }`}
              >
                {!collapsed && (
                  <>
                    <span className="flex-1 truncate text-sm text-[#ececec]">
                      {app.appName || `应用 ${app.id}`}
                    </span>
                    <button
                      onClick={(e) => handleDeleteApp(e, app.id)}
                      disabled={deletingId === app.id}
                      className="hidden shrink-0 rounded p-1 text-[#8e8e93] transition-colors hover:bg-[#333333] hover:text-[#ececec] group-hover:flex disabled:opacity-40"
                      aria-label="删除应用"
                    >
                      <SidebarIcon name="delete" />
                    </button>
                  </>
                )}
                {collapsed && (
                  <div className="flex h-5 w-5 shrink-0 items-center justify-center text-[#8e8e93]">
                    <SidebarIcon name="chat" />
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      {/* Bottom: User Info */}
      <div className="p-3">
        <div
          className={`flex h-12 items-center gap-2.5 rounded-lg px-2.5 ${
            collapsed ? 'justify-center' : ''
          }`}
        >
          <svg viewBox="0 0 28 28" fill="none" className="h-7 w-7 shrink-0" aria-hidden="true">
            <rect width="28" height="28" rx="7" fill="#2e2b5f"/>
            <ellipse cx="14" cy="27" rx="7.5" ry="5" fill="#5b5ea6"/>
            <circle cx="14" cy="12" r="6" fill="#fde9cf"/>
            <path d="M8 11C8 7 10.5 5 14 5C17.5 5 20 7 20 11" fill="#3d2b1f"/>
            <circle cx="11.5" cy="12" r="1.1" fill="#2d1b10"/>
            <circle cx="16.5" cy="12" r="1.1" fill="#2d1b10"/>
            <circle cx="12" cy="11.4" r="0.4" fill="white"/>
            <circle cx="17" cy="11.4" r="0.4" fill="white"/>
            <ellipse cx="10.2" cy="14.5" rx="1.3" ry="0.8" fill="#ffa098" opacity="0.45"/>
            <ellipse cx="17.8" cy="14.5" rx="1.3" ry="0.8" fill="#ffa098" opacity="0.45"/>
            <path d="M11.5 16C12.3 17.4 13.2 17.8 14 17.8C14.8 17.8 15.7 17.4 16.5 16" stroke="#c47a55" strokeWidth="0.9" fill="none" strokeLinecap="round"/>
          </svg>
          {!collapsed && (
            <>
              <span className="min-w-0 flex-1 truncate text-sm font-medium text-[#ececec]">{userAccount}</span>
              <button
                type="button"
                onClick={handleLogout}
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-[#b4b4b8] transition-colors hover:bg-[#212121] hover:text-[#ececec]"
                aria-label="Logout"
              >
                <SidebarIcon name="logout" />
              </button>
            </>
          )}
        </div>
      </div>
    </aside>
  );

  return (
    <div className="flex h-dvh bg-[#212121] text-[#ececec]">
      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
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
      <main className="flex flex-1 flex-col min-w-0 relative">
        {/* Top Bar for Mobile */}
        <header className="flex items-center justify-between px-4 py-3 lg:hidden absolute top-0 left-0 right-0 z-10">
          <button
            onClick={() => setMobileOpen(true)}
            className="flex h-10 w-10 items-center justify-center rounded-lg text-[#ececec] hover:bg-[#2f2f2f]"
            aria-label="打开菜单"
          >
            <SidebarIcon name="expand" />
          </button>
        </header>

        {/* Center Content */}
        <div className="flex flex-1 flex-col items-center justify-center px-4 pb-8 w-full max-w-3xl mx-auto">
          {/* Greeting */}
          <div className="mb-8 text-center w-full">
            <h1 className="text-3xl font-semibold text-[#ececec] sm:text-4xl tracking-tight">
              有什么可以帮忙的
            </h1>
          </div>

          {/* Input Bar */}
          <div className="w-full rounded-2xl bg-[#2f2f2f] shadow-sm transition-colors focus-within:bg-[#333333]">
            <textarea
              ref={inputRef}
              placeholder="有问题，尽管问"
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
              className="block w-full resize-none bg-transparent px-5 pt-4 pb-2 text-[15px] text-[#ececec] placeholder:text-[#8e8e93] outline-none disabled:opacity-50"
              style={{ minHeight: '52px', maxHeight: '200px' }}
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
                    ? 'border-indigo-500 bg-[#3a3a3a] text-indigo-400'
                    : 'border-transparent bg-transparent text-[#b4b4b8] hover:bg-[#404040] hover:text-[#ececec]'
                }`}
              >
                <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" aria-hidden="true">
                  <path d="M12 2a7 7 0 0 1 7 7c0 2.38-1.19 4.47-3 5.74V17a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-2.26C6.19 13.47 5 11.38 5 9a7 7 0 0 1 7-7Z" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M9 21h6M10 17v4M14 17v4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                </svg>
                深度思考
              </button>
              <button
                onClick={handleSend}
                disabled={loading || !input.trim()}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-white text-black transition-all hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed"
                aria-label="发送"
              >
                {loading ? (
                  <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2.5" opacity="0.3" />
                    <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
                  </svg>
                ) : (
                  <SidebarIcon name="send" />
                )}
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
