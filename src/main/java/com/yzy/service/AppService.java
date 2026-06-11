package com.yzy.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.yzy.dto.AppAddRequest;
import com.yzy.dto.AppQueryRequest;
import com.yzy.dto.AppStarQueryRequest;
import com.yzy.dto.AppUpdateRequest;
import com.yzy.entity.App;
import com.yzy.entity.User;
import com.yzy.vo.AppDetailVO;
import com.yzy.vo.AppVO;
import reactor.core.publisher.Flux;

/**
 * 应用 服务层。
 *
 * @author yzy
 * @since 2026-03-12
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param request 创建应用请求
     * @return 应用id
     */
    Long createApp(AppAddRequest request);

    /**
     * 更新应用
     *
     * @param id 应用id
     * @param request 更新应用请求
     * @return 是否成功
     */
    Boolean updateApp(Long id, AppUpdateRequest request);

    /**
     * 删除应用
     *
     * @param id 应用id
     * @return 是否成功
     */
    Boolean deleteApp(Long id);

    /**
     * 获取应用详情
     *
     * @param id 应用id
     * @return 应用详情
     */
    AppDetailVO getAppDetail(Long id);

    /**
     * 分页查询自己的应用列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<AppVO> getMyAppList(AppQueryRequest request);

    /**
     * 分页查询精选的应用列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<AppVO> getStarAppList(AppStarQueryRequest request);

    /**
     * 管理员更新应用
     *
     * @param id 应用id
     * @param request 更新应用请求
     * @return 是否成功
     */
    Boolean adminUpdateApp(Long id, AppUpdateRequest request);

    /**
     * 管理员删除应用
     *
     * @param id 应用id
     * @return 是否成功
     */
    Boolean adminDeleteApp(Long id);

    /**
     * 管理员分页查询应用列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<AppVO> adminGetAppList(AppQueryRequest request);

    /**
     * 根据对话生成代码
     * @param appId 应用id
     * @param msg 用户信息
     * @param user 已登录用户
     * @param agent 是否启用工作流
     * @return 流式输出结果
     */
    Flux<String> chatToGenCode(Long appId, String msg, User user,boolean agent);

    /**
     * 部署应用
     * @param appId 应用id
     * @param loginUser 已登录的用户
     * @return 可访问的url
     */
    String deployApp(Long appId,User loginUser);

    /**
     * 获取应用文件树
     * @param appId 应用id
     * @return 文件树根节点
     */
    com.yzy.dto.FileTreeNode getFileTree(Long appId);

    /**
     * 获取应用文件内容
     * @param appId 应用id
     * @param path 文件相对路径
     * @return 文件内容
     */
    String getFileContent(Long appId, String path);

}
