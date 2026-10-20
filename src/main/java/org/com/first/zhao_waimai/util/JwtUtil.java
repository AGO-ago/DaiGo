package org.com.first.zhao_waimai.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析登录令牌
 */
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发令牌
     *
     * @param userId 用户ID
     * @param role   角色（CommonConstants.ROLE_*）
     */
    public String createToken(Long userId, int role) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpireSeconds() * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌，非法/过期抛业务异常
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}