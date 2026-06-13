import { useEffect, useState, useRef, lazy, Suspense, useCallback } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { appApi, chatApi } from '@/api';
import { useUserStore } from '@/store/user';
import type { AppDetailVO, AgentEvent } from '@/types';

const CodeViewer = lazy(() => import('@/components/CodeViewer'));

type AgentEventType =
  | 'ai_response'
  | 'tool_request'
  | 'tool_executed'
  | 'approval_request'
  | 'approval_result'
  | 'reflection_started'
  | 'reflection_result'
  | 'reflection_retry'
  | 'agent_complete'
  | 'agent_error';

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

function Icon({ name }: { name: 'back' | 'send' | 'external' | 'download' | 'deploy' }) {
  const paths = {
    back: 'M15 18l-6-6 6-6M9 12h12',
    send: 'M4 12l16-8-5 16-3-7-8-1Z',
    external: 'M14 4h6v6M20 4l-9 9M20 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h5',
    download: 'M12 3v11M7 9l5 5 5-5M5 20h14',
    deploy: 'M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3ZM12 12l8-4.5M12 12v9M12 12 4 7.5',
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
    <div className="max-w-[92%] rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">
      <div className="mb-1 font-semibold">Agent 请求确认</div>
      <p className="whitespace-pre-wrap leading-6">{msg.content}</p>
      <div className="mt-3 flex flex-col gap-2">
        {resolved ? (
          <span className="rounded-md bg-white px-2.5 py-1 text-xs font-medium text-amber-800 self-start">
            {msg.approvalStatus === 'approved' ? '已允许' : '已拒绝'}
          </span>
        ) : showRejectInput ? (
          <>
            <input
              type="text"
              className="w-full rounded-md border border-amber-300 bg-white px-2.5 py-1.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-amber-400"
              placeholder="告诉 Agent 为什么拒绝，或者希望它怎么做（可留空）"
              value={rejectMsg}
              onChange={(e) => setRejectMsg(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleRejectConfirm()}
              autoFocus
            />
            <div className="flex gap-2">
              <Button size="sm" variant="outline" onClick={handleRejectConfirm}>
                确认拒绝
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => { setShowRejectInput(false); setRejectMsg(''); }}
              >
                取消
              </Button>
            </div>
          </>
        ) : (
          <div className="flex gap-2">
            <Button size="sm" onClick={() => msg.approvalId && onApprove(msg.approvalId)}>
              允许
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => setShowRejectInput(true)}
            >
              拒绝
            </Button>
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
        <div className="max-w-[88%] rounded-lg bg-slate-950 px-3 py-2 text-sm text-white">
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
        <div className="max-w-[92%] rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600 shadow-sm">
          <div className="flex flex-wrap items-center gap-2">
            <span className={done ? 'text-emerald-700' : 'text-cyan-700'}>
              {done ? '已执行' : '调用工具'}
            </span>
            <span className="font-semibold text-slate-900">{msg.toolName}</span>
            {msg.toolArgs && <span className="truncate text-slate-400">{msg.toolArgs}</span>}
          </div>
          {done && msg.content && <p className="mt-1 line-clamp-2">{msg.content}</p>}
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
        <div className="max-w-[92%] rounded-lg border border-cyan-100 bg-cyan-50 px-3 py-2 text-xs leading-5 text-cyan-900">
          {msg.content}
        </div>
      </div>
    );
  }

  if (msg.eventType === 'agent_error') {
    return (
      <div className="flex justify-start">
        <div className="max-w-[92%] rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm leading-6 text-red-700">
          {msg.content}
        </div>
      </div>
    );
  }

  if (msg.eventType === 'agent_complete') {
    return (
      <div className="flex justify-start">
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs font-medium text-emerald-700">
          生成完成
        </div>
      </div>
    );
  }

  return (
    <div className="flex justify-start">
      <div className="max-w-[88%] rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-900">
        <pre className="whitespace-pre-wrap font-sans leading-6">{msg.content}</pre>
      </div>
    </div>
  );
}

