package com.scenic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scenic.entity.TicketType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketTypeMapper extends BaseMapper<TicketType> {

    /**
     * 原子增加已售数量（支付成功时调用）
     */
    @Update("UPDATE ticket_type SET sold_count = sold_count + #{quantity} WHERE id = #{ticketTypeId}")
    int increaseSoldCount(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") Integer quantity);

    /**
     * 原子减少已售数量（退款/删除订单时调用）
     * 使用 IF 防止 sold_count 变成负数
     */
    @Update("UPDATE ticket_type SET sold_count = IF(sold_count >= #{quantity}, sold_count - #{quantity}, 0) WHERE id = #{ticketTypeId}")
    int decreaseSoldCount(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") Integer quantity);
}
