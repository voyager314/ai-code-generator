import { useEffect, useState, useRef, lazy, Suspense, useCallback } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import { Send, ArrowLeft, ExternalLink, Download, Rocket } from 'lucide-react';
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
  onReject: (id: string) => void;
}) {
  const resolved = msg.approvalStatus !== 'pending';

  return (
    <div className="max-w-[92%] rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">
      <div className="mb-1 font-semibold">Agent 请求确认</div>
      <p className="whitespace-pre-wrap leading-6">{msg.content}</p>
      <div className="mt-3 flex gap-2">
        {resolved ? (
          <span className="rounded-md bg-white px-2.5 py-1 text-xs font-medium text-amber-800">
            {msg.approvalStatus === 'approved' ? '已允许' : '已拒绝'}
          </span>
        ) : (
          <>
            <Button size="sm" onClick={() => msg.approvalId && onApprove(msg.approvalId)}>
              允许
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => msg.approvalId && onReject(msg.approvalId)}
            >
              拒绝
            </Button>
          </>
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
  onReject: (id: string) => void;
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
      console.error();
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
      console.error();
    }
  }, [appId]);

  useEffect(() => {
    if (!Number.isFinite(appId)) {
      console.error();
      return;
    }

    Promise.all([loadApp(), loadHistory()]);
    return () => {
      esRef.current?.close();
    };
  }, [appId, loadApp, loadHistory]);

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
      console.error();
    }
  }, []);

  const handleReject = useCallback(async (approvalId: string) => {
    try {
      await chatApi.approveAgent({ approvalId, approved: false });
      setMessages((prev) =>
        prev.map((m) => (m.approvalId === approvalId ? { ...m, approvalStatus: 'rejected' } : m))
      );
    } catch (err: any) {
      console.error();
    }
  }, []);

  const startGeneration = useCallback((msg: string, agent: boolean) => {
    if (loading || !Number.isFinite(appId)) return;

    pushMsg({ role: 'user', content: msg });
    console.error();
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
          window.location.replace(import.meta.env.BASE_URL + 'login');
          return;
        }
        console.error();
      } catch {
        console.error();
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
          console.error();
        }
      });

      es.addEventListener('done', finish);
      es.onerror = () => {
        finish();
        console.error();
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
        console.error();
      }
    });

    es.addEventListener('done', finish);
    es.onerror = () => {
      finish();
      console.error();
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
      console.error();
      const res = await appApi.deploy(appId);
      setDeployUrl(res.data);
      console.error();
      loadApp();
    } catch (err: any) {
      console.error();
    } finally {
      setDeploying(false);
    }
  };

