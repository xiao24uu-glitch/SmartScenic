package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("group_order")
public class GroupOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String groupName;
    private String contactName;
    private String contactPhone;
    private Long scenicId;
    private LocalDate visitDate;
    private Integer totalCount;
    private BigDecimal totalAmount;
    private String importFileUrl;
    private Integer status;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
