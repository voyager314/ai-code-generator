import { useState } from 'react';
import { Tree, NodeRendererProps } from 'react-arborist';
import { Highlight, themes } from 'prism-react-renderer';

interface FileNode {
  id: string;
  name: string;
  type: 'file' | 'directory';
  path?: string;
  children?: FileNode[];
}

interface Tab {
  id: string;
  name: string;
  path: string;
  content: string;
}

interface CodeViewerProps {
  appId: number;
}

function FileTreeNode({ node, style, dragHandle }: NodeRendererProps<FileNode>) {
  const isFolder = node.data.type === 'directory';
  const icon = isFolder ? (node.isOpen ? '📂' : '📁') : '📄';

  return (
    <div
      ref={dragHandle}
      style={style}
      className={`flex items-center px-2 py-1 text-sm cursor-pointer hover:bg-gray-100 ${
        node.isSelected ? 'bg-blue-50' : ''
      }`}
      onClick={() => node.isInternal && node.toggle()}
    >
      {isFolder && (
        <span className="mr-1 text-xs">{node.isOpen ? '▼' : '▶'}</span>
      )}
      <span className="mr-2">{icon}</span>
      <span className="truncate">{node.data.name}</span>
    </div>
  );
}

export default function CodeViewer({ appId }: CodeViewerProps) {
  const [tabs, setTabs] = useState<Tab[]>([]);
  const [activeTabId, setActiveTabId] = useState<string | null>(null);

  // Mock data - 等待后端接口
  const mockTree: FileNode[] = [
    {
      id: '1',
      name: 'src',
      type: 'directory',
      children: [
        { id: '1-1', name: 'App.jsx', type: 'file', path: 'src/App.jsx' },
        { id: '1-2', name: 'index.css', type: 'file', path: 'src/index.css' },
      ],
    },
    { id: '2', name: 'index.html', type: 'file', path: 'index.html' },
    { id: '3', name: 'package.json', type: 'file', path: 'package.json' },
  ];

  const handleFileClick = async (node: FileNode) => {
    if (node.type === 'file' && node.path) {
      const existing = tabs.find((t) => t.path === node.path);
      if (existing) {
        setActiveTabId(existing.id);
        return;
      }

      // TODO: 调用后端接口获取文件内容
      const mockContent = `// ${node.path}\n// 等待后端接口实现\n// GET /api/app/file/${appId}?path=${node.path}`;

      const newTab: Tab = {
        id: node.id,
        name: node.name,
        path: node.path,
        content: mockContent,
      };

      setTabs([...tabs, newTab]);
      setActiveTabId(newTab.id);
    }
  };

  const closeTab = (tabId: string) => {
    const newTabs = tabs.filter((t) => t.id !== tabId);
    setTabs(newTabs);
    if (activeTabId === tabId) {
      setActiveTabId(newTabs[0]?.id || null);
    }
  };

  const activeTab = tabs.find((t) => t.id === activeTabId);
  const getLanguage = (filename: string) => {
    if (filename.endsWith('.jsx') || filename.endsWith('.js')) return 'jsx';
    if (filename.endsWith('.tsx') || filename.endsWith('.ts')) return 'tsx';
    if (filename.endsWith('.css')) return 'css';
    if (filename.endsWith('.html')) return 'html';
    if (filename.endsWith('.json')) return 'json';
    return 'text';
  };

  return (
    <div className="h-full flex">
      <div className="w-56 border-r bg-gray-50 flex flex-col">
        <div className="px-3 py-2 text-xs font-semibold text-gray-700 border-b">文件</div>
        <div className="flex-1 overflow-hidden">
          <Tree
            data={mockTree}
            openByDefault={false}
            width="100%"
            height="100%"
            indent={16}
            rowHeight={28}
            onSelect={(nodes) => nodes[0] && handleFileClick(nodes[0].data)}
          >
            {FileTreeNode}
          </Tree>
        </div>
      </div>

      <div className="flex-1 flex flex-col">
        {tabs.length > 0 ? (
          <>
            <div className="flex items-center gap-1 px-2 py-1 bg-gray-50 border-b overflow-x-auto">
              {tabs.map((tab) => (
                <div
                  key={tab.id}
                  className={`flex items-center gap-2 px-3 py-1.5 text-sm cursor-pointer rounded-t ${
                    activeTabId === tab.id
                      ? 'bg-white border-t border-x'
                      : 'bg-gray-100 hover:bg-gray-200'
                  }`}
                  onClick={() => setActiveTabId(tab.id)}
                >
                  <span className="truncate max-w-[120px]">{tab.name}</span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      closeTab(tab.id);
                    }}
                    className="text-gray-400 hover:text-gray-600"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>

            <div className="flex-1 overflow-auto bg-gray-50 p-4">
              {activeTab && (
                <Highlight
                  theme={themes.github}
                  code={activeTab.content}
                  language={getLanguage(activeTab.name)}
                >
                  {({ className, style, tokens, getLineProps, getTokenProps }) => (
                    <pre className={`${className} text-sm`} style={{ ...style, background: 'transparent' }}>
                      {tokens.map((line, i) => (
                        <div key={i} {...getLineProps({ line })}>
                          <span className="inline-block w-8 text-right pr-4 select-none text-gray-400">
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
          <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
            <div className="text-center">
              <p>请选择文件查看</p>
              <p className="text-xs mt-2">需要后端提供文件树和文件内容接口</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
