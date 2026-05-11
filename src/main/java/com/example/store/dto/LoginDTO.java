package com.example.store.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
    private String code; // 用户输入的验证码
    private String uuid; // 这次验证码的唯一标识
}