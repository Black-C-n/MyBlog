(function () {
    "use strict";

    const STORAGE_PREFIX = "myblog-learning-v1";
    const progressKey = STORAGE_PREFIX + ":progress";
    const themeKey = STORAGE_PREFIX + ":theme";

    const lessons = Array.from(document.querySelectorAll(".lesson[data-lesson]"));
    const searchInput = document.getElementById("course-search");
    const searchSummary = document.getElementById("search-summary");
    const emptyState = document.getElementById("empty-state");
    const progressFill = document.getElementById("progress-fill");
    const progressValue = document.getElementById("progress-value");
    const progressMeta = document.getElementById("progress-meta");
    const themeButton = document.getElementById("theme-toggle");
    const menuButton = document.getElementById("menu-toggle");
    const sidebar = document.getElementById("course-sidebar");
    const sidebarBackdrop = document.getElementById("sidebar-backdrop");
    const backToTop = document.getElementById("back-to-top");
    const tocLinks = Array.from(document.querySelectorAll(".toc a[href^='#']"));

    function readProgress() {
        try {
            const value = JSON.parse(localStorage.getItem(progressKey) || "[]");
            return new Set(Array.isArray(value) ? value : []);
        } catch (error) {
            return new Set();
        }
    }

    let completedLessons = readProgress();

    function saveProgress() {
        try {
            localStorage.setItem(progressKey, JSON.stringify(Array.from(completedLessons)));
        } catch (error) {
            // file:// 或浏览器隐私设置可能禁用存储；页面本身仍然可以正常学习。
        }
    }

    function updateProgressView() {
        const total = lessons.length;
        const completed = lessons.filter(function (lesson) {
            return completedLessons.has(lesson.dataset.lesson);
        }).length;
        const percent = total === 0 ? 0 : Math.round((completed / total) * 100);

        progressFill.style.width = percent + "%";
        progressValue.textContent = percent + "%";
        progressMeta.textContent = "已完成 " + completed + " / " + total + " 章";

        lessons.forEach(function (lesson) {
            const button = lesson.querySelector(".complete-button");
            if (!button) return;
            const done = completedLessons.has(lesson.dataset.lesson);
            button.classList.toggle("done", done);
            button.setAttribute("aria-pressed", done ? "true" : "false");
            button.textContent = done ? "✓ 已学完" : "标记学完";
        });
    }

    lessons.forEach(function (lesson) {
        const button = lesson.querySelector(".complete-button");
        if (!button) return;
        button.addEventListener("click", function () {
            const id = lesson.dataset.lesson;
            if (completedLessons.has(id)) {
                completedLessons.delete(id);
            } else {
                completedLessons.add(id);
            }
            saveProgress();
            updateProgressView();
        });
    });

    document.getElementById("mark-all").addEventListener("click", function () {
        lessons.forEach(function (lesson) {
            completedLessons.add(lesson.dataset.lesson);
        });
        saveProgress();
        updateProgressView();
    });

    document.getElementById("reset-progress").addEventListener("click", function () {
        if (!window.confirm("确定清空全部学习进度吗？教程文件不会被删除。")) return;
        completedLessons.clear();
        saveProgress();
        updateProgressView();
    });

    function normalizeText(value) {
        return value.toLocaleLowerCase("zh-CN").replace(/\s+/g, " ").trim();
    }

    function filterLessons() {
        const query = normalizeText(searchInput.value);
        let visibleCount = 0;

        lessons.forEach(function (lesson) {
            const searchable = normalizeText(
                lesson.textContent + " " + (lesson.dataset.keywords || "")
            );
            const matched = !query || searchable.includes(query);
            lesson.hidden = !matched;
            if (matched) visibleCount += 1;
        });

        const searching = query.length > 0;
        searchSummary.classList.toggle("visible", searching);
        emptyState.classList.toggle("visible", searching && visibleCount === 0);
        searchSummary.textContent = searching
            ? "关键词“" + searchInput.value.trim() + "”找到 " + visibleCount + " 个章节。按 Esc 可清空。"
            : "";

        tocLinks.forEach(function (link) {
            const target = document.querySelector(link.getAttribute("href"));
            link.hidden = Boolean(query && target && target.hidden);
        });
    }

    searchInput.addEventListener("input", filterLessons);

    function preferredTheme() {
        try {
            const saved = localStorage.getItem(themeKey);
            if (saved === "light" || saved === "dark") return saved;
        } catch (error) {
            // 忽略无法读取 localStorage 的情况。
        }
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    function applyTheme(theme) {
        document.documentElement.dataset.theme = theme;
        themeButton.textContent = theme === "dark" ? "☀" : "☾";
        themeButton.title = theme === "dark" ? "切换到浅色模式" : "切换到深色模式";
        themeButton.setAttribute("aria-label", themeButton.title);
    }

    applyTheme(preferredTheme());

    themeButton.addEventListener("click", function () {
        const next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
        applyTheme(next);
        try {
            localStorage.setItem(themeKey, next);
        } catch (error) {
            // 页面仍可使用，只是不记忆主题。
        }
    });

    function closeSidebar() {
        sidebar.classList.remove("open");
        sidebarBackdrop.classList.remove("visible");
        menuButton.setAttribute("aria-expanded", "false");
    }

    function toggleSidebar() {
        const opening = !sidebar.classList.contains("open");
        sidebar.classList.toggle("open", opening);
        sidebarBackdrop.classList.toggle("visible", opening);
        menuButton.setAttribute("aria-expanded", opening ? "true" : "false");
    }

    menuButton.addEventListener("click", toggleSidebar);
    sidebarBackdrop.addEventListener("click", closeSidebar);
    tocLinks.forEach(function (link) {
        link.addEventListener("click", closeSidebar);
    });

    function fallbackCopy(text, button) {
        const input = document.createElement("textarea");
        input.value = text;
        input.setAttribute("readonly", "");
        input.style.position = "fixed";
        input.style.opacity = "0";
        document.body.appendChild(input);
        input.select();
        try {
            document.execCommand("copy");
            showCopied(button);
        } finally {
            input.remove();
        }
    }

    function showCopied(button) {
        const original = button.textContent;
        button.textContent = "已复制";
        window.setTimeout(function () {
            button.textContent = original;
        }, 1400);
    }

    document.querySelectorAll(".copy-button").forEach(function (button) {
        button.addEventListener("click", function () {
            const codeBlock = button.closest(".code-block");
            const code = codeBlock ? codeBlock.querySelector("pre code") : null;
            if (!code) return;
            const text = code.textContent;
            if (navigator.clipboard && window.isSecureContext) {
                navigator.clipboard.writeText(text).then(function () {
                    showCopied(button);
                }).catch(function () {
                    fallbackCopy(text, button);
                });
            } else {
                fallbackCopy(text, button);
            }
        });
    });

    document.addEventListener("keydown", function (event) {
        const tagName = document.activeElement ? document.activeElement.tagName : "";
        const typing = tagName === "INPUT" || tagName === "TEXTAREA";

        if (event.key === "/" && !typing) {
            event.preventDefault();
            searchInput.focus();
        }

        if (event.key === "Escape") {
            if (searchInput.value) {
                searchInput.value = "";
                filterLessons();
                searchInput.focus();
            }
            closeSidebar();
        }
    });

    const sectionObserver = "IntersectionObserver" in window
        ? new IntersectionObserver(function (entries) {
            const visible = entries
                .filter(function (entry) { return entry.isIntersecting; })
                .sort(function (a, b) { return b.intersectionRatio - a.intersectionRatio; });
            if (visible.length === 0) return;
            const id = visible[0].target.id;
            tocLinks.forEach(function (link) {
                link.classList.toggle("active", link.getAttribute("href") === "#" + id);
            });
        }, { rootMargin: "-18% 0px -70% 0px", threshold: [0, 0.1, 0.3] })
        : null;

    if (sectionObserver) {
        lessons.forEach(function (lesson) {
            sectionObserver.observe(lesson);
        });
    }

    window.addEventListener("scroll", function () {
        backToTop.classList.toggle("visible", window.scrollY > 700);
    }, { passive: true });

    backToTop.addEventListener("click", function () {
        window.scrollTo({ top: 0, behavior: "smooth" });
    });

    document.getElementById("print-course").addEventListener("click", function () {
        window.print();
    });

    updateProgressView();
    filterLessons();
})();
