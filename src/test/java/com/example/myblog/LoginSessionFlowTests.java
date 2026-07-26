package com.example.myblog;

import com.example.myblog.config.AdminLoginInterceptor;
import com.example.myblog.config.WebMvcConfig;
import com.example.myblog.controller.AccountController;
import com.example.myblog.controller.AdminController;
import com.example.myblog.entity.MyUser;
import com.example.myblog.service.AdminService;
import com.example.myblog.service.BlogViewService;
import com.example.myblog.service.FileStorageService;
import com.example.myblog.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {AccountController.class, AdminController.class})
@Import({WebMvcConfig.class, AdminLoginInterceptor.class})
class LoginSessionFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BlogViewService blogViewService;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    void loggedInUserCanOpenAdminDashboard() throws Exception {
        MyUser user = new MyUser();
        user.setId(42);
        user.setUsername("test-admin");
        user.setActive(true);
        user.setStaff(true);

        when(userService.authenticate("test-admin", "correct-password"))
                .thenReturn(Optional.of(user));

        MvcResult loginResult = mockMvc.perform(post("/user/login.html")
                        .param("username", "test-admin")
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/42/1.html"))
                .andReturn();

        MockHttpSession loginSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(loginSession);
        assertSame(user, loginSession.getAttribute(AdminLoginInterceptor.LOGIN_USER));

        mockMvc.perform(get("/admin/").session(loginSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"));
    }
}
