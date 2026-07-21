import { useState, useEffect, useRef, useCallback } from 'react';
import { Panel, Group as PanelGroup, Separator as PanelResizeHandle } from 'react-resizable-panels';
import { Tree, NodeRendererProps } from 'react-arborist';
import { Highlight, themes } from 'prism-react-renderer';
import { appApi } from '@/api';
import type { FileTreeNode } from '@/types';

interface Tab {
  id: string;
  name: string;
  path: string;
  content: string;
}

interface CodeViewerProps {
  appId: number;
  refreshKey?: number;
}

function getLanguage(filename: string) {
  if (filename.endsWith('.jsx') || filename.endsWith('.js')) return 'jsx';
  if (filename.endsWith('.tsx') || filename.endsWith('.ts')) return 'tsx';
  if (filename.endsWith('.css')) return 'css';
  if (filename.endsWith('.html')) return 'html';
  if (filename.endsWith('.json')) return 'json';
  if (filename.endsWith('.vue')) return 'jsx';
  return 'text';
}

function FileTreeNodeRenderer({ node, style, dragHandle }: NodeRendererProps<FileTreeNode>) {
  const isFolder = node.data.type === 'directory';

  return (
    <div
      ref={dragHandle}
      style={style}
      className={`flex items-center px-2 py-1 text-sm cursor-pointer hover:bg-accent ${
        node.isSelected ? 'bg-primary/10 text-primary' : 'text-foreground'
      }`}
      onClick={() => {
        if (node.isInternal) {
          node.toggle();
        } else {
          node.activate();
        }
      }}
    >
      {isFolder && (
        <span className="mr-1 text-xs text-muted-foreground">{node.isOpen ? '▼' : '▶'}</span>
      )}
      <span className="mr-2 text-sm">{isFolder ? (node.isOpen ? '📂' : '📁') : '📄'}</span>
      <span className="truncate">{node.data.name}</span>
    </div>
  );
}

