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

  // ── Admin only ──────────────────────────────────────────────────────────

  /** GET /user/get/vo?id={id} */
  getVO: (id: number) =>
    request.get<any, BaseResponse<UserVO>>('/user/get/vo', { params: { id } }),

  /** GET /user/list — full entity list */
  getList: () =>
    request.get<any, BaseResponse<UserVO[]>>('/user/list'),

  /** GET /user/page?pageNum=&pageSize= */
  getPage: (params: { pageNum: number; pageSize: number }) =>
    request.get<any, BaseResponse<Page<UserVO>>>('/user/page', { params }),

  /** GET /user/list/page/vo — filterable VO page */
  getPageVO: (params: UserQueryRequest) =>
    request.get<any, BaseResponse<Page<UserVO>>>('/user/list/page/vo', { params }),

  /** POST /user/add — default password 123456 */
  add: (data: UserAddRequest) =>
    request.post<any, BaseResponse<number>>('/user/add', data),

  /** POST /user/update */
  update: (data: UserUpdateRequest) =>
    request.post<any, BaseResponse<boolean>>('/user/update', data),

  /** DELETE /user/remove/{id} */
  remove: (id: number) =>
    request.delete<any, BaseResponse<boolean>>(`/user/remove/${id}`),
};

// ─── App ─────────────────────────────────────────────────────────────────────

export const appApi = {
  /** POST /app/create */
  create: (data: AppAddRequest) =>
    request.post<any, BaseResponse<number>>('/app/create', data),

  /** POST /app/update — owner only; name/cover/priority */
  update: (data: AppUpdateRequest) =>
    request.post<any, BaseResponse<boolean>>('/app/update', data),

  /** POST /app/delete */
  delete: (id: number) =>
    request.post<any, BaseResponse<boolean>>('/app/delete', { id }),

  /** GET /app/get/{id} — public */
  getDetail: (id: number) =>
    request.get<any, BaseResponse<AppDetailVO>>(`/app/get/${id}`),

  /** GET /app/list/page — login required, owner's apps */
  getMyList: (params: AppQueryRequest) =>
    request.get<any, BaseResponse<Page<AppVO>>>('/app/list/page', { params }),

  /** GET /app/star/page — public, priority != null */
  getStarList: (params: { pageNum: number; pageSize: number; appName?: string }) =>
    request.get<any, BaseResponse<Page<AppVO>>>('/app/star/page', { params }),

  /** POST /app/deploy */
  deploy: (appId: number) =>
    request.post<any, BaseResponse<string>>('/app/deploy', { appId }),

  /** GET /app/downLoad?appId= — opens browser download */
  download: (appId: number) =>
    window.open(`/api/app/downLoad?appId=${appId}`),

  /** GET /app/files/{appId} */
  getFileTree: (appId: number) =>
    request.get<any, BaseResponse<FileTreeNode>>(`/app/files/${appId}`),

  /** GET /app/file/{appId}?path= */
  getFileContent: (appId: number, path: string) =>
    request.get<any, BaseResponse<string>>(`/app/file/${appId}`, { params: { path } }),

  // ── Admin only ──────────────────────────────────────────────────────────

  /** POST /app/admin/update */
  adminUpdate: (data: AppUpdateRequest) =>
    request.post<any, BaseResponse<boolean>>('/app/admin/update', data),

  /** POST /app/admin/delete/{id} */
  adminDelete: (id: number) =>
    request.post<any, BaseResponse<boolean>>(`/app/admin/delete/${id}`),

  /** GET /app/admin/list — filterable, supports userId */
  adminGetList: (params: AppAdminQueryRequest) =>
    request.get<any, BaseResponse<Page<AppVO>>>('/app/admin/list', { params }),

  /** GET /app/admin/get/{id} */
  adminGetDetail: (id: number) =>
    request.get<any, BaseResponse<AppDetailVO>>(`/app/admin/get/${id}`),
};

// ─── Chat ─────────────────────────────────────────────────────────────────────

export const chatApi = {
  /**
   * GET /chatHistory/list/app
   * Use sortField=createTime&sortOrder=ascend for chronological order.
   * Use createTimeBefore for cursor-based older-page loading.
   */
  getHistory: (params: ChatHistoryQueryRequest) =>
    request.get<any, BaseResponse<Page<ChatHistoryVO>>>('/chatHistory/list/app', { params }),

  /** GET /chatHistory/admin/list — admin, supports userId filter */
  adminGetHistory: (params: ChatHistoryQueryRequest & { userId?: number }) =>
    request.get<any, BaseResponse<Page<ChatHistoryVO>>>('/chatHistory/admin/list', { params }),

  /**
   * POST /app/agent/approve
   * Call when user responds to an approval_request SSE event.
   * Returns true if approvalId exists and hasn't timed out (300 s).
   */
  approveAgent: (data: AgentApprovalRequest) =>
    request.post<any, BaseResponse<boolean>>('/app/agent/approve', data),
};
