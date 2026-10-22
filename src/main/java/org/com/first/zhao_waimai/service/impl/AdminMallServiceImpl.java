package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.AdminMallSaveDTO;
import org.com.first.zhao_waimai.entity.Mall;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.mapper.MallMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.service.AdminMallService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台端美食城管理实现
 */
@Service
@RequiredArgsConstructor
public class AdminMallServiceImpl implements AdminMallService {

    private final MallMapper mallMapper;
    private final ShopMapper shopMapper;

    @Override
    public List<Mall> list() {
        AdminGuard.requireAdmin();
        return mallMapper.selectList(Wrappers.<Mall>lambdaQuery().orderByAsc(Mall::getId));
    }

    @Override
    public Long save(Long mallId, AdminMallSaveDTO dto) {
        AdminGuard.requireAdmin();
        if (mallId == null) {
            Mall mall = new Mall();
            mall.setName(dto.getName());
            mall.setAddress(dto.getAddress());
            mall.setLongitude(dto.getLongitude());
            mall.setLatitude(dto.getLatitude());
            mall.setCoverImg(dto.getCoverImg());
            mall.setBusinessHours(dto.getBusinessHours());
            mall.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            mallMapper.insert(mall);
            return mall.getId();
        }
        Mall exists = mallMapper.selectById(mallId);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "美食城不存在");
        }
        Mall update = new Mall();
        update.setId(mallId);
        update.setName(dto.getName());
        update.setAddress(dto.getAddress());
        update.setLongitude(dto.getLongitude());
        update.setLatitude(dto.getLatitude());
        update.setCoverImg(dto.getCoverImg());
        update.setBusinessHours(dto.getBusinessHours());
        update.setStatus(dto.getStatus() == null ? exists.getStatus() : dto.getStatus());
        mallMapper.updateById(update);
        return mallId;
    }

    @Override
    public void delete(Long mallId) {
        AdminGuard.requireAdmin();
        Mall exists = mallMapper.selectById(mallId);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "美食城不存在");
        }
        long shopCount = shopMapper.selectCount(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getMallId, mallId));
        if (shopCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该美食城下还有档口，无法删除");
        }
        mallMapper.deleteById(mallId);
    }
}
