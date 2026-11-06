package org.com.first.zhao_waimai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置：拦截器注册 + 跨域（开发期小程序真机调试可能用）+ 上传文件静态映射
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** 免登录放行路径（登录接口、公开浏览、接口文档、健康检查、上传文件静态访问） */
    private static final String[] AUTH_WHITE_LIST = {
            "/auth/login",
            "/auth/merchant/login",
            "/admin/auth/login",
            "/error",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/uploads/**",
            "/file/placeholder",
            "/actuator/**",
            "/mall/**",
            "/shop/**",
            "/dish/**",
            "/search",
            "/seckill/**",
            "/config/public",
            "/comment/shop/**"
    };

    /** 需要限流的写入口（防撞库/防刷） */
    private static final String[] RATE_LIMIT_PATHS = {
            "/auth/login",
            "/auth/merchant/login",
            "/admin/auth/login"
    };

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final UploadProperties uploadProperties;
    private final CorsProperties corsProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流先于鉴权，避免恶意请求打穿到业务层
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns(RATE_LIMIT_PATHS);
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(AUTH_WHITE_LIST);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** 映射到本地上传目录（图片访问免登录，见白名单）
        String location = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 白名单从配置读取（默认本机任意端口），避免 allowedOriginPatterns("*") + 携带凭证的隐患
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
