package org.com.first.zhao_waimai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.config.WxProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 微信开放接口客户端：登录换 openid、手机号一键授权
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxApiClient {

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String PHONE_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={token}";

    private final WxProperties wxProperties;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 接口调用凭证缓存 */
    private volatile String accessToken;
    private volatile long accessTokenExpireAt;

    /**
     * 用小程序登录 code 换取 openid
     *
     * @param code wx.login 获取的临时凭证
     * @return openid
     */
    public String code2session(String code) {
        String body = restClient.get()
                .uri(JSCODE2SESSION_URL, wxProperties.getAppid(), wxProperties.getSecret(), code)
                .retrieve()
                .body(String.class);
        try {
            Map<String, Object> result = objectMapper.readValue(body, Map.class);
            Object errcode = result.get("errcode");
            String openid = (String) result.get("openid");
            if (errcode != null && ((Number) errcode).intValue() != 0 || openid == null) {
                log.warn("code2session 失败, errcode={}, errmsg={}", errcode, result.get("errmsg"));
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信登录失败，请重试");
            }
            return openid;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("code2session 响应解析失败", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信登录失败，请重试");
        }
    }

    /**
     * 用 getPhoneNumber 返回的 code 换取手机号
     *
     * @param code 手机号授权动态令牌
     * @return 纯手机号（不含区号）
     */
    public String getPhoneNumber(String code) {
        String body = restClient.post()
                .uri(PHONE_URL, getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("code", code))
                .retrieve()
                .body(String.class);
        try {
            Map<String, Object> result = objectMapper.readValue(body, Map.class);
            Object errcode = result.get("errcode");
            if (errcode != null && ((Number) errcode).intValue() != 0) {
                log.warn("getPhoneNumber 失败, errcode={}, errmsg={}", errcode, result.get("errmsg"));
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号授权失败，请重试");
            }
            Object info = result.get("phone_info");
            if (!(info instanceof Map<?, ?> phoneInfo)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号授权失败，请重试");
            }
            Object purePhone = phoneInfo.get("purePhoneNumber");
            String phone = purePhone == null ? (String) phoneInfo.get("phoneNumber") : purePhone.toString();
            if (phone == null || phone.isBlank()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号授权失败，请重试");
            }
            return phone;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("getPhoneNumber 响应解析失败", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号授权失败，请重试");
        }
    }

    /**
     * 获取并缓存小程序接口调用凭证
     */
    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < accessTokenExpireAt) {
            return accessToken;
        }
        synchronized (this) {
            if (accessToken != null && System.currentTimeMillis() < accessTokenExpireAt) {
                return accessToken;
            }
            String body = restClient.get()
                    .uri(ACCESS_TOKEN_URL, wxProperties.getAppid(), wxProperties.getSecret())
                    .retrieve()
                    .body(String.class);
            try {
                Map<String, Object> result = objectMapper.readValue(body, Map.class);
                String token = (String) result.get("access_token");
                if (token == null) {
                    log.warn("获取 access_token 失败, errcode={}, errmsg={}", result.get("errcode"), result.get("errmsg"));
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信服务暂不可用，请重试");
                }
                Number expiresIn = (Number) result.getOrDefault("expires_in", 7200);
                accessToken = token;
                // 提前 5 分钟过期，避免临界失效
                accessTokenExpireAt = System.currentTimeMillis() + (expiresIn.longValue() - 300) * 1000;
                return token;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("access_token 响应解析失败", e);
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信服务暂不可用，请重试");
            }
        }
    }
}
