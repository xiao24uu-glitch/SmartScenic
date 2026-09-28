package com.scenic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.scenic.common.Result;
import com.scenic.common.exception.BusinessException;
import com.scenic.dto.FaceEntryDTO;
import com.scenic.entity.EntryLog;
import com.scenic.entity.FaceData;
import com.scenic.entity.OrderItem;
import com.scenic.entity.SysUser;
import com.scenic.entity.TicketOrder;
import com.scenic.entity.TicketType;
import com.scenic.mapper.EntryLogMapper;
import com.scenic.mapper.FaceDataMapper;
import com.scenic.mapper.OrderItemMapper;
import com.scenic.mapper.SysUserMapper;
import com.scenic.mapper.TicketOrderMapper;
import com.scenic.mapper.TicketTypeMapper;
import com.scenic.security.SecurityUtil;
import com.scenic.service.FaceService;
import com.scenic.vo.FaceEntryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "检票员操作")
@RestController
@RequestMapping("/api/v1/checker")
@RequiredArgsConstructor
public class CheckerController {

    private final FaceService faceService;
    private final EntryLogMapper entryLogMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final SysUserMapper sysUserMapper;
    private final FaceDataMapper faceDataMapper;

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Operation(summary = "人脸核验检票")
    @PostMapping("/verify-face")
    public Result<FaceEntryVO> verifyFace(@Valid @RequestBody FaceEntryDTO dto) {
        if (dto.getGateNo() == null || dto.getGateNo().isBlank()) {
            dto.setGateNo("A01");
        }
        FaceEntryVO result = faceService.faceEntry(dto);
        log.info("检票员 {} 核验检票: success={}, gate={}", SecurityUtil.getUsername(), result.getSuccess(), dto.getGateNo());
        return Result.success(result);
    }

    @Operation(summary = "人工验票出场（订单号出园）")
    @PostMapping("/manual-exit")
    public Result<Map<String, Object>> manualExit(@RequestBody Map<String, Object> body) {
        String orderNo = (String) body.get("orderNo");
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException("订单编号不能为空");
        }

