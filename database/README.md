# 数据库初始化说明

## 已有 blogdb

如果原 Django 项目已经能使用 `blogdb`，Java 版直接复用，不需要再次导入。

## 全新 MySQL

按顺序执行：

1. `create_database.sql`：创建 UTF-8 数据库；
2. `blogdb.sql`：导入原 Django 表结构和示例内容。

`blogdb.sql` 含 `DROP TABLE`，会重建同名表，所以不要在有重要新数据的数据库上重复导入。建议导入前先备份。

Java 版直接映射以下业务表：

- `account_myuser`
- `article_articletag`
- `article_articleinfo`
- `article_articleinfo_article_tag`
- `article_comment`
- `album_albuminfo`
- `interflow_board`

Django 的 `auth_*`、`django_*` 表仍保留在导出文件中，但 Java 版不依赖它们。
