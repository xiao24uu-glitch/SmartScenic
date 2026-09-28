package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class GroupImportDTO {
    private Long userId;
    @NotBlank(message = "团体名称不能为空")
    private String groupName;
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;
    @NotBlank(message = "联系人电话不能为空")
    private String contactPhone;
    @NotNull(message = "游览日期不能为空")
    private LocalDate visitDate;
}
