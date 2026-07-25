package com.example.myblog.service;

import com.example.myblog.entity.AlbumInfo;
import com.example.myblog.entity.ArticleInfo;
import com.example.myblog.entity.ArticleTag;
import com.example.myblog.entity.Board;
import com.example.myblog.entity.Comment;
import com.example.myblog.entity.MyUser;
import com.example.myblog.repository.AlbumInfoRepository;
import com.example.myblog.repository.ArticleInfoRepository;
import com.example.myblog.repository.ArticleTagRepository;
import com.example.myblog.repository.BoardRepository;
import com.example.myblog.repository.CommentRepository;
import com.example.myblog.repository.MyUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminService {

    private final MyUserRepository userRepository;
    private final ArticleInfoRepository articleRepository;
    private final ArticleTagRepository tagRepository;
    private final CommentRepository commentRepository;
    private final AlbumInfoRepository albumRepository;
    private final BoardRepository boardRepository;
    private final UserService userService;

    public AdminService(MyUserRepository userRepository,
                        ArticleInfoRepository articleRepository,
                        ArticleTagRepository tagRepository,
                        CommentRepository commentRepository,
                        AlbumInfoRepository albumRepository,
                        BoardRepository boardRepository,
                        UserService userService) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.commentRepository = commentRepository;
        this.albumRepository = albumRepository;
        this.boardRepository = boardRepository;
        this.userService = userService;
    }

    public long articleCount(Integer userId) {
        return articleRepository.countByAuthorId(userId);
    }

    public long albumCount(Integer userId) {
        return albumRepository.countByUserId(userId);
    }

    public long tagCount(Integer userId) {
        return tagRepository.countByUserId(userId);
    }

    public long commentCount(Integer userId) {
        return commentRepository.countByArticleAuthorId(userId);
    }

    public long boardCount(Integer userId) {
        return boardRepository.countByUserId(userId);
    }

    public List<ArticleInfo> articles(Integer userId) {
        return articleRepository.findByAuthorIdOrderByCreatedDesc(userId);
    }

    public List<ArticleTag> tags(Integer userId) {
        return tagRepository.findByUserIdOrderByIdAsc(userId);
    }

    public List<Comment> comments(Integer userId) {
        return commentRepository.findByArticleAuthorIdOrderByCreatedDesc(userId);
    }

    public List<AlbumInfo> albums(Integer userId) {
        return albumRepository.findByUserIdOrderByIdAsc(userId);
    }

    public List<Board> boards(Integer userId) {
        return boardRepository.findByUserIdOrderByCreatedDesc(userId);
    }

    public ArticleInfo article(Integer userId, Integer articleId) {
        ArticleInfo article = articleRepository.findDetailById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在"));
        requireOwner(userId, article.getAuthor().getId());
        return article;
    }

    public ArticleTag tag(Integer userId, Integer tagId) {
        return tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));
    }

    public Comment comment(Integer userId, Integer commentId) {
        return commentRepository.findByIdAndArticleAuthorId(commentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("评论不存在"));
    }

    public AlbumInfo album(Integer userId, Integer albumId) {
        return albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new IllegalArgumentException("相册图片不存在"));
    }

    public Board board(Integer userId, Integer boardId) {
        return boardRepository.findByIdAndUserId(boardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("留言不存在"));
    }

    @Transactional
    public MyUser updateProfile(Integer userId,
                                String username,
                                String name,
                                String email,
                                String introduce,
                                String company,
                                String profession,
                                String address,
                                String telephone,
                                String wx,
                                String qq,
                                String wb,
                                String photo,
                                String newPassword) {
        MyUser user = userService.getRequired(userId);
        String newUsername = text(username, user.getUsername());
        userRepository.findByUsername(newUsername)
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("用户名已存在");
                });
        user.setUsername(newUsername);
        user.setName(text(name, "匿名用户"));
        user.setEmail(text(email, ""));
        user.setIntroduce(text(introduce, "暂无介绍"));
        user.setCompany(text(company, "暂无信息"));
        user.setProfession(text(profession, "暂无信息"));
        user.setAddress(text(address, "暂无信息"));
        user.setTelephone(text(telephone, "暂无信息"));
        user.setWx(text(wx, "暂无信息"));
        user.setQq(text(qq, "暂无信息"));
        user.setWb(text(wb, "暂无信息"));
        if (photo != null && !photo.isBlank()) {
            user.setPhoto(photo);
        }
        return userService.saveProfile(user, newPassword);
    }

    @Transactional
    public ArticleInfo saveArticle(Integer userId,
                                   Integer articleId,
                                   String title,
                                   String content,
                                   int reading,
                                   int liking,
                                   String articlePhoto,
                                   List<Integer> tagIds) {
        MyUser user = userService.getRequired(userId);
        ArticleInfo article = articleId == null ? new ArticleInfo() : article(userId, articleId);
        article.setAuthor(user);
        article.setTitle(text(title, "未命名文章"));
        article.setContent(text(content, ""));
        article.setReading(Math.max(0, reading));
        article.setLiking(Math.max(0, liking));
        if (articlePhoto != null && !articlePhoto.isBlank()) {
            article.setArticlePhoto(articlePhoto);
        }

        Set<ArticleTag> selectedTags = new LinkedHashSet<>();
        if (tagIds != null) {
            for (ArticleTag tag : tagRepository.findAllById(tagIds)) {
                if (tag.getUser().getId().equals(userId)) {
                    selectedTags.add(tag);
                }
            }
        }
        article.setTags(selectedTags);
        return articleRepository.save(article);
    }

    @Transactional
    public void deleteArticle(Integer userId, Integer articleId) {
        ArticleInfo article = article(userId, articleId);
        article.getTags().clear();
        articleRepository.saveAndFlush(article);
        commentRepository.deleteByArticleId(articleId);
        articleRepository.delete(article);
    }

    @Transactional
    public ArticleTag saveTag(Integer userId, Integer tagId, String value) {
        ArticleTag tag = tagId == null ? new ArticleTag() : tag(userId, tagId);
        tag.setUser(userService.getRequired(userId));
        tag.setTag(text(value, "未命名标签"));
        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Integer userId, Integer tagId) {
        ArticleTag target = tag(userId, tagId);
        for (ArticleInfo item : articleRepository.findByAuthorIdOrderByCreatedDesc(userId)) {
            ArticleInfo article = article(userId, item.getId());
            if (article.getTags().removeIf(tag -> tag.getId().equals(tagId))) {
                articleRepository.save(article);
            }
        }
        tagRepository.delete(target);
    }

    @Transactional
    public Comment saveComment(Integer userId,
                               Integer commentId,
                               Integer articleId,
                               String commentator,
                               String content) {
        ArticleInfo article = article(userId, articleId);
        Comment comment = commentId == null ? new Comment() : comment(userId, commentId);
        comment.setArticle(article);
        comment.setCommentator(text(commentator, "匿名用户"));
        comment.setContent(text(content, ""));
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Integer userId, Integer commentId) {
        commentRepository.delete(comment(userId, commentId));
    }

    @Transactional
    public AlbumInfo saveAlbum(Integer userId,
                               Integer albumId,
                               String title,
                               String introduce,
                               String photo) {
        AlbumInfo album = albumId == null ? new AlbumInfo() : album(userId, albumId);
        album.setUser(userService.getRequired(userId));
        album.setTitle(text(title, ""));
        album.setIntroduce(text(introduce, ""));
        if (photo != null && !photo.isBlank()) {
            album.setPhoto(photo);
        }
        if (album.getPhoto().isBlank()) {
            throw new IllegalArgumentException("请选择图片");
        }
        return albumRepository.save(album);
    }

    @Transactional
    public void deleteAlbum(Integer userId, Integer albumId) {
        albumRepository.delete(album(userId, albumId));
    }

    @Transactional
    public Board saveBoard(Integer userId,
                           Integer boardId,
                           String name,
                           String email,
                           String content) {
        Board board = boardId == null ? new Board() : board(userId, boardId);
        board.setUser(userService.getRequired(userId));
        board.setName(text(name, "匿名用户"));
        board.setEmail(text(email, ""));
        board.setContent(text(content, ""));
        return boardRepository.save(board);
    }

    @Transactional
    public void deleteBoard(Integer userId, Integer boardId) {
        boardRepository.delete(board(userId, boardId));
    }

    private void requireOwner(Integer expectedUserId, Integer actualUserId) {
        if (!expectedUserId.equals(actualUserId)) {
            throw new IllegalArgumentException("无权访问其他用户的数据");
        }
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
