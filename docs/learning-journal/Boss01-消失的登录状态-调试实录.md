# Boss 01：消失的登录状态——完整调试实录

> 日期：2026-07-25
>
> 项目：`myblog-Java`
>
> 关卡分支：`boss/01-lost-login-state`
>
> 关卡类型：受控故障练习
>
> 说明：这个故障是为了学习而故意加入的，并不是原项目天然存在的问题。

## 为什么要保留这份记录

这是我第一次不靠直接看答案，而是从浏览器表现、路径搜索、断点、拦截器和 Session 一点点追踪一个跨组件问题。

过程中有不少猜错、绕远和看不懂的地方。但这些并不是应该删掉的“黑历史”，而是真正帮助我建立项目理解的过程。因此，这份记录会同时保留：

- 当时观察到的现象；
- 当时真实产生的猜想；
- 猜想为什么看起来合理；
- 后来用什么证据排除了它；
- 最终如何把 Controller、拦截器和 Session 串成一条完整链路。

---

## 一、关卡开始时的现象

登录时输入正确的账号和密码，页面会正常跳到个人文章列表，看上去已经登录成功。

但点击“博客后台管理”，或者访问：

```text
/admin
```

又会被送回登录页面：

```text
/user/login.html
```

当时还不知道问题出在什么地方，只知道：

```text
登录看起来成功了
但后台不承认登录状态
```

---

## 二、第一次搜索：从 `/admin` 找到后台入口

一开始我毫无头绪，后来通过全局搜索：

```text
/admin
```

发现 `/admin` 是进入后台的路径。点击“博客后台管理”后，页面果然跳到了登录页面。

随后定位到后台首页方法：

```java
@GetMapping({"/admin", "/admin/"})
public String dashboard(HttpSession session, Model model) {
    MyUser user = loginUser(session);
    base(model, "网站管理", "首页");
    model.addAttribute("articleCount", adminService.articleCount(user.getId()));
    model.addAttribute("albumCount", adminService.albumCount(user.getId()));
    model.addAttribute("tagCount", adminService.tagCount(user.getId()));
    model.addAttribute("commentCount", adminService.commentCount(user.getId()));
    model.addAttribute("boardCount", adminService.boardCount(user.getId()));
    return "admin/index";
}
```

### 当时的判断

这段代码看起来没有明显问题：

1. 接收 `/admin` 或 `/admin/` 请求；
2. 获取当前用户；
3. 查询文章、相册、标签、评论和留言数量；
4. 返回后台首页 `admin/index`。

所以我开始怀疑：是不是方法内部的代码细节有问题？

这个判断虽然没有直接找到答案，但方向并不荒唐。它是在尝试确定问题发生在“进入后台以后”，还是“进入后台以前”。

---

## 三、关键证据：后台 Controller 的断点没有命中

我在 `dashboard` 方法附近打了断点，然后重新点击“博客后台管理”。

### 当时的设想

如果程序在这里停住，就说明请求已经进入 `dashboard`，问题应该在方法内部。

如果程序没有走到：

```java
@GetMapping({"/admin", "/admin/"})
```

我当时以为程序可能不知道访问 `/admin` 后应该显示什么，页面应该处于“点不动”的状态。

### 实际结果

断点没有命中，但浏览器仍然跳到了登录页面。

当时的反应是：

> 我盯……！（超认真）

这是整次排查的第一个重要突破。它说明：

```text
请求还没有进入 dashboard
某段更早执行的代码就已经让浏览器跳转了
```

当时还不知道“更早的代码”是什么，但已经能排除：

```text
不是 dashboard 方法内部的统计查询导致了跳转
```

---

## 四、第一个错误猜想：`/admin` 会不会触发登录的 POST 方法

看到浏览器最后落在 `/user/login.html` 后，我产生了一个猜想：

> 使用 `/admin` 路径以后，是不是跳转到了
>
> `@PostMapping("/user/login.html")`？
>
> 难道 `/admin` 里的某个参数恰好和登录方法里的参数一致？

于是准备找出所有会跳转到登录页面的方法，再逐个排查。

### 为什么这个猜想不成立

Spring MVC 选择 Controller 方法时，主要看：

```text
HTTP 请求方法 + 请求路径
```

例如：

```text
GET  /user/login.html
POST /user/login.html
GET  /admin
```

它们是三个不同的请求。

`/admin` 中是否存在与登录方法同名的参数，不会让它自动匹配：

