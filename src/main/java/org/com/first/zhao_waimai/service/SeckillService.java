package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.SeckillVO;

import java.util.List;

/**
 * 用户端秒杀
 */
public interface SeckillService {

    /**
     * 进行中的秒杀活动（按结束时间升序）
     */
    List<SeckillVO> listActive();
}
