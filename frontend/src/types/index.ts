export interface BaseResponse<T> {
  code: number;
  data: T;
  message: string;
}

// ─── User ────────────────────────────────────────────────────────────────────

export interface UserLoginVO {
  id: number;
  userAccount: string;
  userName: string;
  userAvatar: string;
  userProfile: string;
  userRole: 'user' | 'admin';
  createTime: string;
  updateTime: string;
}

/** Returned by admin read endpoints (/user/get/vo, /user/list/page/vo) */
export interface UserVO {
  id: number;
  userAccount: string;
  userName: string;
  userAvatar: string;
  userProfile: string;
  userRole: 'user' | 'admin';
  createTime: string;
}

export interface UserLoginRequest {
  userAccount: string;
  userPassword: string;
}

export interface UserRegisterRequest {
  userAccount: string;
  userPassword: string;
  checkPassword: string;
}

/** POST /user/add — default password is 123456 */
export interface UserAddRequest {
  userAccount: string;
  userName?: string;
  userAvatar?: string;
  userProfile?: string;
  userRole?: 'user' | 'admin';
}

/** POST /user/update */
export interface UserUpdateRequest {
  id: number;
  userName?: string;
  userAvatar?: string;
  userProfile?: string;
  userRole?: 'user' | 'admin';
}

/** GET /user/list/page/vo query params */
export interface UserQueryRequest {
  pageNum: number;
  pageSize: number;
  id?: number;
  userName?: string;
  userAccount?: string;
  userProfile?: string;
  userRole?: 'user' | 'admin';
  sortField?: string;
  sortOrder?: string;
}

// ─── App ─────────────────────────────────────────────────────────────────────

export type CodeGenType = 'HTML' | 'MULTI_FILE' | 'VUE_PROJECT';

export interface AppVO {
  id: number;
  appName: string;
  cover: string;
  codeGenType: CodeGenType;
  deployKey: string;
  deployedTime: string;
  priority: number;
  userId: number;
  createTime: string;
  updateTime: string;
}

export interface AppDetailVO extends AppVO {
  initPrompt: string;
  editTime: string;
}

export interface AppAddRequest {
  appName?: string;
  cover?: string;
  initPrompt: string;
  codeGenType?: CodeGenType;
}

/** POST /app/update — only name/cover/priority are updatable by owner */
export interface AppUpdateRequest {
  id: number;
  appName?: string;
  cover?: string;
  priority?: number;
}

export interface AppQueryRequest {
  pageNum: number;
  pageSize: number;
  appName?: string;
  codeGenType?: CodeGenType;
}

/** Extended for admin list — supports extra userId filter */
export interface AppAdminQueryRequest extends AppQueryRequest {
  userId?: number;
  id?: number;
  sortField?: string;
  sortOrder?: string;
}

// ─── Chat / SSE ──────────────────────────────────────────────────────────────

/**
 * API returns `message` + `messageType`, NOT `content` + `role`.
 * GET /chatHistory/list/app
 */
export interface ChatHistoryVO {
  id: number;
  appId: number;
  userId: number;
  message: string;
  messageType: 'user' | 'ai';
  createTime: string;
}

export interface ChatHistoryQueryRequest {
  appId: number;
  pageNum: number;
  pageSize: number;
  sortField?: string;
  sortOrder?: string;
  /** Cursor pagination — pass oldest record's createTime */
  createTimeBefore?: string;
}

export interface AgentApprovalRequest {
  approvalId: string;
  approved: boolean;
  customMessage?: string;
}

/**
 * Agent-mode SSE inner payload.
 * Each SSE frame: `data: {"d":"<AgentEvent JSON string>"}`
 * Requires two-pass JSON.parse.
 */
export interface AgentEvent {
  type:
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
  content: string;
  toolName?: string;
  toolArgs?: string;
  approvalId?: string;
}

// ─── File tree ───────────────────────────────────────────────────────────────

export interface FileTreeNode {
  id?: string;
  name: string;
  type: 'file' | 'directory';
  path?: string;
  children?: FileTreeNode[];
}

// ─── Pagination ───────────────────────────────────────────────────────────────

/**
 * MyBatis Flex page result.
 * Fields: totalRow / pageNum / pageSize (NOT total / current / size).
 */
export interface Page<T> {
  records: T[];
  totalRow: number;
  pageNum: number;
  pageSize: number;
}
