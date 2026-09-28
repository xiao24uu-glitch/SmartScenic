package com.scenic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.dto.GroupAuditDTO;
import com.scenic.dto.GroupImportDTO;
import com.scenic.vo.GroupOrderVO;
import org.springframework.web.multipart.MultipartFile;

public interface GroupOrderService {
    byte[] downloadTemplate();
    GroupOrderVO importGroup(MultipartFile file, GroupImportDTO dto);
    Page<GroupOrderVO> getGroupOrders(Integer page, Integer size, Integer status);
    Page<GroupOrderVO> getUserGroupOrders(Long userId, Integer page, Integer size, Integer status);
    GroupOrderVO getGroupOrderDetail(Long id);
    void auditGroupOrder(Long auditorId, GroupAuditDTO dto);
    void confirmPay(Long groupOrderId, Long userId);
    GroupOrderVO updateGroupOrder(Long id, Long userId, String groupName, String contactName, String contactPhone, String visitDate);
    void deleteGroupOrder(Long id);
    void deleteGroupMember(Long memberId);
}
