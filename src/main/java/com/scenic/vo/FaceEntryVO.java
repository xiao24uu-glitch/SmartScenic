package com.scenic.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FaceEntryVO {
    private Boolean success;
    private BigDecimal score;
    private String message;
    private String userName;
    /** 操作类型：ENTRY=入园, EXIT=出园 */
    private String action;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String gateNo;
    private String captureImagePath;
}
