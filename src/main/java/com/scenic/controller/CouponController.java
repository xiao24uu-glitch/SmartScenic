package com.scenic.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.common.Result;
import com.scenic.entity.Coupon;
import com.scenic.entity.UserCoupon;
import com.scenic.security.SecurityUtil;
import com.scenic.service.CouponService;
import com.scenic.vo.CouponVO;
import com.scenic.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "优惠券管理")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final SecurityUtil securityUtil;

    // ==================== 管理员接口 ====================

    @Operation(summary = "管理员：创建优惠券")
    @PostMapping
    public Result<Coupon> createCoupon(@RequestBody Coupon coupon) {
        return Result.success(couponService.createCoupon(coupon));
    }

    @Operation(summary = "管理员：更新优惠券")
    @PutMapping("/{id}")
    public Result<Coupon> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        coupon.setId(id);
        return Result.success(couponService.updateCoupon(coupon));
    }

    @Operation(summary = "管理员：分页查询优惠券")
    @GetMapping
    public Result<PageVO<Coupon>> getCoupons(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Coupon> result = couponService.getCouponPage(page, size);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "管理员：停用/启用优惠券")
    @PutMapping("/{id}/toggle")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        couponService.toggleStatus(id);
        return Result.success("操作成功", null);
    }

    @Operation(summary = "管理员：批量删除优惠券")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        couponService.batchDelete(ids);
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "管理员：删除单个优惠券")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCoupon(@PathVariable Long id) {
        couponService.batchDelete(List.of(id));
        return Result.success("删除成功", null);
    }

    @Operation(summary = "管理员：批量启用/停用优惠券")
    @PutMapping("/batch/toggle")
    public Result<Void> batchToggle(@RequestBody List<Long> ids, @RequestParam Integer status) {
        couponService.batchToggle(ids, status);
        return Result.success("批量操作成功", null);
    }

    // ==================== 用户接口 ====================

    @Operation(summary = "用户：领取优惠券")
    @PostMapping("/{couponId}/receive")
    public Result<Void> receiveCoupon(@PathVariable Long couponId) {
        Long userId = securityUtil.getCurrentUserId();
        couponService.receiveCoupon(userId, couponId);
        return Result.success("领取成功", null);
    }

    @Operation(summary = "用户：获取可领取的优惠券模板")
    @GetMapping("/available-templates")
    public Result<List<Coupon>> getAvailableTemplates() {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(couponService.getAvailableTemplates(userId));
    }

    @Operation(summary = "用户：获取下单时可用的优惠券列表")
    @GetMapping("/available")
    public Result<List<CouponVO>> getAvailableCoupons(@RequestParam BigDecimal orderAmount) {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(couponService.getMyAvailableCoupons(userId, orderAmount));
    }

    @Operation(summary = "用户：删除已领取的优惠券")
    @DeleteMapping("/user/{userCouponId}")
    public Result<Void> deleteUserCoupon(@PathVariable Long userCouponId) {
        Long userId = securityUtil.getCurrentUserId();
        couponService.deleteUserCoupon(userId, userCouponId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "管理员：清空优惠券领取量")
    @PutMapping("/{id}/clear-received")
    public Result<Void> clearReceivedCount(@PathVariable Long id) {
        couponService.clearReceivedCount(id);
        return Result.success("领取量已清空", null);
    }

    @Operation(summary = "用户：查看我的优惠券")
    @GetMapping("/my")
    public Result<PageVO<UserCoupon>> getMyCoupons(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = securityUtil.getCurrentUserId();
        Page<UserCoupon> result = couponService.getMyCoupons(userId, page, size, status);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }
}
