package org.com.first.zhao_waimai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.com.first.zhao_waimai.entity.OrderItem;

import java.time.LocalDateTime;

/**
 * 订单项 Mapper
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 统计用户在指定活动时段内购买的某菜品份数（已取消/已退款不计）
     */
    @Select("SELECT COALESCE(SUM(oi.count), 0) FROM order_item oi "
            + "JOIN orders o ON oi.order_id = o.id "
            + "WHERE o.user_id = #{userId} AND oi.dish_id = #{dishId} "
            + "AND o.status NOT IN (5, 7) AND o.deleted = 0 AND oi.deleted = 0 "
            + "AND o.create_time >= #{startTime} AND o.create_time <= #{endTime}")
    Integer sumUserDishQuantity(@Param("userId") Long userId,
                                @Param("dishId") Long dishId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
