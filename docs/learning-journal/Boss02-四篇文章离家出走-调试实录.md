# Boss 02：四篇文章离家出走——完整调试实录

> 关卡主题：文章列表、URL 路径变量、Model、Controller、Service、Repository、分页
>
> 关卡分支：`boss/02-missing-articles`
>
> 故障起点：`a315841 boss: introduce missing articles challenge`

## 为什么保留这份记录

Boss 02 的表面现象是：

- 用户页面可以正常打开；
- 页面没有 404 或 500；
- 用户资料和侧栏仍然存在；
- 数据库中的 4 篇文章没有被删除；
- 文章列表却空了。

这份记录不只保存最终那一行修复，还保留学习者寻找入口、误读代码、自我纠正、类比旧知识、形成猜想和质疑学习方式的全过程。

后者尤其重要：如果一项练习只能证明“能找到一行错误”，却没有帮助学习者建立项目模型，那么即使修复成功，教学设计也仍然需要修改。

## 一、从业务名称寻找入口

学习者最初没有从 Spring MVC 原理或 JPA 查询语法开始，而是从页面现象出发：

> 这个文章应该和 article 有关，我应该先去找 controller 层命名里面有 article 的文件。

由此找到了：

```text
ArticleController
```

这是一种适合陌生项目的阅读策略：

1. 先确定当前操作属于哪个业务模块；
2. 再根据模块名寻找 Controller；
3. 通过请求地址和映射注解定位具体方法；
4. 最后沿方法调用继续向下追踪。

它不要求先记住所有框架知识，而是利用项目命名缩小范围。

## 二、开始利用映射注解定位请求

学习者观察到：

```java
@GetMapping("/{id}/{page}.html")
```

并判断它和当前文章页面有关：

> 这次我知道了，我应该先重点看 `@` 注解，然后快速定位到我想要的东西。

这个方向是正确的。

`@GetMapping` 在这里声明的是：

```text
当浏览器发送 GET /{id}/{page}.html 时，
由下面的 articleList 方法处理请求。
```

它本身不负责“拿到所有文章”。真正的用户判断、公共数据准备和文章查询，都发生在方法体中。

## 三、第一次误读：把用户不存在看成文章不存在

接下来看到：

```java
if (userService.findById(id).isEmpty()) {
    return "redirect:/user/register.html";
}
```

学习者最初判断：

> 如果没有这篇文章，就返回到注册页。

紧接着又根据调用对象完成自我纠正：

> 哦哦哦不对，应该是找不到这个用户就先跳到注册页先注册。因为主体是 `userService`。

纠正后的理解更接近代码实际含义：

1. URL 中的第一个数字是用户 id；
2. Controller 先通过 `userService.findById(id)` 确认这个博客用户存在；
3. 如果用户不存在，当前项目选择重定向到注册页；
4. 这里还没有开始查询文章。

这次纠正很有价值，因为它不是依赖背诵，而是通过 `userService` 这个调用对象推断代码正在处理“用户”。

## 四、用运行现象排除用户不存在分支

学习者继续判断：

> 到这里应该是没有问题的，用户已经进去了，说明用户存在。那就接着往下看。

这是一次有效的证据排除：

- 如果用户不存在，方法会直接返回重定向；
- 实际页面成功打开，并展示了该用户的信息；
- 因此用户存在判断不是文章消失的原因。

调试并不总是立即找到答案。能够用运行现象排除一个分支，同样是在推进。

## 五、第二次误读：把公共页面数据当作文章数据

下一行是：

```java
blogViewService.addCommonData(model, id);
```

学习者最初推测：

> 这里表示把对应 id 的文章放到网页视图里面。网页里面的文章消失了，到这里应该是有问题的。应该是文章没加进去。

这个猜想没有成立。

继续查看 `BlogViewService.addCommonData`，可以看到它准备的是多个页面都需要的公共数据：

```java
MyUser user = userService.getRequired(userId);
model.addAttribute("id", userId);
model.addAttribute("user", user);
model.addAttribute("album", albumRepository.findByUserIdOrderByIdAsc(userId));
model.addAttribute("tags", tagRepository.findByUserIdOrderByIdAsc(userId));
```

这里加入 Model 的是：

- 当前博客的用户 id；
- 用户资料；
- 相册；
- 标签。

它不负责文章分页。

这也解释了故障页面为什么会出现一种局部不一致：

```text
公共数据支路使用了正确的用户 id
→ 用户资料、相册和标签正常

文章分页支路收到错误含义的参数
→ 文章列表为空
```

