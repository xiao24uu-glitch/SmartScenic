package com.scenic.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 团体人脸批量注册进度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupFaceProgressVO {
    /** 团体订单ID */
    private Long groupOrderId;
    /** 状态: RUNNING / COMPLETED / FAILED / NOT_STARTED */
    private String status;
    /** 总人数 */
    private int totalMembers;
    /** 已完成数 */
    private int completedCount;
    /** 成功数 */
    private int successCount;
    /** 失败数 */
    private int failedCount;
    /** 当前正在处理的人名（用于提示"正在为XXX录入人脸..."） */
    private String currentName;
    /** 整体错误信息（整体失败时） */
    private String errorMessage;
    /** 开始时间戳 */
    private long startTime;
}
