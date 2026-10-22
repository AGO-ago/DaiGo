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

    /** 免登录放行路径（登录接口、公开浏览、接口文档、上传文件静态访问） */
    private static final String[] AUTH_WHITE_LIST = {
            "/auth/login",
            "/admin/auth/login",
            "/error",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/uploads/**",
            "/mall/**",
            "/shop/**",
            "/dish/**",
            "/search",
            "/seckill/**",
            "/comment/shop/**"
    };

    private final AuthInterceptor authInterceptor;
    private final UploadProperties uploadProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
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
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
