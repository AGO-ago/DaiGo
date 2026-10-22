package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.AdminCommentVO;
import org.com.first.zhao_waimai.dto.CommentHideDTO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.entity.OrderComment;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.User;
import org.com.first.zhao_waimai.mapper.OrderCommentMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserMapper;
import org.com.first.zhao_waimai.service.AdminCommentService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台端评价处理实现
 */
@Service
@RequiredArgsConstructor
public class AdminCommentServiceImpl implements AdminCommentService {

    private final OrderCommentMapper orderCommentMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;

    @Override
    public PageDTO<AdminCommentVO> list(Integer minScore, Integer maxScore, Integer hidden,
                                        String keyword, long page, long size) {
        AdminGuard.requireAdmin();
        Page<OrderComment> result = orderCommentMapper.selectPage(new Page<>(page, size),
                Wrappers.<OrderComment>lambdaQuery()
                        .ge(minScore != null, OrderComment::getScore, minScore)
                        .le(maxScore != null, OrderComment::getScore, maxScore)
                        .eq(hidden != null, OrderComment::getHidden, hidden)
                        .like(keyword != null && !keyword.isBlank(), OrderComment::getContent, keyword)
                        .orderByDesc(OrderComment::getId));
        List<OrderComment> comments = result.getRecords();
        Map<Long, String> shopNames = loadShopNames(comments);
        Map<Long, String> userNames = loadUserNames(comments);
        PageDTO<AdminCommentVO> dto = new PageDTO<>();
        dto.setList(comments.stream().map(c -> toVO(c, shopNames.get(c.getShopId()),
                userNames.get(c.getUserId()))).toList());
        dto.setTotal(result.getTotal());
        dto.setPage(page);
        dto.setSize(size);
        return dto;
    }

    @Override
    public void reply(Long commentId, String content) {
        AdminGuard.requireAdmin();
        requireComment(commentId);
        OrderComment update = new OrderComment();
        update.setId(commentId);
        update.setAdminReply(content);
        orderCommentMapper.updateById(update);
    }

    @Override
    public void hide(Long commentId, CommentHideDTO dto) {
        AdminGuard.requireAdmin();
        requireComment(commentId);
        if (dto.getHidden() == 1 && (dto.getReason() == null || dto.getReason().isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "隐藏评价需填写原因");
        }
        OrderComment update = new OrderComment();
        update.setId(commentId);
        update.setHidden(dto.getHidden());
        update.setHiddenReason(dto.getHidden() == 1 ? dto.getReason().trim() : null);
        orderCommentMapper.updateById(update);
    }

    private OrderComment requireComment(Long commentId) {
        OrderComment comment = orderCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return comment;
    }

    private Map<Long, String> loadShopNames(List<OrderComment> comments) {
        List<Long> ids = comments.stream().map(OrderComment::getShopId).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, ids))) {
            map.put(shop.getId(), shop.getName());
        }
        return map;
    }

    private Map<Long, String> loadUserNames(List<OrderComment> comments) {
        List<Long> ids = comments.stream().map(OrderComment::getUserId).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (User user : userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids))) {
            map.put(user.getId(), user.getNickname());
        }
        return map;
    }

    private AdminCommentVO toVO(OrderComment comment, String shopName, String userName) {
        AdminCommentVO vo = new AdminCommentVO();
        vo.setId(comment.getId());
        vo.setOrderId(comment.getOrderId());
        vo.setShopId(comment.getShopId());
        vo.setShopName(shopName);
        vo.setUserId(comment.getUserId());
        vo.setUserName(userName);
        vo.setScore(comment.getScore());
        vo.setContent(comment.getContent());
        vo.setImages(comment.getImages());
        vo.setReplyContent(comment.getReplyContent());
        vo.setReplyTime(comment.getReplyTime());
        vo.setAdminReply(comment.getAdminReply());
        vo.setHidden(comment.getHidden());
        vo.setHiddenReason(comment.getHiddenReason());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
