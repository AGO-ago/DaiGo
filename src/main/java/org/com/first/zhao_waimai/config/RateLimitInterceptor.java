package org.com.first.zhao_waimai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 登录接口限流：同一 IP 在窗口期内超过阈值直接拒绝，防撞库与刷接口
 * Redis 异常时放行（fail-open），不影响正常登录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 统计窗口（秒） */
    private static final long WINDOW_SECONDS = 60L;
    /** 单 IP 窗口内最大请求次数 */
    private static final long MAX_REQUESTS = 20L;
    /** 限流键前缀 */
    private static final String KEY_PREFIX = "rate:limit:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = KEY_PREFIX + request.getRequestURI() + ":" + clientIp(request);
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }
            if (count != null && count > MAX_REQUESTS) {
                log.warn("接口访问过于频繁，已限流: {}", key);
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "操作过于频繁，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("限流检查异常，放行本次请求: {}", e.getMessage());
        }
        return true;
    }

    /**
     * 真实客户端 IP：优先代理头，其次连接地址
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
