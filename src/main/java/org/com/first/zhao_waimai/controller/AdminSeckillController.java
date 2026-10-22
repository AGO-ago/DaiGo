package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.AdminSeckillVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.dto.SeckillSaveDTO;
import org.com.first.zhao_waimai.service.AdminSeckillService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端秒杀活动接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final AdminSeckillService adminSeckillService;

    @GetMapping("/seckills")
    public Result<PageDTO<AdminSeckillVO>> list(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        AdminGuard.requireAdmin();
        return Result.ok(adminSeckillService.list(keyword, status, page, size));
    }

    @PostMapping("/seckills")
    public Result<Long> create(@Valid @RequestBody SeckillSaveDTO dto) {
        AdminGuard.requireAdmin();
        return Result.ok(adminSeckillService.save(null, dto));
    }

    @PutMapping("/seckills/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SeckillSaveDTO dto) {
        AdminGuard.requireAdmin();
        adminSeckillService.save(id, dto);
        return Result.ok();
    }

    @PutMapping("/seckills/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        AdminGuard.requireAdmin();
        adminSeckillService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/seckills/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AdminGuard.requireAdmin();
        adminSeckillService.delete(id);
        return Result.ok();
    }
}
