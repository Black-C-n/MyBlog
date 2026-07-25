package com.example.myblog.repository;

import com.example.myblog.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    List<Comment> findByArticleIdOrderByCreatedDesc(Integer articleId);

    @EntityGraph(attributePaths = "article")
    List<Comment> findByArticleAuthorIdOrderByCreatedDesc(Integer authorId);

    @EntityGraph(attributePaths = "article")
    Optional<Comment> findByIdAndArticleAuthorId(Integer id, Integer authorId);

    void deleteByArticleId(Integer articleId);

    long countByArticleAuthorId(Integer authorId);
}
