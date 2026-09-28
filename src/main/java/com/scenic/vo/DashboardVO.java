package com.scenic.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardVO {
    /** 今日订单数 */
    private Long todayOrderCount;
    /** 今日销售额 */
    private BigDecimal todaySales;
    /** 今日入园人数 */
    private Long todayEntryCount;
    /** 今日出园人数 */
    private Long todayExitCount;
    /** 当前在园人数 */
    private Long currentInPark;
    /** 近7日销售趋势 */
    private List<ChartDataVO> salesTrend;
    /** 票种销售占比 */
    private List<ChartDataVO> ticketTypeDistribution;
    /** 时段入园分布 */
    private List<ChartDataVO> hourlyEntryDistribution;
}