## 六、用 Boss 01 的知识类比 Model

学习者随后产生了一个很自然的类比：

> 这个 model 是不是相当于 Boss 01 的 Session？就是把这个 id 存到 model 这个盒子里面，就是先存进去，先不操作。

这个类比有一半成立：

- Model 和 Session 都可以用“键 → 值”的方式保存对象；
- 模板或其他代码可以通过名字取得对象。

但二者的使用范围不同：

| 容器 | 主要用途 | 保存多久 |
|---|---|---|
| Model | 把当前请求的数据交给当前模板 | 通常只服务这一次页面渲染 |
| Session | 在同一浏览器的多个请求之间保存状态 | 可以跨多个请求，直到过期或被清除 |

在本方法中：

```java
model.addAttribute("pageInfo", pageInfo);
```

相当于告诉模板：

```text
这次渲染 article 页面时，
名字为 pageInfo 的数据就是这一页文章。
```

刷新或访问另一个地址会产生新请求，也会重新创建和填充新的 Model。

## 七、锁定真正产生文章列表的数据

学习者继续分析：

```java
Page<ArticleInfo> pageInfo = articleService.findPage(page, id);
model.addAttribute("pageInfo", pageInfo);
return "article";
```

当时的理解是：

> 应该就是在数据库里面找这个 id 的文章。

> 如果找到了，就把 `pageInfo` 加到 `model` 里面。

> 最后网页返回文章。

其中第二步已经抓住了真实的数据流：

```text
ArticleService 返回 Page<ArticleInfo>
→ Controller 命名为 pageInfo
→ 放入 Model
→ 返回 article 模板
→ 模板遍历 pageInfo.content
```

需要校准的是最后一句：

```java
return "article";
```

这里返回的 `"article"` 是模板名，不是直接返回文章对象。

Spring MVC 随后会使用：

```text
src/main/resources/templates/article.html
```

渲染完整 HTML。

## 八、在还没完全看懂时形成正确猜想

即使仍不清楚每个组件的具体职责，学习者已经提出：

> 大致判断思路应该还是和第一题的差不多，就是网页存的和数据库的不一样，应该还是参数不一致的问题。

这个猜想抓住了本关的核心方向：传递到文章查询链中的数字含义发生了错位。

但 Boss 01 和 Boss 02 的具体机制并不相同：

| 关卡 | 发生错位的内容 | 影响范围 |
|---|---|---|
| Boss 01 | Session 写入键和读取键 | 多个 HTTP 请求之间的登录状态 |
| Boss 02 | 方法实参与形参的业务含义 | 同一次请求中的文章分页查询 |

二者都可以概括为“组件之间的约定不一致”，但涉及的项目机制分别是 Session 和方法调用。

## 九、对通关问题的原始回答

### 问题 1：地址中的数字表示什么

学习者回答：

> 那个阿拉伯数字表示当前用户在数据库里面拿到的那个 id。

这个回答识别了 `/3/1.html` 中的第一个数字：

```text
3 → 当前博客用户的 id
```

还缺少第二个数字：

```text
1 → 用户看到的第 1 页
```

因此整个地址表示：

```text
查看用户 3 的第 1 页文章
```

### 问题 2：为什么资料正常但文章为空

学习者回答：

> 我猜应该是文章的某个方法出错了，只是那个方法出错了，所以还罪不至此。

“罪不至此”准确描述了局部故障：

- 页面不是通过一次查询取得所有内容；
- 公共资料和文章分页是两次不同的 Service 调用；
- 一条支路正确，不代表另一条支路也正确；
- 因此页面可以只坏一部分。

### 问题 3：Controller、Service、Repository 做什么

学习者回答：

> C 层先拿到网页端的数据，S 层做一系列巴拉巴拉的判断（我也不太清楚，应该是比对 C 层和 R 层的数据对不对的上吧？），R 层负责去数据库拿东西。

其中：

- Controller 接收请求数据：方向正确；
- Repository 负责数据库访问：方向正确；
- Service 负责“比较两层是否对得上”：不准确。

在本条文章链中：

```text
Controller
接收 URL 中的用户 id 和页码，协调公共数据与文章分页，将结果放入 Model。

Service
把用户看到的第 1 页转换成 Spring Data 使用的第 0 页，
设置每页数量与排序规则，并处理页码越界。

Repository
按 author_id 查询文章，按照创建时间倒序，并使用 Pageable 分页。
```

