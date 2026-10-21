package org.com.first.zhao_waimai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.config.WxProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 微信开放接口客户端：code2session 换取 openid
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxApiClient {

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    private final WxProperties wxProperties;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
}
