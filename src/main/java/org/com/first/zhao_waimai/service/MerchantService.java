package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.CommentVO;
import org.com.first.zhao_waimai.dto.DishCategorySaveDTO;
import org.com.first.zhao_waimai.dto.DishSaveDTO;
import org.com.first.zhao_waimai.dto.MerchantDishListVO;
import org.com.first.zhao_waimai.dto.MerchantOrderVO;
import org.com.first.zhao_waimai.dto.MerchantStatVO;
import org.com.first.zhao_waimai.dto.ShopUpdateDTO;
import org.com.first.zhao_waimai.entity.Shop;

import java.util.List;

/**
 * 商家端服务
 */
public interface MerchantService {

    /** 当前商家绑定的档口，未绑定时返回 null */
    Shop myShop(Long userId);

    /** 可绑定的档口（演示环境：未被其他账号绑定的档口） */
    List<Shop> bindableShops();

    /** 绑定档口（一个商家账号仅能绑定一个档口） */
    void bindShop(Long userId, Long shopId);

    /** 修改档口信息 */
    void updateShop(Long userId, ShopUpdateDTO dto);

    /** 切换营业状态：0 休息 1 营业 */
    void updateShopStatus(Long userId, Integer status);

    /** 档口订单列表（status 为空查全部） */
    List<MerchantOrderVO> listOrders(Long userId, Integer status);

    /** 推进订单状态：accept/prepare/deliver/complete */
    void updateOrderStatus(Long userId, Long orderId, String action);

    /** 菜品 + 分类列表 */
    MerchantDishListVO listDishes(Long userId);

    /** 新增/编辑菜品，返回菜品 id */
    Long saveDish(Long userId, Long dishId, DishSaveDTO dto);

    /** 删除菜品（逻辑删除） */
    void deleteDish(Long userId, Long dishId);

    /** 菜品上下架：0 下架 1 上架 */
    void updateDishStatus(Long userId, Long dishId, Integer status);

    /** 新增/编辑菜品分类，返回分类 id */
    Long saveCategory(Long userId, Long categoryId, DishCategorySaveDTO dto);

    /** 删除菜品分类（分类下有菜品时禁止删除） */
    void deleteCategory(Long userId, Long categoryId);

    /** 档口收到的评价 */
    List<CommentVO> listComments(Long userId);

    /** 今日经营统计 */
    MerchantStatVO statToday(Long userId);
}
