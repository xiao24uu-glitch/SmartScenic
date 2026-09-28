package com.scenic.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.common.exception.BusinessException;
import com.scenic.config.ScenicProperties;
import com.scenic.dto.OrderCreateDTO;
import com.scenic.dto.PayDTO;
import com.scenic.entity.*;
import com.scenic.mapper.*;
import com.scenic.service.CouponService;
import com.scenic.service.OrderService;
import com.scenic.vo.OrderItemVO;
import com.scenic.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final TicketOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final ScenicMapper scenicMapper;
    private final ScenicProperties scenicProperties;
    private final CouponService couponService;
    private final UserCouponMapper userCouponMapper;
    private final com.scenic.mapper.FaceDataMapper faceDataMapper;
    private final com.scenic.mapper.EntryLogMapper entryLogMapper;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, OrderCreateDTO createDTO) {
        // 获取景区
        List<Scenic> scenics = scenicMapper.selectList(null);
        if (scenics.isEmpty()) {
            throw new BusinessException("景区信息未配置");
        }
        Scenic scenic = scenics.get(0);
        if (scenic.getStatus() != null && scenic.getStatus() == 0) {
            throw new BusinessException("景区暂停运营，暂不支持购票");
        }

        // 生成订单编号
        String orderNo = "SC" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") +
                IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderCreateDTO.OrderItemDTO itemDTO : createDTO.getItems()) {
            TicketType ticketType = ticketTypeMapper.selectById(itemDTO.getTicketTypeId());
            if (ticketType == null || ticketType.getStatus() == 0) {
                throw new BusinessException("票种不存在或已停售");
            }
            if (itemDTO.getQuantity() <= 0) {
                throw new BusinessException("购买数量必须大于0");
            }
            // 检查每日库存：查询该票种在该游览日期的当日已售数量
            int todaySold = orderItemMapper.countTodaySoldByTicketType(
                    itemDTO.getTicketTypeId(), createDTO.getVisitDate());
            if (ticketType.getDailyStock() - todaySold < itemDTO.getQuantity()) {
                throw new BusinessException("票种【" + ticketType.getName() + "】当日库存不足");
            }

            BigDecimal itemTotal = ticketType.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setTicketTypeId(itemDTO.getTicketTypeId());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setUnitPrice(ticketType.getPrice());
            orderItems.add(orderItem);
        }

        // 优惠券校验
        UserCoupon usedCoupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (createDTO.getUserCouponId() != null) {
            usedCoupon = userCouponMapper.selectById(createDTO.getUserCouponId());
            if (usedCoupon == null) {
                throw new BusinessException("优惠券不存在");
            }
            if (!usedCoupon.getUserId().equals(userId)) {
                throw new BusinessException("无效的优惠券");
            }
            if (usedCoupon.getStatus() != 0) {
                throw new BusinessException("优惠券已使用或已过期");
            }
            if (usedCoupon.getValidUntil() != null && usedCoupon.getValidUntil().isBefore(LocalDate.now())) {
                throw new BusinessException("优惠券已过期");
            }
            if (totalAmount.compareTo(usedCoupon.getThreshold()) < 0) {
                throw new BusinessException("不满足优惠券使用门槛（满" + usedCoupon.getThreshold() + "元可用）");
            }
            discountAmount = couponService.calculateDiscount(usedCoupon, totalAmount);
        }

        // 创建订单
        TicketOrder order = new TicketOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setScenicId(scenic.getId());
        order.setVisitDate(createDTO.getVisitDate());
        order.setTotalAmount(totalAmount);
        if (usedCoupon != null) {
            order.setCouponId(usedCoupon.getId());
        }
        order.setDiscountAmount(discountAmount);
        order.setStatus(0); // 待支付
        order.setIsGroup(0);
        orderMapper.insert(order);

        // 创建订单详情
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        log.info("订单创建成功: {}", orderNo);
        return buildOrderVO(order, orderItems);
    }

    @Override
    public OrderVO getOrderDetail(Long userId, String orderNo) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        return buildOrderVO(order, items);
    }

    @Override
    public Page<OrderVO> getUserOrders(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<TicketOrder>()
                .eq(TicketOrder::getUserId, userId)
                .and(w -> w.isNull(TicketOrder::getIsGroup).or().eq(TicketOrder::getIsGroup, 0))
                .orderByDesc(TicketOrder::getCreateTime);
        if (status != null) {
            wrapper.eq(TicketOrder::getStatus, status);
        }

        Page<TicketOrder> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);
        Page<OrderVO> result = new Page<>(page, size, orderPage.getTotal());

        List<OrderVO> voList = orderPage.getRecords().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            return buildOrderVO(order, items);
        }).collect(Collectors.toList());

        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional
    public OrderVO mockPay(PayDTO payDTO) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, payDTO.getOrderNo())
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不正确，无法支付");
        }

        if (Boolean.TRUE.equals(payDTO.getSuccess())) {
            // 支付成功 - 实付 = 总金额 - 优惠金额
            order.setStatus(1);
            BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
            order.setPayAmount(order.getTotalAmount().subtract(discount));
            order.setPayType(0); // 模拟支付
            order.setPayTime(LocalDateTime.now());
            order.setPayTradeNo("MOCK_" + IdUtil.fastSimpleUUID());
            orderMapper.updateById(order);

            // 标记优惠券已使用
            if (order.getCouponId() != null) {
                UserCoupon uc = userCouponMapper.selectById(order.getCouponId());
                if (uc != null && uc.getStatus() == 0) {
                    uc.setStatus(1);
                    uc.setOrderId(order.getId());
                    uc.setUseTime(LocalDateTime.now());
                    userCouponMapper.updateById(uc);
                }
            }

            // 原子更新票种销量（使用 UPDATE ... SET sold_count = sold_count + ? 避免并发问题）
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
            );
            for (OrderItem item : items) {
                ticketTypeMapper.increaseSoldCount(item.getTicketTypeId(), item.getQuantity());
            }

            log.info("模拟支付成功: {}", payDTO.getOrderNo());
        } else {
            log.info("模拟支付失败: {}", payDTO.getOrderNo());
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("只能取消待支付订单");
        }

        order.setStatus(2); // 已取消
        orderMapper.updateById(order);

        // 取消订单时释放优惠券，恢复为可用状态
        if (order.getCouponId() != null) {
            UserCoupon uc = userCouponMapper.selectById(order.getCouponId());
            if (uc != null && uc.getStatus() == 0) {
                // 优惠券是待支付阶段锁定，取消订单后恢复
                uc.setStatus(0); // 恢复未使用
                uc.setOrderId(null);
                uc.setUseTime(null);
                userCouponMapper.updateById(uc);
                log.info("订单 {} 取消，优惠券 {} 已释放", orderNo, uc.getId());
            }
        }

        log.info("订单已取消: {}", orderNo);
    }

    @Override
    @Transactional
    public void deleteOrder(Long userId, String orderNo) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() == 1 || order.getStatus() == 5 || order.getStatus() == 6) {
            throw new BusinessException("该状态订单不能删除，已支付订单请先申请退款");
        }

        Long orderId = order.getId();

        // 删除关联的入园记录（含抓拍照片文件）
        List<com.scenic.entity.EntryLog> logs = entryLogMapper.selectList(
                new LambdaQueryWrapper<com.scenic.entity.EntryLog>()
                        .eq(com.scenic.entity.EntryLog::getOrderId, orderId)
        );
        for (com.scenic.entity.EntryLog log : logs) {
            deleteLocalFaceImage(log.getCaptureImagePath());
        }
        entryLogMapper.delete(
                new LambdaQueryWrapper<com.scenic.entity.EntryLog>()
                        .eq(com.scenic.entity.EntryLog::getOrderId, orderId)
        );

        // 删除关联的人脸数据
        faceDataMapper.delete(
                new LambdaQueryWrapper<com.scenic.entity.FaceData>()
                        .eq(com.scenic.entity.FaceData::getOrderId, orderId)
        );

        // 删除关联的订单详情
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        orderMapper.deleteById(orderId);
        log.info("订单已删除: {}", orderNo);
    }

    /**
     * 删除本地图片文件
     */
    private void deleteLocalFaceImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return;
        try {
            Path basePath = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
            Path filePath = basePath.resolve(imagePath.replace("/uploads/", ""));
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("删除本地图片文件失败: {}", imagePath, e);
        }
    }

    @Override
    @Transactional
    public void requestModifyOrder(Long userId, String orderNo, LocalDate visitDate) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 1 && order.getStatus() != 4) {
            throw new BusinessException("只有已支付的订单可以申请修改");
        }
        // 检查游览日期是否已过期（已过期的票不可再修改）
        if (order.getVisitDate() != null && order.getVisitDate().isBefore(LocalDate.now())) {
            throw new BusinessException("该订单游览日期已过期（" + order.getVisitDate() + "），无法修改");
        }

        // 写入待审核的新日期，状态改为"修改待审核"
        order.setPendingVisitDate(visitDate);
        order.setStatus(4);
        orderMapper.updateById(order);
        log.info("订单修改申请已提交(需审核): {} -> 新日期 {}", orderNo, visitDate);
    }

    @Override
    @Transactional
    public void auditModify(Long orderId, boolean approved) {
        TicketOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getStatus() != 4) throw new BusinessException("该订单不在待审核状态");
        if (approved) {
            // 审核通过：将 pendingVisitDate 写入正式的 visitDate，状态恢复为已支付
            order.setVisitDate(order.getPendingVisitDate());
            order.setPendingVisitDate(null);
            order.setStatus(1);
            log.info("管理员审核通过订单修改: {} -> 日期改为 {}", order.getOrderNo(), order.getVisitDate());
        } else {
            // 审核拒绝：清除待审核日期，状态恢复为已支付
            order.setPendingVisitDate(null);
            order.setStatus(1);
            log.info("管理员审核拒绝订单修改: {}", order.getOrderNo());
        }
        orderMapper.updateById(order);
    }

    private OrderVO buildOrderVO(TicketOrder order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setScenicId(order.getScenicId());
        vo.setScenicName(scenicProperties.getName());
        vo.setVisitDate(order.getVisitDate());
        vo.setPendingVisitDate(order.getPendingVisitDate());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        vo.setIsGroup(order.getIsGroup());
        vo.setGroupOrderId(order.getGroupOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusText(getStatusText(order));
        vo.setCouponId(order.getCouponId());
        vo.setDiscountAmount(order.getDiscountAmount());
        // 查询优惠券名称
        if (order.getCouponId() != null) {
            UserCoupon uc = userCouponMapper.selectById(order.getCouponId());
            vo.setCouponName(uc != null ? uc.getCouponName() : null);
        }
        vo.setCreateTime(order.getCreateTime());

        // 统计人脸/入园/出园数量
        Long orderId = order.getId();
        Long faceCount = faceDataMapper.selectCount(
                new LambdaQueryWrapper<com.scenic.entity.FaceData>()
                        .eq(com.scenic.entity.FaceData::getOrderId, orderId)
                        .eq(com.scenic.entity.FaceData::getStatus, 1)
        );
        Long entryCount = entryLogMapper.selectCount(
                new LambdaQueryWrapper<com.scenic.entity.EntryLog>()
                        .eq(com.scenic.entity.EntryLog::getOrderId, orderId)
                        .eq(com.scenic.entity.EntryLog::getStatus, 1)
        );
        Long exitCount = entryLogMapper.selectCount(
                new LambdaQueryWrapper<com.scenic.entity.EntryLog>()
                        .eq(com.scenic.entity.EntryLog::getOrderId, orderId)
                        .eq(com.scenic.entity.EntryLog::getStatus, 1)
                        .isNotNull(com.scenic.entity.EntryLog::getExitTime)
        );
        vo.setFaceCount(faceCount != null ? faceCount.intValue() : 0);
        vo.setEntryCount(entryCount != null ? entryCount.intValue() : 0);
        vo.setExitCount(exitCount != null ? exitCount.intValue() : 0);
        vo.setInParkCount(entryCount.intValue() - exitCount.intValue());

        // 订单总票数（所有票种数量之和）
        long totalTickets = items.stream().mapToLong(OrderItem::getQuantity).sum();
        vo.setTotalTickets((int) totalTickets);

        // 最近一次人脸录入时间
        FaceData latestFace = faceDataMapper.selectOne(
                new LambdaQueryWrapper<FaceData>()
                        .eq(FaceData::getOrderId, orderId)
                        .eq(FaceData::getStatus, 1)
                        .orderByDesc(FaceData::getCreateTime)
                        .last("LIMIT 1")
        );
        vo.setFaceTime(latestFace != null ? latestFace.getCreateTime() : null);

        // 首批入园时间
        EntryLog firstEntry = entryLogMapper.selectOne(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, orderId)
                        .eq(EntryLog::getStatus, 1)
                        .orderByAsc(EntryLog::getEntryTime)
                        .last("LIMIT 1")
        );
        vo.setFirstEntryTime(firstEntry != null ? firstEntry.getEntryTime() : null);

        // 最后一次出园时间
        EntryLog lastExit = entryLogMapper.selectOne(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, orderId)
                        .eq(EntryLog::getStatus, 1)
                        .isNotNull(EntryLog::getExitTime)
                        .orderByDesc(EntryLog::getExitTime)
                        .last("LIMIT 1")
        );
        vo.setLastExitTime(lastExit != null ? lastExit.getExitTime() : null);

        // 批量查询所有票种，避免 N+1 问题
        List<Long> ticketTypeIds = items.stream()
                .map(OrderItem::getTicketTypeId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> ticketTypeNameMap = new HashMap<>();
        if (!ticketTypeIds.isEmpty()) {
            List<TicketType> ticketTypes = ticketTypeMapper.selectBatchIds(ticketTypeIds);
            for (TicketType tt : ticketTypes) {
                ticketTypeNameMap.put(tt.getId(), tt.getName());
            }
        }

        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setTicketTypeId(item.getTicketTypeId());
            itemVO.setTicketTypeName(ticketTypeNameMap.getOrDefault(item.getTicketTypeId(), "未知"));
            itemVO.setQuantity(item.getQuantity());
            itemVO.setUnitPrice(item.getUnitPrice());
            itemVO.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return itemVO;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    @Override
    @Transactional
    public OrderVO useCoupon(Long userId, String orderNo, Long userCouponId) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("只能为待支付订单使用优惠券");
        }
        if (order.getCouponId() != null) {
            throw new BusinessException("该订单已使用优惠券，请先取消");
        }

        UserCoupon usedCoupon = userCouponMapper.selectById(userCouponId);
        if (usedCoupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (!usedCoupon.getUserId().equals(userId)) {
            throw new BusinessException("无效的优惠券");
        }
        if (usedCoupon.getStatus() != 0) {
            throw new BusinessException("优惠券已使用或已过期");
        }
        if (usedCoupon.getValidUntil() != null && usedCoupon.getValidUntil().isBefore(LocalDate.now())) {
            throw new BusinessException("优惠券已过期");
        }
        if (order.getTotalAmount().compareTo(usedCoupon.getThreshold()) < 0) {
            throw new BusinessException("不满足优惠券使用门槛（满" + usedCoupon.getThreshold() + "元可用）");
        }

        BigDecimal discountAmount = couponService.calculateDiscount(usedCoupon, order.getTotalAmount());

        order.setCouponId(usedCoupon.getId());
        order.setDiscountAmount(discountAmount);
        orderMapper.updateById(order);

        log.info("订单 {} 已使用优惠券「{}」，优惠 ¥{}", orderNo, usedCoupon.getCouponName(), discountAmount);

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        return buildOrderVO(order, items);
    }

    @Override
    @Transactional
    public OrderVO removeCoupon(Long userId, String orderNo) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("只能取消待支付订单的优惠券");
        }
        if (order.getCouponId() == null) {
            throw new BusinessException("该订单未使用优惠券");
        }

        // 使用 UpdateWrapper.setSql 直接置 null，绕过 MyBatis-Plus 字段策略
        orderMapper.update(null,
                new UpdateWrapper<TicketOrder>()
                        .setSql("coupon_id = NULL, discount_amount = 0")
                        .eq("order_no", orderNo)
        );

        log.info("订单 {} 已取消优惠券使用", orderNo);

        order.setCouponId(null);
        order.setDiscountAmount(BigDecimal.ZERO);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        return buildOrderVO(order, items);
    }

    /**
     * 获取订单状态文本（含子状态信息）
     */
    private String getStatusText(TicketOrder order) {
        Integer status = order.getStatus();
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> buildPaidStatusText(order);
            case 2 -> "已取消";
            case 3 -> "已退款";
            case 4 -> "修改待审核";
            case 5 -> "已入园";
            case 6 -> "已出园";
            default -> "未知";
        };
    }

    /**
     * 构建已支付状态的详细文本：
     * - 未录入任何人脸 → "已支付（未录入）"
     * - 部分录入人脸 → "已支付（部分已录入）"
     * - 全部录入人脸但未入园 → "已录入（未入园）"
     * - 部分已入园 → "已支付（部分已入园）"
     */
    private String buildPaidStatusText(TicketOrder order) {
        Long orderId = order.getId();

        // 统计该订单关联的有效人脸数量
        Long faceCount = faceDataMapper.selectCount(
                new LambdaQueryWrapper<com.scenic.entity.FaceData>()
                        .eq(com.scenic.entity.FaceData::getOrderId, orderId)
                        .eq(com.scenic.entity.FaceData::getStatus, 1)
        );

        // 统计该订单的已入园数量
        Long entryCount = entryLogMapper.selectCount(
                new LambdaQueryWrapper<com.scenic.entity.EntryLog>()
                        .eq(com.scenic.entity.EntryLog::getOrderId, orderId)
                        .eq(com.scenic.entity.EntryLog::getStatus, 1)
        );

        // 统计该订单下应录入的人脸总数（基于订单票种数量，近似为 orderItem 的 quantity 总和）
        Long orderTotalTickets = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        ).stream().mapToLong(OrderItem::getQuantity).sum();

        if (faceCount == 0) {
            return "已支付（未录入）";
        } else if (faceCount < orderTotalTickets) {
            return "已支付（部分已录入）";
        } else {
            // 所有人脸都已录入
            if (entryCount == 0) {
                return "已录入（未入园）";
            } else if (entryCount < faceCount) {
                return "已支付（部分已入园）";
            } else {
                // 全部已入园，理论上 status 应该已经是5，但以防万一
                return "已入园";
            }
        }
    }

    /** 提供给管理端等不需要订单详情的优先文本方法 */
    private String getStatusTextSimple(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已退款";
            case 4 -> "修改待审核";
            case 5 -> "已入园";
            case 6 -> "已出园";
            default -> "未知";
        };
    }
}
