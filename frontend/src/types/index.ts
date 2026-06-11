export interface BaseResponse<T> {
  code: number;
  data: T;
  message: string;
}

export interface UserLoginVO {
  id: number;
  userAccount: string;
  userName: string;
  userAvatar: string;
  userProfile: string;
  userRole: string;
  createTime: string;
  updateTime: string;
}

export interface AppVO {
  id: number;
  appName: string;
  cover: string;
  codeGenType: string;
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

export interface ChatHistoryVO {
  id: number;
  appId: number;
  userId: number;
  content: string;
  role: string;
  createTime: string;
}

export interface Page<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
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

export interface AppAddRequest {
  appName?: string;
  cover?: string;
  initPrompt: string;
  codeGenType?: string;
}

export interface AppUpdateRequest {
  id: number;
  appName?: string;
  cover?: string;
  initPrompt?: string;
  codeGenType?: string;
}

export interface AppQueryRequest {
  pageNum: number;
  pageSize: number;
  appName?: string;
  codeGenType?: string;
}

export interface ChatHistoryQueryRequest {
  pageNum: number;
  pageSize: number;
  appId: number;
}

export interface AgentApprovalRequest {
  approvalId: string;
  approved: boolean;
}

export interface FileTreeNode {
  name: string;
  type: 'file' | 'directory';
  path?: string;
  children?: FileTreeNode[];
}
