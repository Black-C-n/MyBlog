# Test 01：让模拟浏览器记住 Session——完整学习实录

> 日期：2026-07-26
>
> 项目：`myblog-Java`
>
> 学习分支：`boss/01-lost-login-state`
>
> 类型：Boss 01 回归测试

## 为什么会有这次实践

Boss 01 的故障是：

```text
登录 Controller 使用 "currentUser" 写入 Session
后台拦截器使用 "loginUser" 读取 Session
```

用户已经通过实际调试找到并修复了它。

但是故障存在时，项目原有 7 个自动化测试仍然全部通过。这说明原有测试没有保护下面这个跨组件业务事实：

```text
登录成功
→ MyUser 被保存到 Session
→ 同一个浏览器访问后台
→ 拦截器从 Session 取得用户
→ 后台 Controller 被放行
```

因此，本次实践的目标不是学习一句新的测试语法，而是为已经理解过的登录链路增加一名“自动门卫”。

如果以后有人再次把 Session 键名写错，测试应当立即失败，而不是等到人工点击后台时才发现。

---

## 一、最初的测试设计

新增测试：

```text
src/test/java/com/example/myblog/LoginSessionFlowTests.java
```

它选择使用 Spring MVC 的 `MockMvc`，模拟浏览器向 Controller 发送请求。

测试中的主要组件分为两类。

### 使用真实组件

```text
AccountController
WebMvcConfig
AdminLoginInterceptor
AdminController
```

这些组件正好组成 Boss 01 的状态传递链，不能全部替换成假的，否则测试不到真正的问题。

### 使用模拟组件

```text
UserService
BlogViewService
AdminService
FileStorageService
```

本关不需要验证数据库、Django 密码算法、文章计数或文件上传，所以这些 Service 使用模拟对象。

这样一旦测试失败，排查范围可以集中在：

```text
登录请求
→ Session
→ 后台拦截器
→ 后台 Controller
```

而不会扩散到 MySQL 和其他业务。

---

## 二、Spring Boot 4 测试依赖的第一个门槛

测试最初使用：

```java
@WebMvcTest
```

但第一次编译时报错：

```text
找不到 WebMvcTest
```

本机 Maven 缓存中虽然已经下载过相关 JAR，但这不表示当前项目已经依赖它。

项目使用 Spring Boot 4.1.0。MVC 测试支持位于独立模块，因此在 `pom.xml` 中加入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

`test` 作用域表示：

```text
只在编译和运行测试时使用
不会成为正式博客运行时的一部分
```

这次错误带来的项目知识是：

```text
电脑上存在某个依赖文件
不等于
当前 Maven 项目的依赖关系中包含它
```

---

## 三、测试运行环境的弯路

测试代码能够编译前后，还遇到过两类环境问题。

### IDEA 测试运行器问题

通过 IDEA 内置 JUnit 运行器执行时曾出现 `NoSuchMethodError`。

这类错误还没有进入登录业务代码，而是测试运行器与项目依赖之间的兼容问题。

因此改用项目自己的 Maven Wrapper：

```text
mvnw test
```

### Maven 使用了错误的 Java

终端当时实际使用 Java 8，而项目及测试依赖需要 Java 17，出现了 class file 版本不兼容：

```text
61 → Java 17 编译的字节码
52 → Java 8 运行时支持的最高字节码
```

并且当时输出：

```text
Tests run: 0
```

这意味着测试方法根本还没有开始执行，不能把它判断成登录业务测试失败。

将当前用户的 Java 环境切换到 Java 17 后，Maven 成功启动 Spring 测试环境，并真正运行了 1 条测试。

这个弯路帮助区分：

```text
编译/运行环境错误
与
业务断言失败
```

只有当输出已经出现：

```text
Tests run: 1
```

并指出具体状态码断言不一致时，才真正进入了本关的业务问题。

公开实录不记录本机具体 JDK 安装路径，只保留 Java 版本判断方法。

---

## 四、为什么真实网页正常，测试却失败

第一次看到题目时，用户运行的是：

```text
MyBlogApplication.main()
```

并在真实浏览器中登录、点击后台管理。

实际后台可以正常打开，页面上也没有直接显示 200 或 302，因此产生了疑问：

> 我现在运行的是 main 里面的程序，并没有看到 200 或者 302。
>
> 我点到后台管理也是可以正常打开的。
>
> 没看懂这个题目。

