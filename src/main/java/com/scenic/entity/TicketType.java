package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ticket_type")
public class TicketType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicId;
    private String name;
    private BigDecimal price;
    private Integer totalStock;
    private Integer dailyStock;
    private Integer soldCount;
    private String description;
    private Integer isGroup;
    private Integer minGroupSize;
    /** 最大可预约天数（从今天起算），默认散客7天、团体14天 */
    private Integer maxBookingDays;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
