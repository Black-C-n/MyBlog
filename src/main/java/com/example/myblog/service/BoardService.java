package com.example.myblog.service;

import com.example.myblog.entity.Board;
import com.example.myblog.entity.MyUser;
import com.example.myblog.form.BoardForm;
import com.example.myblog.repository.BoardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserService userService;

    public BoardService(BoardRepository boardRepository, UserService userService) {
        this.boardRepository = boardRepository;
        this.userService = userService;
    }

    public Page<Board> findPage(Integer userId, int pageNumber) {
        int safePage = Math.max(1, pageNumber) - 1;
        Page<Board> page = boardRepository.findByUserIdOrderByCreatedDesc(userId,
                PageRequest.of(safePage, 10, Sort.by(Sort.Direction.DESC, "created")));
        if (page.getTotalPages() > 0 && safePage >= page.getTotalPages()) {
            page = boardRepository.findByUserIdOrderByCreatedDesc(userId,
                    PageRequest.of(page.getTotalPages() - 1, 10,
                            Sort.by(Sort.Direction.DESC, "created")));
        }
        return page;
    }

    @Transactional
    public void addBoard(Integer userId, BoardForm form) {
        MyUser user = userService.getRequired(userId);
        Board board = new Board();
        board.setUser(user);
        board.setName(form.getName().trim());
        board.setEmail(form.getEmail().trim());
        board.setContent(form.getContent().trim());
        boardRepository.save(board);
    }
}
