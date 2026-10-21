package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.CommentRequest;
import org.com.first.zhao_waimai.dto.CommentVO;
import org.com.first.zhao_waimai.service.CommentService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单评价接口
 */
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 提交评价
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CommentRequest request) {
        commentService.create(UserContext.getUserId(), request);
        return Result.ok();
    }

    /**
     * 档口评价列表（公开）
     */
    @GetMapping("/shop/{shopId}")
    public Result<List<CommentVO>> listByShop(@PathVariable Long shopId) {
        return Result.ok(commentService.listByShop(shopId));
    }
}
