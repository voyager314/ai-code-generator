import request from '@/utils/request';
import type {
  BaseResponse,
  UserLoginRequest,
  UserRegisterRequest,
  UserLoginVO,
  UserVO,
  UserAddRequest,
  UserUpdateRequest,
  UserQueryRequest,
  AppAddRequest,
  AppUpdateRequest,
  AppQueryRequest,
  AppAdminQueryRequest,
  AppVO,
  AppDetailVO,
  Page,
  ChatHistoryQueryRequest,
  ChatHistoryVO,
  AgentApprovalRequest,
  FileTreeNode,
} from '@/types';

// ─── SSE Stream Reader ──────────────────────────────────────────────────────

export interface SSECallbacks {
  onMessage: (data: string) => void;
  onBusinessError: (data: string) => void;
  onDone: () => void;
  onError: (err: unknown) => void;
}

export async function readSSE(response: Response, callbacks: SSECallbacks) {
  if (!response.body) {
    callbacks.onError(new Error('Response body is null'));
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let eventType = '';
  let dataLines: string[] = [];

  const dispatch = () => {
    if (eventType || dataLines.length) {
      const data = dataLines.join('\n');
      if (eventType === 'done') {
        callbacks.onDone();
      } else if (eventType === 'business-error') {
        callbacks.onBusinessError(data);
      } else if (data) {
        callbacks.onMessage(data);
      }
      eventType = '';
      dataLines = [];
    }
  };

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line === '') {
          dispatch();
        } else if (line.startsWith('event:')) {
          eventType = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5));
        }
      }
    }
    dispatch();
  } catch (err) {
    callbacks.onError(err);
  }
}

// ─── User ────────────────────────────────────────────────────────────────────

export const userApi = {
  register: (data: UserRegisterRequest) =>
    request.post<any, BaseResponse<number>>('/user/register', data),

  login: (data: UserLoginRequest) =>
    request.post<any, BaseResponse<UserLoginVO>>('/user/login', data),

  logout: () =>
    request.delete<any, BaseResponse<boolean>>('/user/logout'),

  getLoginUser: () =>
    request.get<any, BaseResponse<UserLoginVO>>('/user/get/login'),

  getVO: (id: number) =>
    request.get<any, BaseResponse<UserVO>>('/user/get/vo', { params: { id } }),

  getList: () =>
    request.get<any, BaseResponse<UserVO[]>>('/user/list'),

  getPage: (params: { pageNum: number; pageSize: number }) =>
    request.get<any, BaseResponse<Page<UserVO>>>('/user/page', { params }),

  getPageVO: (params: UserQueryRequest) =>
    request.get<any, BaseResponse<Page<UserVO>>>('/user/list/page/vo', { params }),

  add: (data: UserAddRequest) =>
    request.post<any, BaseResponse<number>>('/user/add', data),

  update: (data: UserUpdateRequest) =>
    request.post<any, BaseResponse<boolean>>('/user/update', data),

  remove: (id: number) =>
    request.delete<any, BaseResponse<boolean>>(`/user/remove/${id}`),
};

// ─── App ─────────────────────────────────────────────────────────────────────

export const appApi = {
  create: (data: AppAddRequest) =>
    request.post<any, BaseResponse<number>>('/app/create', data),

  chatToGenCode: (params: { appId: number; msg: string; files?: File[]; signal?: AbortSignal }) => {
    const formData = new FormData();
    formData.append('appId', String(params.appId));
    formData.append('msg', params.msg);
    formData.append('agent', 'true');
    if (params.files) {
      params.files.forEach((f) => formData.append('file', f));
    }
    return fetch('/api/app/chat/gen/code', {
      method: 'POST',
      body: formData,
      credentials: 'include',
      signal: params.signal,
    });
  },

  update: (data: AppUpdateRequest) =>
    request.post<any, BaseResponse<boolean>>('/app/update', data),

  delete: (id: number) =>
    request.post<any, BaseResponse<boolean>>('/app/delete', { id }),

  getDetail: (id: number) =>
    request.get<any, BaseResponse<AppDetailVO>>(`/app/get/${id}`),

  getMyList: (params: AppQueryRequest) =>
    request.get<any, BaseResponse<Page<AppVO>>>('/app/list/page', { params }),

  getStarList: (params: { pageNum: number; pageSize: number; appName?: string }) =>
    request.get<any, BaseResponse<Page<AppVO>>>('/app/star/page', { params }),

  deploy: (appId: number) =>
    request.post<any, BaseResponse<string>>('/app/deploy', { appId }),

  download: (appId: number) =>
    window.open(`/api/app/downLoad?appId=${appId}`),

  getFileTree: (appId: number) =>
    request.get<any, BaseResponse<FileTreeNode>>(`/app/files/${appId}`),

  getFileContent: (appId: number, path: string) =>
    request.get<any, BaseResponse<string>>(`/app/file/${appId}`, { params: { path } }),

  adminUpdate: (data: AppUpdateRequest) =>
    request.post<any, BaseResponse<boolean>>('/app/admin/update', data),

  adminDelete: (id: number) =>
    request.post<any, BaseResponse<boolean>>(`/app/admin/delete/${id}`),

  adminGetList: (params: AppAdminQueryRequest) =>
    request.get<any, BaseResponse<Page<AppVO>>>('/app/admin/list', { params }),

  adminGetDetail: (id: number) =>
    request.get<any, BaseResponse<AppDetailVO>>(`/app/admin/get/${id}`),
};

// ─── Chat ─────────────────────────────────────────────────────────────────────

export const chatApi = {
  getHistory: (params: ChatHistoryQueryRequest) =>
    request.get<any, BaseResponse<Page<ChatHistoryVO>>>('/chatHistory/list/app', { params }),

  adminGetHistory: (params: ChatHistoryQueryRequest & { userId?: number }) =>
    request.get<any, BaseResponse<Page<ChatHistoryVO>>>('/chatHistory/admin/list', { params }),

  approveAgent: (data: AgentApprovalRequest) =>
    request.post<any, BaseResponse<boolean>>('/app/agent/approve', data),
};
