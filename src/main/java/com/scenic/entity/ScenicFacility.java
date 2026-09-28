package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scenic_facility")
public class ScenicFacility {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicId;
    private String name;
    private Integer type;
    private String description;
    private String imageUrl;
    private BigDecimal longitude;
    private BigDecimal latitude;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
