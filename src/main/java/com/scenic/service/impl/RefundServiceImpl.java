package com.scenic.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.common.exception.BusinessException;
import com.scenic.dto.RefundApplyDTO;
import com.scenic.dto.RefundAuditDTO;
import com.scenic.entity.*;
import com.scenic.mapper.*;
import com.scenic.service.RefundService;
import com.scenic.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final TicketOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final RefundMapper refundMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional
    public void applyRefund(Long userId, RefundApplyDTO dto) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, dto.getOrderNo())
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("只有已支付的订单才能申请退款");
        }

        // 检查是否已有退款记录
        Long count = refundMapper.selectCount(
                new LambdaQueryWrapper<Refund>().eq(Refund::getOrderId, order.getId())
        );
        if (count > 0) {
            throw new BusinessException("该订单已有退款申请，请勿重复提交");
        }

        String refundNo = "RF" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") +
                IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();

        Refund refund = new Refund();
        refund.setOrderId(order.getId());
        refund.setRefundNo(refundNo);
        refund.setRefundAmount(order.getTotalAmount());
        refund.setReason(dto.getReason());
        refund.setStatus(0); // 待审核
        refundMapper.insert(refund);

        log.info("退款申请已提交: {}", refundNo);
    }

    @Override
    public Page<RefundVO> getRefundList(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<TicketOrder> orderWrapper = new LambdaQueryWrapper<TicketOrder>()
                .eq(TicketOrder::getUserId, userId);
        List<TicketOrder> userOrders = orderMapper.selectList(orderWrapper);
        List<Long> orderIds = userOrders.stream().map(TicketOrder::getId).collect(Collectors.toList());

        if (orderIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<Refund>()
                .in(Refund::getOrderId, orderIds)
                .orderByDesc(Refund::getCreateTime);

        Page<Refund> refundPage = refundMapper.selectPage(new Page<>(page, size), wrapper);
        return buildRefundVOPage(refundPage);
    }

    @Override
    @Transactional
    public void auditRefund(Long auditorId, RefundAuditDTO dto) {
        Refund refund = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>().eq(Refund::getRefundNo, dto.getRefundNo())
        );
        if (refund == null) {
            throw new BusinessException("退款记录不存在");
        }
        if (refund.getStatus() != 0) {
            throw new BusinessException("该退款已处理");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            refund.setStatus(1); // 已通过
            // 更新订单状态
            TicketOrder order = orderMapper.selectById(refund.getOrderId());
            if (order != null) {
                order.setStatus(3); // 已退款
                orderMapper.updateById(order);

                // 回退库存：退款通过后将已售数量减回去
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
                );
                for (OrderItem item : items) {
                    ticketTypeMapper.decreaseSoldCount(item.getTicketTypeId(), item.getQuantity());
                    log.info("退款回退库存: ticketTypeId={}, quantity={}", item.getTicketTypeId(), item.getQuantity());
                }
            }
        } else {
            refund.setStatus(2); // 已拒绝
        }

        refund.setAuditUserId(auditorId);
        refund.setAuditTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        log.info("退款审核完成: {} -> {}", dto.getRefundNo(), dto.getApproved() ? "通过" : "拒绝");
    }

    @Override
    public void deleteRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款记录不存在");
        }
        refundMapper.deleteById(refundId);
        log.info("退款记录已删除: {} (订单ID: {})", refund.getRefundNo(), refund.getOrderId());
    }

    @Override
    public Page<RefundVO> getAllRefunds(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<Refund>()
                .orderByDesc(Refund::getCreateTime);
        if (status != null) {
            wrapper.eq(Refund::getStatus, status);
        }

        Page<Refund> refundPage = refundMapper.selectPage(new Page<>(page, size), wrapper);
        return buildRefundVOPage(refundPage);
    }

    private Page<RefundVO> buildRefundVOPage(Page<Refund> refundPage) {
        Page<RefundVO> result = new Page<>(refundPage.getCurrent(), refundPage.getSize(), refundPage.getTotal());

        List<RefundVO> voList = refundPage.getRecords().stream().map(refund -> {
            RefundVO vo = new RefundVO();
            vo.setId(refund.getId());
            vo.setOrderId(refund.getOrderId());

            TicketOrder order = orderMapper.selectById(refund.getOrderId());
            vo.setOrderNo(order != null ? order.getOrderNo() : "未知");

            vo.setRefundNo(refund.getRefundNo());
            vo.setRefundAmount(refund.getRefundAmount());
            vo.setReason(refund.getReason());
            vo.setStatus(refund.getStatus());
            vo.setStatusText(getStatusText(refund.getStatus()));
            vo.setAuditTime(refund.getAuditTime());
            vo.setCreateTime(refund.getCreateTime());

            if (refund.getAuditUserId() != null) {
                SysUser auditor = sysUserMapper.selectById(refund.getAuditUserId());
                vo.setAuditUserName(auditor != null ? auditor.getRealName() : "未知");
            }

            return vo;
        }).collect(Collectors.toList());

        result.setRecords(voList);
        return result;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            case 3 -> "已退款";
            default -> "未知";
        };
    }
}
