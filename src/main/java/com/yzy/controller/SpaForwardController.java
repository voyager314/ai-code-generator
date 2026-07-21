package com.yzy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 前端路由转发
 * 将前端路由请求转发到 index.html，由 React Router 处理客户端路由。
 * 刷新 /login、/app/123 等页面时不会 404。
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/admin", "/apps"})
    public String forward() {
        return "forward:/index.html";
    }

    @GetMapping("/app/{id:\\d+}")
    public String forwardAppPage() {
        return "forward:/index.html";
    }
}
