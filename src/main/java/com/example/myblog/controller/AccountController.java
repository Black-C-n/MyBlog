package com.example.myblog.controller;

import com.example.myblog.config.AdminLoginInterceptor;
import com.example.myblog.entity.MyUser;
import com.example.myblog.form.RegisterForm;
import com.example.myblog.service.BlogViewService;
import com.example.myblog.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final UserService userService;
    private final BlogViewService blogViewService;

    public AccountController(UserService userService, BlogViewService blogViewService) {
        this.userService = userService;
        this.blogViewService = blogViewService;
    }

    @GetMapping("/user/register.html")
    public String registerPage(Model model) {
        prepareUserPage(model, true);
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "user";
    }

    @PostMapping("/user/register.html")
    public String register(@Valid @ModelAttribute RegisterForm registerForm,
                           BindingResult bindingResult,
                           Model model,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        prepareUserPage(model, true);
        if (bindingResult.hasErrors()) {
            model.addAttribute("tips", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "user";
        }
        try {
            userService.register(registerForm);
            session.invalidate();
            redirectAttributes.addFlashAttribute("tips", "注册成功，请登录");
            return "redirect:/user/login.html";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("tips", exception.getMessage());
            return "user";
        }
    }

    @GetMapping("/user/login.html")
    public String loginPage(HttpSession session, Model model) {
        MyUser loginUser = (MyUser) session.getAttribute(AdminLoginInterceptor.LOGIN_USER);
        if (loginUser != null) {
            return "redirect:/" + loginUser.getId() + "/1.html";
        }
        prepareUserPage(model, false);
        return "user";
    }

    @PostMapping("/user/login.html")

    public String login(@RequestParam(defaultValue = "") String username,
                        @RequestParam(defaultValue = "") String password,
                        HttpSession session,
                        Model model) {
        prepareUserPage(model, false);
        if (username.isBlank()) {
            model.addAttribute("tips", "请输入用户名");
            return "user";
        }else if (password.isBlank()) {
            model.addAttribute("tips", "请输入密码");
            return "user";
        }
        return userService.authenticate(username, password)
                .map(user -> {
                    session.setAttribute(AdminLoginInterceptor.LOGIN_USER, user);
                    return "redirect:/" + user.getId() + "/1.html";
                })
                .orElseGet(() -> {
                    String tips = userService.usernameExists(username)
                            ? "账号密码错误，请重新输入" : "用户不存在，请注册";
                    model.addAttribute("tips", tips);
                    return "user";
                });
    }

    @GetMapping("/user/logout.html")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login.html";
    }

    @GetMapping("/user/about/{id}.html")
    public String about(@PathVariable Integer id, Model model) {
        if (userService.findById(id).isEmpty()) {
            return "redirect:/user/register.html";
        }
        blogViewService.addCommonData(model, id);
        return "about";
    }

    private void prepareUserPage(Model model, boolean register) {
        model.addAttribute("title", register ? "注册博客" : "登录博客");
        model.addAttribute("pageTitle", register ? "创建博客账号" : "欢迎登录博客");
        model.addAttribute("confirmPassword", register);
        model.addAttribute("button", register ? "注册" : "登录");
        model.addAttribute("urlText", register ? "用户登录" : "用户注册");
        model.addAttribute("url", register ? "/user/login.html" : "/user/register.html");
    }
}
