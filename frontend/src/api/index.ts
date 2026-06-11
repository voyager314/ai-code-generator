import request from '@/utils/request';
import type {
  BaseResponse,
  UserLoginRequest,
  UserRegisterRequest,
  UserLoginVO,
  AppAddRequest,
  AppUpdateRequest,
  AppQueryRequest,
  AppVO,
  AppDetailVO,
  Page,
  ChatHistoryQueryRequest,
  ChatHistoryVO,
  AgentApprovalRequest,
  FileTreeNode,
} from '@/types';

export const userApi = {
  register: (data: UserRegisterRequest) =>
    request.post<any, BaseResponse<number>>('/user/register', data),

  login: (data: UserLoginRequest) =>
    request.post<any, BaseResponse<UserLoginVO>>('/user/login', data),

  logout: () =>
    request.delete<any, BaseResponse<boolean>>('/user/logout'),

  getLoginUser: () =>
    request.get<any, BaseResponse<UserLoginVO>>('/user/get/login'),
};

export const appApi = {
  create: (data: AppAddRequest) =>
    request.post<any, BaseResponse<number>>('/app/create', data),

  update: (data: AppUpdateRequest) =>
    request.post<any, BaseResponse<boolean>>('/app/update', data),

  delete: (id: number) =>
    request.post<any, BaseResponse<boolean>>('/app/delete', { id }),

  getDetail: (id: number) =>
    request.get<any, BaseResponse<AppDetailVO>>(`/app/get/${id}`),

  getMyList: (params: AppQueryRequest) =>
    request.get<any, BaseResponse<Page<AppVO>>>('/app/list/page', { params }),

  getStarList: (params: { pageNum: number; pageSize: number }) =>
    request.get<any, BaseResponse<Page<AppVO>>>('/app/star/page', { params }),

  deploy: (appId: number) =>
    request.post<any, BaseResponse<string>>('/app/deploy', { appId }),

  download: (appId: number) =>
    window.open(`/api/app/downLoad?appId=${appId}`),

  getFileTree: (appId: number) =>
    request.get<any, BaseResponse<FileTreeNode>>(`/app/files/${appId}`),

  getFileContent: (appId: number, path: string) =>
    request.get<any, BaseResponse<string>>(`/app/file/${appId}`, { params: { path } }),
};

export const chatApi = {
  getHistory: (params: ChatHistoryQueryRequest) =>
    request.get<any, BaseResponse<Page<ChatHistoryVO>>>('/chatHistory/list/app', { params }),

  approveAgent: (data: AgentApprovalRequest) =>
    request.post<any, BaseResponse<boolean>>('/app/agent/approve', data),
};
