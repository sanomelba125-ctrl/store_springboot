package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.entity.Category;
import com.example.store.mapper.CategoryMapper;
import com.example.store.service.CategoryService;
import com.example.store.utils.Result;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public Result getCategoryTree() {
        // 查出所有未删除的分类
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        wrapper.orderByAsc("sort"); // 按排序权重排序
        List<Category> allList = this.list(wrapper);

        // 组装成树形结构
        List<Category> rootList = allList.stream()
                .filter(cat -> "0".equals(cat.getParentId()))
                .map(root -> {
                    root.setChildren(getChildren(root, allList));
                    return root;
                })
                .collect(Collectors.toList());

        return new Result().success().setData(rootList);
    }

    // 递归查找子节点
    private List<Category> getChildren(Category root, List<Category> allList) {
        return allList.stream()
                .filter(cat -> cat.getParentId().equals(root.getId()))
                .map(cat -> {
                    cat.setChildren(getChildren(cat, allList));
                    return cat;
                })
                .collect(Collectors.toList());
    }
}