function AgentSwitch({
  enabled,
  disabled,
  onToggle,
}: {
  enabled: boolean;
  disabled: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={enabled}
      disabled={disabled}
      onClick={onToggle}
      className="flex min-h-11 items-center gap-2 rounded-lg px-2 text-sm text-slate-600 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-500 disabled:cursor-not-allowed disabled:opacity-50"
    >
      <span
        className={`relative h-5 w-9 rounded-full transition-colors ${
          enabled ? 'bg-cyan-600' : 'bg-slate-300'
        }`}
      >
        <span
          className={`absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform ${
            enabled ? 'translate-x-4' : 'translate-x-0.5'
          }`}
        />
      </span>
      Agent
    </button>
  );
}

interface NavState {
  initMsg?: string;
  agentMode?: boolean;
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
  const [agentMode, setAgentMode] = useState(false);
  const [view, setView] = useState<'code' | 'preview'>('code');
  const [notice, setNotice] = useState('');

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const esRef = useRef<EventSource | null>(null);

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
      esRef.current?.close();
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

  const startGeneration = useCallback((msg: string, agent: boolean) => {
    if (loading || !Number.isFinite(appId)) return;

    pushMsg({ role: 'user', content: msg });
    setNotice('');
    setLoading(true);

    esRef.current?.close();

    const url =
      `/api/app/chat/gen/code` +
      `?appId=${appId}` +
      `&msg=${encodeURIComponent(msg)}` +
      `&agent=${agent}`;

    const es = new EventSource(url);
    esRef.current = es;

    const finish = () => {
      es.close();
      esRef.current = null;
      setLoading(false);
    };

    es.addEventListener('business-error', (event) => {
      try {
        const error = JSON.parse((event as MessageEvent).data) as {
          errorCode?: number;
          msg?: string;
        };
        if (error.errorCode === 40100) {
          setUser(null);
          window.location.replace('/login');
          return;
        }
        setNotice(error.msg || '生成失败，请稍后重试。');
      } catch {
        setNotice('生成失败，请稍后重试。');
      } finally {
        finish();
      }
    });

    if (!agent) {
      es.addEventListener('message', (e) => {
        try {
          const { d } = JSON.parse(e.data) as { d: string };
          if (d) appendAiText(d);
        } catch {
          setNotice('收到的生成内容格式异常。');
        }
      });

      es.addEventListener('done', finish);
      es.onerror = () => {
        finish();
        setNotice('连接中断，请重试。');
      };
      return;
    }

    es.addEventListener('message', (e) => {
      try {
        const outer = JSON.parse(e.data) as { d: string };
        if (!outer.d) return;
        const event = JSON.parse(outer.d) as AgentEvent;

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
    });

    es.addEventListener('done', finish);
    es.onerror = () => {
      finish();
      setNotice('连接中断，请重试。');
    };
  }, [appId, loading, pushMsg, appendAiText, setUser, loadApp]);

  const handleSend = () => {
    if (!input.trim()) return;
    const msg = input.trim();
    setInput('');
    startGeneration(msg, agentMode);
  };

  const initConsumed = useRef(false);
  useEffect(() => {
    if (navState?.initMsg && !appLoading && !initConsumed.current) {
      initConsumed.current = true;
      setAgentMode(navState.agentMode ?? false);
      startGeneration(navState.initMsg, navState.agentMode ?? false);
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

  return (
    <main className="flex min-h-dvh flex-col bg-slate-50 text-slate-950">
      <header className="border-b border-slate-200 bg-white px-4 py-3">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex min-w-0 items-center gap-3">
            <button
              type="button"
              onClick={() => navigate('/')}
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-slate-600 transition hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-500"
              aria-label="返回作品列表"
            >
              <Icon name="back" />
            </button>
            <div className="min-w-0">
              <div className="truncate text-sm font-semibold text-slate-950">
                {appLoading ? '加载应用...' : app?.appName || `应用 ${appId}`}
              </div>
              <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                <span>{app?.codeGenType || '未生成'}</span>
                {app?.deployKey && <span className="h-1 w-1 rounded-full bg-slate-300" />}
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
                className="inline-flex h-10 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-500"
              >
                <Icon name="external" />
                访问
              </a>
            )}
            <Button variant="outline" size="sm" onClick={() => appApi.download(appId)}>
              <Icon name="download" />
              <span className="ml-2">下载</span>
            </Button>
            <Button size="sm" onClick={handleDeploy} disabled={deploying || appLoading}>
              <Icon name="deploy" />
              <span className="ml-2">{deploying ? '部署中' : '部署'}</span>
            </Button>
          </div>
        </div>
      </header>

      {notice && (
        <div className="border-b border-cyan-100 bg-cyan-50 px-4 py-2 text-sm text-cyan-900" role="status">
          {notice}
        </div>
      )}

      <section className="flex flex-1 flex-col overflow-hidden lg:flex-row">
        <div className="flex min-h-[46dvh] flex-col border-b border-slate-200 bg-white lg:min-h-0 lg:w-[420px] lg:border-b-0 lg:border-r">
          <div className="border-b border-slate-100 px-4 py-3">
            <div className="text-sm font-semibold">对话</div>
            <div className="mt-1 text-xs text-slate-500">
              描述修改需求，系统会继续生成并更新代码。
            </div>
          </div>

          <div className="flex-1 overflow-y-auto p-4">
            {messages.length === 0 && !loading ? (
              <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
                还没有对话记录。输入你想调整的页面、功能或交互，开始一次生成。
              </div>
            ) : (
              <div className="space-y-3">
                {messages.map((msg) => (
                  <MessageBubble
                    key={msg.id}
                    msg={msg}
                    onApprove={handleApprove}
                    onReject={handleReject}
                  />
                ))}
                {loading && (
                  <div className="flex justify-start">
                    <div className="rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-500">
                      {agentMode ? 'Agent 正在执行...' : '正在生成...'}
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          <div className="border-t border-slate-200 bg-white p-3">
            <div className="mb-2 flex items-center justify-between">
              <span className="text-xs text-slate-500">生成模式</span>
              <AgentSwitch
                enabled={agentMode}
                disabled={loading}
                onToggle={() => setAgentMode((v) => !v)}
              />
            </div>
            <div className="flex gap-2">
              <Input
                placeholder={agentMode ? '描述目标，Agent 会自动执行文件操作' : '描述你想生成或修改的内容'}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                disabled={loading}
                className="h-11 text-sm"
                aria-label="对话输入"
              />
              <button
                type="button"
                onClick={handleSend}
                disabled={loading || !input.trim()}
                className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-slate-950 text-white transition hover:bg-slate-800 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-cyan-200 disabled:cursor-not-allowed disabled:opacity-50"
                aria-label="发送消息"
              >
                <Icon name="send" />
              </button>
            </div>
          </div>
        </div>

        <div className="flex min-h-[48dvh] flex-1 flex-col overflow-hidden bg-slate-50 lg:min-h-0">
          <div className="flex items-center gap-2 border-b border-slate-200 bg-white px-4 py-2">
            {(['code', 'preview'] as const).map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => setView(value)}
                className={`min-h-10 rounded-lg px-3 text-sm font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-500 ${
                  view === value
                    ? 'bg-slate-950 text-white'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'
                }`}
              >
                {value === 'code' ? '代码' : '预览'}
              </button>
            ))}
          </div>

          <div className="flex-1 overflow-hidden">
            {view === 'code' ? (
              <Suspense
                fallback={
                  <div className="flex h-full items-center justify-center text-sm text-slate-500">
                    正在加载代码...
                  </div>
                }
              >
                <CodeViewer appId={appId} />
              </Suspense>
            ) : (
              <div className="flex h-full items-center justify-center bg-white p-6 text-center">
                <div className="max-w-sm">
                  <div className="text-base font-semibold text-slate-900">预览暂未接入</div>
                  <p className="mt-2 text-sm leading-6 text-slate-500">
                    当前可以先查看生成代码。部署后可通过右上角“访问”打开线上页面。
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </section>
    </main>
  );
}
