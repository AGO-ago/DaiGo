package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.AdminCommentVO;
import org.com.first.zhao_waimai.dto.CommentHideDTO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.dto.ReplyDTO;
import org.com.first.zhao_waimai.service.AdminCommentService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端评价处理接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    @GetMapping("/comments")
    public Result<PageDTO<AdminCommentVO>> list(@RequestParam(required = false) Integer minScore,
                                                @RequestParam(required = false) Integer maxScore,
                                                @RequestParam(required = false) Integer hidden,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        AdminGuard.requireAdmin();
        return Result.ok(adminCommentService.list(minScore, maxScore, hidden, keyword, page, size));
    }

    @PutMapping("/comment/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody ReplyDTO dto) {
        AdminGuard.requireAdmin();
        adminCommentService.reply(id, dto.getContent());
        return Result.ok();
    }

    @PutMapping("/comment/{id}/hide")
    public Result<Void> hide(@PathVariable Long id, @Valid @RequestBody CommentHideDTO dto) {
        AdminGuard.requireAdmin();
        adminCommentService.hide(id, dto);
        return Result.ok();
    }
}
