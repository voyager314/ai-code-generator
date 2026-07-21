import { useEffect, useState, useRef, lazy, Suspense, useCallback } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { appApi, chatApi, readSSE } from '@/api';
import { useUserStore } from '@/store/user';
import type { AppDetailVO, AgentEvent } from '@/types';

const CodeViewer = lazy(() => import('@/components/CodeViewer'));

type AgentEventType = AgentEvent['type'];

interface ChatMessage {
  id: string;
  role: 'user' | 'ai' | 'event';
  content: string;
  eventType?: AgentEventType;
  toolName?: string;
  toolArgs?: string;
  approvalId?: string;
  approvalStatus?: 'pending' | 'approved' | 'rejected';
}

let nextMessageNumber = 0;
const nextId = () => `message-${++nextMessageNumber}`;

function Icon({ name }: { name: 'back' | 'send' | 'external' | 'download' | 'deploy' | 'attach' }) {
  const paths: Record<string, string> = {
    back: 'M15 18l-6-6 6-6M9 12h12',
    send: 'M4 12l16-8-5 16-3-7-8-1Z',
    external: 'M14 4h6v6M20 4l-9 9M20 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h5',
    download: 'M12 3v11M7 9l5 5 5-5M5 20h14',
    deploy: 'M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3ZM12 12l8-4.5M12 12v9M12 12 4 7.5',
    attach: 'M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48',
  };

  return (
    <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" aria-hidden="true">
      <path
        d={paths[name]}
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ApprovalCard({
  msg,
  onApprove,
  onReject,
}: {
  msg: ChatMessage;
  onApprove: (id: string) => void;
  onReject: (id: string, customMessage: string) => void;
}) {
  const [showRejectInput, setShowRejectInput] = useState(false);
  const [rejectMsg, setRejectMsg] = useState('');
  const resolved = msg.approvalStatus !== 'pending';

  const handleRejectConfirm = () => {
    if (!msg.approvalId) return;
    onReject(msg.approvalId, rejectMsg);
  };

  return (
    <div className="max-w-[92%] rounded-xl border border-amber-500/20 bg-amber-500/10 p-3 text-sm text-amber-200">
      <div className="mb-1 font-semibold text-amber-300">Agent 请求确认</div>
      <p className="whitespace-pre-wrap leading-6">{msg.content}</p>
      <div className="mt-3 flex flex-col gap-2">
        {resolved ? (
          <span className="self-start rounded-lg bg-amber-500/20 px-2.5 py-1 text-xs font-medium text-amber-300">
            {msg.approvalStatus === 'approved' ? '已允许' : '已拒绝'}
          </span>
        ) : showRejectInput ? (
          <>
            <input
              type="text"
              className="w-full rounded-lg border border-amber-500/30 bg-card px-2.5 py-1.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-amber-500/40"
              placeholder="告诉 Agent 为什么拒绝，或者希望它怎么做（可留空）"
              value={rejectMsg}
              onChange={(e) => setRejectMsg(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleRejectConfirm()}
              autoFocus
            />
            <div className="flex gap-2">
              <button
                onClick={handleRejectConfirm}
                className="rounded-lg bg-amber-500/20 px-3 py-1.5 text-xs font-medium text-amber-300 hover:bg-amber-500/30 transition-colors"
              >
                确认拒绝
              </button>
              <button
                onClick={() => {
                  setShowRejectInput(false);
                  setRejectMsg('');
                }}
                className="rounded-lg bg-secondary px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-accent transition-colors"
              >
                取消
              </button>
            </div>
          </>
        ) : (
          <div className="flex gap-2">
            <button
              onClick={() => msg.approvalId && onApprove(msg.approvalId)}
              className="rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/80 transition-colors"
            >
              允许
            </button>
            <button
              onClick={() => setShowRejectInput(true)}
              className="rounded-lg bg-secondary px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-accent transition-colors"
            >
              拒绝
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function MessageBubble({
  msg,
  onApprove,
  onReject,
}: {
  msg: ChatMessage;
  onApprove: (id: string) => void;
  onReject: (id: string, customMessage: string) => void;
}) {
  if (msg.role === 'user') {
    return (
      <div className="flex justify-end">
        <div className="max-w-[88%] rounded-xl bg-primary px-3.5 py-2.5 text-sm text-primary-foreground">
          <pre className="whitespace-pre-wrap font-sans leading-6">{msg.content}</pre>
        </div>
      </div>
    );
  }

  if (msg.eventType === 'approval_request') {
    return (
      <div className="flex justify-start">
        <ApprovalCard msg={msg} onApprove={onApprove} onReject={onReject} />
      </div>
    );
  }

  if (msg.eventType === 'tool_request' || msg.eventType === 'tool_executed') {
    const done = msg.eventType === 'tool_executed';
    return (
      <div className="flex justify-start">
        <div className="max-w-[92%] rounded-xl border border-blue-500/20 bg-blue-500/10 px-3 py-2 text-xs">
          <div className="flex flex-wrap items-center gap-2">
            <span className={done ? 'text-emerald-400' : 'text-blue-400'}>
              {done ? '已执行' : '调用工具'}
            </span>
            <span className="font-semibold text-foreground">{msg.toolName}</span>
            {msg.toolArgs && <span className="truncate text-muted-foreground">{msg.toolArgs}</span>}
          </div>
          {done && msg.content && <p className="mt-1 line-clamp-2 text-muted-foreground">{msg.content}</p>}
        </div>
      </div>
    );
  }

  if (
    msg.eventType === 'reflection_started' ||
    msg.eventType === 'reflection_result' ||
    msg.eventType === 'reflection_retry'
  ) {
    return (
      <div className="flex justify-start">
        <div className="max-w-[92%] rounded-xl border border-cyan-500/20 bg-cyan-500/10 px-3 py-2 text-xs leading-5 text-cyan-300">
          {msg.content}
        </div>
      </div>
    );
  }

  if (msg.eventType === 'agent_error') {
    return (
      <div className="flex justify-start">
        <div className="max-w-[92%] rounded-xl border border-red-500/20 bg-red-500/10 px-3 py-2 text-sm leading-6 text-red-300">
          {msg.content}
        </div>
      </div>
    );
  }

  if (msg.eventType === 'agent_complete') {
    return (
      <div className="flex justify-start">
        <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 px-3 py-2 text-xs font-medium text-emerald-400">
          生成完成
        </div>
      </div>
    );
  }

  return (
    <div className="flex justify-start">
      <div className="max-w-[88%] rounded-xl bg-secondary px-3.5 py-2.5 text-sm text-foreground">
        <pre className="whitespace-pre-wrap font-sans leading-6">{msg.content}</pre>
      </div>
    </div>
  );
}

interface NavState {
  initMsg?: string;
  initFiles?: File[];
}

export default function AppChat() {
  const { id } = useParams<{ id: string }>();
  const appId = Number(id);
  const navigate = useNavigate();
  const location = useLocation();
  const setUser = useUserStore((s) => s.setUser);
  const navState = location.state as NavState | null;

  const [app, setApp] = useState<AppDetailVO | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [appLoading, setAppLoading] = useState(true);
  const [deploying, setDeploying] = useState(false);
  const [deployUrl, setDeployUrl] = useState('');
  const [notice, setNotice] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const [codeRefreshKey, setCodeRefreshKey] = useState(0);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadApp = useCallback(async () => {
    try {
      setAppLoading(true);
      const res = await appApi.getDetail(appId);
      setApp(res.data);
    } catch (err: any) {
      setNotice(err.message || '应用加载失败。');
    } finally {
      setAppLoading(false);
    }
  }, [appId]);

  const loadHistory = useCallback(async () => {
    try {
      const res = await chatApi.getHistory({
        appId,
        pageNum: 1,
        pageSize: 50,
        sortField: 'createTime',
        sortOrder: 'ascend',
      });
      const historyMessages: ChatMessage[] = res.data.records.map((h) => ({
        id: nextId(),
        role: h.messageType === 'user' ? 'user' : 'ai',
        content: h.message,
      }));
      setMessages(historyMessages);
    } catch (err: any) {
      setNotice(err.message || '历史对话加载失败。');
    }
  }, [appId]);

  useEffect(() => {
    if (!Number.isFinite(appId)) {
      setNotice('应用地址无效。');
      return;
    }
    Promise.all([loadApp(), loadHistory()]);
    return () => {
      abortRef.current?.abort();
    };
  }, [appId, loadApp, loadHistory]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const pushMsg = useCallback((msg: Omit<ChatMessage, 'id'>) => {
    setMessages((prev) => [...prev, { ...msg, id: nextId() }]);
  }, []);

  const appendAiText = useCallback((text: string) => {
    setMessages((prev) => {
      const last = prev[prev.length - 1];
      if (last?.role === 'ai' && !last.eventType) {
        return [...prev.slice(0, -1), { ...last, content: last.content + text }];
      }
      return [...prev, { id: nextId(), role: 'ai', content: text }];
    });
  }, []);

  const handleApprove = useCallback(async (approvalId: string) => {
    try {
      await chatApi.approveAgent({ approvalId, approved: true });
      setMessages((prev) =>
        prev.map((m) => (m.approvalId === approvalId ? { ...m, approvalStatus: 'approved' } : m))
      );
    } catch (err: any) {
      setNotice(err.message || '审批失败。');
    }
  }, []);

  const handleReject = useCallback(async (approvalId: string, customMessage: string) => {
    try {
      await chatApi.approveAgent({ approvalId, approved: false, customMessage: customMessage || undefined });
      setMessages((prev) =>
        prev.map((m) => (m.approvalId === approvalId ? { ...m, approvalStatus: 'rejected' } : m))
      );
    } catch (err: any) {
      setNotice(err.message || '操作失败。');
    }
  }, []);

  const startGeneration = useCallback(
    async (msg: string, sendFiles?: File[]) => {
      if (loading || !Number.isFinite(appId)) return;
      const prompt = msg.trim();
      if (!prompt) {
        setNotice('消息不能为空');
        return;
      }

      pushMsg({ role: 'user', content: prompt });
      setNotice('');
      setLoading(true);

      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;

      let finished = false;
      const finish = () => {
        if (finished) return;
        finished = true;
        abortRef.current = null;
        setLoading(false);
      };

      try {
        const response = await appApi.chatToGenCode({
          appId,
          msg: prompt,
          files: sendFiles,
          signal: controller.signal,
        });

        if (!response.ok) {
          const text = await response.text().catch(() => '');
          throw new Error(text || `HTTP ${response.status}`);
        }

        await readSSE(response, {
          onMessage: (data) => {
            try {
              const outer = JSON.parse(data) as { d: string };
              if (!outer.d) return;

              let event: AgentEvent;
              try {
                event = JSON.parse(outer.d) as AgentEvent;
              } catch {
                appendAiText(outer.d);
                return;
              }

              switch (event.type) {
                case 'ai_response':
                  appendAiText(event.content);
                  break;
                case 'tool_request':
                  pushMsg({
                    role: 'event',
                    content: event.content ?? '',
                    eventType: 'tool_request',
                    toolName: event.toolName,
                    toolArgs: event.toolArgs,
                  });
                  break;
                case 'tool_executed':
                  pushMsg({
                    role: 'event',
                    content: event.content ?? '',
                    eventType: 'tool_executed',
                    toolName: event.toolName,
                    toolArgs: event.toolArgs,
                  });
                  break;
                case 'approval_request':
                  pushMsg({
                    role: 'event',
                    content: event.content,
                    eventType: 'approval_request',
                    approvalId: event.approvalId,
                    approvalStatus: 'pending',
                  });
                  break;
                case 'approval_result':
                  setMessages((prev) =>
                    prev.map((m) =>
                      m.approvalId === event.approvalId
                        ? { ...m, approvalStatus: event.content === 'approved' ? 'approved' : 'rejected' }
                        : m
                    )
                  );
                  break;
                case 'reflection_started':
                  pushMsg({
                    role: 'event',
                    content: '正在进行代码质量检查...',
                    eventType: 'reflection_started',
                  });
                  break;
                case 'reflection_result':
                  pushMsg({ role: 'event', content: event.content, eventType: 'reflection_result' });
                  break;
                case 'reflection_retry':
                  pushMsg({ role: 'event', content: event.content, eventType: 'reflection_retry' });
                  break;
                case 'agent_complete':
                  pushMsg({ role: 'event', content: '', eventType: 'agent_complete' });
                  setCodeRefreshKey((k) => k + 1);
                  loadApp();
                  break;
                case 'agent_error':
                  pushMsg({ role: 'event', content: event.content, eventType: 'agent_error' });
                  finish();
                  break;
              }
            } catch {
              setNotice('Agent 消息格式异常。');
            }
          },
          onBusinessError: (data) => {
            try {
              const error = JSON.parse(data) as { errorCode?: number; msg?: string };
              if (error.errorCode === 40100) {
                setUser(null);
                window.location.replace(import.meta.env.BASE_URL + 'login');
                return;
              }
              setNotice(error.msg || '生成失败，请稍后重试。');
            } catch {
              setNotice('生成失败，请稍后重试。');
            }
            finish();
          },
          onDone: finish,
          onError: (err) => {
            if (err instanceof DOMException && err.name === 'AbortError') return;
            finish();
            setNotice('连接中断，请重试。');
          },
        });

        finish();
      } catch (err: any) {
        if (err?.name === 'AbortError') return;
        finish();
        setNotice(err.message || '连接失败。');
      }
    },
    [appId, loading, pushMsg, appendAiText, setUser, loadApp]
  );

  const handleSend = () => {
    if (!input.trim()) return;
    const msg = input.trim();
    setInput('');
    const sendFiles = files.length > 0 ? [...files] : undefined;
    setFiles([]);
    startGeneration(msg, sendFiles);
  };

  const initConsumed = useRef(false);
  useEffect(() => {
    if (navState?.initMsg && !appLoading && !initConsumed.current) {
      initConsumed.current = true;
      startGeneration(navState.initMsg, navState.initFiles);
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [navState, appLoading, startGeneration, navigate, location.pathname]);

  const handleDeploy = async () => {
    try {
      setDeploying(true);
      setNotice('');
      const res = await appApi.deploy(appId);
      setDeployUrl(res.data);
      setNotice(`部署成功：${res.data}`);
      loadApp();
    } catch (err: any) {
      setNotice(err.message || '部署失败。');
    } finally {
      setDeploying(false);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(e.target.files || []);
    setFiles((prev) => [...prev, ...selected]);
    e.target.value = '';
  };

  const removeFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <main className="flex min-h-dvh flex-col bg-background text-foreground">
      <header className="border-b border-border bg-card px-4 py-3">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex min-w-0 items-center gap-3">
            <button
              type="button"
              onClick={() => navigate('/')}
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-muted-foreground transition hover:bg-accent hover:text-foreground"
              aria-label="返回作品列表"
            >
              <Icon name="back" />
            </button>
            <div className="min-w-0">
              <div className="truncate text-sm font-semibold text-foreground">
                {appLoading ? '加载应用...' : app?.appName || `应用 ${appId}`}
              </div>
              <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                <span>{app?.codeGenType || '未生成'}</span>
                {app?.deployKey && <span className="h-1 w-1 rounded-full bg-muted-foreground" />}
                {app?.deployKey && <span>已部署</span>}
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {(deployUrl || app?.deployKey) && (
              <a
                href={deployUrl || `http://${app?.deployKey}`}
                target="_blank"
                rel="noreferrer"
                className="inline-flex h-10 items-center gap-2 rounded-xl border border-border bg-card px-3 text-sm font-medium text-foreground transition hover:bg-accent"
              >
                <Icon name="external" />
                访问
              </a>
            )}
            <button
              onClick={() => appApi.download(appId)}
              className="inline-flex h-10 items-center gap-2 rounded-xl border border-border bg-card px-3 text-sm font-medium text-foreground transition hover:bg-accent"
            >
              <Icon name="download" />
              下载
            </button>
            <button
              onClick={handleDeploy}
              disabled={deploying || appLoading}
              className="inline-flex h-10 items-center gap-2 rounded-xl bg-primary px-4 text-sm font-medium text-primary-foreground transition hover:bg-primary/80 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Icon name="deploy" />
              {deploying ? '部署中' : '部署'}
            </button>
          </div>
        </div>
      </header>

      {notice && (
        <div className="border-b border-primary/20 bg-primary/10 px-4 py-2 text-sm text-primary" role="status">
          {notice}
        </div>
      )}

      <section className="flex flex-1 flex-col overflow-hidden lg:flex-row">
        <div className="flex min-h-[46dvh] flex-col border-b border-border bg-card lg:min-h-0 lg:w-[420px] lg:border-b-0 lg:border-r">
          <div className="border-b border-border px-4 py-3">
            <div className="text-sm font-semibold">对话</div>
            <div className="mt-1 text-xs text-muted-foreground">
              描述修改需求，Agent 会自动执行文件操作并更新代码。
            </div>
          </div>

          <div className="flex-1 overflow-y-auto p-4">
            {messages.length === 0 && !loading ? (
              <div className="rounded-xl border border-dashed border-border bg-secondary p-4 text-sm leading-6 text-muted-foreground">
                还没有对话记录。输入你想调整的页面、功能或交互，开始一次生成。
              </div>
            ) : (
              <div className="space-y-3">
                {messages.map((msg) => (
                  <MessageBubble key={msg.id} msg={msg} onApprove={handleApprove} onReject={handleReject} />
                ))}
                {loading && (
                  <div className="flex justify-start">
                    <div className="rounded-xl bg-secondary px-3 py-2 text-sm text-muted-foreground">
                      Agent 正在执行...
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          <div className="border-t border-border bg-card p-3">
            {files.length > 0 && (
              <div className="mb-2 flex flex-wrap gap-1.5 px-1">
                {files.map((file, i) => (
                  <span key={i} className="flex items-center gap-1 rounded-lg bg-secondary px-2 py-1 text-xs text-foreground">
                    <Icon name="attach" />
                    <span className="max-w-[120px] truncate">{file.name}</span>
                    <button onClick={() => removeFile(i)} className="ml-0.5 text-muted-foreground hover:text-foreground">
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}
            <div className="flex items-center gap-3 rounded-full bg-secondary px-2 py-2 transition-colors focus-within:bg-accent">
              <input ref={fileInputRef} type="file" multiple className="hidden" onChange={handleFileSelect} />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={loading}
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-background hover:text-foreground disabled:opacity-50"
                aria-label="添加附件"
              >
                <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" aria-hidden="true">
                  <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </button>
              <input
                placeholder="描述目标，Agent 会自动执行文件操作"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                disabled={loading}
                className="min-w-0 flex-1 bg-transparent text-sm text-foreground placeholder:text-muted-foreground outline-none disabled:opacity-50"
                aria-label="对话输入"
              />
              <button
                type="button"
                onClick={handleSend}
                disabled={loading || !input.trim()}
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-foreground text-background transition-all hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                aria-label="发送消息"
              >
                <Icon name="send" />
              </button>
            </div>
          </div>
        </div>

        <div className="flex min-h-[48dvh] flex-1 flex-col overflow-hidden bg-background lg:min-h-0">
          <div className="flex-1 overflow-hidden">
            <Suspense
              fallback={
                <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                  正在加载代码...
                </div>
              }
            >
              <CodeViewer appId={appId} refreshKey={codeRefreshKey} />
            </Suspense>
          </div>
        </div>
      </section>
    </main>
  );
}
