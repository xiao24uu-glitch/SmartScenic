package com.scenic.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CrowdHeatmapVO {
    /** 总在园人数 */
    private Long currentInPark;
    /** 最大承载量 */
    private Integer maxCapacity;
    /** 拥挤程度 1-宽松 2-适中 3-拥挤 4-爆满 */
    private Integer crowdLevel;
    /** 拥挤描述 */
    private String crowdDesc;
    /** 时段拥挤数据 */
    private List<HourlyCrowdVO> hourlyData;
    /** 各时段入园人数 */
    private List<ChartDataVO> hourlyEntry;
    /** 建议游览时段 */
    private String suggestion;

    @Data
    @Builder
    public static class HourlyCrowdVO {
        private String hour;
        private Long count;
        private Integer level; // 1-4
        private String label;
    }
}