```java
@PostMapping("/user/login.html")
```

后来还原出的实际情况是：

```text
第一次请求：GET /admin
服务器返回重定向响应
第二次请求：GET /user/login.html
```

不是 `/admin` 直接调用了登录 Controller，也不是参数碰巧匹配。

---

## 五、找到真正发出重定向的代码

继续搜索登录页面路径后，找到了：

```java
if (user == null || !user.isActive() || !user.isStaff()) {
    response.sendRedirect(request.getContextPath() + "/user/login.html");
    return false;
}
```

它位于：

```text
AdminLoginInterceptor.preHandle
```

### 当时对条件的理解

这段代码表示，如果：

- 没有取得用户；
- 用户没有启用；
- 用户不是后台工作人员；

其中任何一个条件成立，就把浏览器送到登录页，并阻止后面的 Controller 继续执行。

### 当时产生的猜想

> 难道点击“博客后台管理”会导致用户出错吗？
>
> 如果真的和这段代码有关，那是不是涉及数据库层？

这个猜想后来只对了一半：

- 确实是这里把浏览器送回了登录页；
- 但拦截器当时并没有重新查询数据库；
- 它检查的是 Session 中保存的用户对象。

因此，不应该立刻去修改数据库里的 `active` 或 `staff` 字段。

---

## 六、在 `dashboard` 和登录页之间来回追踪

重新查看 `dashboard`，第一句是：

```java
MyUser user = loginUser(session);
```

于是开始怀疑 `loginUser`。

继续定位到：

```java
private MyUser loginUser(HttpSession session) {
    return (MyUser) session.getAttribute(
            AdminLoginInterceptor.LOGIN_USER
    );
}
```

当时还看不懂 `session.getAttribute(...)`，但能感觉到它和：

```java
AdminLoginInterceptor.LOGIN_USER
```

有关。

随后又在 `AccountController` 里找到：

```java
@GetMapping("/user/login.html")
public String loginPage(HttpSession session, Model model) {
    MyUser loginUser = (MyUser) session.getAttribute(
            AdminLoginInterceptor.LOGIN_USER
    );
    if (loginUser != null) {
        return "redirect:/" + loginUser.getId() + "/1.html";
    }
    prepareUserPage(model, false);
    return "user";
}
```

这里再次出现了：

```java
AdminLoginInterceptor.LOGIN_USER
```

### 当时混在一起的两个猜想

当时大致想了两种可能：

1. `user == null || !user.isActive() || !user.isStaff()` 中有一个或多个条件成立；
2. `session.getAttribute(LOGIN_USER)` 这句话“触发”了 `AccountController`，然后进入登录页。

第二种猜想并不成立：

```java
session.getAttribute(...)
```

只是从 Session 取东西，不会调用另一个 Controller。

登录页之所以被执行，是因为拦截器先发送了重定向响应，随后浏览器又发出了一次新的 GET 请求。

### 此时的真实状态

当时已经找到多个关键零件：

- 后台 Controller；
- 登录页 Controller；
- 后台拦截器；
- Session；
- 共同出现的 `LOGIN_USER`。

但还没有搞清楚这些零件按照什么顺序工作，所以感觉一直在代码之间“来回捯”，最后推不动了。

---

## 七、拦截器到底是怎么被触发的

新的疑问出现了：

> 代码里没有看到谁主动调用 `preHandle`。
>
> `AdminLoginInterceptor` 好像只和 `HandlerInterceptor` 有关。
>
> 那拦截器到底怎么触发？

