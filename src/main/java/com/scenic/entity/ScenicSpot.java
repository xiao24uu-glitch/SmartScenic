package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scenic_spot")
public class ScenicSpot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicId;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
