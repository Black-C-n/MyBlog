# Boss 04：夹带空格的关键词——业务规则实录

> 关卡主题：让标题搜索忽略关键词首尾空格

> 关卡分支：`boss/04-whitespace-search`

## 为什么选择这一关

Boss 03 第一次把标题搜索接通了：

```text
HTML
→ Controller
→ Service
→ Repository
→ 数据库
→ Page
→ Model
→ HTML
```

功能已经能用，但还有一个很普通的使用场景没有处理：

```text
用户本来想搜索 Django，
却不小心在关键词后面多输入了两个空格。
```

对于用户来说，`django` 和 `django  ` 表达的是同一个搜索意图。

旧实现却把空格也当成标题的一部分，因此会出现：

```text
搜索 django
→ 能找到“Django项目实战”

搜索 django 加两个尾随空格
→ 找不到文章
```

这次没有预埋参数顺序或键名错误。问题来自一条尚未实现的真实业务规则：

> 搜索标题前，应该忽略关键词首尾无意义的空格。

## 一、先看页面，不急着看代码

第一步只做两次搜索：

```text
django
django␠␠
```

学习者观察到：

> 两次结果是不一样的，并且地址栏也发生了变化。keyword 变成了 django++，而不是我预测的“django  ”。

这个预测不是完全错误。

学习者已经预料到“两个空格会跟着关键词进入请求”，只是还不知道浏览器会先把空格换成适合 URL 运输的写法。

GET 表单提交查询参数时，空格通常会显示成 `+`：

```text
输入框：django␠␠
地址栏：keyword=django++
```

这里的 `++` 并不一定表示 Java 收到了两个加号。

它更像包裹上的运输标签：浏览器用 `+` 搬运空格，服务器收到后还会解码。

## 二、用 Controller 断点确认 Java 真正收到什么

在 `ArticleController.articleList` 中观察断点变量，实际看到：

```text
keyword = "django  "
```

这证明当前数据经过了：

```text
输入框中的两个空格
→ 地址栏编码成 ++
→ Spring MVC 解码
→ @RequestParam 得到两个真实空格
```

学习者同时观察到浏览器没有立刻更新页面，并解释：

> 这次没有跳转，是因为数据只是被放进了 Model 里面，还没有渲染到网页上。

这个方向基本正确，但还需要补充一个更直接的原因：

```text
请求命中断点
→ 处理请求的服务端线程暂停
→ 浏览器一直等待服务器返回响应
```

而且断点停在某一行时，那一行通常还没有执行。

如果断点停在：

```java
model.addAttribute("keyword", keyword);
```

那么关键词此时可能还没真正放进 Model。

只有继续运行后，程序才会完成：

```text
写入 Model
→ 调用 Service
→ Repository 查询
→ pageInfo 写入 Model
→ Controller 返回 article
→ Thymeleaf 渲染 HTML
→ 浏览器收到响应
```

## 三、空格进入 Service 后没有自动消失

下一步在 `ArticleService.findPage` 中继续观察同一个请求。

变量区仍然显示：

```text
keyword = "django  "
```

学习者对此判断：

> 对的，还是带着两个空格。这是很正常的现象。这一步的下一步 `Page<ArticleInfo> page = articleRepository.findByAuthorIdAndTitleContainingIgnoreCaseOrderByCreatedDesc` 是按照规则找对应的文章，所以这个才是关键。

这个判断抓住了查询真正落到数据库的边界。

Repository 方法可以拆成：

```text
findBy
AuthorId
And
Title
Containing
IgnoreCase
OrderByCreatedDesc
```

人话是：

```text
查这个作者的文章，
标题中要包含给定关键词，
英文大小写不敏感，
最后按创建时间倒序。
```

`IgnoreCase` 只会忽略 `Django` 和 `django` 的大小写差异，不会忽略空格。

因此旧查询实际要求的是：

```text
标题包含“django␠␠”
```

“Django项目实战”包含 `django`，却不包含紧跟两个空格的 `django␠␠`，结果自然为空。

这里还要区分“关键位置”和“错误位置”：

- Repository 调用是空格真正影响查询结果的关键位置；
- Repository 本身没有查错，它只是忠实使用收到的条件；
- 缺少的是查询前整理关键词的业务规则。

## 四、规则应该放在 Controller 还是 Service

接下来没有立即写 `trim()`，而是先讨论这条规则应该归谁管理。

两个候选位置是：

```text
Controller：网页刚收到关键词时整理
Service：调用 Repository 前整理
```

如果只在 Controller 处理，当前网页当然能够正常搜索。

但是又提出了一个假设：

```java
articleService.findPage(id, page, "django  ");
```

如果将来测试、后台功能或另一个 Controller 直接这样调用 Service，绕过原来的 Controller，它还能得到保护吗？

学习者回答：

> 得不到保护，因为去掉空格的部分在 Controller 层。如果绕过这一层，就会直接原封不动地查找关键词。

> 所以我觉得要不在 Controller 层就做规范，要不在 Service 层 Repository 方法之前做规范。

这个回答已经开始根据“规则要保护哪些调用者”判断分层，而不是只看哪一行最方便修改。

最终选择 Service 作为后端查询规则的保护位置：

```text
无论谁调用文章搜索，
Service 都保证交给 Repository 的关键词已经去掉首尾空格。
```

## 五、Service 改了关键词，Controller 会不会一起变化

Controller 中原本有两条方向：

```java
model.addAttribute("keyword", keyword);
Page<ArticleInfo> pageInfo = articleService.findPage(id, page, keyword);
```