这里混在一起的是三个不同概念：

```text
MyBlogApplication.main()
→ 启动真实博客

Git main 分支
→ 一条版本历史

LoginSessionFlowTests
→ 独立运行的自动化测试
```

真实博客已经修复，所以浏览器能进入后台是正确现象。

测试实验室则故意让第二次模拟请求暂时不携带第一次的 Session，所以测试会失败。

真实浏览器通常会自动保存并发送 Session 对应的 Cookie；MockMvc 的两个 `perform(...)` 不会自动假定它们来自同一个浏览器。

---

## 五、200 和 302 为什么平时看不到

HTTP 状态码是服务器响应的一部分。

```text
200 OK
→ 服务器直接成功返回当前页面

302 Found
→ 服务器要求客户端再访问另一个地址
```

浏览器通常会自动跟随 302，所以用户最终看到的是跳转后的页面，而不是一个写着“302”的页面。

例如登录成功：

```text
POST /user/login.html
→ 302 Location: /42/1.html
→ 浏览器再请求 /42/1.html
→ 用户看到个人文章页
```

因此：

```text
302 本身不一定表示失败
必须结合这个请求原本应该做什么来判断
```

登录 POST 按项目设计就应该重定向，所以第一次 302 是成功。

后台 GET 按项目设计应该直接返回后台页面，所以它应该得到 200；如果它得到 302 并回到登录页，才表示登录状态没有被识别。

---

## 六、从测试代码本身开始追踪

面对大量陌生测试注解和方法时，用户决定：

> 看不懂（各种意义上的看不懂），好懵啊。
>
> 我决定从代码本身出发。

最先关注的是：

```java
when(userService.authenticate("test-admin", "correct-password"))
        .thenReturn(Optional.of(user));
```

### 当时的想法

用户认为应该关注真实的：

```java
userService.authenticate(...)
```

会返回什么，再放进 `when` 判断。

于是继续进入 `UserService`，看到：

```java
Optional<MyUser> result =
        userRepository.findByUsername(
                username == null ? "" : username.trim()
        );
```

因为 `Optional`、三元表达式和 Repository 调用同时出现，阅读开始变得困难，于是先往下看。

### 实际含义

这里的 `when` 不是普通业务判断。

```java
when(调用).thenReturn(结果)
```

是在告诉 Mockito：

> 测试期间，如果 Controller 用这组参数调用模拟的 `userService`，不要进入真实 Service，也不要访问 Repository，直接返回指定结果。

即：

```text
输入：
"test-admin", "correct-password"

模拟返回：
Optional.of(user)
```

`Optional.of(user)` 在这里表示：

```text
认证有结果
结果中装着这个测试创建的 MyUser
```

所以这一步继续追真实 Repository 并不是必需路线。

不过这个岔路很有价值：它暴露出阅读测试前必须先确认一个对象是真对象还是模拟对象。

---

## 七、把请求参数误认为 Session 内容

随后看到：

```java
mockMvc.perform(post("/user/login.html")
        .param("username", "test-admin")
        .param("password", "correct-password"))
```

当时判断：

> 进入登录页面以后存储的应该是这些。

并把：

```java
.param(...)
```

理解成了 Session 中保存的东西。

### 实际区别

`.param(...)` 模拟的是一次登录表单提交中的请求参数：

```text
当前 POST 请求：
username = test-admin
password 参数 → correct-password（测试占位值）
```

这些数据只服务于这一次登录请求。

真正写入 Session 的仍然是生产代码：

```java
session.setAttribute(
        AdminLoginInterceptor.LOGIN_USER,
        user
);
```

因此第一次请求结束后的 Session 是：

```text
"loginUser" → MyUser 对象
```

而不是：

```text
"username" → "test-admin"
"password" → "correct-password"
```

密码也不应该被长期保存到 Session。

---

## 八、对 `andExpect` 和 `andReturn` 执行顺序的误解

测试代码为：

```java
MvcResult loginResult = mockMvc.perform(post("/user/login.html")
                .param("username", "test-admin")
                .param("password", "correct-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/42/1.html"))
        .andReturn();
```

用户当时理解为：

> 先走 302，再走登录页，最后重定向到登录页。

并继续猜测：

> 这个重定向的过程会不会已经把之前 `loginResult` 里面存的东西置空？

### 实际执行模型

真正执行请求的是：

