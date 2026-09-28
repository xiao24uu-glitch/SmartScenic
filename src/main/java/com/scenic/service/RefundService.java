package com.scenic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.dto.RefundApplyDTO;
import com.scenic.dto.RefundAuditDTO;
import com.scenic.vo.RefundVO;

public interface RefundService {
    void applyRefund(Long userId, RefundApplyDTO dto);
    Page<RefundVO> getRefundList(Long userId, Integer page, Integer size);
    void auditRefund(Long auditorId, RefundAuditDTO dto);
    Page<RefundVO> getAllRefunds(Integer page, Integer size, Integer status);
    void deleteRefund(Long refundId);
}
