package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.ShopVO;
import org.com.first.zhao_waimai.service.FavoriteService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收藏接口（档口）
 */
@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 我的收藏列表
     */
    @GetMapping
    public Result<List<ShopVO>> list() {
        return Result.ok(favoriteService.list(UserContext.getUserId()));
    }

    /**
     * 已收藏档口 id 列表（详情页回显）
     */
    @GetMapping("/ids")
    public Result<List<Long>> ids() {
        return Result.ok(favoriteService.shopIds(UserContext.getUserId()));
    }

    /**
     * 收藏档口
     */
    @PostMapping("/{shopId}")
    public Result<Void> add(@PathVariable Long shopId) {
        favoriteService.add(UserContext.getUserId(), shopId);
        return Result.ok();
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{shopId}")
    public Result<Void> remove(@PathVariable Long shopId) {
        favoriteService.remove(UserContext.getUserId(), shopId);
        return Result.ok();
    }
}
