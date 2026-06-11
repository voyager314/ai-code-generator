# Wise Code - 前端项目说明

## 项目概览

基于 **Vite + React 18 + TypeScript + Tailwind CSS** 构建的现代化前端应用，对接后端 Spring Boot AI 代码生成服务。

## 快速开始

### 1. 安装依赖
```bash
cd frontend
npm install
```

### 2. 启动开发服务器
```bash
npm run dev
```
访问：http://localhost:5173

### 3. 构建生产版本
```bash
npm run build
```
构建产物会输出到：`src/main/resources/static/`

## 技术架构

### 核心技术栈
- **React 18** - UI 框架
- **TypeScript** - 类型安全
- **Vite** - 构建工具
- **Tailwind CSS** - 样式方案
- **Zustand** - 状态管理（轻量级）
- **React Router** - 路由管理
- **Axios** - HTTP 请求

### 项目结构
```
frontend/
├── src/
│   ├── api/              # API 层
│   │   └── index.ts      # 接口定义（userApi, appApi, chatApi）
│   ├── components/ui/    # UI 组件库
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   └── Card.tsx
│   ├── pages/            # 页面组件
│   │   ├── Login.tsx     # 登录/注册
│   │   ├── AppList.tsx   # 应用列表
│   │   ├── AppChat.tsx   # AI 对话（核心）
│   │   └── Admin.tsx     # 管理后台
│   ├── store/            # 状态管理
│   │   └── user.ts       # 用户状态
│   ├── types/            # TypeScript 类型
│   │   └── index.ts      # 所有类型定义
│   ├── utils/            # 工具函数
│   │   └── request.ts    # Axios 封装
│   ├── App.tsx           # 路由配置
│   ├── main.tsx          # 应用入口
│   └── index.css         # 全局样式
├── vite.config.ts        # Vite 配置
├── tailwind.config.js    # Tailwind 配置
├── tsconfig.json         # TypeScript 配置
└── package.json          # 依赖管理
```

## 功能模块

### 1. 认证系统
- **登录/注册** - Session 持久化，Zustand 状态管理
- **路由守卫** - ProtectedRoute 保护私有页面
- **权限控制** - AdminRoute 限制管理员功能

### 2. 应用管理
- **应用列表** - 我的应用/精选应用切换
- **创建应用** - 表单验证，初始化 prompt
- **应用详情** - 查看、编辑、部署、下载

### 3. AI 对话生成（核心功能）⭐
- **实时流式响应** - 基于 SSE（Server-Sent Events）
- **深度思考模式** - 切换按钮控制 `agent=true/false`
  - 💡 **普通模式** - 快速生成
  - 🧠 **深度思考** - Agent 模式，多步推理
- **对话历史** - 自动保存，滚动加载

### 4. 管理后台
- **用户管理** - 列表展示，删除用户
- **仅管理员可访问**

## 关键实现细节

### Vite 代理配置
开发环境下，所有 `/api` 请求自动代理到后端：
```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8081',
      changeOrigin: true,
    },
  },
}
```

### SSE 流式响应处理
```typescript
const es = new EventSource(`/api/app/chat/gen/code?appId=${id}&msg=${msg}&agent=${deepThink}`);

es.addEventListener('message', (e) => {
  const data = JSON.parse(e.data);
  if (data.d) {
    aiContent += data.d;  // 累加内容
    setMessages(prev => [...]);  // 更新 UI
  }
});

es.addEventListener('done', () => {
  es.close();  // 结束连接
});
```

### 深度思考开关
```typescript
const [deepThink, setDeepThink] = useState(false);

<Button
  variant={deepThink ? 'default' : 'outline'}
  onClick={() => setDeepThink(!deepThink)}
>
  {deepThink ? '🧠 深度思考' : '💡 普通模式'}
</Button>
```

## 后端接口对接

### BaseResponse 格式
```typescript
interface BaseResponse<T> {
  code: number;    // 状态码
  data: T;         // 数据
  message: string; // 消息
}
```

### 主要接口
| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 登录 | POST | `/user/login` | 返回用户信息 |
| 注册 | POST | `/user/register` | 返回用户 ID |
| 应用列表 | GET | `/app/list/page` | 分页查询 |
| 创建应用 | POST | `/app/create` | 返回应用 ID |
| AI 对话 | GET | `/app/chat/gen/code` | SSE 流式响应 |
| 部署应用 | POST | `/app/deploy` | 返回部署 URL |
| 下载应用 | GET | `/app/downLoad` | 文件下载 |

## 开发指南

### 添加新页面
1. 在 `src/pages/` 创建组件
2. 在 `src/App.tsx` 添加路由
3. 需要认证则包裹 `<ProtectedRoute>`

### 添加新接口
1. 在 `src/types/index.ts` 定义类型
2. 在 `src/api/index.ts` 添加方法
3. 在组件中调用

### 样式定制
修改 `src/index.css` 中的 CSS 变量：
```css
:root {
  --primary: 222.2 47.4% 11.2%;  # 主色调
  --radius: 0.5rem;              # 圆角大小
}
```

## 环境变量
如需配置，可在 `frontend/.env` 中添加：
```
VITE_API_BASE_URL=http://localhost:8081/api
```

## 常见问题

### Q: 如何修改代理地址？
A: 编辑 `vite.config.ts` 中的 `server.proxy.target`

### Q: 构建后为什么无法访问？
A: 确保后端 Spring Boot 启动，静态资源会托管在 `/api/` 下

### Q: 如何调试 SSE 连接？
A: 浏览器开发者工具 → Network → EventStream 类型

## 浏览器支持
- Chrome/Edge 90+
- Firefox 88+
- Safari 14+

---

**技术支持：** 基于后端 CLAUDE.md 规范构建
