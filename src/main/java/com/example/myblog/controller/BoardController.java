package com.example.myblog.controller;

import com.example.myblog.form.BoardForm;
import com.example.myblog.service.BlogViewService;
import com.example.myblog.service.BoardService;
import com.example.myblog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class BoardController {

    private final BoardService boardService;
    private final BlogViewService blogViewService;
    private final UserService userService;

    public BoardController(BoardService boardService,
                           BlogViewService blogViewService,
                           UserService userService) {
        this.boardService = boardService;
        this.blogViewService = blogViewService;
        this.userService = userService;
    }

    @GetMapping("/board/{id}/{page}.html")
    public String board(@PathVariable Integer id,
                        @PathVariable int page,
                        Model model) {
        if (userService.findById(id).isEmpty()) {
            return "redirect:/user/register.html";
        }
        preparePage(model, id, page);
        model.addAttribute("boardForm", new BoardForm());
        return "board";
    }

    @PostMapping("/board/{id}/{page}.html")
    public String addBoard(@PathVariable Integer id,
                           @PathVariable int page,
                           @Valid @ModelAttribute BoardForm boardForm,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            preparePage(model, id, page);
            model.addAttribute("tips", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "board";
        }
        boardService.addBoard(id, boardForm);
        return "redirect:/board/" + id + "/1.html";
    }

    private void preparePage(Model model, Integer id, int page) {
        blogViewService.addCommonData(model, id);
        model.addAttribute("pageInfo", boardService.findPage(id, page));
    }
}
