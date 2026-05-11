package com.example.store.dto;

import lombok.Data;

/**
 * 用户分页查询 DTO
 * 用于接收前端传递的查询参数
 */
@Data
public class UserPageDTO {
    // 当前页码，默认 1
    private Integer pageNum = 1;

    // 每页条数，默认 10
    private Integer pageSize = 10;

    // 用户类型 (2=信息管理员, 3=前端用户)
    private Integer type;

    // 搜索关键词 (用户名/昵称)
    private String keyword;
}