package com.example.myblog.controller;

import com.example.myblog.config.AdminLoginInterceptor;
import com.example.myblog.entity.AlbumInfo;
import com.example.myblog.entity.ArticleInfo;
import com.example.myblog.entity.ArticleTag;
import com.example.myblog.entity.Board;
import com.example.myblog.entity.Comment;
import com.example.myblog.entity.MyUser;
import com.example.myblog.service.AdminService;
import com.example.myblog.service.FileStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminController {

    private final AdminService adminService;
    private final FileStorageService fileStorageService;

    public AdminController(AdminService adminService, FileStorageService fileStorageService) {
        this.adminService = adminService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping({"/admin", "/admin/"})
    public String dashboard(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "网站管理", "首页");
        model.addAttribute("articleCount", adminService.articleCount(user.getId()));
        model.addAttribute("albumCount", adminService.albumCount(user.getId()));
        model.addAttribute("tagCount", adminService.tagCount(user.getId()));
        model.addAttribute("commentCount", adminService.commentCount(user.getId()));
        model.addAttribute("boardCount", adminService.boardCount(user.getId()));
        return "admin/index";
    }

    @GetMapping("/admin/logout/")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login.html";
    }

    @GetMapping("/admin/account/myuser/")
    public String users(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "选择 用户 来修改", "用户管理");
        model.addAttribute("users", List.of(user));
        return "admin/user_list";
    }

    @GetMapping("/admin/account/myuser/{id}/change/")
    public String userForm(@PathVariable Integer id, HttpSession session, Model model) {
        MyUser user = loginUser(session);
        requireSameUser(user, id);
        base(model, "修改 用户", "用户管理");
        model.addAttribute("item", user);
        return "admin/user_form";
    }

    @PostMapping("/admin/account/myuser/{id}/change/")
    public String updateUser(@PathVariable Integer id,
                             @RequestParam String username,
                             @RequestParam(defaultValue = "") String name,
                             @RequestParam(defaultValue = "") String email,
                             @RequestParam(defaultValue = "") String introduce,
                             @RequestParam(defaultValue = "") String company,
                             @RequestParam(defaultValue = "") String profession,
                             @RequestParam(defaultValue = "") String address,
                             @RequestParam(defaultValue = "") String telephone,
                             @RequestParam(defaultValue = "") String wx,
                             @RequestParam(defaultValue = "") String qq,
                             @RequestParam(defaultValue = "") String wb,
                             @RequestParam(defaultValue = "") String newPassword,
                             @RequestParam(required = false) MultipartFile photoFile,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        MyUser loginUser = loginUser(session);
        requireSameUser(loginUser, id);
        String photo = fileStorageService.store(photoFile, "images/user");
        MyUser saved = adminService.updateProfile(id, username, name, email, introduce,
                company, profession, address, telephone, wx, qq, wb, photo, newPassword);
        session.setAttribute(AdminLoginInterceptor.LOGIN_USER, saved);
        redirectAttributes.addFlashAttribute("message", "用户修改成功");
        return "redirect:/admin/account/myuser/";
    }

    @GetMapping("/admin/article/articleinfo/")
    public String articles(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "选择 博文管理 来修改", "文章管理");
        model.addAttribute("articles", adminService.articles(user.getId()));
        return "admin/article_list";
    }

    @GetMapping("/admin/article/articleinfo/add/")
    public String addArticleForm(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        prepareArticleForm(model, user, new ArticleInfo(), "增加 博文管理");
        return "admin/article_form";
    }

    @PostMapping("/admin/article/articleinfo/add/")
    public String addArticle(@RequestParam String title,
                             @RequestParam(defaultValue = "") String content,
                             @RequestParam(defaultValue = "0") int reading,
                             @RequestParam(defaultValue = "0") int liking,
                             @RequestParam(required = false) List<Integer> tagIds,
                             @RequestParam(required = false) MultipartFile articlePhotoFile,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        MyUser user = loginUser(session);
        String photo = fileStorageService.store(articlePhotoFile, "images/article");
        adminService.saveArticle(user.getId(), null, title, content, reading, liking, photo, tagIds);
        redirectAttributes.addFlashAttribute("message", "博文增加成功");
        return "redirect:/admin/article/articleinfo/";
    }

    @GetMapping("/admin/article/articleinfo/{id}/change/")
    public String editArticleForm(@PathVariable Integer id, HttpSession session, Model model) {
        MyUser user = loginUser(session);
        prepareArticleForm(model, user, adminService.article(user.getId(), id), "修改 博文管理");
        return "admin/article_form";
    }

    @PostMapping("/admin/article/articleinfo/{id}/change/")
    public String updateArticle(@PathVariable Integer id,
                                @RequestParam String title,
                                @RequestParam(defaultValue = "") String content,
                                @RequestParam(defaultValue = "0") int reading,
                                @RequestParam(defaultValue = "0") int liking,
                                @RequestParam(required = false) List<Integer> tagIds,
                                @RequestParam(required = false) MultipartFile articlePhotoFile,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        MyUser user = loginUser(session);
        String photo = fileStorageService.store(articlePhotoFile, "images/article");
        adminService.saveArticle(user.getId(), id, title, content, reading, liking, photo, tagIds);
        redirectAttributes.addFlashAttribute("message", "博文修改成功");
        return "redirect:/admin/article/articleinfo/";
    }

    @PostMapping("/admin/article/articleinfo/{id}/delete/")
    public String deleteArticle(@PathVariable Integer id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        adminService.deleteArticle(loginUser(session).getId(), id);
        redirectAttributes.addFlashAttribute("message", "博文删除成功");
        return "redirect:/admin/article/articleinfo/";
    }

    @GetMapping("/admin/article/articletag/")
    public String tags(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "选择 博文分类 来修改", "文章管理");
        model.addAttribute("tags", adminService.tags(user.getId()));
        return "admin/tag_list";
    }

    @GetMapping("/admin/article/articletag/add/")
    public String addTagForm(Model model) {
        base(model, "增加 博文分类", "文章管理");
        model.addAttribute("item", new ArticleTag());
        return "admin/tag_form";
    }

    @GetMapping("/admin/article/articletag/{id}/change/")
    public String editTagForm(@PathVariable Integer id, HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "修改 博文分类", "文章管理");
        model.addAttribute("item", adminService.tag(user.getId(), id));
        return "admin/tag_form";
    }

    @PostMapping({"/admin/article/articletag/add/", "/admin/article/articletag/{id}/change/"})
    public String saveTag(@PathVariable(required = false) Integer id,
                          @RequestParam String tag,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        adminService.saveTag(loginUser(session).getId(), id, tag);
        redirectAttributes.addFlashAttribute("message", id == null ? "分类增加成功" : "分类修改成功");
        return "redirect:/admin/article/articletag/";
    }

    @PostMapping("/admin/article/articletag/{id}/delete/")
    public String deleteTag(@PathVariable Integer id,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        adminService.deleteTag(loginUser(session).getId(), id);
        redirectAttributes.addFlashAttribute("message", "分类删除成功");
        return "redirect:/admin/article/articletag/";
    }

    @GetMapping("/admin/article/comment/")
    public String comments(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "选择 评论管理 来修改", "文章管理");
        model.addAttribute("comments", adminService.comments(user.getId()));
        return "admin/comment_list";
    }

    @GetMapping("/admin/article/comment/add/")
    public String addCommentForm(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        prepareCommentForm(model, user, new Comment(), "增加 评论管理");
        return "admin/comment_form";
    }

    @GetMapping("/admin/article/comment/{id}/change/")
    public String editCommentForm(@PathVariable Integer id, HttpSession session, Model model) {
        MyUser user = loginUser(session);
        prepareCommentForm(model, user, adminService.comment(user.getId(), id), "修改 评论管理");
        return "admin/comment_form";
    }

    @PostMapping({"/admin/article/comment/add/", "/admin/article/comment/{id}/change/"})
    public String saveComment(@PathVariable(required = false) Integer id,
                              @RequestParam Integer articleId,
                              @RequestParam String commentator,
                              @RequestParam String content,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        adminService.saveComment(loginUser(session).getId(), id, articleId, commentator, content);
        redirectAttributes.addFlashAttribute("message", id == null ? "评论增加成功" : "评论修改成功");
        return "redirect:/admin/article/comment/";
    }

    @PostMapping("/admin/article/comment/{id}/delete/")
    public String deleteComment(@PathVariable Integer id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        adminService.deleteComment(loginUser(session).getId(), id);
        redirectAttributes.addFlashAttribute("message", "评论删除成功");
        return "redirect:/admin/article/comment/";
    }

    @GetMapping("/admin/album/albuminfo/")
    public String albums(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "选择 图片墙管理 来修改", "图片墙管理");
        model.addAttribute("albums", adminService.albums(user.getId()));
        return "admin/album_list";
    }

    @GetMapping("/admin/album/albuminfo/add/")
    public String addAlbumForm(Model model) {
        base(model, "增加 图片墙管理", "图片墙管理");
        model.addAttribute("item", new AlbumInfo());
        return "admin/album_form";
    }

    @GetMapping("/admin/album/albuminfo/{id}/change/")
    public String editAlbumForm(@PathVariable Integer id, HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "修改 图片墙管理", "图片墙管理");
        model.addAttribute("item", adminService.album(user.getId(), id));
        return "admin/album_form";
    }

    @PostMapping({"/admin/album/albuminfo/add/", "/admin/album/albuminfo/{id}/change/"})
    public String saveAlbum(@PathVariable(required = false) Integer id,
                            @RequestParam(defaultValue = "") String title,
                            @RequestParam(defaultValue = "") String introduce,
                            @RequestParam(required = false) MultipartFile photoFile,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String photo = fileStorageService.store(photoFile, "images/album");
        adminService.saveAlbum(loginUser(session).getId(), id, title, introduce, photo);
        redirectAttributes.addFlashAttribute("message", id == null ? "图片增加成功" : "图片修改成功");
        return "redirect:/admin/album/albuminfo/";
    }

    @PostMapping("/admin/album/albuminfo/{id}/delete/")
    public String deleteAlbum(@PathVariable Integer id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        adminService.deleteAlbum(loginUser(session).getId(), id);
        redirectAttributes.addFlashAttribute("message", "图片删除成功");
        return "redirect:/admin/album/albuminfo/";
    }

    @GetMapping("/admin/interflow/board/")
    public String boards(HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "选择 博客留言 来修改", "留言管理");
        model.addAttribute("boards", adminService.boards(user.getId()));
        return "admin/board_list";
    }

    @GetMapping("/admin/interflow/board/add/")
    public String addBoardForm(Model model) {
        base(model, "增加 博客留言", "留言管理");
        model.addAttribute("item", new Board());
        return "admin/board_form";
    }

    @GetMapping("/admin/interflow/board/{id}/change/")
    public String editBoardForm(@PathVariable Integer id, HttpSession session, Model model) {
        MyUser user = loginUser(session);
        base(model, "修改 博客留言", "留言管理");
        model.addAttribute("item", adminService.board(user.getId(), id));
        return "admin/board_form";
    }

    @PostMapping({"/admin/interflow/board/add/", "/admin/interflow/board/{id}/change/"})
    public String saveBoard(@PathVariable(required = false) Integer id,
                            @RequestParam String name,
                            @RequestParam(defaultValue = "") String email,
                            @RequestParam String content,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        adminService.saveBoard(loginUser(session).getId(), id, name, email, content);
        redirectAttributes.addFlashAttribute("message", id == null ? "留言增加成功" : "留言修改成功");
        return "redirect:/admin/interflow/board/";
    }

    @PostMapping("/admin/interflow/board/{id}/delete/")
    public String deleteBoard(@PathVariable Integer id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        adminService.deleteBoard(loginUser(session).getId(), id);
        redirectAttributes.addFlashAttribute("message", "留言删除成功");
        return "redirect:/admin/interflow/board/";
    }

    @PostMapping(value = "/ckeditor/upload/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String uploadEditorImage(@RequestParam("upload") MultipartFile upload,
                                    @RequestParam(name = "CKEditorFuncNum", defaultValue = "1") String functionNumber) {
        String safeNumber = functionNumber.matches("\\d+") ? functionNumber : "1";
        String path = fileStorageService.store(upload, "article_images");
        String url = "/media/" + path;
        return "<script>window.parent.CKEDITOR.tools.callFunction(" + safeNumber
                + ", '" + url + "', '上传成功');</script>";
    }

    private void prepareArticleForm(Model model, MyUser user, ArticleInfo article, String title) {
        base(model, title, "文章管理");
        model.addAttribute("item", article);
        model.addAttribute("allTags", adminService.tags(user.getId()));
        model.addAttribute("selectedTagIds", article.getTags().stream()
                .map(ArticleTag::getId)
                .toList());
    }

    private void prepareCommentForm(Model model, MyUser user, Comment comment, String title) {
        base(model, title, "文章管理");
        model.addAttribute("item", comment);
        model.addAttribute("articles", adminService.articles(user.getId()));
    }

    private void base(Model model, String title, String appName) {
        model.addAttribute("title", title);
        model.addAttribute("appName", appName);
    }

    private MyUser loginUser(HttpSession session) {
        return (MyUser) session.getAttribute(AdminLoginInterceptor.LOGIN_USER);
    }

    private void requireSameUser(MyUser user, Integer id) {
        if (!user.getId().equals(id)) {
            throw new IllegalArgumentException("无权访问其他用户的数据");
        }
    }
}
