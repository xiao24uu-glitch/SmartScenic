package com.scenic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scenic.entity.TicketOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM ticket_order WHERE DATE(create_time) = CURDATE() AND status IN (1, 5, 6)")
    BigDecimal todaySales();

    @Select("SELECT COALESCE(COUNT(*), 0) FROM ticket_order WHERE DATE(create_time) = CURDATE() AND status IN (1, 5, 6)")
    Long todayOrderCount();

    @Select("SELECT DATE(create_time) as name, COALESCE(SUM(total_amount), 0) as value FROM ticket_order " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND status IN (1, 5, 6) " +
            "GROUP BY DATE(create_time) ORDER BY name")
    List<Map<String, Object>> salesTrend7Days();

    @Select("SELECT tt.name, COALESCE(SUM(oi.quantity), 0) as value FROM order_item oi " +
            "INNER JOIN ticket_order o ON oi.order_id = o.id " +
            "INNER JOIN ticket_type tt ON oi.ticket_type_id = tt.id " +
            "WHERE o.status IN (1, 5, 6) AND DATE(o.create_time) = CURDATE() " +
            "GROUP BY tt.id, tt.name")
    List<Map<String, Object>> ticketTypeDistribution();

    @Select("SELECT COALESCE(COUNT(*), 0) FROM entry_log WHERE DATE(entry_time) = CURDATE() AND status = 1")
    Long todayEntryCount();

    @Select("SELECT COALESCE(COUNT(*), 0) FROM entry_log WHERE status = 1 AND exit_time IS NULL")
    Long currentInPark();

    @Select("SELECT COALESCE(COUNT(*), 0) FROM entry_log WHERE DATE(exit_time) = CURDATE() AND status = 1")
    Long todayExitCount();

    @Select("SELECT HOUR(entry_time) as name, COUNT(*) as value FROM entry_log " +
            "WHERE DATE(entry_time) = CURDATE() AND status = 1 " +
            "GROUP BY HOUR(entry_time) ORDER BY name")
    List<Map<String, Object>> hourlyEntryDistribution();
}
