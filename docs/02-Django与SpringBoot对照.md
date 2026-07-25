# 02 Django 与 Spring Boot 对照

| Django 原项目 | Spring Boot 新项目 | 作用 |
|---|---|---|
| `settings.py` | `application.properties`、`WebMvcConfig` | 数据库、静态资源、上传、应用配置 |
| 项目 `urls.py` | Controller 的 `@GetMapping/@PostMapping` | 总路由 |
| 应用 `urls.py` | 各业务 Controller | 模块路由 |
| `views.py` | Controller + Service | 请求处理和业务逻辑 |
| `models.py` | `entity` + `repository` | 表映射和数据访问 |
| Django Template | Thymeleaf | 服务端渲染 HTML |
| `request.session` | `HttpSession` | 会话状态 |
| `Paginator` | Spring Data `Page` / `PageRequest` | 分页 |
| `admin.py` | `AdminController` + Admin 模板 | 后台管理 |
| `authenticate/login` | `UserService` + `HttpSession` | 登录 |
| Django PBKDF2 | `DjangoPasswordUtil` | 兼容旧密码 |
| `ImageField` | `MultipartFile` + `FileStorageService` | 图片上传 |

## 路由对照示例

Django：

```python
path('detail/<int:id>/<int:aId>.html', detail, name='detail')
```

Spring MVC：

```java
@GetMapping("/detail/{id}/{articleId}.html")
public String detail(@PathVariable Integer id,
                     @PathVariable Integer articleId) {
    // ...
}
```

## 查询对照示例

Django：

```python
ArticleInfo.objects.filter(author_id=id).order_by('-created')
```

Spring Data JPA：

```java
Page<ArticleInfo> findByAuthorIdOrderByCreatedDesc(
        Integer authorId, Pageable pageable);
```

方法名被拆解为：

- `findBy`：查询；
- `AuthorId`：条件是 `author.id`；
- `OrderByCreatedDesc`：按 `created` 倒序。

## 模板对照示例

Django：

```html
{% for list in pageInfo.object_list %}
  <h3>{{ list.title }}</h3>
{% endfor %}
```

Thymeleaf：

```html
<li th:each="item : ${pageInfo.content}">
  <h3 th:text="${item.title}">文章标题</h3>
</li>
```

## 迁移中最关键的兼容点

1. 表名仍用 Django 表名，避免丢失原数据。
2. 多对多中间表及字段名保持原样。
3. 旧密码不是明文，需要兼容 Django PBKDF2 格式。
4. `open-in-view=false` 后，要在 Repository 中明确抓取模板需要的关联对象。
5. Django 静态文件和上传文件概念不同；Spring Boot 分别用 `static` 和 `/media/**` 资源映射处理。
