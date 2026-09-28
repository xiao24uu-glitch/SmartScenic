package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TravelogueDownloadDTO {
    /** 游记Markdown内容 */
    @NotBlank(message = "游记内容不能为空")
    private String content;

    /** 导出格式: pdf / word */
    @NotBlank(message = "导出格式不能为空")
    private String format;
}
