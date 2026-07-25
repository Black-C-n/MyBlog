package com.example.myblog.controller;

import com.example.myblog.config.AdminLoginInterceptor;
import com.example.myblog.entity.AlbumInfo;
import com.example.myblog.entity.MyUser;
import com.example.myblog.repository.AlbumInfoRepository;
import com.example.myblog.service.BlogViewService;
import com.example.myblog.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AlbumController {

    private final AlbumInfoRepository albumRepository;
    private final BlogViewService blogViewService;
    private final UserService userService;

    public AlbumController(AlbumInfoRepository albumRepository,
                           BlogViewService blogViewService,
                           UserService userService) {
        this.albumRepository = albumRepository;
        this.blogViewService = blogViewService;
        this.userService = userService;
    }

    @GetMapping("/album/")
    public String albumIndex(HttpSession session) {
        MyUser loginUser = (MyUser) session.getAttribute(AdminLoginInterceptor.LOGIN_USER);
        if (loginUser != null) {
            return "redirect:/album/" + loginUser.getId() + "/1.html";
        }
        return userService.firstUser()
                .map(user -> "redirect:/album/" + user.getId() + "/1.html")
                .orElse("redirect:/user/register.html");
    }

    @GetMapping("/album/{id}/{page}.html")
    public String album(@PathVariable Integer id,
                        @PathVariable int page,
                        Model model) {
        if (userService.findById(id).isEmpty()) {
            return "redirect:/user/register.html";
        }
        blogViewService.addCommonData(model, id);
        int safePage = Math.max(1, page) - 1;
        Page<AlbumInfo> pageInfo = albumRepository.findByUserIdOrderByIdAsc(id,
                PageRequest.of(safePage, 8, Sort.by(Sort.Direction.ASC, "id")));
        if (pageInfo.getTotalPages() > 0 && safePage >= pageInfo.getTotalPages()) {
            pageInfo = albumRepository.findByUserIdOrderByIdAsc(id,
                    PageRequest.of(pageInfo.getTotalPages() - 1, 8,
                            Sort.by(Sort.Direction.ASC, "id")));
        }
        model.addAttribute("pageInfo", pageInfo);
        return "album";
    }
}
