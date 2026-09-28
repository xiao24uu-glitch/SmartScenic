package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gate")
public class Gate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String gateNo;
    private String name;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
