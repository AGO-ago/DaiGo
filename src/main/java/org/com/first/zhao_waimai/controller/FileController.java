package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.config.UploadProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
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

    /** 占位图字体：优先中文字体，缺失时回退无衬线 */
    private static final String FONT_FAMILY = resolveFontFamily();

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

    /**
     * 占位图（免登录）：外网图片不可用时兜底，按文字生成带首字的渐变色块
     * 例：/file/placeholder?text=川香快餐&amp;size=240
     */
    @GetMapping("/placeholder")
    public ResponseEntity<byte[]> placeholder(@RequestParam(defaultValue = "") String text,
                                              @RequestParam(defaultValue = "240") int size) {
        int side = Math.max(48, Math.min(600, size));
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color base = colorOf(text);
            g.setPaint(new GradientPaint(0, 0, base, side, side, base.darker()));
            g.fillRect(0, 0, side, side);

            String label = text == null ? "" : text.trim();
            if (label.length() > 2) {
                label = label.substring(0, 2);
            }
            if (label.isEmpty()) {
                label = "\u4ee3go";
            }
            g.setColor(new Color(255, 255, 255, 235));
            g.setFont(new Font(FONT_FAMILY, Font.BOLD, Math.round(side * 0.32f)));
            FontMetrics metrics = g.getFontMetrics();
            int x = (side - metrics.stringWidth(label)) / 2;
            int y = (side - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(label, x, y);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", buffer);
        } catch (IOException e) {
            log.error("\u5360\u4f4d\u56fe\u751f\u6210\u5931\u8d25", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "\u5360\u4f4d\u56fe\u751f\u6210\u5931\u8d25");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(buffer.toByteArray());
    }

    /** 按文字取稳定色相，同一档口每次刷新颜色一致 */
    private Color colorOf(String text) {
        int hash = Math.abs(Objects.hashCode(text));
        float hue = (hash % 360) / 360f;
        return Color.getHSBColor(hue, 0.55f, 0.82f);
    }

    /** 挑一个支持中文的系统字体，找不到就用无衬线 */
    private static String resolveFontFamily() {
        try {
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                if ("Microsoft YaHei".equals(name) || "SimHei".equals(name) || "PingFang SC".equals(name)) {
                    return name;
                }
            }
        } catch (Exception e) {
            log.warn("\u83b7\u53d6\u7cfb\u7edf\u5b57\u4f53\u5931\u8d25\uff0c\u56de\u9000\u9ed8\u8ba4\u5b57\u4f53", e);
        }
        return Font.SANS_SERIF;
    }
}