### 问题 4：为什么错误能通过编译

学习者回答：

> 语法正确嘛（？）

这个回答已经碰到了关键点。

更精确地说，Java 编译器主要检查类型是否匹配：

```java
findPage(Integer userId, int pageNumber)
```

Controller 中的 `id` 和 `page` 都是整数类型。把它们放反后，Java 仍然能够完成自动装箱与拆箱，所以类型检查通过。

编译器不知道：

```text
这个整数在业务上代表用户，
另一个整数在业务上代表页码。
```

语法正确不等于业务正确。

### 问题 5：为什么 8 个测试仍然通过

学习者回答：

> 不晓得，我也不晓得这 8 个测试数据都是啥，所以我也挺奇怪的。

当时项目中的 8 个测试覆盖：

- Django 密码工具：3 个；
- 媒体资源：3 个；
- 应用上下文启动：1 个；
- 登录 Session 跨请求：1 个。

没有测试执行：

```text
GET /3/1.html
```

也没有测试检查：

```text
Controller 是否把用户 id 和页码按正确顺序交给 ArticleService。
```

所以测试全绿只能说明已有测试所描述的行为没有失败，不能说明文章列表一定正确。

## 十、最终突破：查看 Service 方法签名

故障代码为：

```java
Page<ArticleInfo> pageInfo = articleService.findPage(page, id);
```

Service 方法声明为：

```java
public Page<ArticleInfo> findPage(Integer userId, int pageNumber)
```

将 `/3/1.html` 的真实值代入：

| Controller 变量 | 当前值 | 业务含义 | 实际传入的位置 |
|---|---:|---|---|
| `id` | 3 | 用户 id | 第二个参数 `pageNumber` |
| `page` | 1 | 用户看到的页码 | 第一个参数 `userId` |

因此 Service 实际理解为：

```text
查询用户 1 的第 3 页文章
```

正确含义应该是：

```text
查询用户 3 的第 1 页文章
```

Repository 最终按照错误的 `userId=1` 查询 `author_id`，自然得不到用户 3 的 4 篇文章。

## 十一、修复

修复时只恢复两个参数的业务顺序：

```java
Page<ArticleInfo> pageInfo = articleService.findPage(id, page);
```

它表达的是：

```text
第一个参数：查询谁的文章
第二个参数：查询第几页
```

没有修改数据库、模板、分页规则或用户数据。

## 十二、修复后的文章列表请求链

```text
浏览器 GET /3/1.html
        ↓
ArticleController.articleList
        ↓
得到 id=3、page=1
        ↓
userService.findById(3)
确认博客用户存在
        ↓
blogViewService.addCommonData(model, 3)
准备用户、相册和标签
        ↓
articleService.findPage(3, 1)
        ↓
把用户页码 1 转换为 PageRequest 页码 0
        ↓
ArticleInfoRepository
.findByAuthorIdOrderByCreatedDesc(3, pageable)
        ↓
数据库 article_articleinfo
按 author_id=3 查询并按创建时间倒序
        ↓
Page<ArticleInfo>
        ↓
model.addAttribute("pageInfo", pageInfo)
        ↓
article.html 遍历 pageInfo.content
        ↓
浏览器看到 4 篇文章
```

## 十三、本关接触到的项目知识

### 1. URL 可以同时携带多个业务数字

`/{id}/{page}.html` 中：

- `id` 决定查看哪个用户的博客；
- `page` 决定查看第几页。

变量都是整数，但业务含义不同。

### 2. 同一个页面可以由多条查询支路共同组成

用户资料、相册、标签与文章列表不是一次查询取得的。

一条支路失败时，页面其他部分仍可能正常。

### 3. Model 是当前页面的数据包

Controller 把本次渲染需要的数据放入 Model，模板根据属性名读取。

它不负责像 Session 一样跨多个请求保存登录状态。

### 4. Service 在本关负责分页业务

它不只是把 Repository 包装一层，而是负责：

- 把 1 基页码转换成 0 基页码；
- 限制最小页码；
- 定义每页 10 条；
- 定义创建时间倒序；
- 在页码超出范围时回到最后一页。

### 5. Repository 方法名描述查询条件

```java
findByAuthorIdOrderByCreatedDesc
```

可以拆成：

```text
find
By AuthorId
OrderBy Created Desc
```

也就是按作者 id 查询，再按创建时间倒序。

### 6. 同类型参数存在语义调换风险

Java 能检查整数类型，却不能自动证明每个整数的业务意义。

