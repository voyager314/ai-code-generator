# Wise Code 前端项目 - 快速开始

## ✅ 项目已完成

前端项目已成功构建，基于 **Vite + React + TypeScript + Tailwind CSS + shadcn/ui**

## 📦 已实现功能

### 1. 用户认证
- ✅ 登录/注册页面
- ✅ Session 持久化
- ✅ 路由守卫

### 2. 应用管理
- ✅ 应用列表（我的应用 + 精选应用）
- ✅ 创建应用
- ✅ 应用详情

### 3. AI 对话生成 ⭐ 核心功能
- ✅ SSE 流式响应
- ✅ **深度思考开关** (💡 普通模式 / 🧠 深度思考)
- ✅ 对话历史加载
- ✅ 应用部署
- ✅ 应用下载

### 4. 管理后台
- ✅ 用户管理
- ✅ 仅管理员访问

## 🚀 启动方式

### 开发环境
```bash
cd frontend
npm run dev
```
访问：http://localhost:5173
API 请求自动代理到 http://localhost:8081/api

### 生产构建
```bash
cd frontend
npm run build
```
构建产物输出到：`src/main/resources/static/`

### Windows 快捷启动
直接运行项目根目录下的：
```
start-frontend.bat
```

## 🎨 技术特性

### Vite 配置
- ✅ 开发代理配置：`/api` → `http://localhost:8081`
- ✅ 构建输出：`../src/main/resources/static/`
- ✅ TypeScript 路径别名：`@/` → `src/`

### 深度思考模式实现
```typescript
const [deepThink, setDeepThink] = useState(false);

// 按钮切换
<Button variant={deepThink ? 'default' : 'outline'}>
  {deepThink ? '🧠 深度思考' : '💡 普通模式'}
</Button>

// API 请求
`/api/app/chat/gen/code?appId=${id}&msg=${msg}&agent=${deepThink}`
```

### SSE 流式响应
```typescript
const es = new EventSource(url);

es.addEventListener('message', (e) => {
  const data = JSON.parse(e.data);
  // 累加内容并更新 UI
});

es.addEventListener('done', () => {
  es.close();
});
```

## 📁 项目结构

```
frontend/
├── src/
│   ├── api/              # API 封装
│   ├── components/ui/    # UI 组件（Button, Input, Card）
│   ├── pages/            # 页面组件
│   │   ├── Login.tsx         # 登录/注册
│   │   ├── AppList.tsx       # 应用列表
│   │   ├── AppChat.tsx       # AI 对话（含深度思考）
│   │   └── Admin.tsx         # 管理后台
│   ├── store/            # Zustand 状态管理
│   ├── types/            # TypeScript 类型
│   ├── utils/            # 工具函数
│   ├── App.tsx           # 路由配置
│   └── main.tsx          # 入口
├── vite.config.ts        # Vite 配置 ⭐
├── tailwind.config.js    # Tailwind 配置
└── package.json          # 依赖管理
```

## 🔌 API 接口对接

所有接口已完成对接，格式：`BaseResponse<T>`

### 用户相关
- POST `/user/register` - 注册
- POST `/user/login` - 登录
- DELETE `/user/logout` - 登出
- GET `/user/get/login` - 获取当前用户

### 应用相关
- POST `/app/create` - 创建应用
- GET `/app/list/page` - 我的应用列表
- GET `/app/star/page` - 精选应用列表
- GET `/app/get/{id}` - 应用详情
- POST `/app/deploy` - 部署应用
- GET `/app/downLoad` - 下载应用

### AI 对话
- GET `/app/chat/gen/code?appId={id}&msg={msg}&agent={true/false}` - SSE 流式生成

### 管理员
- GET `/user/list` - 用户列表
- DELETE `/user/remove/{id}` - 删除用户

## 🎯 路由配置

| 路径 | 组件 | 权限 |
|------|------|------|
| `/login` | Login | 公开 |
| `/apps` | AppList | 需登录 |
| `/app/:id` | AppChat | 需登录 |
| `/admin` | Admin | 仅管理员 |
| `/` | 重定向到 `/apps` | - |

## 💡 使用流程

1. **启动后端**：确保 Spring Boot 服务运行在 `localhost:8081`
2. **启动前端**：运行 `npm run dev` 或 `start-frontend.bat`
3. **注册账号**：访问 http://localhost:5173/login
4. **创建应用**：在应用列表点击"创建应用"
5. **AI 对话**：进入应用详情，开启深度思考模式，输入需求
6. **部署/下载**：对话完成后可部署或下载生成的代码

## 🧩 深度思考功能说明

### 普通模式 💡
- 快速响应
- 适合简单需求
- 后端 `agent=false`

### 深度思考模式 🧠
- Agent 模式
- 多步推理
- 质量更高
- 后端 `agent=true`

切换方式：点击对话框下方的按钮即可切换模式。

## 📝 待办事项（可选扩展）

- [ ] 对话历史查看页面（数据已加载，UI 未展示）
- [ ] Agent 审批回调 UI
- [ ] 应用编辑功能
- [ ] 分页加载优化
- [ ] 主题切换（深色模式）
- [ ] 响应式布局优化（移动端）

## 🔧 开发建议

### 修改样式
编辑 `src/index.css` 中的 CSS 变量：
```css
:root {
  --primary: 222.2 47.4% 11.2%;  /* 主色调 */
  --radius: 0.5rem;              /* 圆角 */
}
```

### 添加新页面
1. 在 `src/pages/` 创建组件
2. 在 `src/App.tsx` 添加路由
3. 需要认证则用 `<ProtectedRoute>` 包裹

### 添加新 API
1. 在 `src/types/index.ts` 定义类型
2. 在 `src/api/index.ts` 添加方法

## ✨ 技术亮点

- **极简设计**：简约风格，无冗余代码
- **类型安全**：完整的 TypeScript 类型定义
- **实时响应**：SSE 流式显示，用户体验流畅
- **状态管理**：Zustand 轻量级方案
- **构建优化**：Vite 快速构建，75.88 kB gzipped
- **代理配置**：开发环境无需手动配置后端地址

---

**项目状态**：✅ 已完成并通过构建验证

**技术栈**：Vite + React 18 + TypeScript + Tailwind CSS + Zustand + React Router

**构建产物**：已输出到 `src/main/resources/static/`