```java
mockMvc.perform(...)
```

后面的调用分别表示：

```text
andExpect(status().is3xxRedirection())
→ 检查响应状态是不是 3xx

andExpect(redirectedUrl("/42/1.html"))
→ 检查响应的跳转目标是不是 /42/1.html

andReturn()
→ 取回这次已经完成的请求与响应结果
```

`andExpect` 不会引发新的页面跳转，只是检查已经产生的响应。

MockMvc 在这里也不会自动跟随 `/42/1.html`。

因此实际只有一次请求：

```text
POST /user/login.html
→ 返回 302
→ 响应头 Location = /42/1.html
→ 测试把结果保存为 loginResult
```

重定向响应不会自动清空 Session。

Session 通常在下面这些情况才失效：

```text
代码调用 session.invalidate()
Session 超时
服务器会话被清理
```

---

## 九、为什么不能提前取得 `loginSession`

随后看到：

```java
MockHttpSession loginSession =
        (MockHttpSession) loginResult
                .getRequest()
                .getSession(false);
```

用户尝试把它挪到登录请求上面，但怎么写都不符合语法，并判断这可能是错误思路。

这个自我判断是正确的。

变量之间存在明确的产生顺序：

```text
先执行登录请求
→ andReturn() 产生 loginResult
→ 从 loginResult 的请求中取得 loginSession
```

如果登录请求尚未执行：

```text
loginResult 还不存在
→ 无法从它里面取得 Session
```

`getSession(false)` 中的 `false` 表示：

```text
存在 Session 就返回
不存在就返回 null
不要为了测试临时创建一个新的
```

这保证测试取得的是登录请求真正创建的 Session。

---

## 十、看到 `isOk()` 后产生的新疑问

用户继续找到 HTTP 状态定义：

```java
OK(200, HttpStatus.Series.SUCCESSFUL, "OK")
```

并确认：

> 确实应该返回 200，怎么会返回 302 呢？

同时又注意到前面已经明确写着：

```java
.andExpect(status().is3xxRedirection())
```

所以产生冲突：

```text
前面期望 302
后面期望 200
为什么同一个测试里要求不一样？
```

答案是它们属于两个不同请求：

| 请求 | 业务目的 | 正确状态 |
|---|---|---:|
| `POST /user/login.html` | 登录成功后跳到个人文章页 | 302 |
| `GET /admin/` | 登录用户直接进入后台页面 | 200 |

同一个状态码不能脱离请求目的判断成功或失败。

---

## 十一、关键突破：第二次请求根本没有使用 `loginSession`

用户最终把注意力放到：

```java
mockMvc.perform(get("/admin/"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/index"));
```

并得出：

> 这段代码里面根本就没看到 `loginSession`。

这是本关最重要的自主发现。

测试前面确实已经取得：

```java
MockHttpSession loginSession = ...
```

但 Java 变量存在，并不代表后续创建的请求会自动使用它。

当前测试实际是：

```text
第一次 perform
→ 创建 POST 登录请求
→ 产生带 loginUser 的 Session
→ Java 变量 loginSession 指向它

第二次 perform
→ 创建全新的 GET 后台请求
→ 没有指定 Session
→ 拦截器取不到 loginUser
→ 返回 302 到登录页
```

两个请求写在同一个测试方法里，不会自动让它们成为“同一个浏览器”。

---

## 十二、对五个问题的原始回答与纠正

### 问题 1：为什么第一次 302 成功，第二次 302 失败

用户回答：

> 第一次请求登录返回 302 是因为网页要重新定向，所以第一次 302 是成功的。
>
> 第二次应该是 200，显示 302 就是失败的。

核心判断正确，但第一次的具体目标有偏差。

测试并没有先访问根路径，也没有跳到登录页。它直接：

```text
POST /user/login.html
→ 302 到 /42/1.html
```

### 问题 2：第一次 Session 中有什么

用户回答：

```java
.param("username", "test-admin")
.param("password", "correct-password")
```

这里把请求参数当成了 Session 属性。

正确内容是：

```text
"loginUser" → MyUser 对象
```

### 问题 3：第二次 Session 在哪里

用户回答：

> 我不知道第二次的 Session 在哪里。

这实际上非常接近答案：

```text
第二次请求没有携带第一次的 Session
```

对拦截器来说，它没有可用的 `loginUser`。

### 问题 4：同一个方法里的请求为什么不是同一个浏览器