        faceService.exitPark(orderNo);
        log.info("检票员 {} 执行出园操作: orderNo={}", SecurityUtil.getUsername(), orderNo);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderNo", orderNo);
        result.put("exitTime", LocalDateTime.now().toString());
        return Result.success(result);
    }

    @Operation(summary = "人工验票入场（订单号+身份证）")
    @PostMapping("/manual-entry")
    public Result<Map<String, Object>> manualEntry(@RequestBody Map<String, Object> body) {
        String orderNo = (String) body.get("orderNo");
        String gateNo = (String) body.getOrDefault("gateNo", "MANUAL");

        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException("订单编号不能为空");
        }

        // 查找订单
        TicketOrder order = ticketOrderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderNo);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("订单状态异常，无法入园");
        }
        if (!order.getVisitDate().equals(LocalDate.now())) {
            throw new BusinessException("订单游览日期不是今天");
        }

        // 检查是否已入园
        Long entryCount = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, order.getId())
                        .eq(EntryLog::getStatus, 1));
        if (entryCount > 0) {
            throw new BusinessException("该订单已入园，请勿重复入场");
        }

        // 创建人工入园记录（数据库唯一索引兜底防并发）
        EntryLog entryLog = new EntryLog();
        entryLog.setOrderId(order.getId());
        entryLog.setUserId(order.getUserId());
        entryLog.setEntryTime(LocalDateTime.now());
        entryLog.setGateNo(gateNo);
        entryLog.setStatus(1);
        entryLog.setFailReason("人工通道入场");
        entryLog.setCompareScore(BigDecimal.valueOf(1.0));
        try {
            entryLogMapper.insert(entryLog);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException("该订单已入园，请勿重复入场");
        }

        // 检查是否全部人已入园，更新订单状态
        try {
            Long faceDataCount = faceDataMapper.selectCount(
                    new LambdaQueryWrapper<FaceData>()
                            .eq(FaceData::getOrderId, order.getId())
                            .eq(FaceData::getStatus, 1)
            );
            entryCount = entryLogMapper.selectCount(
                    new LambdaQueryWrapper<EntryLog>()
                            .eq(EntryLog::getOrderId, order.getId())
                            .eq(EntryLog::getStatus, 1)
            );
            if (faceDataCount > 0 && faceDataCount.longValue() == entryCount.longValue()) {
                ticketOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TicketOrder>()
                        .eq(TicketOrder::getId, order.getId())
                        .set(TicketOrder::getStatus, 5)
                );
                log.info("订单全部入园(人工通道): orderId={}, faceDataCount={}, entryCount={}",
                        order.getId(), faceDataCount, entryCount);
            }
        } catch (Exception e) {
            log.warn("更新订单入园状态失败(人工通道): orderId={}", order.getId(), e);
        }

        log.info("检票员 {} 人工验票入场: orderNo={}, gate={}", SecurityUtil.getUsername(), orderNo, gateNo);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderNo", order.getOrderNo());
        result.put("enterTime", entryLog.getEntryTime().toString());
        result.put("gateNo", gateNo);
        return Result.success(result);
    }

    @Operation(summary = "查询今日入园记录")
    @GetMapping("/entries")
    public Result<Map<String, Object>> getEntries(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String gateNo) {
        LambdaQueryWrapper<EntryLog> wrapper = new LambdaQueryWrapper<EntryLog>()
                .apply("DATE(entry_time) = CURDATE()")
                .orderByDesc(EntryLog::getEntryTime);
        if (gateNo != null && !gateNo.isBlank()) {
            wrapper.eq(EntryLog::getGateNo, gateNo);
        }

        Page<EntryLog> result = entryLogMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查询关联的订单信息和票种信息
        List<Long> orderIds = result.getRecords().stream()
                .map(EntryLog::getOrderId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, TicketOrder> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : ticketOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(TicketOrder::getId, o -> o));
        // 查订单包含的票种(取第一个票种)
        Map<Long, String> orderTicketTypeMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
            if (!items.isEmpty()) {
                List<Long> typeIds = items.stream().map(OrderItem::getTicketTypeId).filter(id -> id != null).distinct().collect(Collectors.toList());
                Map<Long, String> typeNameMap = typeIds.isEmpty() ? Collections.emptyMap()
                        : ticketTypeMapper.selectBatchIds(typeIds).stream()
                        .collect(Collectors.toMap(TicketType::getId, TicketType::getName));
                for (OrderItem item : items) {
                    if (!orderTicketTypeMap.containsKey(item.getOrderId()) && item.getTicketTypeId() != null) {
                        orderTicketTypeMap.put(item.getOrderId(), typeNameMap.getOrDefault(item.getTicketTypeId(), ""));
                    }
                }
            }
        }

        // 补全姓名、录入照片、订单号、票种等信息
        List<Map<String, Object>> voList = result.getRecords().stream().map(log -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", log.getId());
            vo.put("orderId", log.getOrderId());
            vo.put("userId", log.getUserId());
            vo.put("captureImagePath", log.getCaptureImagePath());
            vo.put("compareScore", log.getCompareScore());
            vo.put("entryTime", log.getEntryTime());
            vo.put("exitTime", log.getExitTime());
            vo.put("gateNo", log.getGateNo());
            vo.put("status", log.getStatus());
            vo.put("failReason", log.getFailReason());
            // 关联订单号和票种
            if (log.getOrderId() != null && log.getOrderId() > 0) {
                TicketOrder order = orderMap.get(log.getOrderId());
                vo.put("orderNo", order != null ? order.getOrderNo() : null);
                vo.put("ticketTypeName", orderTicketTypeMap.getOrDefault(log.getOrderId(), null));
            }
            // 查询录入照片和真实姓名（人脸数据中的姓名才是票使用者姓名）
            if (log.getFaceDataId() != null) {
                FaceData faceData = faceDataMapper.selectById(log.getFaceDataId());
                vo.put("enrollImagePath", faceData != null ? faceData.getFaceImagePath() : null);
                vo.put("realName", faceData != null ? faceData.getRealName() : null);
            }
            return vo;
        }).collect(Collectors.toList());

        // 今日统计
        Long totalEntry = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .apply("DATE(entry_time) = CURDATE()")
                        .eq(EntryLog::getStatus, 1));
        Long failCount = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .apply("DATE(entry_time) = CURDATE()")
                        .eq(EntryLog::getStatus, 0));

        Map<String, Object> data = new HashMap<>();
        data.put("records", voList);
        data.put("total", result.getTotal());
        data.put("todayTotal", totalEntry);
        data.put("todayFail", failCount);
        return Result.success(data);
    }

    @Operation(summary = "查询今日有效订单")
    @GetMapping("/valid-orders")
    public Result<List<TicketOrder>> getValidOrders(
            @RequestParam(required = false) String orderNo) {
        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<TicketOrder>()
                .eq(TicketOrder::getVisitDate, LocalDate.now())
                .eq(TicketOrder::getStatus, 1)
                .orderByDesc(TicketOrder::getCreateTime);
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.like(TicketOrder::getOrderNo, orderNo);
        }
        wrapper.last("LIMIT 100");
        return Result.success(ticketOrderMapper.selectList(wrapper));
    }

    @Operation(summary = "检票员仪表盘统计")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        // 今日入园人数
        Long todayEntry = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .apply("DATE(entry_time) = CURDATE()")
                        .eq(EntryLog::getStatus, 1));

        // 今日出园人数
        Long todayExit = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .apply("DATE(exit_time) = CURDATE()")
                        .eq(EntryLog::getStatus, 1)
                        .isNotNull(EntryLog::getExitTime));

        // 当前在园人数（已入园但未出园）
        Long inParkCount = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getStatus, 1)
                        .isNull(EntryLog::getExitTime));
        
        // 今日人脸核验次数
        Long todayFaceCheck = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .apply("DATE(entry_time) = CURDATE()"));
        
        // 人工通道次数
        Long manualEntry = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .apply("DATE(entry_time) = CURDATE()")
                        .eq(EntryLog::getGateNo, "MANUAL"));
        
        // 今日有效订单数
        Long todayOrders = ticketOrderMapper.selectCount(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getVisitDate, LocalDate.now())
                        .in(TicketOrder::getStatus, 1, 5, 6));

        Map<String, Object> data = new HashMap<>();
        data.put("todayEntry", todayEntry);
        data.put("todayExit", todayExit);
        data.put("inParkCount", inParkCount);
        data.put("todayFaceCheck", todayFaceCheck);
        data.put("manualEntry", manualEntry);
        data.put("todayOrders", todayOrders);
        return Result.success(data);
    }

    @Operation(summary = "批量删除入园记录")
    @DeleteMapping("/entries/batch")
    public Result<String> deleteEntries(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的记录");
        }
        List<EntryLog> logs = entryLogMapper.selectBatchIds(ids);
        for (EntryLog log : logs) {
            deleteImageFile(log.getCaptureImagePath());
        }
        entryLogMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 条记录");
    }

    /**
     * 删除本地图片文件
     */
    private void deleteImageFile(String imagePath) {
        if (StrUtil.isBlank(imagePath)) return;
        try {
            Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(imagePath.replace("/uploads/", ""));
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("删除本地图片文件失败: {}", imagePath, e);
        }
    }
}
