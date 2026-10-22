package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.SeckillVO;
import org.com.first.zhao_waimai.service.SeckillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端秒杀接口
 */
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @GetMapping("/list")
    public Result<List<SeckillVO>> list() {
        return Result.ok(seckillService.listActive());
    }
}
