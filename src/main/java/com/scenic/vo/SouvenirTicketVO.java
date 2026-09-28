package com.scenic.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SouvenirTicketVO {
    private String orderNo;
    private String scenicName;
    private LocalDate visitDate;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusDesc;
    private LocalDateTime payTime;
    private LocalDateTime entryTime;
    private String gateName;
    private String entryPhotoUrl;
    private List<TicketItem> tickets;
    private List<String> spotImages;

    @Data
    @Builder
    public static class TicketItem {
        private String ticketName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
