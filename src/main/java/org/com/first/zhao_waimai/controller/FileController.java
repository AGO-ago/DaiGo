package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.config.UploadProperties;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件上传接口（需登录）
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final UploadProperties uploadProperties;

    /**
     * 上传图片，返回相对访问路径（如 /uploads/2026/10/21/uuid.png）
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        if (!Arrays.asList(uploadProperties.getAllowExts().split(",")).contains(ext)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 " + uploadProperties.getAllowExts());
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = Paths.get(uploadProperties.getDir(), dateDir, filename);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toAbsolutePath());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }
        // context-path=/api，小程序用 baseUrl + 该路径即可访问
        return Result.ok("/uploads/" + dateDir + "/" + filename);
    }
}
