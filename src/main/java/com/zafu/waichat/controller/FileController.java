package com.zafu.waichat.controller;

import com.zafu.waichat.util.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@Api(tags = "文件相关接口")
public class FileController {
    @PostMapping("/upload")
    public Result uploadAudio(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("上传文件不能为空");
        try {
            // 获取项目根目录的绝对路径 (例如 D:\MyProject\)
            String projectPath = System.getProperty("user.dir");
            // 设定保存的文件夹
            String uploadDir = projectPath + File.separator + "uploads" + File.separator + "audio" + File.separator;
            File dir = new File(uploadDir);
            // 核心：确保物理磁盘上真正创建了这些文件夹
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                log.info("目录不存在，创建目录: {}, 结果: {}", uploadDir, created);
            }
            // 生成唯一文件名
            String fileName = UUID.randomUUID() + ".webm";
            // 必须使用绝对路径构建文件对象
            File destFile = new File(dir, fileName).getAbsoluteFile();
            log.info("文件将保存至: {}", destFile.getPath());
            // 执行保存
            file.transferTo(destFile);
            // 返回可访问的 URL
//            String url = "http://localhost:8080/uploads/audio/" + fileName;
            String url = "http://116.62.163.226:8080/uploads/audio/" + fileName;
            return Result.success(url);
        } catch (Exception e) {
            log.error("语音上传实际失败原因: ", e);
            return Result.error("语音上传失败: " + e.getMessage());
        }
    }
}
