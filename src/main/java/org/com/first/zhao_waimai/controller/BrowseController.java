package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.DishTreeVO;
import org.com.first.zhao_waimai.dto.HotDishVO;
import org.com.first.zhao_waimai.dto.MallVO;
import org.com.first.zhao_waimai.dto.SearchResultVO;
import org.com.first.zhao_waimai.dto.ShopVO;
import org.com.first.zhao_waimai.service.BrowseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 主数据浏览接口（美食城/档口/菜品）
 */
@RestController
@RequiredArgsConstructor
public class BrowseController {

    private final BrowseService browseService;

    /**
     * 营业中的美食城列表（lat/lng 传当前位置则返回距离）
     */
    @GetMapping("/mall/list")
    public Result<List<MallVO>> listMalls(@RequestParam(required = false) Double lat,
                                          @RequestParam(required = false) Double lng) {
        return Result.ok(browseService.listMalls(lat, lng));
    }

    /**
     * 美食城详情（进入美食城后的头部信息）
     */
    @GetMapping("/mall/{id}")
    public Result<MallVO> getMall(@PathVariable Long id,
                                  @RequestParam(required = false) Double lat,
                                  @RequestParam(required = false) Double lng) {
        return Result.ok(browseService.getMall(id, lat, lng));
    }

    /**
     * 美食城下档口列表
     */
    @GetMapping("/mall/{id}/shops")
    public Result<List<ShopVO>> listShops(@PathVariable Long id) {
        return Result.ok(browseService.listShops(id));
    }

    /**
     * 档口菜品树（按分类分组，仅上架）
     */
    @GetMapping("/shop/{id}/dishes")
    public Result<List<DishTreeVO>> getDishTree(@PathVariable Long id) {
        return Result.ok(browseService.getDishTree(id));
    }

    /**
     * 热销菜品（首页今日抢购）
     */
    @GetMapping("/dish/hot")
    public Result<List<HotDishVO>> listHotDishes(@RequestParam(defaultValue = "6") int limit,
                                                 @RequestParam(required = false) Double lat,
                                                 @RequestParam(required = false) Double lng) {
        return Result.ok(browseService.listHotDishes(limit, lat, lng));
    }

    /**
     * 搜索档口与菜品
     */
    @GetMapping("/search")
    public Result<SearchResultVO> search(@RequestParam String keyword,
                                         @RequestParam(required = false) Double lat,
                                         @RequestParam(required = false) Double lng) {
        return Result.ok(browseService.search(keyword, lat, lng));
    }
}
