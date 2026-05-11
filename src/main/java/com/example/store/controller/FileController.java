package com.example.store.controller;

import com.example.store.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/file")
public class FileController {

    //定义物理存储路径
    private static final String UPLOAD_DIR = "D:/1A/store_imgs/";

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public Result upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return new Result().fail("上传文件不能为空");
        }

        // 检查目录是否存在，不存在则创建
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //生成新的文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + suffix;

        // 保存文件
        File dest = new File(UPLOAD_DIR + newFileName);
        file.transferTo(dest);

        // 返回相对路径
        String url = "/image/" + newFileName;
        return new Result().success("上传成功").setData(url);
    }
}