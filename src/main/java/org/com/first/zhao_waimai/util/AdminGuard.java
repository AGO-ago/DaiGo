package org.com.first.zhao_waimai.util;

import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.CommonConstants;
import org.com.first.zhao_waimai.common.ErrorCode;

/**
 * 平台管理员权限守卫：与商家端 requireMerchantRole 风格一致，不引入 Spring Security
 */
public final class AdminGuard {

    private AdminGuard() {
    }

    /**
     * 校验当前令牌为平台管理员，返回管理员 id
     */
    public static Long requireAdmin() {
        Integer role = UserContext.getRole();
        if (role == null || role != CommonConstants.ROLE_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请使用平台管理员账号登录");
        }
        Long adminId = UserContext.getUserId();
        if (adminId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return adminId;
    }
}
