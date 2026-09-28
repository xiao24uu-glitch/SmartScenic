package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("scenic")
public class Scenic {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String address;
    private String description;
    private String logoUrl;
    private java.time.LocalTime openTime;
    private java.time.LocalTime closeTime;
    private Integer maxCapacity;
    private Integer status;
    /** 首页轮播图(JSON数组) */
    private String bannerImages;
    /** 各页面背景图 */
    private String homeBgImage;
    private String ticketsBgImage;
    private String aiBgImage;
    private String ordersBgImage;
    private String profileBgImage;
    private String loginBgImage;
    private String registerBgImage;
    /** 主题色（导航栏、按钮、强调色统一使用） */
    private String primaryColor;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
