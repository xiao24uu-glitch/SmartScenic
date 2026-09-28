package com.scenic.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long scenicId;
    private String scenicName;
    private LocalDate visitDate;
    private LocalDate pendingVisitDate;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer payType;
    private LocalDateTime payTime;
    private Integer isGroup;
    private Long groupOrderId;
    private Integer status;
    private String statusText;
    private Long couponId;
    private BigDecimal discountAmount;
    private String couponName;
    private List<OrderItemVO> items;
    /** 订单关联的人脸数量 */
    private Integer faceCount;
    /** 订单总票数（需录入人脸总数） */
    private Integer totalTickets;
    /** 已入园数量 */
    private Integer entryCount;
    /** 已出园数量 */
    private Integer exitCount;
    /** 当前在园人数 */
    private Integer inParkCount;
    /** 最近一次人脸录入时间 */
    private LocalDateTime faceTime;
    /** 首批入园时间 */
    private LocalDateTime firstEntryTime;
    /** 最后一次出园时间 */
    private LocalDateTime lastExitTime;
    private LocalDateTime createTime;
}
