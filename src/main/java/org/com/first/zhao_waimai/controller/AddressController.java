package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.AddressRequest;
import org.com.first.zhao_waimai.dto.AddressResponse;
import org.com.first.zhao_waimai.service.AddressService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户地址接口（写字楼工位）
 */
@RestController
@RequestMapping("/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 地址列表（默认地址置前）
     */
    @GetMapping
    public Result<List<AddressResponse>> list() {
        return Result.ok(addressService.list(UserContext.getUserId()));
    }

    /**
     * 新增地址
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.create(UserContext.getUserId(), request));
    }

    /**
     * 修改地址
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        addressService.update(UserContext.getUserId(), id, request);
        return Result.ok();
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.getUserId(), id);
        return Result.ok();
    }

    /**
     * 设为默认地址
     */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.getUserId(), id);
        return Result.ok();
    }
}
