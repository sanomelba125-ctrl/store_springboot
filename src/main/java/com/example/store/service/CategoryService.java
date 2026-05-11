package com.example.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.store.entity.Category;
import com.example.store.utils.Result;

public interface CategoryService extends IService<Category> {
    // 获取分类树形结构
    Result getCategoryTree();
}