package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.CommonConstants;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.config.WxProperties;
import org.com.first.zhao_waimai.dto.BindPhoneRequest;
import org.com.first.zhao_waimai.dto.LoginRequest;
import org.com.first.zhao_waimai.dto.LoginResponse;
import org.com.first.zhao_waimai.dto.MerchantLoginRequest;
import org.com.first.zhao_waimai.dto.MerchantPasswordRequest;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.User;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserMapper;
import org.com.first.zhao_waimai.service.AuthService;
import org.com.first.zhao_waimai.util.JwtUtil;
import org.com.first.zhao_waimai.util.PasswordUtil;
import org.com.first.zhao_waimai.util.WxApiClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 鉴权服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 大陆手机号 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    /** 商家网页登录连续失败上限 */
    private static final int MAX_FAIL_COUNT = 5;
    /** 商家网页登录锁定时长 */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);
    /** 商家网页登录失败计数 key 前缀 */
    private static final String FAIL_KEY_PREFIX = "merchant:login:fail:";

    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final JwtUtil jwtUtil;
    private final WxProperties wxProperties;
    private final WxApiClient wxApiClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request.getRole() != CommonConstants.ROLE_USER
                && request.getRole() != CommonConstants.ROLE_MERCHANT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法角色");
        }

        String openid = resolveOpenid(request.getCode(), request.getDeviceId());
        User user = upsertUser(openid);
        bindPhoneIfPresent(user, request.getPhone(), request.getPhoneCode());

        String token = jwtUtil.createToken(user.getId(), request.getRole());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .role(request.getRole())
                .build();
    }

    @Override
    public LoginResponse merchantLogin(MerchantLoginRequest request) {
        String phone = request.getPhone().trim();
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式不正确");
        }
        String failKey = FAIL_KEY_PREFIX + phone;
        String failCount = stringRedisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_FAIL_COUNT) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "登录失败次数过多，请 10 分钟后再试");
        }

        List<User> candidates = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone)
                .isNotNull(User::getPassword));
        User matched = null;
        Shop matchedShop = null;
        boolean passwordMatched = false;
        for (User candidate : candidates) {
            if (candidate.getStatus() != null && candidate.getStatus() == 0) {
                continue;
            }
            if (!PasswordUtil.matches(request.getPassword(), candidate.getSalt(), candidate.getPassword())) {
                continue;
            }
            passwordMatched = true;
            Shop shop = firstShop(candidate.getId());
            if (shop != null) {
                matched = candidate;
                matchedShop = shop;
                break;
            }
        }
        if (matched == null) {
            recordLoginFailure(failKey);
            if (passwordMatched) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该账号未绑定档口，请联系平台开通");
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号或密码错误");
        }

        stringRedisTemplate.delete(failKey);
        log.info("商家网页端登录成功, userId={}, shopId={}", matched.getId(), matchedShop.getId());
        return LoginResponse.builder()
                .token(jwtUtil.createToken(matched.getId(), CommonConstants.ROLE_MERCHANT))
                .userId(matched.getId())
                .nickname(matched.getNickname())
                .avatar(matched.getAvatar())
                .phone(matched.getPhone())
                .role(CommonConstants.ROLE_MERCHANT)
                .shopId(matchedShop.getId())
                .shopName(matchedShop.getName())
                .build();
    }

    @Override
    public void setMerchantPassword(Long userId, MerchantPasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(user.getPassword())
                && !PasswordUtil.matches(request.getOldPassword(), user.getSalt(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "原密码错误");
        }
        String salt = PasswordUtil.generateSalt();
        User update = new User();
        update.setId(userId);
        update.setSalt(salt);
        update.setPassword(PasswordUtil.hash(request.getNewPassword(), salt));
        userMapper.updateById(update);
        log.info("商家网页登录密码已更新, userId={}", userId);
    }

    @Override
    public boolean hasPassword(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && StringUtils.hasText(user.getPassword());
    }

    /**
     * 查询商家绑定的首个档口
     */
    private Shop firstShop(Long merchantUserId) {
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getMerchantUserId, merchantUserId)
                .last("LIMIT 1"));
        return shops.isEmpty() ? null : shops.get(0);
    }

    /**
     * 记录商家登录失败次数，首次失败设置 10 分钟有效期
     */
    private void recordLoginFailure(String failKey) {
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, LOCK_DURATION);
        }
    }

    @Override
    public String bindPhone(Long userId, BindPhoneRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        String phone = resolvePhone(request.getPhone(), request.getPhoneCode());
        if (phone == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未获取到手机号，请重新授权");
        }
        User update = new User();
        update.setId(userId);
        update.setPhone(phone);
        userMapper.updateById(update);
        return phone;
    }

    private void bindPhoneIfPresent(User user, String phone, String phoneCode) {
        String resolved = resolvePhone(phone, phoneCode);
        if (resolved == null || resolved.equals(user.getPhone())) {
            return;
        }
        User update = new User();
        update.setId(user.getId());
        update.setPhone(resolved);
        userMapper.updateById(update);
        user.setPhone(resolved);
    }

    /**
     * 解析手机号：mock 模式直接使用传入手机号；生产模式用 getPhoneNumber 令牌换取
     */
    private String resolvePhone(String phone, String phoneCode) {
        if (!StringUtils.hasText(phone) && !StringUtils.hasText(phoneCode)) {
            return null;
        }
        if (wxProperties.isMockEnabled()) {
            if (!StringUtils.hasText(phone)) {
                log.warn("mock 模式未传手机号，忽略手机号绑定");
                return null;
            }
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式不正确");
            }
            return phone;
        }
        if (!StringUtils.hasText(phoneCode)) {
            return null;
        }
        return wxApiClient.getPhoneNumber(phoneCode);
    }

    /**
     * 解析 openid：mock 模式优先用设备标识生成稳定假 openid（本地联调），生产用 wx.login code 换 openid
     */
    private String resolveOpenid(String code, String deviceId) {
        if (wxProperties.isMockEnabled()) {
            String seed = StringUtils.hasText(deviceId) ? deviceId : code;
            return "mock-" + DigestUtils.md5DigestAsHex(seed.getBytes(StandardCharsets.UTF_8));
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "登录凭证不能为空");
        }
        return wxApiClient.code2session(code);
    }

    /**
     * 按 openid 查用户，不存在则注册
     */
    private User upsertUser(String openid) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid));
        if (user != null) {
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号已被禁用");
            }
            return user;
        }
        user = new User();
        user.setOpenid(openid);
        user.setNickname("用户" + openid.substring(Math.max(0, openid.length() - 4)));
        user.setStatus(1);
        userMapper.insert(user);
        log.info("新用户注册, userId={}", user.getId());
        return user;
    }
}
