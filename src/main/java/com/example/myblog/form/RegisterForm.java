package com.example.myblog.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(message = "请输入用户名")
    @Size(max = 150, message = "用户名不能超过150个字符")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, message = "密码至少需要6个字符")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
