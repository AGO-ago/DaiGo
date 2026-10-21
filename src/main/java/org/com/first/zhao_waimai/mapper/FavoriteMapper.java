package org.com.first.zhao_waimai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.com.first.zhao_waimai.entity.Favorite;

/**
 * 收藏 Mapper
 */
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 收藏（幂等）：唯一索引冲突时恢复逻辑删除标记，避免取消收藏后无法再次收藏
     */
    @Insert("INSERT INTO favorite (user_id, shop_id, type) VALUES (#{userId}, #{shopId}, 1) "
            + "ON DUPLICATE KEY UPDATE deleted = 0, update_time = NOW()")
    int upsert(@Param("userId") Long userId, @Param("shopId") Long shopId);
}
