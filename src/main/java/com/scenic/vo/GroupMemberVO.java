package com.scenic.vo;

import lombok.Data;

@Data
public class GroupMemberVO {
    private Long id;
    private String realName;
    private String idCard;
    private String phone;
    private Long userId;
    private String faceImagePath;
    private Integer faceStatus;
    /** 人脸注册失败原因 */
    private String failReason;
    private Integer entryStatus;
}
