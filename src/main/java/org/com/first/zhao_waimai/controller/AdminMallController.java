package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.AdminMallSaveDTO;
import org.com.first.zhao_waimai.entity.Mall;
import org.com.first.zhao_waimai.service.AdminMallService;
import org.com.first.zhao_waimai.util.AdminGuard;
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
 * 平台端美食城管理接口
 */
@RestController
@RequestMapping("/admin/mall")
@RequiredArgsConstructor
public class AdminMallController {

    private final AdminMallService adminMallService;

    @GetMapping
    public Result<List<Mall>> list() {
        AdminGuard.requireAdmin();
        return Result.ok(adminMallService.list());
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdminMallSaveDTO dto) {
        AdminGuard.requireAdmin();
        return Result.ok(adminMallService.save(null, dto));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody AdminMallSaveDTO dto) {
        AdminGuard.requireAdmin();
        return Result.ok(adminMallService.save(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AdminGuard.requireAdmin();
        adminMallService.delete(id);
        return Result.ok();
    }
}
