package com.example.myblog.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentForm {

    @NotBlank(message = "请写下您的名字")
    @Size(max = 90, message = "名称不能超过90个字符")
    private String name;

    @NotBlank(message = "请写下您想说的话")
    private String content;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
