package com.scenic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.entity.Coupon;
import com.scenic.entity.UserCoupon;
import com.scenic.vo.CouponVO;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    /** 管理员：创建优惠券 */
    Coupon createCoupon(Coupon coupon);
    /** 管理员：更新优惠券 */
    Coupon updateCoupon(Coupon coupon);
    /** 管理员：分页查询优惠券 */
    Page<Coupon> getCouponPage(Integer page, Integer size);
    /** 用户：获取可领取的优惠券模板 */
    List<Coupon> getAvailableTemplates(Long userId);

    /** 管理员：停用/启用 */
    void toggleStatus(Long id);
    /** 管理员：批量删除 */
    void batchDelete(List<Long> ids);
    /** 管理员：批量启用/停用 */
    void batchToggle(List<Long> ids, Integer status);
    /** 用户：领取优惠券 */
    void receiveCoupon(Long userId, Long couponId);
    /** 用户：查看自己可用的优惠券列表 */
    List<CouponVO> getMyAvailableCoupons(Long userId, BigDecimal orderAmount);
    /** 用户：查看自己所有优惠券 */
    Page<UserCoupon> getMyCoupons(Long userId, Integer page, Integer size, Integer status);
    /** 计算优惠金额 */
    BigDecimal calculateDiscount(UserCoupon userCoupon, BigDecimal orderAmount);
    /** 用户：删除已领取的优惠券 */
    void deleteUserCoupon(Long userId, Long userCouponId);
    /** 管理员：清空优惠券领取量 */
    void clearReceivedCount(Long id);
}
