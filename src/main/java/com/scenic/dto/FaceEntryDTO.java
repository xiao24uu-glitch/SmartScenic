package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaceEntryDTO {
    /** 摄像头抓拍的图片Base64 */
    @NotBlank(message = "人脸图片不能为空")
    private String imageBase64;
    /** 闸机编号 */
    private String gateNo;
}
