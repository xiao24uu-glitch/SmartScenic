package com.scenic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scenic.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 查询某票种在某游览日期已支付订单的总售出数量（用于每日库存校验）
     * 同时也会统计已退款订单之前售出的（退款审核通过时已回退soldCount，这里不应计入）
     */
    @Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi " +
            "INNER JOIN ticket_order o ON oi.order_id = o.id " +
            "WHERE oi.ticket_type_id = #{ticketTypeId} " +
            "AND o.visit_date = #{visitDate} " +
            "AND o.status IN (1, 4)")
    int countTodaySoldByTicketType(@Param("ticketTypeId") Long ticketTypeId,
                                   @Param("visitDate") java.time.LocalDate visitDate);
}