继续搜索后，在 MVC 配置中找到了：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(adminLoginInterceptor)
            .addPathPatterns("/admin/**", "/ckeditor/**")
            .excludePathPatterns("/admin/logout/");
}
```

当时终于反应过来：

> 原来拦截器在这里啊！
>
> 进入 admin 的时候还得先淌一遍这个拦截器。

### `HandlerInterceptor` 在本项目里的意义

可以暂时不研究它的底层源码，只需要先理解它是 Spring MVC 规定的一种扩展接口。

项目把 `adminLoginInterceptor` 注册给 Spring，并指定：

```text
/admin/**
/ckeditor/**
```

以后只要请求路径符合这些规则，Spring 就会在 Controller 之前自动调用：

```java
preHandle(...)
```

所以不需要在业务代码中手动写：

```java
adminLoginInterceptor.preHandle(...)
```

完整顺序变成了：

```text
GET /admin
    ↓
WebMvcConfig 中的路径规则命中
    ↓
AdminLoginInterceptor.preHandle
    ↓
返回 true  → 继续执行 AdminController
返回 false → Controller 不执行
```

这也解释了为什么 `dashboard` 的断点不会命中。

---

## 八、第二个错误猜想：用户名或密码是不是被置空了

理解拦截器的执行顺序后，我又根据提示猜测：

> 是不是登录以后用户名或者密码被置空了，或者改变了？
>
> 所以 `/admin` 那个请求根本没有用？

后来这个猜想也被排除了。

正确账号密码能够进入：

```java
.map(user -> {
    // ...
    return "redirect:/" + user.getId() + "/1.html";
})
```

说明：

- `UserService.authenticate(...)` 已经认证成功；
- `user` 是一个真实存在的 `MyUser` 对象；
- 程序还成功使用了 `user.getId()` 组成跳转地址。

如果账号或密码在认证前就被错误置空，一般不会进入这个成功分支。

另外，`/admin` 请求并不是“没有用”，而是：

```text
请求已经到达服务器
但是在进入 Controller 前被拦截器拒绝
```

---

## 九、终于搜索 `setAttribute`

接着重新查看登录成功分支：

```java
.map(user -> {
    session.setAttribute("currentUser", user);
    return "redirect:/" + user.getId() + "/1.html";
})
```

这时意识到最关键的一句可能是：

```java
session.setAttribute("currentUser", user);
```

并想到：

> 原来在这里！应该就是调用这个方法存储的数据！

### 为什么要搜索 `setAttribute`

前面已经知道拦截器通过下面的代码取登录用户：

```java
session.getAttribute(...)
```

既然要“取”，就一定曾经有人负责“存”。

所以应该在项目业务代码中寻找：

```java
session.setAttribute(...)
```

这不是突然跳到一个无关的语法点，而是在追踪同一份登录状态：

```text
谁把用户放进 Session？
谁又从 Session 把用户取出来？
```

---

## 十、搜索结果中的排除过程

全局搜索 `setAttribute` 后，还找到了其他位置。

### 1. 修改用户资料后的 Session 更新

类似：

```java
session.setAttribute(AdminLoginInterceptor.LOGIN_USER, saved);
```

当时判断：

> 这是进入系统以后修改资料使用的，我现在连后台都没进去，所以应该不是这次问题的起点。

这个排除方向基本正确。

它的作用是：数据库中的用户资料更新后，把最新的 `MyUser` 对象重新放进 Session，避免 Session 继续保存旧资料。

### 2. 文章阅读标记

还找到了：

```java
if (firstReading) {
    session.setAttribute(readingKey, Boolean.TRUE);
}
```

当时判断：

> 这里看到了 article，应该和文章有关，和后台登录没关系。

这个排除也是正确的。

它虽然同样使用 Session，但保存的是“这篇文章是否已在当前会话中计过阅读”，不是登录用户。

这说明同一个 Session 可以同时保存多份不同用途的数据，只要它们使用不同的名字。

---

## 十一、一个很真实的搜索岔路：误入 JPA `SetAttribute`

搜索过程中又找到了：

```java
public interface SetAttribute<X, E>
        extends PluralAttribute<X, Set<E>, E> {
}
```

于是继续追踪：

```java
public interface PluralAttribute<X, C, E>
        extends Attribute<X, C>, Bindable<E> {
    // ...
}
```

最后又找到了：

```java
public interface Attribute<X, Y> {
    // ...
}
```

看完以后觉得这些代码都很正常，也没有发现任何东西被置空。

### 为什么这是一个岔路

这里混淆了两个不同的东西：

```java
SetAttribute          // 首字母大写，是 JPA 元模型中的一种类型
session.setAttribute  // 首字母小写，是 HttpSession 对象的方法
```

这两个名字看起来相似，但所属模块、用途和问题完全不同。

这次岔路带来的经验是：

1. 搜索方法时，不只看名字，还要看调用者是谁；
2. 当前问题里的调用者是 `session`；
3. 因此应该追踪 `HttpSession.setAttribute` 的业务调用位置；
4. 不需要为了使用框架提供的方法，把框架和 JPA 的底层接口全部读完。

这条路虽然没有找到答案，但帮助排除了“用户对象被某个 Attribute 类型置空”的猜想。

---

## 十二、最终突破：对比 Session 的写入键和读取键

登录成功时写入：

```java
session.setAttribute("currentUser", user);
```

也就是：

```text
写入键 = "currentUser"
写入值 = user
```

拦截器读取：

```java
session.getAttribute(AdminLoginInterceptor.LOGIN_USER);
```

继续查看常量定义：

```java
public static final String LOGIN_USER = "loginUser";
```

也就是：

```text
读取键 = "loginUser"
```

最后对比：

```text
"currentUser" != "loginUser"
```

Boss 的真正原因终于出现了。

---

## 十三、为什么键名不同会让用户“消失”

可以把 Session 暂时理解为服务器为当前浏览器保管的一个“名字 → 东西”的储物柜：

```java
session.setAttribute("名字", 东西);
session.getAttribute("名字");
```

发生故障时，储物柜实际上是：

```text
"currentUser" → MyUser 对象
```

但是拦截器询问的是：

```text
请把名字为 "loginUser" 的东西给我
```

Session 中没有这个名字，所以返回：

```java
null
```

需要特别注意：

```text
MyUser 对象没有被置空
用户名没有被置空
密码没有被置空
数据库用户也没有消失
```

真正的问题只是：

```text
存的时候和取的时候使用了两把不同的钥匙
```

---

## 十四、为什么登录后仍然能看到个人文章页

这个现象一度让问题很迷惑：

```text
如果没有登录状态，为什么登录后还能跳到个人文章页？
```

登录成功分支里已经持有当前的 `user`：

```java
return "redirect:/" + user.getId() + "/1.html";
```

组成跳转地址只需要刚刚认证成功返回的 `user.getId()`，不需要重新从 Session 读取用户。

而个人文章列表属于公开访问页面，所以浏览器仍然能打开它。

因此：

```text
认证成功并完成跳转
不等于
Session 登录状态保存正确
```

这也是为什么问题表现为“看似登录成功”。

---

## 十五、`@ModelAttribute("loginUser")` 为什么也出现了相同名字

排查中还找到：

```java
@ModelAttribute("loginUser")
public MyUser loginUser(HttpSession session) {
    return (MyUser) session.getAttribute(
            AdminLoginInterceptor.LOGIN_USER
    );
}
```

当时的疑问是：

> 为什么这段代码和 `preHandle` 里面读取 Session 的代码一模一样？

这里同时存在两个不同的“名字空间”：

### Session 中的键

```java
session.getAttribute(AdminLoginInterceptor.LOGIN_USER)
```

负责从跨请求保存的 Session 中取得当前登录用户。

### Model 中的键

```java
@ModelAttribute("loginUser")
```

负责把取得的用户以 `loginUser` 为名字交给页面模板，使 Thymeleaf 页面可以显示当前用户。

可以理解为：

```text
Session --取出 MyUser--> GlobalViewData --放入 Model--> Thymeleaf 页面
```

两边都使用 `loginUser` 是项目作者的统一命名习惯，但它们不是同一个容器。

---

## 十六、当时提出的修复方案

发现键名不一致后，我问：

> 登录成功写入的是 `("currentUser", user)`，
>
> 拦截读取的是 `LOGIN_USER`，也就是 `"loginUser"`，
>
> 那直接把 `loginUser` 改成 `"currentUser", user` 成吗？

这个问题已经抓住根因，但修复位置还需要选择。

### 不推荐的修法

到所有读取位置逐个改成：

```java
session.getAttribute("currentUser")
```

这样会产生多个重复的字符串。以后某个位置少改一次，同样的问题还会再次出现。

只修改：

```java
@ModelAttribute("loginUser")
```

也不能解决问题，因为它修改的是页面 Model 的名字，不是 Session 的读取键。

### 最合适的修法

把登录成功时的写入改回共享常量：

```java
session.setAttribute(AdminLoginInterceptor.LOGIN_USER, user);
```

这样写入方和读取方都依赖同一个约定：

```java
AdminLoginInterceptor.LOGIN_USER
```

如果以后真的需要更换 Session 键名，只修改常量定义即可，不需要到多个文件中寻找字符串。

---

## 十七、修复后的完整请求链

修复后，登录流程是：

```text
POST /user/login.html
    ↓
AccountController 接收用户名和密码
    ↓
UserService.authenticate 完成认证
    ↓
得到 MyUser
    ↓
session.setAttribute(LOGIN_USER, user)
    ↓
浏览器保存 Session 对应的 Cookie
```

随后访问后台：

```text
GET /admin
    ↓
WebMvcConfig 的 /admin/** 规则命中
    ↓
AdminLoginInterceptor.preHandle
    ↓
session.getAttribute(LOGIN_USER)
    ↓
取得同一个 MyUser
    ↓
检查 active 和 staff
    ↓
返回 true
    ↓
AdminController.dashboard 执行
```

退出登录后再访问后台：

```text
Session 中没有 LOGIN_USER
    ↓
拦截器取得 null
    ↓
发送重定向到 /user/login.html
    ↓
dashboard 不执行
```

---

## 十八、验证结果与新的问题

代码修复后，项目原有测试结果为：

```text
Tests run: 7
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

但是需要注意：Boss 故障还存在时，这 7 个测试同样全部通过。

这说明：

```text
测试全绿
只表示已经写出来的测试通过
不表示项目中的所有业务都正确
```

当前测试覆盖了应用启动、密码工具和媒体资源等内容，却没有覆盖：

```text
登录成功
→ Session 保存用户
→ 携带同一 Session 请求后台
→ 拦截器识别用户并放行
```

因此，下一步很适合补一条登录与后台拦截的回归测试，让这类键名不一致以后能够被自动发现。

---

## 十九、这次真正学到的项目知识

### 1. Controller 并不一定是请求遇到的第一段业务代码

请求可以先经过拦截器。Controller 断点没有命中，不代表路径无人处理，也可能是请求在前置阶段被拒绝。

### 2. 重定向会产生新的请求

`sendRedirect(...)` 不是在服务器内部直接调用另一个 Controller，而是告诉浏览器再请求一个新地址。

### 3. Session 负责在多个请求之间保存登录状态

登录 POST 和之后的后台 GET 是两个请求。Controller 中的局部变量不会自动跨请求保留，所以需要 Session。

### 4. Session 键名是一种跨组件协议

登录 Controller、后台拦截器、登录页 Controller、全局页面数据都必须对“当前登录用户使用哪个键名”达成一致。

### 5. 共享常量可以防止协议漂移

使用：

```java
AdminLoginInterceptor.LOGIN_USER
```

比在多个文件中重复写 `"loginUser"` 更安全。

### 6. 同一个 Session 可以保存多种数据

登录用户、文章阅读标记等都能存在 Session 中，靠不同的键名互相区分。

### 7. 搜索结果需要结合调用者和所属模块判断

`SetAttribute` 类型和 `session.setAttribute(...)` 方法名字相似，但属于完全不同的问题。

### 8. 真实调试不是一次就猜中

这次经历了：

```text
怀疑 Controller
→ 怀疑路由参数
→ 怀疑数据库状态
→ 怀疑 loginUser 方法会触发 Controller
→ 怀疑用户名或密码被置空
→ 误入 JPA Attribute 源码
→ 最终通过写入和读取对比找到键名不一致
```

每个错误猜想只要能够被证据排除，就不是白走。

---

## 二十、用自己的话复述本关

访问 `/admin` 时，Spring 会根据 `WebMvcConfig` 的配置先执行后台登录拦截器，而不是直接进入 `dashboard`。

拦截器通过 Session 的 `LOGIN_USER` 键读取当前用户。故障版本虽然认证成功，也把 `MyUser` 放进了 Session，但使用的键是 `"currentUser"`；拦截器读取的实际键是 `"loginUser"`，所以得到 `null`，把请求重定向回登录页。

最终把登录成功时的写入改成：

```java
session.setAttribute(AdminLoginInterceptor.LOGIN_USER, user);
```

写入和读取重新遵守同一份 Session 协议，后台便可以识别登录用户。

这次修复改变的不是账号密码验证，而是：

```text
认证成功后的登录状态传递
```

---

## 通关结论

Boss 01 的核心不是记住 `setAttribute` 的语法，而是理解这条项目链路：

```text
登录 Controller
→ UserService 认证
→ MyUser
→ Session 保存状态
→ WebMvcConfig 注册拦截器
→ AdminLoginInterceptor 检查状态
→ AdminController
```

第一次调试仍然会模糊、会被提示推动，这是正常的。现在已经建立了第一版完整心智模型；下一关的目标不是要求一次猜中，而是减少无依据的猜测，更多地利用请求路径、断点位置、状态写入点和状态读取点来缩小范围。