清楚的方法名、参数名、测试和较强的领域类型都可以降低这类风险。

## 十四、这次推理中成立与未成立的部分

### 成立的部分

- 根据 `article` 找到 `ArticleController`；
- 优先查看 `@GetMapping` 定位当前 URL；
- 根据 `userService` 自我纠正为用户存在判断；
- 通过页面已打开排除重定向分支；
- 找到真正产生 `pageInfo` 的 Service 调用；
- 判断页面数据与数据库查询参数可能不一致；
- 认识到错误能通过编译是因为语法和类型仍然合法；
- 最终核对方法签名并修复参数顺序。

### 未成立或不完整的部分

- `@GetMapping` 本身负责拿所有文章；
- `userService.findById` 在判断文章是否存在；
- `addCommonData` 会把文章加入页面；
- Model 与 Session 可以视为同一个机制；
- Service 的职责是比较 Controller 与 Repository 的数据；
- `/3/1.html` 只有一个数字需要解释；
- 测试全绿应当意味着所有业务都正确。

这些错误想法没有被删除，因为它们记录了理解是如何逐步形成的。

## 十五、比通关更重要的反馈

修复后，学习者提出：

> 我觉得现在只改一句代码，我根本就还是没太能理得清楚，感觉我是来修 Bug 了，而不是来学习来的。

并进一步指出：

> 而且 Boss 01 和 02 都高度相似，好没意思啊。

这个反馈成立。

Boss 01 与 Boss 02 虽然分别涉及 Session 和文章分页，但教学形式都是：

```text
预埋一处约定错配
→ 根据异常现象搜索
→ 找到两个不一致的值
→ 修改一行
```

这样的练习可以训练局部排错，却不足以帮助初学者：

- 理解一个功能为什么需要多个层；
- 预测新增需求会影响哪些文件；
- 参与业务规则设计；
- 把浏览器输入一直追到数据库查询；
- 再把数据库结果带回页面；
- 建立对项目整体结构的主动认识。

因此，不能因为故障被修复就认为学习目标已经完全达成。

## 十六、Boss 03 的学习方式调整

Boss 03 不再继续注入同类单行 Bug。

新的关卡形式改为实现一个真实的小功能：

```text
按标题搜索当前用户的文章
```

预计会分阶段经过：

```text
页面搜索框
    ↓
GET 请求参数
    ↓
ArticleController
    ↓
ArticleService 的空关键词与搜索规则
    ↓
ArticleInfoRepository 的作者 + 标题查询
    ↓
分页结果
    ↓
Model
    ↓
article.html 回显结果与关键词
    ↓
自动测试
```

学习者不会一次收到完整答案，而会先预测每一层是否需要修改、为什么需要修改，再逐层完成和验证。

成功标准不再是“修对一行”，而是能够回答：

1. 浏览器把关键词放在哪里？
2. Controller 怎样拿到关键词？
3. 为什么空关键词应该沿用原文章列表？
4. Service 在搜索规则中负责什么？
5. Repository 怎样同时限制作者和标题？
6. 搜索后翻页时为什么还要保留关键词？
7. 数据库结果怎样重新回到模板？
8. 哪条测试能够证明这个完整链路没有断？

## 十七、用自己的话复述 Boss 02

可以把本关概括为：

> 浏览器访问用户 3 的第 1 页文章。Controller 正确取得了 `id=3` 和 `page=1`，公共页面数据也使用 `id=3`，所以侧栏正常。但是调用文章 Service 时把两个整数的顺序放反了，Service 误以为要查询用户 1 的第 3 页。Repository 因而使用错误的作者 id 查询，返回空分页结果。Controller 又把这个空结果放进 Model，模板正常渲染，所以页面返回 200，却没有文章。将调用恢复为用户 id 在前、页码在后后，4 篇文章重新出现。

## 通关结论

Boss 02 已完成修复。

本关确认了：

- 可以从业务名和 URL 映射找到 Controller 入口；
- 可以利用调用对象判断代码正在处理哪种实体；
- 一个页面可以由多条 Service 查询支路共同组成；
- Model 与 Session 都能保存命名数据，但生命周期不同；
- 文章列表经过 Controller、Service、Repository、数据库，再通过 Model 回到模板；
- 编译通过和测试全绿都不等于所有业务正确。

同时，本关也暴露了学习方案本身的问题：连续重复单行约定错配，无法满足“真正理解项目”的目标。

因此 Boss 03 将从“修复预埋 Bug”转向“设计并实现真实功能”。
