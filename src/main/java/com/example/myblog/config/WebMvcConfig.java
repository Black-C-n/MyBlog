package com.example.myblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminLoginInterceptor adminLoginInterceptor;
    private final Path uploadPath;

    public WebMvcConfig(AdminLoginInterceptor adminLoginInterceptor,
                        @Value("${app.upload-dir}") String uploadDir) {
        this.adminLoginInterceptor = adminLoginInterceptor;
        this.uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminLoginInterceptor)
                .addPathPatterns("/admin/**", "/ckeditor/**")
                .excludePathPatterns("/admin/logout/");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/media/**")
                // 先读取用户在后台新上传的图片。
                .addResourceLocations(location)
                // 找不到时读取随项目和 JAR 一起打包的原始博客图片。
                .addResourceLocations("classpath:/static/media/");
    }
}
