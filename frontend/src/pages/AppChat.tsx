import { useEffect, useState, useRef, lazy, Suspense, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { appApi, chatApi } from '@/api';
import type { AppDetailVO, AgentEvent } from '@/types';

const CodeViewer = lazy(() => import('@/components/CodeViewer'));

// ─── Message model ────────────────────────────────────────────────────────────

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

let _id = 0;
const nextId = () => `m${++_id}`;

// ─── Sub-components (defined outside AppChat to avoid rerender-no-inline-components) ──

function ApprovalCard({
  msg,
  onApprove,
  onReject,
}: {
  msg: ChatMessage;
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
}) {
  return (
    <div className="border border-orange-300 bg-orange-50 rounded-lg p-3 max-w-[85%]">
      <p className="text-xs font-semibold text-orange-700 mb-1">⚠️ Agent 请求审批</p>
      <p className="text-sm text-gray-800 mb-3 whitespace-pre-wrap">{msg.content}</p>
      {msg.approvalStatus === 'pending' ? (
        <div className="flex gap-2">
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
        </div>
      ) : (
        <span
          className={`text-xs font-medium ${
            msg.approvalStatus === 'approved' ? 'text-green-600' : 'text-red-600'
          }`}
        >
          {msg.approvalStatus === 'approved' ? '✓ 已允许' : '✗ 已拒绝'}
        </span>
      )}
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
  // User message
  if (msg.role === 'user') {
    return (
      <div className="flex justify-end">
        <div className="max-w-[85%] px-3 py-2 rounded bg-blue-500 text-white text-sm">
          <pre className="whitespace-pre-wrap font-sans">{msg.content}</pre>
        </div>
      </div>
    );
  }

  // Agent approval request
  if (msg.eventType === 'approval_request') {
    return (
      <div className="flex justify-start">
        <ApprovalCard msg={msg} onApprove={onApprove} onReject={onReject} />
      </div>
    );
  }

  // Tool call / result
  if (msg.eventType === 'tool_request' || msg.eventType === 'tool_executed') {
    const done = msg.eventType === 'tool_executed';
    return (
      <div className="flex justify-start">
        <div className="max-w-[85%] px-3 py-1.5 rounded-md bg-gray-50 border border-gray-200 text-xs text-gray-600">
          <div className="flex items-center gap-1">
            <span>{done ? '✅' : '⚙️'}</span>
            <span className="font-medium">{msg.toolName}</span>
            {msg.toolArgs && (
              <span className="text-gray-400 truncate max-w-[200px]">{msg.toolArgs}</span>
            )}
          </div>
          {done && msg.content && (
            <p className="mt-1 text-gray-500 line-clamp-2">{msg.content}</p>
          )}
        </div>
      </div>
    );
  }

  // Reflection events
  if (
    msg.eventType === 'reflection_started' ||
    msg.eventType === 'reflection_result' ||
    msg.eventType === 'reflection_retry'
  ) {
    return (
      <div className="flex justify-start">
        <div className="max-w-[85%] px-3 py-1.5 rounded-md bg-purple-50 border border-purple-200 text-xs text-purple-700">
          🔍 {msg.content}
        </div>
      </div>
    );
  }

  // Agent error
  if (msg.eventType === 'agent_error') {
    return (
      <div className="flex justify-start">
        <div className="max-w-[85%] px-3 py-2 rounded bg-red-50 border border-red-200 text-sm text-red-700">
          ❌ {msg.content}
        </div>
      </div>
    );
  }

  // Agent complete
  if (msg.eventType === 'agent_complete') {
    return (
      <div className="flex justify-start">
        <div className="px-3 py-1 rounded-md bg-green-50 border border-green-200 text-xs text-green-700">
          ✓ 生成完成
        </div>
      </div>
    );
  }

  // Normal AI text bubble
  return (
    <div className="flex justify-start">
      <div className="max-w-[85%] px-3 py-2 rounded bg-gray-100 text-gray-900 text-sm">
        <pre className="whitespace-pre-wrap font-sans">{msg.content}</pre>
      </div>
    </div>
  );
}

// ─── AgentModeToggle ──────────────────────────────────────────────────────────

function AgentModeToggle({
  enabled,
  disabled,
  onToggle,
}: {
  enabled: boolean;
  disabled: boolean;
  onToggle: () => void;
}) {
  return (
    <label
      className={`flex items-center gap-1.5 select-none ${
        disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
      }`}
    >
      <div
        onClick={disabled ? undefined : onToggle}
        className={`relative w-8 h-4 rounded-full transition-colors ${
          enabled ? 'bg-blue-500' : 'bg-gray-300'
        }`}
      >
        <div
          className={`absolute top-0.5 w-3 h-3 bg-white rounded-full shadow transition-transform ${
            enabled ? 'translate-x-4' : 'translate-x-0.5'
          }`}
        />
      </div>
      <span className="text-xs text-gray-500">Agent</span>
    </label>
  );
}

// ─── AppChat ──────────────────────────────────────────────────────────────────

export default function AppChat() {
  const { id } = useParams<{ id: string }>();
  const appId = Number(id);

  const [app, setApp] = useState<AppDetailVO | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [agentMode, setAgentMode] = useState(false);
  const [view, setView] = useState<'code' | 'preview'>('code');

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const esRef = useRef<EventSource | null>(null);

  // ── Init ──────────────────────────────────────────────────────────────────

  useEffect(() => {
    Promise.all([loadApp(), loadHistory()]);
    return () => { esRef.current?.close(); };
  }, [appId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadApp = async () => {
    try {
      const res = await appApi.getDetail(appId);
      setApp(res.data);
    } catch (err: any) {
      alert(err.message);
    }
  };

  const loadHistory = async () => {
    try {
      const res = await chatApi.getHistory({
        appId,
        pageNum: 1,
        pageSize: 50,
        sortField: 'createTime',
        sortOrder: 'ascend',
      });
      // API uses messageType ('user'|'ai') and message — not role/content
      const msgs: ChatMessage[] = res.data.records.map((h) => ({
        id: nextId(),
        role: h.messageType === 'user' ? 'user' : 'ai',
        content: h.message,
      }));
      setMessages(msgs);
    } catch (err: any) {
      console.error('加载历史失败:', err);
    }
  };

  // ── Message helpers (stable — only depend on setMessages) ─────────────────

  const pushMsg = useCallback((msg: Omit<ChatMessage, 'id'>) => {
    setMessages((prev) => [...prev, { ...msg, id: nextId() }]);
  }, []);

  /**
   * Append text to the last plain AI bubble, or create a new one.
   * Resets when the previous message is a non-text event bubble.
   */
  const appendAiText = useCallback((text: string) => {
    setMessages((prev) => {
      const last = prev[prev.length - 1];
      if (last?.role === 'ai' && !last.eventType) {
        return [
          ...prev.slice(0, -1),
          { ...last, content: last.content + text },
        ];
      }
      return [...prev, { id: nextId(), role: 'ai', content: text }];
    });
  }, []);

  // ── Approval actions ──────────────────────────────────────────────────────

  const handleApprove = useCallback(async (approvalId: string) => {
    try {
      await chatApi.approveAgent({ approvalId, approved: true });
      setMessages((prev) =>
        prev.map((m) =>
          m.approvalId === approvalId ? { ...m, approvalStatus: 'approved' } : m
        )
      );
    } catch (err: any) {
      alert(err.message || '审批失败');
    }
  }, []);

  const handleReject = useCallback(async (approvalId: string) => {
    try {
      await chatApi.approveAgent({ approvalId, approved: false });
      setMessages((prev) =>
        prev.map((m) =>
          m.approvalId === approvalId ? { ...m, approvalStatus: 'rejected' } : m
        )
      );
    } catch (err: any) {
      alert(err.message || '操作失败');
    }
  }, []);

  // ── Send message ──────────────────────────────────────────────────────────

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMsg = input.trim();
    setInput('');
    pushMsg({ role: 'user', content: userMsg });
    setLoading(true);

    // Close any in-flight SSE stream
    esRef.current?.close();

    const url =
      `/api/app/chat/gen/code` +
      `?appId=${appId}` +
      `&msg=${encodeURIComponent(userMsg)}` +
      `&agent=${agentMode}`;

    const es = new EventSource(url);
    esRef.current = es;

    const finish = () => {
      es.close();
      esRef.current = null;
      setLoading(false);
    };

    // ── Normal mode (agent=false): d is a raw text fragment ──────────────
    if (!agentMode) {
      es.addEventListener('message', (e) => {
        try {
          const { d } = JSON.parse(e.data) as { d: string };
          if (d) appendAiText(d);
        } catch (err) {
          console.error('SSE parse error:', err);
        }
      });

      es.addEventListener('done', finish);
      es.onerror = () => { finish(); alert('连接中断，请重试'); };
      return;
    }

    // ── Agent mode (agent=true): d is a JSON-encoded AgentEvent string ───
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
            // Server confirms the approval outcome via SSE; sync local state
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
            break;

          case 'agent_error':
            pushMsg({ role: 'event', content: event.content, eventType: 'agent_error' });
            finish();
            break;
        }
      } catch (err) {
        console.error('Agent SSE parse error:', err);
      }
    });

    es.addEventListener('done', finish);
    es.onerror = () => { finish(); alert('连接中断，请重试'); };
  };

  // ── Deploy ────────────────────────────────────────────────────────────────

  const handleDeploy = async () => {
    try {
      const res = await appApi.deploy(appId);
      alert(`部署成功：${res.data}`);
      loadApp();
    } catch (err: any) {
      alert(err.message);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div className="h-screen flex flex-col bg-white">
      {/* Header */}
      <header className="border-b px-4 py-3 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-6 h-6 bg-teal-600 rounded" />
          <span className="text-sm font-medium">{app?.appName || '加载中...'}</span>
          {app?.codeGenType && (
            <span className="hidden sm:inline-block text-xs px-2 py-0.5 bg-gray-100 rounded text-gray-500">
              {app.codeGenType}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {app?.deployKey && (
            <a
              href={`http://${app.deployKey}`}
              target="_blank"
              rel="noreferrer"
              className="text-xs text-blue-600 hover:underline"
            >
              访问 ↗
            </a>
          )}
          <Button variant="outline" size="sm" onClick={() => appApi.download(appId)}>
            下载
          </Button>
          <Button variant="outline" size="sm" onClick={handleDeploy}>
            部署
          </Button>
        </div>
      </header>

      <div className="flex-1 flex overflow-hidden">
        {/* ── Chat panel ── */}
        <div className="w-[40%] border-r flex flex-col">
          {/* Message list */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
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
                <div className="px-3 py-2 rounded bg-gray-100 text-gray-400 text-sm animate-pulse">
                  {agentMode ? 'Agent 执行中...' : '生成中...'}
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input area */}
          <div className="border-t p-4 space-y-2 shrink-0">
            <div className="flex items-center justify-between text-xs text-gray-400">
              <div className="flex gap-3">
                <button className="hover:text-gray-600">上传</button>
                <button className="hover:text-gray-600">识图</button>
              </div>
              <AgentModeToggle
                enabled={agentMode}
                disabled={loading}
                onToggle={() => setAgentMode((v) => !v)}
              />
            </div>
            <div className="flex gap-2">
              <Input
                placeholder={agentMode ? '描述需求，Agent 将自动操作文件...' : '描述你的想法...'}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                disabled={loading}
                className="text-sm"
              />
              <Button
                onClick={handleSend}
                disabled={loading || !input.trim()}
                className="rounded-full w-9 h-9 p-0 shrink-0 text-base"
              >
                ↑
              </Button>
            </div>
          </div>
        </div>

        {/* ── Code / Preview panel ── */}
        <div className="flex-1 flex flex-col overflow-hidden">
          <div className="border-b px-4 py-2 flex gap-4 shrink-0">
            {(['code', 'preview'] as const).map((v) => (
              <button
                key={v}
                onClick={() => setView(v)}
                className={`text-sm pb-1.5 border-b-2 transition-colors ${
                  view === v
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                {v === 'code' ? '代码' : '预览'}
              </button>
            ))}
          </div>

          <div className="flex-1 overflow-hidden">
            {view === 'code' ? (
              <Suspense
                fallback={
                  <div className="flex items-center justify-center h-full text-sm text-gray-400">
                    加载中...
                  </div>
                }
              >
                <CodeViewer appId={appId} />
              </Suspense>
            ) : (
              <div className="h-full flex items-center justify-center bg-gray-50 text-gray-400 text-sm">
                预览功能暂未实现
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
