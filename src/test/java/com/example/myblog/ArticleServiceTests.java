package com.example.myblog;

import com.example.myblog.entity.ArticleInfo;
import com.example.myblog.repository.ArticleInfoRepository;
import com.example.myblog.repository.CommentRepository;
import com.example.myblog.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceTests {

    @Test
    @SuppressWarnings("unchecked")
    void trimsKeywordBeforeNormalAndFallbackQueries() {
        ArticleInfoRepository articleRepository = mock(ArticleInfoRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        ArticleService articleService = new ArticleService(articleRepository, commentRepository);

        Page<ArticleInfo> requestedPage = mock(Page.class);
        Page<ArticleInfo> fallbackPage = mock(Page.class);
        when(requestedPage.getTotalPages()).thenReturn(2);
        when(articleRepository.findByAuthorIdAndTitleContainingIgnoreCaseOrderByCreatedDesc(
                eq(7), eq("Django"), any(Pageable.class)))
                .thenReturn(requestedPage, fallbackPage);

        Page<ArticleInfo> result = articleService.findPage(7, 5, "  Django  ");

        assertSame(fallbackPage, result);
        verify(articleRepository, times(2))
                .findByAuthorIdAndTitleContainingIgnoreCaseOrderByCreatedDesc(
                        eq(7), eq("Django"), any(Pageable.class));
    }
}