用户猜测：

> 可能它会跳转，比如跳到 user/login 或者 /admin。

真正原因与跳转无关：

```text
每一次 mockMvc.perform(...) 都执行一个独立请求
```

测试必须明确把同一个 Session 交给第二次请求。

### 问题 5：为什么取得了 `loginSession` 仍然没登录

用户回答：

> 第二次请求好像和任何参数都没关系。
>
> 这段代码里面根本就没看到 `loginSession`。

这个回答完全命中根因。

---

## 十三、第一次修改时的括号错误

确定需要把 `loginSession` 交给后台请求后，第一次写成：

```java
mockMvc.perform(get("/admin/"))
        .session(loginSession)
```

IDEA 提示：

```text
无法解析 ResultActions 中的方法 session
```

### 为什么会这样

括号已经让：

```java
mockMvc.perform(get("/admin/"))
```

完整执行结束。

`perform(...)` 返回的是：

```text
ResultActions
```

它负责检查执行结果，因此可以继续：

```java
.andExpect(...)
```

但不能再给已经执行完的请求添加 Session。

`.session(...)` 属于：

```java
get("/admin/")
```

产生的请求构造器。

所以必须先把 Session 加进请求，再把请求交给 `perform`。

---

## 十四、最终修改

修改前：

```java
mockMvc.perform(get("/admin/"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/index"));
```

修改后：

```java
mockMvc.perform(get("/admin/").session(loginSession))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/index"));
```

新增的核心是：

```java
.session(loginSession)
```

括号表达的顺序是：

```text
get("/admin/")
→ 创建 GET 请求构造器

.session(loginSession)
→ 给这个请求带上登录 Session

mockMvc.perform(...)
→ 执行准备完成的请求

.andExpect(...)
→ 检查执行结果
```

最终用户重新运行测试并确认完全通过。

---

## 十五、完整测试如何工作

### 1. 创建一个测试用户

```java
MyUser user = new MyUser();
user.setId(42);
user.setUsername("test-admin");
user.setActive(true);
user.setStaff(true);
```

这个用户不是从真实数据库查询的。

`active` 和 `staff` 设置为 `true`，是为了满足后台拦截器规则。

### 2. 规定模拟认证结果

```java
when(userService.authenticate("test-admin", "correct-password"))
        .thenReturn(Optional.of(user));
```

这让 Controller 可以走登录成功分支，同时避免本关测试密码和数据库。

### 3. 模拟提交登录表单

```java
MvcResult loginResult = mockMvc.perform(post("/user/login.html")
                .param("username", "test-admin")
                .param("password", "correct-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/42/1.html"))
        .andReturn();
```

它验证：

```text
登录成功
→ 返回重定向
→ 跳转目标使用当前用户 id
```

### 4. 从登录请求取得 Session

```java
MockHttpSession loginSession =
        (MockHttpSession) loginResult
                .getRequest()
                .getSession(false);
```

### 5. 检查 Session 协议

```java
assertNotNull(loginSession);
assertSame(
        user,
        loginSession.getAttribute(
                AdminLoginInterceptor.LOGIN_USER
        )
);
```

它验证：

```text
登录请求确实创建了 Session
LOGIN_USER 键下保存的就是认证成功返回的 user
```

### 6. 携带同一 Session 访问后台

```java
mockMvc.perform(get("/admin/").session(loginSession))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/index"));
```

它验证：

```text
真实后台拦截器能够识别同一 Session 中的用户
请求被放行
AdminController 返回 admin/index
```

---

## 十六、为什么这条测试能抓住 Boss 01

稳定代码写入：

```java
session.setAttribute(
        AdminLoginInterceptor.LOGIN_USER,
        user
);
```

测试随后读取同一个约定：

```java
loginSession.getAttribute(
        AdminLoginInterceptor.LOGIN_USER
);
```

后台拦截器也读取：

```java
session.getAttribute(LOGIN_USER);
```

如果登录代码再次错误写成：

```java
session.setAttribute("currentUser", user);
```

测试会在两个地方暴露问题：

```text
直接 Session 断言取不到 LOGIN_USER
或
后台拦截器取得 null 并返回 302
```

原来的 7 个测试不知道这条业务协议，所以 Boss 可以悄悄存在。

新增测试把它变成了明确、可重复执行的规则。

---

## 十七、测试数量变化意味着什么

