package com.example.myblog.controller;

import com.example.myblog.entity.ArticleInfo;
import com.example.myblog.form.CommentForm;
import com.example.myblog.service.ArticleService;
import com.example.myblog.service.BlogViewService;
import com.example.myblog.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ArticleController {

    private final ArticleService articleService;
    private final BlogViewService blogViewService;
    private final UserService userService;

    public ArticleController(ArticleService articleService,
                             BlogViewService blogViewService,
                             UserService userService) {
        this.articleService = articleService;
        this.blogViewService = blogViewService;
        this.userService = userService;
    }

    @GetMapping("/{id}/{page}.html")
    public String articleList(@PathVariable Integer id,
                              @PathVariable int page,
                              Model model) {
        if (userService.findById(id).isEmpty()) {
            return "redirect:/user/register.html";
        }
        blogViewService.addCommonData(model, id);
        Page<ArticleInfo> pageInfo = articleService.findPage(page, id);
        model.addAttribute("pageInfo", pageInfo);
        return "article";
    }

    @GetMapping("/detail/{id}/{articleId}.html")
    public String detail(@PathVariable Integer id,
                         @PathVariable Integer articleId,
                         HttpSession session,
                         Model model) {
        if (userService.findById(id).isEmpty()) {
            return "redirect:/user/register.html";
        }
        String readingKey = "reading" + id + articleId;
        boolean firstReading = session.getAttribute(readingKey) == null;
        ArticleInfo article = articleService.findDetail(articleId, firstReading);
        if (firstReading) {
            session.setAttribute(readingKey, Boolean.TRUE);
        }
        prepareDetail(model, id, article);
        model.addAttribute("commentForm", new CommentForm());
        return "detail";
    }

    @PostMapping("/detail/{id}/{articleId}.html")
    public String addComment(@PathVariable Integer id,
                             @PathVariable Integer articleId,
                             @Valid @ModelAttribute CommentForm commentForm,
                             BindingResult bindingResult,
                             Model model) {
        if (bindingResult.hasErrors()) {
            prepareDetail(model, id, articleService.findDetail(articleId, false));
            model.addAttribute("tips", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "detail";
        }
        articleService.addComment(articleId, commentForm);
        return "redirect:/detail/" + id + "/" + articleId + ".html";
    }

    private void prepareDetail(Model model, Integer id, ArticleInfo article) {
        blogViewService.addCommonData(model, id);
        model.addAttribute("article", article);
        model.addAttribute("articleTags", article.getTags());
        model.addAttribute("comments", articleService.findComments(article.getId()));
    }
}
