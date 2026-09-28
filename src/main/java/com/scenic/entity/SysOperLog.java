package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String description;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String ip;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
