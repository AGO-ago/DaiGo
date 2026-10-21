package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.CommentRequest;
import org.com.first.zhao_waimai.dto.CommentVO;
import org.com.first.zhao_waimai.entity.OrderComment;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.mapper.OrderCommentMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.service.CommentService;
import org.springframework.stereotype.Service;

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

    private final OrderCommentMapper orderCommentMapper;
    private final OrdersMapper ordersMapper;

    @Override
    public void create(Long userId, CommentRequest request) {
        Orders order = ordersMapper.selectById(request.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != STATUS_FINISHED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单完成后才能评价");
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
