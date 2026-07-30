package com.example.myblog.repository;

import com.example.myblog.entity.ArticleInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleInfoRepository extends JpaRepository<ArticleInfo, Integer> {

    Page<ArticleInfo> findByAuthorIdAndTitleContainingIgnoreCaseOrderByCreatedDesc(Integer authorId, String keyword, Pageable pageable);

    List<ArticleInfo> findByAuthorIdOrderByCreatedDesc(Integer authorId);

    Optional<ArticleInfo> findByIdAndAuthorId(Integer id, Integer authorId);

    long countByAuthorId(Integer authorId);

    @EntityGraph(attributePaths = {"tags", "author"})
    @Query("select a from ArticleInfo a where a.id = :id")
    Optional<ArticleInfo> findDetailById(@Param("id") Integer id);

    @Modifying(clearAutomatically = true)
    @Query("update ArticleInfo a set a.reading = a.reading + 1 where a.id = :id")
    int increaseReading(@Param("id") Integer id);
}
