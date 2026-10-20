package org.com.first.zhao_waimai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.CommonConstants;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.util.JwtUtil;
import org.com.first.zhao_waimai.util.UserContext;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器：校验 JWT 并写入 UserContext
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(CommonConstants.HEADER_TOKEN);
        if (token == null || !token.startsWith(CommonConstants.TOKEN_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtil.parseToken(token.substring(CommonConstants.TOKEN_PREFIX.length()));
        UserContext.set(Long.valueOf(claims.getSubject()), claims.get("role", Integer.class));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}