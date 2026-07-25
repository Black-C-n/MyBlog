package com.example.myblog.repository;

import com.example.myblog.entity.ArticleTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleTagRepository extends JpaRepository<ArticleTag, Integer> {

    List<ArticleTag> findByUserIdOrderByIdAsc(Integer userId);

    Optional<ArticleTag> findByIdAndUserId(Integer id, Integer userId);

    long countByUserId(Integer userId);
}
