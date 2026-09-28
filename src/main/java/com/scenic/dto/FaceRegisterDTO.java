package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaceRegisterDTO {
    /** 人脸图片Base64 */
    @NotBlank(message = "人脸图片不能为空")
    private String imageBase64;
    /** 关联的订单编号（不传表示录入内部通道人脸，仅限管理员/检票员） */
    private String orderNo;
    /** 要替换的人脸ID（不传表示新增，传了则先删除该人脸再重新录入） */
    private Long faceId;
    /** 该票使用者的真实姓名 */
    private String realName;
    /** 该票使用者的手机号 */
    private String phone;
}
