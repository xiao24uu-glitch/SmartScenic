package com.scenic.service.impl;

import com.scenic.config.ScenicProperties;
import com.scenic.mapper.TicketOrderMapper;
import com.scenic.service.DashboardService;
import com.scenic.vo.ChartDataVO;
import com.scenic.vo.CrowdHeatmapVO;
import com.scenic.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TicketOrderMapper orderMapper;
    private final ScenicProperties scenicProperties;

    @Override
    public DashboardVO getDashboard() {
        // 今日订单数
        Long todayOrderCount = orderMapper.todayOrderCount();

        // 今日销售额
        BigDecimal todaySales = orderMapper.todaySales();
        if (todaySales == null) todaySales = BigDecimal.ZERO;

        // 今日入园人数
        Long todayEntryCount = orderMapper.todayEntryCount();

        // 今日出园人数
        Long todayExitCount = orderMapper.todayExitCount();
        if (todayExitCount == null) todayExitCount = 0L;

        // 当前在园人数（已入园但未出园）
        Long currentInPark = orderMapper.currentInPark();

        // 近7日销售趋势
        List<Map<String, Object>> salesTrend = orderMapper.salesTrend7Days();
        List<ChartDataVO> salesTrendVO = new ArrayList<>();
        if (salesTrend != null) {
            for (Map<String, Object> item : salesTrend) {
                salesTrendVO.add(new ChartDataVO(
                        String.valueOf(item.get("name")),
                        item.get("value") != null ? new BigDecimal(item.get("value").toString()) : BigDecimal.ZERO
                ));
            }
        }

        // 票种销售占比
        List<Map<String, Object>> ticketDist = orderMapper.ticketTypeDistribution();
        List<ChartDataVO> ticketDistVO = new ArrayList<>();
        if (ticketDist != null) {
            for (Map<String, Object> item : ticketDist) {
                ticketDistVO.add(new ChartDataVO(
                        String.valueOf(item.get("name")),
                        item.get("value") != null ? new BigDecimal(item.get("value").toString()) : BigDecimal.ZERO
                ));
            }
        }

        // 时段入园分布
        List<Map<String, Object>> hourlyEntry = orderMapper.hourlyEntryDistribution();
        List<ChartDataVO> hourlyEntryVO = new ArrayList<>();
        if (hourlyEntry != null) {
            for (Map<String, Object> item : hourlyEntry) {
                hourlyEntryVO.add(new ChartDataVO(
                        String.valueOf(item.get("name")),
                        item.get("value") != null ? new BigDecimal(item.get("value").toString()) : BigDecimal.ZERO
                ));
            }
        }

        return DashboardVO.builder()
                .todayOrderCount(todayOrderCount)
                .todaySales(todaySales)
                .todayEntryCount(todayEntryCount)
                .todayExitCount(todayExitCount)
                .currentInPark(currentInPark)
                .salesTrend(salesTrendVO)
                .ticketTypeDistribution(ticketDistVO)
                .hourlyEntryDistribution(hourlyEntryVO)
                .build();
    }

    @Override
    public CrowdHeatmapVO getCrowdHeatmap() {
        DashboardVO dashboard = getDashboard();
        Long currentInPark = dashboard.getCurrentInPark();
        Integer maxCapacity = scenicProperties.getMaxCapacity();

        double ratio = maxCapacity != null && maxCapacity > 0
                ? currentInPark.doubleValue() / maxCapacity : 0;
        int crowdLevel;
        String crowdDesc;
        if (ratio < 0.3) { crowdLevel = 1; crowdDesc = "宽松"; }
        else if (ratio < 0.6) { crowdLevel = 2; crowdDesc = "适中"; }
        else if (ratio < 0.85) { crowdLevel = 3; crowdDesc = "拥挤"; }
        else { crowdLevel = 4; crowdDesc = "爆满"; }

        List<ChartDataVO> hourlyEntry = dashboard.getHourlyEntryDistribution();
        List<CrowdHeatmapVO.HourlyCrowdVO> hourlyData = new ArrayList<>();
        if (hourlyEntry != null) {
            for (ChartDataVO item : hourlyEntry) {
                long count = item.getValue().longValue();
                double hRatio = maxCapacity != null && maxCapacity > 0
                        ? (double) count / maxCapacity : 0;
                int hLevel;
                String hLabel;
                if (hRatio < 0.1) { hLevel = 1; hLabel = "空闲"; }
                else if (hRatio < 0.2) { hLevel = 2; hLabel = "适中"; }
                else if (hRatio < 0.35) { hLevel = 3; hLabel = "较挤"; }
                else { hLevel = 4; hLabel = "拥挤"; }
                hourlyData.add(CrowdHeatmapVO.HourlyCrowdVO.builder()
                        .hour(item.getName()).count(count).level(hLevel).label(hLabel).build());
            }
        }

        String suggestion = crowdLevel >= 3
                ? "当前景区人流较大，建议错峰游览，避开高峰时段"
                : "当前景区游览舒适度较好，欢迎前来游玩";

        return CrowdHeatmapVO.builder()
                .currentInPark(currentInPark).maxCapacity(maxCapacity)
                .crowdLevel(crowdLevel).crowdDesc(crowdDesc)
                .hourlyData(hourlyData).hourlyEntry(hourlyEntry)
                .suggestion(suggestion).build();
    }
}
