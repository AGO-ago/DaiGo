package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.AddressRequest;
import org.com.first.zhao_waimai.dto.AddressResponse;
import org.com.first.zhao_waimai.entity.UserAddress;
import org.com.first.zhao_waimai.mapper.UserAddressMapper;
import org.com.first.zhao_waimai.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户地址服务实现
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<AddressResponse> list(Long userId) {
        List<UserAddress> addresses = userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime));
        return addresses.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, AddressRequest request) {
        long count = userAddressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId));
        UserAddress address = new UserAddress();
        applyRequest(address, request);
        address.setUserId(userId);
        // 首个地址自动设为默认
        address.setIsDefault(count == 0 || Boolean.TRUE.equals(request.getIsDefault()) ? 1 : 0);
        userAddressMapper.insert(address);
        if (address.getIsDefault() == 1) {
            clearOtherDefaults(userId, address.getId());
        }
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = getOwned(userId, addressId);
        applyRequest(address, request);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            address.setIsDefault(1);
            clearOtherDefaults(userId, addressId);
        }
        userAddressMapper.updateById(address);
    }

    @Override
    public void delete(Long userId, Long addressId) {
        UserAddress address = getOwned(userId, addressId);
        userAddressMapper.deleteById(address.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long addressId) {
        getOwned(userId, addressId);
        clearOtherDefaults(userId, addressId);
        UserAddress address = new UserAddress();
        address.setId(addressId);
        address.setIsDefault(1);
        userAddressMapper.updateById(address);
    }

    /**
     * 查询本人地址，不存在/非本人统一返回 404，避免暴露他人数据
     */
    private UserAddress getOwned(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return address;
    }

    private void clearOtherDefaults(Long userId, Long excludeAddressId) {
        UserAddress reset = new UserAddress();
        reset.setIsDefault(0);
        userAddressMapper.update(reset, Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .ne(UserAddress::getId, excludeAddressId));
    }

    private void applyRequest(UserAddress address, AddressRequest request) {
        address.setContact(request.getContact());
        address.setCompany(request.getCompany());
        address.setBuilding(request.getBuilding());
        address.setFloor(request.getFloor());
        address.setOffice(request.getOffice());
        address.setAddress(request.getAddress());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setPhone(request.getPhone());
    }

    private AddressResponse toResponse(UserAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .contact(address.getContact())
                .company(address.getCompany())
                .building(address.getBuilding())
                .floor(address.getFloor())
                .office(address.getOffice())
                .address(address.getAddress())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .phone(address.getPhone())
                .isDefault(address.getIsDefault())
                .build();
    }
}
