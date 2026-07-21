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
    send: 'M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z',
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
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [files, setFiles] = useState<File[]>([]);

  const inputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

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
      navigate(`/app/${res.data}`, { state: { initMsg: prompt, initFiles: files.length > 0 ? files : undefined } });
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
      className={`flex h-full flex-col bg-background transition-all duration-300 ${
        collapsed ? 'w-[68px]' : 'w-[260px]'
      }`}
    >
      <div className="flex items-center justify-end px-3 pt-3 pb-2">
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="flex h-10 w-10 items-center justify-center rounded-xl text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
          aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}
        >
          <SidebarIcon name={collapsed ? 'expand' : 'collapse'} />
        </button>
      </div>

      {!collapsed && (
        <div className="px-3 pb-3">
          <div className="relative flex items-center h-10 rounded-xl hover:bg-accent transition-colors cursor-pointer text-foreground">
            <div className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
              <SidebarIcon name="search" />
            </div>
            <input
              type="text"
              placeholder="搜索应用"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="h-full w-full bg-transparent pl-9 pr-3 text-sm text-foreground placeholder:text-muted-foreground outline-none"
            />
          </div>
        </div>
      )}

      <div className="flex-1 overflow-y-auto px-2">
        {!collapsed && (
          <div className="px-2 pb-2 pt-2 text-[12px] font-semibold text-muted-foreground">
            最近
          </div>
        )}
        <div className="space-y-0.5">
          {filteredApps.length === 0 ? (
            !collapsed && (
              <div className="px-2 py-6 text-center text-xs text-muted-foreground">
                {search ? '没有匹配的应用' : '暂无应用'}
              </div>
            )
          ) : (
            filteredApps.map((app) => (
              <div
                key={app.id}
                onClick={() => navigate(`/app/${app.id}`)}
                className={`group flex h-10 items-center gap-2.5 rounded-xl px-2.5 cursor-pointer transition-colors hover:bg-accent ${
                  collapsed ? 'justify-center' : ''
                }`}
              >
                {!collapsed && (
                  <>
                    <span className="flex-1 truncate text-sm text-foreground">
                      {app.appName || `应用 ${app.id}`}
                    </span>
                    <button
                      onClick={(e) => handleDeleteApp(e, app.id)}
                      disabled={deletingId === app.id}
                      className="hidden shrink-0 rounded-lg p-1 text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground group-hover:flex disabled:opacity-40"
                      aria-label="删除应用"
                    >
                      <SidebarIcon name="delete" />
                    </button>
                  </>
                )}
                {collapsed && (
                  <div className="flex h-5 w-5 shrink-0 items-center justify-center text-muted-foreground">
                    <SidebarIcon name="chat" />
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      <div className="p-3">
        <div
          className={`flex h-12 items-center gap-2.5 rounded-xl px-2.5 ${
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
              <span className="min-w-0 flex-1 truncate text-sm font-medium text-foreground">{userAccount}</span>
              <button
                type="button"
                onClick={handleLogout}
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
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
    <div className="flex h-dvh bg-card text-foreground">
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <div className="hidden lg:flex h-full">{sidebar}</div>

      <div
        className={`fixed inset-y-0 left-0 z-50 lg:hidden transition-transform duration-300 ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {sidebar}
      </div>

      <main className="flex flex-1 flex-col min-w-0 relative">
        <header className="flex items-center justify-between px-4 py-3 lg:hidden absolute top-0 left-0 right-0 z-10">
          <button
            onClick={() => setMobileOpen(true)}
            className="flex h-10 w-10 items-center justify-center rounded-xl text-foreground hover:bg-accent"
            aria-label="打开菜单"
          >
            <SidebarIcon name="expand" />
          </button>
        </header>

        <div className="flex flex-1 flex-col items-center justify-center px-4 pb-8 w-full max-w-3xl mx-auto">
          <div className="mb-8 text-center w-full">
            <h1 className="text-3xl font-semibold text-foreground sm:text-4xl tracking-tight">
              有什么可以帮忙的
            </h1>
          </div>

          {files.length > 0 && (
            <div className="mb-2 flex flex-wrap gap-1.5 w-full max-w-3xl mx-auto px-2">
              {files.map((file, i) => (
                <span key={i} className="flex items-center gap-1 rounded-lg bg-secondary px-2 py-1 text-xs text-foreground">
                  <span className="max-w-[120px] truncate">{file.name}</span>
                  <button onClick={() => setFiles((prev) => prev.filter((_, idx) => idx !== i))} className="ml-0.5 text-muted-foreground hover:text-foreground">
                    ×
                  </button>
                </span>
              ))}
            </div>
          )}
          <input ref={fileInputRef} type="file" multiple className="hidden" onChange={(e) => { const selected = Array.from(e.target.files || []); if (selected.length > 0) setFiles((prev) => [...prev, ...selected]); e.target.value = ''; }} />
          <div className="flex w-full items-center gap-3 rounded-full bg-secondary px-2 py-2 transition-colors focus-within:bg-accent">
            <button
              type="button"
              disabled={loading}
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-background hover:text-foreground disabled:opacity-50"
              aria-label="上传文件"
              onClick={() => fileInputRef.current?.click()}
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" aria-hidden="true">
                <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
            <input
              ref={inputRef}
              type="text"
              placeholder="描述你想生成的应用"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  handleSend();
                }
              }}
              disabled={loading}
              className="min-w-0 flex-1 bg-transparent text-[15px] text-foreground placeholder:text-muted-foreground outline-none disabled:opacity-50"
            />
            <button
              onClick={handleSend}
              disabled={loading || !input.trim()}
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-foreground text-background transition-all hover:opacity-80 disabled:opacity-30 disabled:cursor-not-allowed"
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
      </main>
    </div>
  );
}
