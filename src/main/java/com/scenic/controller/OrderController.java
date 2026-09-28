package com.scenic.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.common.Result;
import com.scenic.dto.OrderCreateDTO;
import com.scenic.dto.PayDTO;
import com.scenic.dto.RefundApplyDTO;
import com.scenic.security.SecurityUtil;
import com.scenic.service.OrderService;
import com.scenic.service.RefundService;
import com.scenic.vo.OrderVO;
import com.scenic.vo.PageVO;
import com.scenic.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RefundService refundService;
    private final SecurityUtil securityUtil;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO createDTO) {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(orderService.createOrder(userId, createDTO));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> getOrderDetail(@PathVariable String orderNo) {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(orderService.getOrderDetail(userId, orderNo));
    }

    @Operation(summary = "查询用户订单列表")
    @GetMapping("/list")
    public Result<PageVO<OrderVO>> getUserOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = securityUtil.getCurrentUserId();
        Page<OrderVO> result = orderService.getUserOrders(userId, page, size, status);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "模拟支付")
    @PostMapping("/pay")
    public Result<OrderVO> mockPay(@Valid @RequestBody PayDTO payDTO) {
        return Result.success(orderService.mockPay(payDTO));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        Long userId = securityUtil.getCurrentUserId();
        orderService.cancelOrder(userId, orderNo);
        return Result.success("订单已取消", null);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{orderNo}")
    public Result<Void> deleteOrder(@PathVariable String orderNo) {
        Long userId = securityUtil.getCurrentUserId();
        orderService.deleteOrder(userId, orderNo);
        return Result.success("订单已删除", null);
    }

    @Operation(summary = "申请修改订单（需审核）")
    @PutMapping("/{orderNo}/modify")
    public Result<Void> requestModifyOrder(@PathVariable String orderNo, @RequestParam String visitDate) {
        Long userId = securityUtil.getCurrentUserId();
        orderService.requestModifyOrder(userId, orderNo, java.time.LocalDate.parse(visitDate));
        return Result.success("修改申请已提交，请等待审核", null);
    }

    @Operation(summary = "订单使用优惠券")
    @PutMapping("/{orderNo}/coupon")
    public Result<OrderVO> useCoupon(@PathVariable String orderNo, @RequestParam Long userCouponId) {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(orderService.useCoupon(userId, orderNo, userCouponId));
    }

    @Operation(summary = "移除订单优惠券")
    @DeleteMapping("/{orderNo}/coupon")
    public Result<OrderVO> removeCoupon(@PathVariable String orderNo) {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(orderService.removeCoupon(userId, orderNo));
    }

    @Operation(summary = "申请退款")
    @PostMapping("/refund")
    public Result<Void> applyRefund(@Valid @RequestBody RefundApplyDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        refundService.applyRefund(userId, dto);
        return Result.success("退款申请已提交", null);
    }

    @Operation(summary = "查询退款列表")
    @GetMapping("/refund/list")
    public Result<PageVO<RefundVO>> getRefundList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = securityUtil.getCurrentUserId();
        Page<RefundVO> result = refundService.getRefundList(userId, page, size);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }
}
