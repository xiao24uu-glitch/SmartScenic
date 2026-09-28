package com.scenic.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.common.Result;
import com.scenic.common.exception.BusinessException;
import com.scenic.dto.GroupAuditDTO;
import com.scenic.dto.GroupImportDTO;
import com.scenic.security.SecurityUtil;
import com.scenic.service.FaceService;
import com.scenic.service.GroupOrderService;
import com.scenic.vo.GroupFaceProgressVO;
import com.scenic.vo.GroupOrderVO;
import com.scenic.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "团体票管理")
@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupOrderController {

    private final GroupOrderService groupOrderService;
    private final FaceService faceService;
    private final SecurityUtil securityUtil;

    @Operation(summary = "下载Excel模板")
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = groupOrderService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=group_member_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(template);
    }

    @Operation(summary = "上传团体名单导入")
    @PostMapping("/import")
    public Result<GroupOrderVO> importGroup(
            @RequestParam("file") MultipartFile file,
            @RequestParam("groupName") String groupName,
            @RequestParam("contactName") String contactName,
            @RequestParam("contactPhone") String contactPhone,
            @RequestParam("visitDate") String visitDate) {

        GroupImportDTO dto = new GroupImportDTO();
        dto.setUserId(securityUtil.getCurrentUserId());
        dto.setGroupName(groupName);
        dto.setContactName(contactName);
        dto.setContactPhone(contactPhone);
        dto.setVisitDate(java.time.LocalDate.parse(visitDate));

        return Result.success(groupOrderService.importGroup(file, dto));
    }

    @Operation(summary = "查询团体订单列表")
    @GetMapping("/orders")
    public Result<PageVO<GroupOrderVO>> getGroupOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Page<GroupOrderVO> result = groupOrderService.getGroupOrders(page, size, status);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "查询我的团体订单列表")
    @GetMapping("/my-orders")
    public Result<PageVO<GroupOrderVO>> getUserGroupOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = securityUtil.getCurrentUserId();
        Page<GroupOrderVO> result = groupOrderService.getUserGroupOrders(userId, page, size, status);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "查询团体订单详情")
    @GetMapping("/orders/{id}")
    public Result<GroupOrderVO> getGroupOrderDetail(@PathVariable Long id) {
        return Result.success(groupOrderService.getGroupOrderDetail(id));
    }

    @Operation(summary = "审核团体订单")
    @PostMapping("/audit")
    public Result<Void> auditGroupOrder(@Valid @RequestBody GroupAuditDTO dto) {
        Long auditorId = securityUtil.getCurrentUserId();
        groupOrderService.auditGroupOrder(auditorId, dto);
        return Result.success("审核完成", null);
    }

    @Operation(summary = "团体订单支付确认（仅订单拥有者可支付）")
    @PostMapping("/pay/{groupOrderId}")
    public Result<Void> confirmPay(@PathVariable Long groupOrderId) {
        Long userId = securityUtil.getCurrentUserId();
        groupOrderService.confirmPay(groupOrderId, userId);
        return Result.success("支付成功", null);
    }

    @Operation(summary = "修改团体登记信息（修改后需重新审核）")
    @PutMapping("/orders/{id}")
    public Result<GroupOrderVO> updateGroupOrder(
            @PathVariable Long id,
            @RequestParam String groupName,
            @RequestParam String contactName,
            @RequestParam String contactPhone,
            @RequestParam String visitDate) {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(groupOrderService.updateGroupOrder(id, userId, groupName, contactName, contactPhone, visitDate));
    }

    @Operation(summary = "删除团体订单")
    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteGroupOrder(@PathVariable Long id) {
        groupOrderService.deleteGroupOrder(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除团体订单")
    @DeleteMapping("/orders/batch")
    public Result<String> deleteGroupOrders(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的团体订单");
        }
        for (Long id : ids) {
            groupOrderService.deleteGroupOrder(id);
        }
        return Result.success((String) null, "成功删除 " + ids.size() + " 个团体订单");
    }

    @Operation(summary = "删除团体成员")
    @DeleteMapping("/members/{id}")
    public Result<Void> deleteGroupMember(@PathVariable Long id) {
        groupOrderService.deleteGroupMember(id);
        return Result.success("成员已删除", null);
    }

    @Operation(summary = "查询团体人脸批量注册进度")
    @GetMapping("/face-register-progress/{groupOrderId}")
    public Result<GroupFaceProgressVO> getFaceRegisterProgress(@PathVariable Long groupOrderId) {
        return Result.success(faceService.getGroupFaceProgress(groupOrderId));
    }
}
