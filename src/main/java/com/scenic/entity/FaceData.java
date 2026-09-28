package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("face_data")
public class FaceData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long touristId;
    private Long userId;
    /** 关联的订单ID（一张票绑定一张人脸） */
    private Long orderId;
    /** 该票使用者的真实姓名 */
    private String realName;
    /** 该票使用者的手机号 */
    private String phone;
    private String baiduFaceToken;
    /** 百度人脸库中的唯一 user_id，用于 1:N 搜索时精确匹配到具体人脸（同一订单多张票每人有独立的 userId） */
    private String baiduUserId;
    private String baiduGroupId;
    private String faceImagePath;
    private BigDecimal qualityScore;
    private LocalDateTime expireTime;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
