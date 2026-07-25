package com.example.myblog.service;

import com.example.myblog.entity.MyUser;
import com.example.myblog.repository.AlbumInfoRepository;
import com.example.myblog.repository.ArticleTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class BlogViewService {

    private final UserService userService;
    private final AlbumInfoRepository albumRepository;
    private final ArticleTagRepository tagRepository;

    public BlogViewService(UserService userService,
                           AlbumInfoRepository albumRepository,
                           ArticleTagRepository tagRepository) {
        this.userService = userService;
        this.albumRepository = albumRepository;
        this.tagRepository = tagRepository;
    }

    public MyUser addCommonData(Model model, Integer userId) {
        MyUser user = userService.getRequired(userId);
        model.addAttribute("id", userId);
        model.addAttribute("user", user);
        model.addAttribute("album", albumRepository.findByUserIdOrderByIdAsc(userId));
        model.addAttribute("tags", tagRepository.findByUserIdOrderByIdAsc(userId));
        return user;
    }
}
