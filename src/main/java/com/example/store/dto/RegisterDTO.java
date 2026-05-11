package com.example.store.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private String mobile;
    private String email;
    private Integer sex; // 0=女, 1=男, 2=保密
}