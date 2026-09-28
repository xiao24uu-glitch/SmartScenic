package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementDTO {
    @NotBlank(message = "公告标题不能为空")
    private String title;
    private String content;
    private Integer type;
    private Integer isTop;
    private Integer status;
}
