package com.scenic.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupOrderVO {
    private Long id;
    private Long userId;
    private String groupName;
    private String contactName;
    private String contactPhone;
    private Long scenicId;
    private String scenicName;
    private LocalDate visitDate;
    private Integer totalCount;
    private BigDecimal totalAmount;
    private String importFileUrl;
    private Integer status;
    private String statusText;
    private Integer enteredCount;
    private String auditRemark;
    private LocalDateTime createTime;
    private List<GroupMemberVO> members;
}
