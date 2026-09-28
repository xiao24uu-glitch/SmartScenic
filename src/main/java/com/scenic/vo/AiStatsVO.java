package com.scenic.vo;

import lombok.Data;

@Data
public class AiStatsVO {
    /** 今日AI服务次数 */
    private long todayCount;
    /** 累计AI服务次数 */
    private long totalCount;
}
