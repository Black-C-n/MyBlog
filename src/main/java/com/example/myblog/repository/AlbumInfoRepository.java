package com.example.myblog.repository;

import com.example.myblog.entity.AlbumInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumInfoRepository extends JpaRepository<AlbumInfo, Integer> {

    List<AlbumInfo> findByUserIdOrderByIdAsc(Integer userId);

    Page<AlbumInfo> findByUserIdOrderByIdAsc(Integer userId, Pageable pageable);

    Optional<AlbumInfo> findByIdAndUserId(Integer id, Integer userId);

    long countByUserId(Integer userId);
}
