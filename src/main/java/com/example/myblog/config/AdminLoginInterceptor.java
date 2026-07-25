package com.example.myblog.config;

import com.example.myblog.entity.MyUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminLoginInterceptor implements HandlerInterceptor {

    public static final String LOGIN_USER = "loginUser";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        MyUser user = session == null ? null : (MyUser) session.getAttribute(LOGIN_USER);
        if (user == null || !user.isActive() || !user.isStaff()) {
            response.sendRedirect(request.getContextPath() + "/user/login.html");
            return false;
        }
        return true;
    }
}
