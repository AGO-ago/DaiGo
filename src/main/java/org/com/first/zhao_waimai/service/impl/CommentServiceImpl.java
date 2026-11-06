package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.CommentRequest;
import org.com.first.zhao_waimai.dto.CommentVO;
import org.com.first.zhao_waimai.entity.OrderComment;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.PlatformConfig;
import org.com.first.zhao_waimai.mapper.OrderCommentMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.service.CommentService;
import org.com.first.zhao_waimai.service.PlatformConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单评价服务实现
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /** 订单已完成状态 */
    private static final int STATUS_FINISHED = 4;
    /** 档口评价最多展示条数 */
    private static final int SHOP_COMMENT_LIMIT = 50;
    /** 评价时限默认天数（平台配置缺失时使用） */
    private static final int DEFAULT_COMMENT_DAYS = 7;

    private final OrderCommentMapper orderCommentMapper;
    private final OrdersMapper ordersMapper;
    private final PlatformConfigService platformConfigService;

    @Override
    public void create(Long userId, CommentRequest request) {
        Orders order = ordersMapper.selectById(request.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != STATUS_FINISHED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单完成后才能评价");
        }
        int commentDays = Math.max(1,
                platformConfigService.getInt(PlatformConfig.KEY_COMMENT_DAYS, DEFAULT_COMMENT_DAYS));
        LocalDateTime deadline = order.getFinishTime();
        if (deadline != null && deadline.plusDays(commentDays).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "订单完成已超过 " + commentDays + " 天，无法评价");
        }
        long exists = orderCommentMapper.selectCount(Wrappers.<OrderComment>lambdaQuery()
                .eq(OrderComment::getOrderId, order.getId()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该订单已评价");
        }
        OrderComment comment = new OrderComment();
        comment.setOrderId(order.getId());
        comment.setUserId(userId);
        comment.setShopId(order.getShopId());
        comment.setScore(request.getScore());
        comment.setContent(request.getContent());
        orderCommentMapper.insert(comment);
    }

    @Override
    public List<CommentVO> listByShop(Long shopId) {
        List<OrderComment> comments = orderCommentMapper.selectList(Wrappers.<OrderComment>lambdaQuery()
                .eq(OrderComment::getShopId, shopId)
                .eq(OrderComment::getHidden, 0)
                .orderByDesc(OrderComment::getId)
                .last("LIMIT " + SHOP_COMMENT_LIMIT));
        return comments.stream().map(c -> CommentVO.builder()
                .id(c.getId())
                .shopId(c.getShopId())
                .score(c.getScore())
                .content(c.getContent())
                .images(c.getImages())
                .userName(maskName(c.getUserId()))
                .createTime(c.getCreateTime())
                .replyContent(c.getReplyContent())
                .replyTime(c.getReplyTime())
                .build()).toList();
    }

    /**
     * 评价人脱敏展示
     */
    private String maskName(Long userId) {
        if (userId == null) {
            return "匿名用户";
        }
        String id = String.valueOf(userId);
        return "用户" + (id.length() > 4 ? id.substring(id.length() - 4) : id);
    }
}
