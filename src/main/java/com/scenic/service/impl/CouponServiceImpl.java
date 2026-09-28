package com.scenic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.common.exception.BusinessException;
import com.scenic.entity.Coupon;
import com.scenic.entity.TicketOrder;
import com.scenic.entity.UserCoupon;
import com.scenic.mapper.CouponMapper;
import com.scenic.mapper.TicketOrderMapper;
import com.scenic.mapper.UserCouponMapper;
import com.scenic.service.CouponService;
import com.scenic.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final TicketOrderMapper ticketOrderMapper;

    @Override
    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        coupon.setStatus(1);
        couponMapper.insert(coupon);
        log.info("优惠券创建成功: {}", coupon.getName());
        return coupon;
    }

    @Override
    @Transactional
    public Coupon updateCoupon(Coupon coupon) {
        Coupon exist = couponMapper.selectById(coupon.getId());
        if (exist == null) {
            throw new BusinessException("优惠券不存在");
        }
        couponMapper.updateById(coupon);
        log.info("优惠券更新成功: id={}", coupon.getId());
        return couponMapper.selectById(coupon.getId());
    }

    @Override
    public Page<Coupon> getCouponPage(Integer page, Integer size) {
        return couponMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getCreateTime)
        );
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        coupon.setStatus(coupon.getStatus() == 1 ? 0 : 1);
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的优惠券");
        }
        couponMapper.deleteBatchIds(ids);
        log.info("批量删除优惠券: {}", ids);
    }

    @Override
    @Transactional
    public void batchToggle(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要操作的优惠券");
        }
        for (Long id : ids) {
            Coupon coupon = couponMapper.selectById(id);
            if (coupon != null) {
                coupon.setStatus(status);
                couponMapper.updateById(coupon);
            }
        }
        log.info("批量更新优惠券状态: ids={}, status={}", ids, status);
    }

    @Override
    public List<Coupon> getAvailableTemplates(Long userId) {
        // 查询用户已支付订单数（用于新用户判定）
        long paidOrderCount = getPaidOrderCount(userId);

        // 所有启用的优惠券模板（还有库存）
        List<Coupon> all = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, 1)
                        .apply("received_count < total_count")
        );

        // 过滤：1. 新用户专属限制  2. 领取上限
        return all.stream().filter(coupon -> {
            // 设了订单上限时，检查用户订单数是否超标
            Integer maxOrder = coupon.getMaxOrderCount();
            if (maxOrder != null && maxOrder >= 0 && paidOrderCount > maxOrder) {
                return false;
            }
            long already = userCouponMapper.selectCount(
                    new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId)
                            .eq(UserCoupon::getCouponId, coupon.getId())
            );
            return already < coupon.getPerUserLimit();
        }).collect(Collectors.toList());
    }

    /** 查询用户已支付订单数 */
    private long getPaidOrderCount(Long userId) {
        return ticketOrderMapper.selectCount(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getUserId, userId)
                        .eq(TicketOrder::getStatus, 1)  // 已支付
        );
    }

    @Override
    @Transactional
    public void receiveCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (coupon.getStatus() != 1) {
            throw new BusinessException("该优惠券已停用");
        }
        if (coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new BusinessException("优惠券已被领完");
        }
        // 新用户专属限制：超出订单上限的用户不可领取
        Integer maxOrder = coupon.getMaxOrderCount();
        if (maxOrder != null && maxOrder >= 0 && getPaidOrderCount(userId) > maxOrder) {
            throw new BusinessException("该优惠券仅限订单数不超过" + maxOrder + "笔的用户领取");
        }

        // 检查每人限领数量
        long alreadyReceived = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
        );
        if (alreadyReceived >= coupon.getPerUserLimit()) {
            throw new BusinessException("您已领取过该优惠券（每人限领" + coupon.getPerUserLimit() + "张）");
        }

        // 原子增加已领取数
        int affected = couponMapper.increaseReceivedCount(couponId);
        if (affected <= 0) {
            throw new BusinessException("优惠券已被领完");
        }

        // 创建用户优惠券记录
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setCouponName(coupon.getName());
        uc.setType(coupon.getType());
        uc.setThreshold(coupon.getThreshold());
        uc.setDiscountValue(coupon.getDiscountValue());
        uc.setMaxDiscount(coupon.getMaxDiscount());
        uc.setStatus(0);
        LocalDate now = LocalDate.now();
        uc.setValidFrom(now);
        uc.setValidUntil(now.plusDays(coupon.getValidDays()));
        uc.setReceiveTime(LocalDateTime.now());
        userCouponMapper.insert(uc);

        log.info("用户 {} 领取优惠券: {}", userId, coupon.getName());
    }

    @Override
    public List<CouponVO> getMyAvailableCoupons(Long userId, BigDecimal orderAmount) {
        // 查询未使用的优惠券
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0)
                        .ge(UserCoupon::getValidUntil, LocalDate.now())  // 未过期
        );

        List<CouponVO> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            // 检查是否满足门槛
            if (orderAmount.compareTo(uc.getThreshold()) < 0) {
                continue; // 不满足门槛，不可用
            }

            CouponVO vo = new CouponVO();
            vo.setUserCouponId(uc.getId());
            vo.setCouponId(uc.getCouponId());
            vo.setCouponName(uc.getCouponName());
            vo.setType(uc.getType());
            vo.setThreshold(uc.getThreshold());
            vo.setDiscountValue(uc.getDiscountValue());
            vo.setMaxDiscount(uc.getMaxDiscount());
            vo.setValidUntil(uc.getValidUntil());

            // 计算实际优惠金额
            BigDecimal discount = calculateDiscount(uc, orderAmount);
            vo.setDiscountAmount(discount);

            // 描述文字
            if (uc.getType() == 1) {
                vo.setDesc("满" + uc.getThreshold() + "减" + uc.getDiscountValue());
            } else {
                vo.setDesc("满" + uc.getThreshold() + "打" +
                        uc.getDiscountValue().multiply(BigDecimal.valueOf(100)).setScale(0) + "折");
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    public Page<UserCoupon> getMyCoupons(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getReceiveTime);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        return userCouponMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void deleteUserCoupon(Long userId, Long userCouponId) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (!uc.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该优惠券");
        }
        if (uc.getStatus() == 1) {
            throw new BusinessException("该优惠券已使用，无法删除");
        }
        userCouponMapper.deleteById(userCouponId);
        log.info("用户 {} 删除优惠券: userCouponId={}", userId, userCouponId);
    }

    @Override
    @Transactional
    public void clearReceivedCount(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        couponMapper.updateById(coupon);
        // 同步删除该优惠券下所有用户持有记录
        LambdaQueryWrapper<UserCoupon> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.eq(UserCoupon::getCouponId, id);
        long deleted = userCouponMapper.delete(ucWrapper);
        log.info("清空优惠券领取量和已使用量，并删除{}条用户持有记录: id={}", deleted, id);
    }

    @Override
    public BigDecimal calculateDiscount(UserCoupon userCoupon, BigDecimal orderAmount) {
        if (userCoupon.getType() == 1) {
            // 满减券：直接减固定金额
            return userCoupon.getDiscountValue().min(orderAmount);
        } else {
            // 折扣券：orderAmount * (1 - discountValue)
            BigDecimal discount = orderAmount.multiply(
                    BigDecimal.ONE.subtract(userCoupon.getDiscountValue())
            ).setScale(2, RoundingMode.HALF_UP);
            // 如果有最大优惠限制
            if (userCoupon.getMaxDiscount() != null) {
                discount = discount.min(userCoupon.getMaxDiscount());
            }
            return discount;
        }
    }
}
