package com.example.myblog.service;

import com.example.myblog.entity.ArticleInfo;
import com.example.myblog.entity.Comment;
import com.example.myblog.form.CommentForm;
import com.example.myblog.repository.ArticleInfoRepository;
import com.example.myblog.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleService {

    private final ArticleInfoRepository articleRepository;
    private final CommentRepository commentRepository;

    public ArticleService(ArticleInfoRepository articleRepository,
                          CommentRepository commentRepository) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
    }

    public Page<ArticleInfo> findPage(Integer userId, int pageNumber) {
        int safePage = Math.max(1, pageNumber) - 1;
        Page<ArticleInfo> page = articleRepository.findByAuthorIdOrderByCreatedDesc(userId,
                PageRequest.of(safePage, 10, Sort.by(Sort.Direction.DESC, "created")));
        if (page.getTotalPages() > 0 && safePage >= page.getTotalPages()) {
            page = articleRepository.findByAuthorIdOrderByCreatedDesc(userId,
                    PageRequest.of(page.getTotalPages() - 1, 10,
                            Sort.by(Sort.Direction.DESC, "created")));
        }
        return page;
    }

    @Transactional
    public ArticleInfo findDetail(Integer articleId, boolean increaseReading) {
        if (increaseReading) {
            articleRepository.increaseReading(articleId);
        }
        return articleRepository.findDetailById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在：" + articleId));
    }

    public List<Comment> findComments(Integer articleId) {
        return commentRepository.findByArticleIdOrderByCreatedDesc(articleId);
    }

    @Transactional
    public void addComment(Integer articleId, CommentForm form) {
        ArticleInfo article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在：" + articleId));
        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setCommentator(form.getName().trim());
        comment.setContent(form.getContent().trim());
        commentRepository.save(comment);
    }
}
