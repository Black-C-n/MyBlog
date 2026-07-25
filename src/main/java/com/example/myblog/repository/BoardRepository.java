package com.example.myblog.repository;

import com.example.myblog.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Integer> {

    Page<Board> findByUserIdOrderByCreatedDesc(Integer userId, Pageable pageable);

    List<Board> findByUserIdOrderByCreatedDesc(Integer userId);

    Optional<Board> findByIdAndUserId(Integer id, Integer userId);

    long countByUserId(Integer userId);
}
