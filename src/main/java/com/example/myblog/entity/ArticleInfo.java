package com.example.myblog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "article_articleinfo")
public class ArticleInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private MyUser author;

    @Column(nullable = false, length = 200)
    private String title = "";

    @Lob
    @Column(nullable = false)
    private String content = "";

    @Column(name = "articlephoto", nullable = false, length = 100)
    private String articlePhoto = "";

    @Column(nullable = false)
    private int reading;

    @Column(nullable = false)
    private int liking;

    @Column(nullable = false)
    private LocalDateTime created;

    @Column(nullable = false)
    private LocalDateTime updated;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "article_articleinfo_article_tag",
            joinColumns = @JoinColumn(name = "articleinfo_id"),
            inverseJoinColumns = @JoinColumn(name = "articletag_id"))
    private Set<ArticleTag> tags = new LinkedHashSet<>();

    @PrePersist
    public void beforeCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (created == null) {
            created = now;
        }
        updated = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updated = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MyUser getAuthor() {
        return author;
    }

    public void setAuthor(MyUser author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getArticlePhoto() {
        return articlePhoto;
    }

    public void setArticlePhoto(String articlePhoto) {
        this.articlePhoto = articlePhoto;
    }

    public int getReading() {
        return reading;
    }

    public void setReading(int reading) {
        this.reading = reading;
    }

    public int getLiking() {
        return liking;
    }

    public void setLiking(int liking) {
        this.liking = liking;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public Set<ArticleTag> getTags() {
        return tags;
    }

    public void setTags(Set<ArticleTag> tags) {
        this.tags = tags;
    }
}
