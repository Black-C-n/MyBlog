# myblog Java 版

这是原 Django `myblog` 的 Spring Boot 等价实现。前台页面、中文内容、CSS、JavaScript、图片、URL、分页、阅读量、评论、相册、留言和个人资料均按原项目迁移；Django Admin 则用基础 Spring MVC + Thymeleaf 自建为相同风格的管理后台。

> 初次学习建议直接双击打开 [docs/学习中心.html](docs/学习中心.html)。这是完全离线的交互式教程，包含源码导读、请求流程、IDEA 调试、常见错误、练习和面试讲解稿。

## 技术栈

- JDK 17
- Spring Boot 4.1.0
- Spring MVC
- Thymeleaf
- Spring Data JPA / Hibernate
- Bean Validation
- MySQL 8
- Maven Wrapper

项目没有使用 Spring Security、Lombok、MyBatis-Plus、微服务或前后端分离，目的是让初学者能看到完整的 Java 对象、方法调用和 MVC 请求流程。

## 用 IDEA 启动

1. 在 IDEA 中打开本目录，也就是包含 `pom.xml` 的 `myblog-Java/myblog-Java`。
2. 打开 `File → Project Structure → Project`，把 Project SDK 设为 JDK 17，Language level 设为 17。
3. 等待 IDEA 完成 Maven 依赖下载。
4. 确保 MySQL 已启动，账号和密码都是 `root`。
5. 如果本机已有原来的 `blogdb`，不要重复导入；可以直接进行第 7 步。
6. 如果没有数据库：
   - 先执行 `database/create_database.sql`；
   - 再导入 `database/blogdb.sql`。
7. 运行 `src/main/java/com/example/myblog/MyBlogApplication.java`。
8. 浏览器打开 `http://localhost:8080`。

原博客中的头像、文章图和相册图已经放在 `src/main/resources/static/media`，会随项目一起编译，不再读取外层 Python 项目的 `myblog/media`。
后台新上传的图片默认保存在本项目的 `storage/media`。请让 IDEA 使用默认的模块工作目录，也就是包含本文件和 `pom.xml` 的目录。

如果不知道旧账号的明文密码，可先打开 `/user/register.html` 注册新账号。为了保持原 Django 行为，新注册用户可直接进入自己的后台。

## 数据库配置

默认开发配置已经是：

```properties
spring.datasource.username=root
spring.datasource.password=root
```

实际属性使用“环境变量优先、root/root 作为默认值”的写法：

- `DB_URL`：覆盖 JDBC 地址；
- `DB_USERNAME`：覆盖数据库账号；
- `DB_PASSWORD`：覆盖数据库密码；
- `MYBLOG_UPLOAD_DIR`：覆盖上传文件目录。

默认上传目录是 `${user.dir}/storage/media`。`user.dir` 是程序启动时的工作目录；在 IDEA 的默认 Spring Boot 运行配置中就是当前 Maven 模块目录。只有部署到其他机器并且希望把上传文件放到别处时，才需要设置 `MYBLOG_UPLOAD_DIR`。

这既能让本机开箱即用，也能避免部署时把真实密码写死在代码里。

## 常用 URL

| 功能 | URL |
|---|---|
| 首页 | `/` |
| 登录 | `/user/login.html` |
| 注册 | `/user/register.html` |
| 文章列表 | `/{用户id}/{页码}.html` |
| 文章详情 | `/detail/{用户id}/{文章id}.html` |
| 相册 | `/album/{用户id}/{页码}.html` |
| 关于我 | `/user/about/{用户id}.html` |
| 留言板 | `/board/{用户id}/{页码}.html` |
| 后台 | `/admin/` |
| 退出 | `/user/logout.html` |

## 编译和测试

Windows：

```powershell
.\mvnw.cmd test
```

打包：

```powershell
.\mvnw.cmd clean package
```

测试使用 H2 内存数据库，不会修改本机 MySQL。真实 MySQL 的端到端测试已验证登录、页面、静态资源以及后台新增、查询、修改、删除。

## 推荐阅读顺序

1. `MyBlogApplication.java`：程序入口。
2. `controller/AccountController.java`：最容易理解的登录注册流程。
3. `service/UserService.java`：业务规则和密码校验。
4. `repository/MyUserRepository.java`：数据库访问接口。
5. `entity/MyUser.java`：Java 对象如何映射数据库表。
6. `templates/user.html`：Controller 传入的数据如何显示为 HTML。
7. 再阅读文章、相册、留言和后台模块。

进一步学习请看 [docs/01-项目导读.md](docs/01-项目导读.md)。
