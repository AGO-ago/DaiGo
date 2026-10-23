package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.CommentVO;
import org.com.first.zhao_waimai.dto.CouponSaveDTO;
import org.com.first.zhao_waimai.dto.DishCategorySaveDTO;
import org.com.first.zhao_waimai.dto.DishSaveDTO;
import org.com.first.zhao_waimai.dto.DishSpecSaveDTO;
import org.com.first.zhao_waimai.dto.DishSpecVO;
import org.com.first.zhao_waimai.dto.MerchantDishListVO;
import org.com.first.zhao_waimai.dto.MerchantCouponVO;
import org.com.first.zhao_waimai.dto.MerchantOrderVO;
import org.com.first.zhao_waimai.dto.MerchantStatVO;
import org.com.first.zhao_waimai.dto.MerchantApplySaveDTO;
import org.com.first.zhao_waimai.dto.MerchantApplyVO;
import org.com.first.zhao_waimai.dto.ReplyDTO;
import org.com.first.zhao_waimai.dto.ShopUpdateDTO;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.service.MerchantService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端接口（仅商家令牌可用）
 */
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 当前商家绑定的档口（未绑定返回 null）
     */
    @GetMapping("/shop")
    public Result<Shop> myShop() {
        return Result.ok(merchantService.myShop(UserContext.getUserId()));
    }

    /**
     * 修改档口信息
     */
    @PutMapping("/shop")
    public Result<Void> updateShop(@Valid @RequestBody ShopUpdateDTO dto) {
        merchantService.updateShop(UserContext.getUserId(), dto);
        return Result.ok();
    }

    /**
     * 切换营业状态
     */
    @PutMapping("/shop/status")
    public Result<Void> updateShopStatus(@RequestParam Integer status) {
        merchantService.updateShopStatus(UserContext.getUserId(), status);
        return Result.ok();
    }

    /**
     * 档口订单列表
     */
    @GetMapping("/orders")
    public Result<List<MerchantOrderVO>> orders(@RequestParam(required = false) Integer status) {
        return Result.ok(merchantService.listOrders(UserContext.getUserId(), status));
    }

    /**
     * 推进订单状态：accept / prepare / deliver / complete
     */
    @PutMapping("/order/{id}/{action}")
    public Result<Void> updateOrder(@PathVariable Long id, @PathVariable String action) {
        merchantService.updateOrderStatus(UserContext.getUserId(), id, action);
        return Result.ok();
    }

    /**
     * 菜品与分类列表
     */
    @GetMapping("/dishes")
    public Result<MerchantDishListVO> dishes() {
        return Result.ok(merchantService.listDishes(UserContext.getUserId()));
    }

    /**
     * 新增菜品
     */
    @PostMapping("/dish")
    public Result<Long> createDish(@Valid @RequestBody DishSaveDTO dto) {
        return Result.ok(merchantService.saveDish(UserContext.getUserId(), null, dto));
    }

    /**
     * 编辑菜品
     */
    @PutMapping("/dish/{id}")
    public Result<Long> updateDish(@PathVariable Long id, @Valid @RequestBody DishSaveDTO dto) {
        return Result.ok(merchantService.saveDish(UserContext.getUserId(), id, dto));
    }

    /**
     * 删除菜品
     */
    @DeleteMapping("/dish/{id}")
    public Result<Void> deleteDish(@PathVariable Long id) {
        merchantService.deleteDish(UserContext.getUserId(), id);
        return Result.ok();
    }

    /**
     * 菜品上下架
     */
    @PutMapping("/dish/{id}/status")
    public Result<Void> updateDishStatus(@PathVariable Long id, @RequestParam Integer status) {
        merchantService.updateDishStatus(UserContext.getUserId(), id, status);
        return Result.ok();
    }

    /**
     * 新增菜品分类
     */
    @PostMapping("/dish-category")
    public Result<Long> createCategory(@Valid @RequestBody DishCategorySaveDTO dto) {
        return Result.ok(merchantService.saveCategory(UserContext.getUserId(), null, dto));
    }

    /**
     * 编辑菜品分类
     */
    @PutMapping("/dish-category/{id}")
    public Result<Long> updateCategory(@PathVariable Long id, @Valid @RequestBody DishCategorySaveDTO dto) {
        return Result.ok(merchantService.saveCategory(UserContext.getUserId(), id, dto));
    }

    /**
     * 删除菜品分类
     */
    @DeleteMapping("/dish-category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        merchantService.deleteCategory(UserContext.getUserId(), id);
        return Result.ok();
    }

    /**
     * 菜品规格列表（含停用）
     */
    @GetMapping("/specs")
    public Result<List<DishSpecVO>> specs(@RequestParam Long dishId) {
        return Result.ok(merchantService.listSpecs(UserContext.getUserId(), dishId));
    }

    /**
     * 新增菜品规格
     */
    @PostMapping("/specs")
    public Result<Long> createSpec(@RequestParam Long dishId, @Valid @RequestBody DishSpecSaveDTO dto) {
        return Result.ok(merchantService.saveSpec(UserContext.getUserId(), dishId, null, dto));
    }

    /**
     * 编辑菜品规格
     */
    @PutMapping("/specs/{id}")
    public Result<Long> updateSpec(@PathVariable Long id, @RequestParam Long dishId,
                                   @Valid @RequestBody DishSpecSaveDTO dto) {
        return Result.ok(merchantService.saveSpec(UserContext.getUserId(), dishId, id, dto));
    }

    /**
     * 删除菜品规格
     */
    @DeleteMapping("/specs/{id}")
    public Result<Void> deleteSpec(@PathVariable Long id) {
        merchantService.deleteSpec(UserContext.getUserId(), id);
        return Result.ok();
    }

    /**
     * 档口评价
     */
    @GetMapping("/comments")
    public Result<List<CommentVO>> comments() {
        return Result.ok(merchantService.listComments(UserContext.getUserId()));
    }

    /**
     * 回复评价
     */
    @PutMapping("/comment/{id}/reply")
    public Result<Void> replyComment(@PathVariable Long id, @Valid @RequestBody ReplyDTO dto) {
        merchantService.replyComment(UserContext.getUserId(), id, dto.getContent());
        return Result.ok();
    }

    /**
     * 店铺券列表
     */
    @GetMapping("/coupons")
    public Result<List<MerchantCouponVO>> coupons() {
        return Result.ok(merchantService.listCoupons(UserContext.getUserId()));
    }

    /**
     * 新增店铺券
     */
    @PostMapping("/coupons")
    public Result<Long> createCoupon(@Valid @RequestBody CouponSaveDTO dto) {
        return Result.ok(merchantService.saveCoupon(UserContext.getUserId(), null, dto));
    }

    /**
     * 编辑店铺券
     */
    @PutMapping("/coupons/{id}")
    public Result<Long> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponSaveDTO dto) {
        return Result.ok(merchantService.saveCoupon(UserContext.getUserId(), id, dto));
    }

    /**
     * 删除店铺券
     */
    @DeleteMapping("/coupons/{id}")
    public Result<Void> deleteCoupon(@PathVariable Long id) {
        merchantService.deleteCoupon(UserContext.getUserId(), id);
        return Result.ok();
    }

    /**
     * 启用/停用店铺券
     */
    @PutMapping("/coupons/{id}/status")
    public Result<Void> updateCouponStatus(@PathVariable Long id, @RequestParam Integer status) {
        merchantService.updateCouponStatus(UserContext.getUserId(), id, status);
        return Result.ok();
    }

    /**
     * 今日经营统计
     */
    @GetMapping("/stat/today")
    public Result<MerchantStatVO> statToday() {
        return Result.ok(merchantService.statToday(UserContext.getUserId()));
    }

    /**
     * 提交入驻申请
     */
    @PostMapping("/apply")
    public Result<Long> submitApply(@Valid @RequestBody MerchantApplySaveDTO dto) {
        return Result.ok(merchantService.submitApply(UserContext.getUserId(), dto));
    }

    /**
     * 最近一次入驻申请进度
     */
    @GetMapping("/apply")
    public Result<MerchantApplyVO> myApply() {
        return Result.ok(merchantService.myLatestApply(UserContext.getUserId()));
    }
}
