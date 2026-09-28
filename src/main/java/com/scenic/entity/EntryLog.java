package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("entry_log")
public class EntryLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderItemId;
    private Long orderId;
    private Long userId;
    private Long touristId;
    private Long faceDataId;
    private String captureImagePath;
    private BigDecimal compareScore;
    private LocalDateTime entryTime;
    /** 出园时间（null 表示仍在园内） */
    private LocalDateTime exitTime;
    private String gateNo;
    private Integer status;
    private String failReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
