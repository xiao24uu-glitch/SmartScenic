package com.scenic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "scenic")
public class ScenicProperties {
    /** 景区名称（全局引用，一键替换） */
    private String name;
    /** 景区地址 */
    private String address;
    /** 景区介绍 */
    private String description;
    /** 开放时间 */
    private String openTime;
    /** 关闭时间 */
    private String closeTime;
    /** Logo路径 */
    private String logoUrl;
    /** 最大日承载量 */
    private Integer maxCapacity;
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
    private String primaryColor = "#1A1A1D";
    /** 运营状态：1=正常运营，0=暂停运营 */
    private Integer status;
}