修改前：

```text
7 个测试
```

它们主要保护：

```text
Django 密码工具
Spring 应用上下文启动
媒体与静态资源
```

修改后：

```text
8 个测试
```

新增的第 8 个测试保护：

```text
登录 Controller
→ Session 登录状态
→ 后台拦截器
→ 后台 Controller
```

测试数量增加并不是目标本身。

真正的变化是项目第一次把“登录后能够进入后台”写成了机器可以反复验证的业务规则。

---

## 十八、本轮最重要的心得

用户在测试通过后说：

> 我理解为什么会这样了。
>
> 虽然对很多代码细节都不太懂，也不太清楚它们的功能，就算讲了还是一知半解。
>
> 因为之前根本没接触过，没有什么知识储备，只能盯着代码本身看。
>
> 或许以后工作了，接手其他项目也会是这个状态，没有人会把东西嚼烂了喂到嘴里。

这个判断很接近真实项目开发。

接手陌生项目时，不需要先把所有框架源码、注解和工具一次性学完，才能开始工作。

更现实的推进方式是：

```text
先确定一个具体行为
→ 找到输入和输出
→ 找到负责该行为的少数组件
→ 观察运行证据
→ 提出一个可以被验证的猜想
→ 用最小修改验证
→ 再补需要的局部知识
```

这次即使不懂 Mockito 和 MockMvc 的大量细节，用户仍然完成了有效推理：

```text
第二次请求为什么被拦截？
→ 第一次 Session 明明存在
→ 第二次代码却没有出现 loginSession
→ 两次请求没有建立会话联系
```

这不是“只盯代码所以没学懂”，而是在没有完整知识地图时，依靠局部证据搭建第一版地图。

之后再次遇到测试工具时，`when`、`perform`、`param`、`andExpect` 和 `session` 才会逐渐从陌生符号变成已经见过的角色。

---

## 十九、本轮正确和错误思路的价值

### 当时没有成立的想法

```text
when 是普通条件判断
需要进入真实 authenticate 和 Repository
.param 是 Session 保存的数据
andExpect 会推动页面跳转
重定向可能清空 loginResult
可以在 loginResult 产生前取得 loginSession
同一个测试方法里的两个请求会自动共享状态
.session 可以加在 perform(...) 执行结果后面
```

### 最终成立的认识

```text
when 为模拟对象规定返回结果
.param 是单次请求参数
andExpect 只检查响应
andReturn 取回完成后的请求结果
loginSession 必须在登录请求完成后取得
每个 perform(...) 都是一次独立请求
Java 变量存在不等于请求自动使用它
.session 必须在请求执行前加入请求构造器
```

错误思路并非需要删除的部分。

每一次错误只要能被代码结构、IDE 类型提示或运行输出来推翻，就在帮助形成更可靠的判断方法。

---

## 二十、用自己的话复述这次修改

测试先模拟提交正确账号密码。模拟的 `UserService` 直接返回一个启用且具有 staff 权限的 `MyUser`，所以登录 Controller 把它存入 Session，并返回 302 到用户文章页。

测试从这次登录请求中取得 `loginSession`，确认 `LOGIN_USER` 键下保存的是该用户。

然后测试创建第二个 GET `/admin/` 请求。最初这个新请求没有携带第一次的 Session，所以真实后台拦截器认为它没有登录，返回 302 到登录页。

最终在 GET 请求构造器上添加：

```java
.session(loginSession)
```

使第二次请求模拟同一个浏览器继续访问。拦截器能够取得用户并放行，后台 Controller 返回 `admin/index` 和 HTTP 200，测试通过。

---

## 通关结论

这次新增的是一条测试，但真正建立的是一个更完整的项目模型：

```text
请求参数
≠ Session 属性

Java 中持有 Session
≠ 新请求自动携带 Session

写在同一个测试方法
≠ 来自同一个浏览器

HTTP 302
≠ 永远失败

测试通过
= 当前测试描述的业务规则成立
```

面对陌生项目时，可以暂时不懂很多细节，但不能只凭感觉让所有概念混在一起。

先问清：

```text
这是哪一次请求？
输入放在哪里？
状态保存在哪里？
谁写入？
谁读取？
当前输出是在执行前失败、环境中失败，还是业务断言失败？
```

这套提问方式可以迁移到以后接手的其他项目，而不仅仅适用于本次 Spring MVC 测试。