第一条把关键词交给页面，第二条把关键词交给后端查询。

于是继续追问：如果 Service 内部把自己的 `keyword` 整理成 `django`，Controller 放进 Model 的关键词会不会自动变成 `django`？

学习者回答：

> 不会，因为没有任何一个方法说会传一个 keyword 到 Controller 层来。

这个人话解释完全成立。

数据只从 Controller 传进 Service；Service 没有返回整理后的关键词，所以整理结果不会凭空倒流回 Controller。

从 Java 语法上补充原因：

- Service 方法拿到的是自己的参数变量；
- `String` 不能在原对象上直接修改；
- `trim()` 会产生一份新的字符串；
- Service 使用新字符串，不会自动替换 Controller 原来的变量。

这再次对应 Boss 03 的关键突破：

```text
给 HTML 用的 keyword
和
给后端查询用的 keyword
可以来自同一次输入，
但承担不同任务，也不一定保持完全相同的形式。
```

## 六、实际修改

在 `ArticleService.findPage` 中增加一份整理后的关键词：

```java
String normalizedKeyword = keyword.trim();
```

这里没有覆盖原始 `keyword`，而是明确创建：

```text
原始关键词：keyword
查询关键词：normalizedKeyword
```

随后正常查询与页码越界后的备用查询都改用 `normalizedKeyword`。

之所以两处都要修改，是因为它们是两条可能到达数据库的路径：

```text
正常页码
→ 执行第一次 Repository 查询

越界页码
→ 第一次查询发现越界
→ 执行第二次 Repository 备用查询
```

如果只修改第一处，越界后的备用查询仍可能重新使用带空格关键词。

## 七、修改后的页面现象

再次搜索带两个尾随空格的 `django  ` 后，文章能够正常出现。

后端的数据变成：

```text
Controller 收到：django␠␠
Service 原始参数：django␠␠
Service 查询参数：django
Repository 使用：django
```

但地址栏仍然显示：

```text
keyword=django++
```

学习者解释：

> 还是 django++，说明只是在 Service 层做了改变，并没有传回 Controller 层。

这正好用运行现象验证了前面的推理。

本关解决的是“数据库查询忽略首尾空格”，不是“把浏览器当前 URL 重写成干净地址”。

若以后要让当前地址栏也变成 `keyword=django`，还需要 Controller 重定向或前端提交前整理等另一条页面规则，不能假装 Service 的局部修改已经完成了这件事。

## 八、自动测试保护什么

只靠手动搜索一次，不能保证以后重构时规则不会丢失。

因此新增一个不启动 Spring、也不连接数据库的 Service 单元测试。

测试使用 Mockito 创建假的 Repository，并安排：

```text
第一次查询返回“请求页码越界”的 Page
→ Service 必须进行第二次备用查询
```

随后验证两次 Repository 调用收到的关键词都是：

```text
Django
```

而不是：

```text
␠␠Django␠␠
```

这条测试同时保护：

- 正常查询会整理关键词；
- 页码越界后的备用查询也会整理关键词；
- Service 返回的是第二次查询得到的最后一页。

目标测试结果：

```text
Tests run: 1
Failures: 0
Errors: 0
BUILD SUCCESS
```

完整测试结果：

```text
Tests run: 9
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

测试使用 Mockito 与 H2 内存数据库，没有修改本机 MySQL。

## 九、这次推理中成立的部分

### 1. 预测空格会跟着关键词进入请求

虽然一开始不知道地址栏会编码成 `++`，但对数据内容变化的预测成立。

### 2. 用断点区分地址栏格式与 Java 字符串

变量区证明 Controller 收到的不是两个加号，而是两个真实空格。

### 3. 找到 Repository 查询这个关键边界

学习者意识到只有关键词进入 Repository 查询后，才会真正改变文章结果。

### 4. 根据保护范围选择 Service

不是因为“Service 看起来更高级”，而是因为绕过 Controller 的其他调用者仍应遵守相同搜索规则。

### 5. 判断整理结果不会自动返回 Controller

学习者用“没有任何方法把 keyword 传回来”解释了为什么地址栏和 Model 不会一起变干净。

## 十、仍然没有完全掌握的内容

这次能够定位并修复规则，不代表已经完全掌握：

- URL 查询参数的全部编码规则；
- Java 参数传递的所有细节；
- `String.trim()` 与其他空白处理方法的区别；
- Mockito 的完整 API；
- `Page`、`Pageable` 和越界分页的全部用法；
- 如何让浏览器当前 URL 自动变成规范地址。

这些内容可以在后续真实需求出现时继续学习，不需要一次背完。

## 通关结论

Boss 04 增加了一条很小但完整的业务规则：

```text
标题搜索忽略关键词首尾空格
```

它的真实流程是：

```text
浏览器提交带空格关键词
→ URL 把空格显示为 +
→ Spring 解码回空格
→ Controller 接收原始关键词
→ Service 生成整理后的查询关键词
→ Repository 使用干净关键词查询
→ Page 返回 Controller
→ Thymeleaf 渲染搜索结果
```

这次学习重点不是 `trim()` 这一行本身，而是三个判断：

1. 地址栏中的运输格式不等于 Java 中的真实值；
2. Repository 忠实查询不代表输入已经符合业务规则；
3. 规则放在哪一层，取决于希望保护哪些调用者。

代码已经能够运行并通过测试，但仍然需要在以后继续复述和使用，不能因为这次改对了就标记为已经完全掌握 Spring、JPA 或测试。