export default function CodeViewer({ appId, refreshKey }: CodeViewerProps) {
  const [tabs, setTabs] = useState<Tab[]>([]);
  const [activeTabId, setActiveTabId] = useState<string | null>(null);
  const [treeData, setTreeData] = useState<FileTreeNode[]>([]);
  const [treeHeight, setTreeHeight] = useState(400);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const treeContainerRef = useRef<HTMLDivElement>(null);

  const addIds = (nodes: FileTreeNode[], parentPath = ''): FileTreeNode[] =>
    nodes.map((node) => {
      const fullPath = parentPath ? `${parentPath}/${node.name}` : node.name;
      return {
        ...node,
        id: node.path || fullPath,
        children: node.children ? addIds(node.children, fullPath) : undefined,
      };
    });

  const loadFileTree = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    try {
      const res = await appApi.getFileTree(appId);
      if (res.data?.children) {
        setTreeData(addIds(res.data.children));
      } else {
        setTreeData([]);
      }
    } catch (err: any) {
      console.error('加载文件树失败:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [appId]);

  useEffect(() => {
    loadFileTree();
  }, [appId, loadFileTree]);

  useEffect(() => {
    if (refreshKey !== undefined && refreshKey > 0) {
      loadFileTree(true);
    }
  }, [refreshKey, loadFileTree]);

  useEffect(() => {
    const container = treeContainerRef.current;
    if (!container) return;

    const updateTreeHeight = () => {
      setTreeHeight(container.clientHeight || 400);
    };

    updateTreeHeight();
    const resizeObserver = new ResizeObserver(updateTreeHeight);
    resizeObserver.observe(container);

    return () => {
      resizeObserver.disconnect();
    };
  }, []);

  const handleFileClick = async (node: FileTreeNode) => {
    if (node.type === 'file' && node.path) {
      const existing = tabs.find((t) => t.path === node.path);
      if (existing) {
        setActiveTabId(existing.id);
        return;
      }

      try {
        const res = await appApi.getFileContent(appId, node.path);
        const newTab: Tab = {
          id: `${appId}-${node.path}`,
          name: node.name,
          path: node.path,
          content: res.data || '',
        };

        setTabs((prev) => [...prev, newTab]);
        setActiveTabId(newTab.id);
      } catch (err: any) {
        console.error('加载文件内容失败:', err);
      }
    }
  };

  const closeTab = (tabId: string) => {
    setTabs((prev) => {
      const newTabs = prev.filter((t) => t.id !== tabId);
      if (activeTabId === tabId) {
        setActiveTabId(newTabs[0]?.id || null);
      }
      return newTabs;
    });
  };

  const activeTab = tabs.find((t) => t.id === activeTabId);

  const fileTreePanel = (
    <div className="h-full flex flex-col bg-card">
      <div className="flex items-center justify-between px-3 py-2 border-b border-border shrink-0">
        <span className="text-xs font-semibold text-muted-foreground">文件</span>
        <button
          onClick={() => loadFileTree(true)}
          disabled={refreshing}
          className="flex h-6 w-6 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-foreground disabled:opacity-50"
          aria-label="刷新文件树"
          title="刷新"
        >
          <svg viewBox="0 0 24 24" className={`h-3.5 w-3.5 ${refreshing ? 'animate-spin' : ''}`} fill="none" aria-hidden="true">
            <path d="M21 12a9 9 0 1 1-2.63-6.36" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            <path d="M21 3v6h-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>
      <div ref={treeContainerRef} className="flex-1 overflow-hidden min-h-0">
        {loading ? (
          <div className="p-4 text-sm text-muted-foreground">加载中...</div>
        ) : treeData.length === 0 ? (
          <div className="p-4 text-sm text-muted-foreground">暂无文件</div>
        ) : (
          <Tree
            data={treeData}
            openByDefault={false}
            width="100%"
            height={treeHeight}
            indent={16}
            rowHeight={28}
            onActivate={(node) => handleFileClick(node.data)}
          >
            {FileTreeNodeRenderer}
          </Tree>
        )}
      </div>
    </div>
  );

  const codePanel = (
    <div className="h-full flex flex-col bg-background min-w-0">
      {tabs.length > 0 ? (
        <>
          <div className="flex items-center gap-1 px-2 py-1 bg-card border-b border-border overflow-x-auto shrink-0">
            {/* Mobile: toggle sidebar button */}
            <button
              onClick={() => setSidebarCollapsed((v) => !v)}
              className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-foreground lg:hidden"
              aria-label="切换文件列表"
            >
              <svg viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" aria-hidden="true">
                <path d="M3 7h18M3 12h18M3 17h18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
            {tabs.map((tab) => (
              <div
                key={tab.id}
                className={`flex items-center gap-1.5 px-3 py-1.5 text-sm cursor-pointer rounded-t transition-colors whitespace-nowrap ${
                  activeTabId === tab.id
                    ? 'bg-background text-foreground border-t border-x border-border'
                    : 'text-muted-foreground hover:bg-accent hover:text-foreground'
                }`}
                onClick={() => setActiveTabId(tab.id)}
              >
                <span className="truncate max-w-[120px]">{tab.name}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    closeTab(tab.id);
                  }}
                  className="text-muted-foreground hover:text-foreground shrink-0"
                >
                  ×
                </button>
              </div>
            ))}
          </div>

          <div className="flex-1 overflow-auto min-h-0">
            {activeTab && (
              <Highlight
                theme={themes.oneDark}
                code={activeTab.content}
                language={getLanguage(activeTab.name)}
              >
                {({ className, style, tokens, getLineProps, getTokenProps }) => (
                  <pre
                    className={`${className} text-sm p-4`}
                    style={{ ...style, background: 'transparent', margin: 0, overflowX: 'auto' }}
                  >
                    {tokens.map((line, i) => (
                      <div key={i} {...getLineProps({ line })} style={{ ...getLineProps({ line }).style, whiteSpace: 'pre' }}>
                        <span className="inline-block w-10 text-right pr-4 select-none text-muted-foreground shrink-0">
                          {i + 1}
                        </span>
                        {line.map((token, key) => (
                          <span key={key} {...getTokenProps({ token })} />
                        ))}
                      </div>
                    ))}
                  </pre>
                )}
              </Highlight>
            )}
          </div>
        </>
      ) : (
        <div className="flex-1 flex items-center justify-center text-muted-foreground text-sm">
          <p>请选择文件查看</p>
        </div>
      )}
    </div>
  );

  return (
    <div className="h-full">
      {/* Desktop: resizable panels */}
      <div className="h-full hidden lg:block">
        <PanelGroup orientation="horizontal">
          <Panel id="file-tree" defaultSize="25" minSize="15" maxSize="50">
            {fileTreePanel}
          </Panel>
          <PanelResizeHandle className="w-1.5 bg-border hover:bg-primary/40 active:bg-primary/60 transition-colors cursor-col-resize" />
          <Panel id="code-area" minSize="30">
            {codePanel}
          </Panel>
        </PanelGroup>
      </div>

      {/* Mobile: collapsible sidebar overlay */}
      <div className="h-full flex lg:hidden relative">
        {/* Sidebar */}
        {!sidebarCollapsed && (
          <>
            <div
              className="absolute inset-0 z-10 bg-black/30"
              onClick={() => setSidebarCollapsed(true)}
            />
            <div className="absolute inset-y-0 left-0 z-20 w-64 max-w-[75%] shadow-lg">
              {fileTreePanel}
            </div>
          </>
        )}
        {/* Main code area */}
        <div className="flex-1 min-w-0">
          {tabs.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-muted-foreground text-sm gap-3">
              <button
                onClick={() => setSidebarCollapsed(false)}
                className="flex items-center gap-2 rounded-lg bg-secondary px-4 py-2 text-sm text-foreground transition-colors hover:bg-accent"
              >
                <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" aria-hidden="true">
                  <path d="M3 7h18M3 12h18M3 17h18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
                打开文件列表
              </button>
            </div>
          ) : (
            codePanel
          )}
        </div>
      </div>
    </div>
  );
}
