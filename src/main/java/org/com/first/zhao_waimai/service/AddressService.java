package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.AddressRequest;
import org.com.first.zhao_waimai.dto.AddressResponse;

import java.util.List;

/**
 * 用户地址服务
 */
public interface AddressService {

    /**
     * 查询当前用户地址列表（默认地址置前）
     */
    List<AddressResponse> list(Long userId);

    /**
     * 新增地址（首个地址自动设为默认）
     *
     * @return 新地址 id
     */
    Long create(Long userId, AddressRequest request);

    /**
     * 修改地址（校验归属）
     */
    void update(Long userId, Long addressId, AddressRequest request);

    /**
     * 删除地址（校验归属）
     */
    void delete(Long userId, Long addressId);

    /**
     * 设为默认地址
     */
    void setDefault(Long userId, Long addressId);
}
