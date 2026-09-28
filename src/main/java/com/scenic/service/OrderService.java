package com.scenic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.dto.OrderCreateDTO;
import com.scenic.dto.PayDTO;
import com.scenic.vo.OrderVO;

import java.time.LocalDate;

public interface OrderService {
    OrderVO createOrder(Long userId, OrderCreateDTO createDTO);
    OrderVO getOrderDetail(Long userId, String orderNo);
    Page<OrderVO> getUserOrders(Long userId, Integer page, Integer size, Integer status);
    OrderVO mockPay(PayDTO payDTO);
    void cancelOrder(Long userId, String orderNo);
    void deleteOrder(Long userId, String orderNo);
    void requestModifyOrder(Long userId, String orderNo, LocalDate visitDate);
    void auditModify(Long orderId, boolean approved);
    OrderVO removeCoupon(Long userId, String orderNo);
    OrderVO useCoupon(Long userId, String orderNo, Long userCouponId);
}
