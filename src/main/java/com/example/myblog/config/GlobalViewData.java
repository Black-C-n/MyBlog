package com.example.myblog.config;

import com.example.myblog.entity.MyUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewData {

    @ModelAttribute("loginUser")
    public MyUser loginUser(HttpSession session) {
        return (MyUser) session.getAttribute(AdminLoginInterceptor.LOGIN_USER);
    }
}
