package com.scenic.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.client.BaiduFaceApiClient;
import com.scenic.common.Result;
import com.scenic.common.annotation.OperLog;
import com.scenic.common.exception.BusinessException;
import com.scenic.config.ScenicProperties;
import com.scenic.dto.*;
import com.scenic.entity.*;
import com.scenic.mapper.*;
import com.scenic.security.SecurityUtil;
import com.scenic.service.*;
import com.scenic.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "后台管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final DashboardService dashboardService;
    private final RefundService refundService;
    private final OrderService orderService;
    private final SysConfigService sysConfigService;
    private final TicketTypeMapper ticketTypeMapper;
    private final TicketOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final EntryLogMapper entryLogMapper;
    private final FaceDataMapper faceDataMapper;
    private final ScenicSpotMapper scenicSpotMapper;
    private final ScenicFacilityMapper scenicFacilityMapper;
    private final AiConversationMapper aiConversationMapper;
    private final SysOperLogMapper sysOperLogMapper;
    private final SecurityUtil securityUtil;
    private final PasswordEncoder passwordEncoder;
    private final BaiduFaceApiClient baiduFaceApiClient;
    private final TouristMapper touristMapper;
    private final ScenicProperties scenicProperties;

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    // ==================== 仪表盘 ====================

    @Operation(summary = "获取仪表盘数据")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(dashboardService.getDashboard());
    }

    @Operation(summary = "导出财务报表Excel")
    @GetMapping("/reports/export")
    public void exportReport(HttpServletResponse response) throws IOException {
        DashboardVO dashboard = dashboardService.getDashboard();

        String filename = "财务报表_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            // ========== Sheet 1: 财务概览 ==========
            Sheet overviewSheet = workbook.createSheet("财务概览");
            int rowIdx = 0;

            Row titleRow = overviewSheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("景区财务报表 - 今日概览");
            titleCell.setCellStyle(titleStyle);
            overviewSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
            rowIdx++;

            addOverviewRow(overviewSheet, headerStyle, dataStyle, rowIdx++, "指标", "数值", "备注");
            addOverviewRow(overviewSheet, headerStyle, moneyStyle, rowIdx++, "今日销售额",
                    "¥" + (dashboard.getTodaySales() != null ? dashboard.getTodaySales().toString() : "0"), "");
            addOverviewRow(overviewSheet, headerStyle, dataStyle, rowIdx++, "今日订单数",
                    String.valueOf(dashboard.getTodayOrderCount() != null ? dashboard.getTodayOrderCount() : 0), "");
            addOverviewRow(overviewSheet, headerStyle, dataStyle, rowIdx++, "今日入园人数",
                    String.valueOf(dashboard.getTodayEntryCount() != null ? dashboard.getTodayEntryCount() : 0), "");
            addOverviewRow(overviewSheet, headerStyle, dataStyle, rowIdx++, "当前在园人数",
                    String.valueOf(dashboard.getCurrentInPark() != null ? dashboard.getCurrentInPark() : 0), "");
            addOverviewRow(overviewSheet, headerStyle, dataStyle, rowIdx++, "报表生成日期",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "");

            overviewSheet.setColumnWidth(0, 5000);
            overviewSheet.setColumnWidth(1, 5000);
            overviewSheet.setColumnWidth(2, 5000);

            // ========== Sheet 2: 近7日销售趋势 ==========
            Sheet trendSheet = workbook.createSheet("近7日销售趋势");
            rowIdx = 0;

            Row trendTitleRow = trendSheet.createRow(rowIdx++);
            Cell trendTitleCell = trendTitleRow.createCell(0);
            trendTitleCell.setCellValue("近7日销售趋势");
            trendTitleCell.setCellStyle(titleStyle);
            trendSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 1));
            rowIdx++;

            Row trendHeaderRow = trendSheet.createRow(rowIdx++);
            Cell dateHeader = trendHeaderRow.createCell(0);
            dateHeader.setCellValue("日期");
            dateHeader.setCellStyle(headerStyle);
            Cell amountHeader = trendHeaderRow.createCell(1);
            amountHeader.setCellValue("销售额");
            amountHeader.setCellStyle(headerStyle);

            if (dashboard.getSalesTrend() != null) {
                for (ChartDataVO item : dashboard.getSalesTrend()) {
                    Row dataRow = trendSheet.createRow(rowIdx++);
                    Cell dateCell = dataRow.createCell(0);
                    dateCell.setCellValue(item.getName());
                    dateCell.setCellStyle(dataStyle);
                    Cell amountCell = dataRow.createCell(1);
                    amountCell.setCellValue(item.getValue() != null ? "¥" + item.getValue().toString() : "¥0");
                    amountCell.setCellStyle(moneyStyle);
                }
            }

            trendSheet.setColumnWidth(0, 5000);
            trendSheet.setColumnWidth(1, 5000);

            // ========== Sheet 3: 票种销售占比 ==========
            Sheet ticketSheet = workbook.createSheet("票种销售占比");
            rowIdx = 0;

            Row ticketTitleRow = ticketSheet.createRow(rowIdx++);
            Cell ticketTitleCell = ticketTitleRow.createCell(0);
            ticketTitleCell.setCellValue("票种销售占比");
            ticketTitleCell.setCellStyle(titleStyle);
            ticketSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 1));
            rowIdx++;

            Row ticketHeaderRow = ticketSheet.createRow(rowIdx++);
            Cell typeHeader = ticketHeaderRow.createCell(0);
            typeHeader.setCellValue("票种名称");
            typeHeader.setCellStyle(headerStyle);
            Cell qtyHeader = ticketHeaderRow.createCell(1);
            qtyHeader.setCellValue("售出数量");
            qtyHeader.setCellStyle(headerStyle);

            if (dashboard.getTicketTypeDistribution() != null) {
                for (ChartDataVO item : dashboard.getTicketTypeDistribution()) {
                    Row dataRow = ticketSheet.createRow(rowIdx++);
                    Cell nameCell = dataRow.createCell(0);
                    nameCell.setCellValue(item.getName());
                    nameCell.setCellStyle(dataStyle);
                    Cell qtyCell = dataRow.createCell(1);
                    qtyCell.setCellValue(item.getValue() != null ? item.getValue().intValue() : 0);
                    qtyCell.setCellStyle(dataStyle);
                }
            }

            ticketSheet.setColumnWidth(0, 5000);
            ticketSheet.setColumnWidth(1, 4000);

            // ========== Sheet 4: 时段入园分布 ==========
            if (dashboard.getHourlyEntryDistribution() != null && !dashboard.getHourlyEntryDistribution().isEmpty()) {
                Sheet hourlySheet = workbook.createSheet("时段入园分布");
                rowIdx = 0;

                Row hourlyTitleRow = hourlySheet.createRow(rowIdx++);
                Cell hourlyTitleCell = hourlyTitleRow.createCell(0);
                hourlyTitleCell.setCellValue("今日时段入园分布");
                hourlyTitleCell.setCellStyle(titleStyle);
                hourlySheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 1));
                rowIdx++;

                Row hourlyHeaderRow = hourlySheet.createRow(rowIdx++);
                Cell hourHeader = hourlyHeaderRow.createCell(0);
                hourHeader.setCellValue("时段");
                hourHeader.setCellStyle(headerStyle);
                Cell countHeader = hourlyHeaderRow.createCell(1);
                countHeader.setCellValue("入园人数");
                countHeader.setCellStyle(headerStyle);

                for (ChartDataVO item : dashboard.getHourlyEntryDistribution()) {
                    Row dataRow = hourlySheet.createRow(rowIdx++);
                    Cell hourCell = dataRow.createCell(0);
                    hourCell.setCellValue(item.getName() + "时");
                    hourCell.setCellStyle(dataStyle);
                    Cell countCell = dataRow.createCell(1);
                    countCell.setCellValue(item.getValue() != null ? item.getValue().intValue() : 0);
                    countCell.setCellStyle(dataStyle);
                }

                hourlySheet.setColumnWidth(0, 4000);
                hourlySheet.setColumnWidth(1, 4000);
            }

            // 输出到响应流
            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
                os.flush();
            }
        }
    }

    // ==================== 票种管理 ====================

    @Operation(summary = "获取票种列表")
    @GetMapping("/ticket-types")
    public Result<List<TicketType>> getTicketTypes() {
        return Result.success(ticketTypeMapper.selectList(
                new LambdaQueryWrapper<TicketType>().orderByAsc(TicketType::getId)));
    }

    @Operation(summary = "创建票种")
    @OperLog(module = "票务管理", action = "创建票种", description = "新增了一个票种")
    @PostMapping("/ticket-types")
    public Result<Void> createTicketType(@Valid @RequestBody TicketTypeDTO dto) {
        TicketType ticketType = new TicketType();
        ticketType.setScenicId(1L);
        ticketType.setName(dto.getName());
        ticketType.setPrice(dto.getPrice());
        ticketType.setTotalStock(dto.getTotalStock());
        ticketType.setDailyStock(dto.getDailyStock());
        ticketType.setDescription(dto.getDescription());
        ticketType.setIsGroup(dto.getIsGroup() != null ? dto.getIsGroup() : 0);
        ticketType.setMinGroupSize(dto.getMinGroupSize());
        ticketType.setMaxBookingDays(dto.getMaxBookingDays());
        ticketType.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        ticketTypeMapper.insert(ticketType);
        return Result.success("票种创建成功", null);
    }

    @Operation(summary = "更新票种")
    @OperLog(module = "票务管理", action = "更新票种", description = "修改了票种信息")
    @PutMapping("/ticket-types/{id}")
    public Result<Void> updateTicketType(@PathVariable Long id, @Valid @RequestBody TicketTypeDTO dto) {
        TicketType ticketType = ticketTypeMapper.selectById(id);
        if (ticketType == null) {
            throw new BusinessException("票种不存在");
        }
        ticketType.setName(dto.getName());
        ticketType.setPrice(dto.getPrice());
        ticketType.setTotalStock(dto.getTotalStock());
        ticketType.setDailyStock(dto.getDailyStock());
        ticketType.setDescription(dto.getDescription());
        ticketType.setIsGroup(dto.getIsGroup() != null ? dto.getIsGroup() : 0);
        ticketType.setMinGroupSize(dto.getMinGroupSize());
        ticketType.setMaxBookingDays(dto.getMaxBookingDays());
        ticketType.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        ticketTypeMapper.updateById(ticketType);
        return Result.success("票种更新成功", null);
    }

    @Operation(summary = "删除票种")
    @OperLog(module = "票务管理", action = "删除票种", description = "删除了一个票种")
    @DeleteMapping("/ticket-types/{id}")
    public Result<Void> deleteTicketType(@PathVariable Long id) {
        ticketTypeMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除票种")
    @OperLog(module = "票务管理", action = "批量删除票种", description = "批量删除了票种")
    @DeleteMapping("/ticket-types/batch")
    public Result<String> deleteTicketTypes(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的票种");
        }
        ticketTypeMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 个票种");
    }

    // ==================== 订单管理 ====================

    @Operation(summary = "获取所有订单")
    @GetMapping("/orders")
    public Result<PageVO<OrderVO>> getAllOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {

        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<TicketOrder>()
                .and(w -> w.isNull(TicketOrder::getIsGroup).or().eq(TicketOrder::getIsGroup, 0))
                .orderByDesc(TicketOrder::getCreateTime);
        if (status != null) wrapper.eq(TicketOrder::getStatus, status);
        if (orderNo != null && !orderNo.isBlank()) wrapper.like(TicketOrder::getOrderNo, orderNo);

        Page<TicketOrder> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);

        List<OrderVO> voList = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setUserId(order.getUserId());
            vo.setVisitDate(order.getVisitDate());
            vo.setPendingVisitDate(order.getPendingVisitDate());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setPayAmount(order.getPayAmount());
            vo.setPayType(order.getPayType());
            vo.setPayTime(order.getPayTime());
            vo.setIsGroup(order.getIsGroup());
            vo.setStatus(order.getStatus());
            vo.setStatusText(getOrderStatusText(order.getStatus()));
            vo.setCreateTime(order.getCreateTime());

            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            vo.setItems(items.stream().map(item -> {
                OrderItemVO itemVO = new OrderItemVO();
                itemVO.setId(item.getId());
                itemVO.setTicketTypeId(item.getTicketTypeId());
                TicketType tt = ticketTypeMapper.selectById(item.getTicketTypeId());
                itemVO.setTicketTypeName(tt != null ? tt.getName() : "未知");
                itemVO.setQuantity(item.getQuantity());
                itemVO.setUnitPrice(item.getUnitPrice());
                itemVO.setSubtotal(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                return itemVO;
            }).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        return Result.success(new PageVO<>(orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize(), voList));
    }

    @Operation(summary = "更新订单")
    @OperLog(module = "订单管理", action = "更新订单", description = "修改了订单信息")
    @PutMapping("/orders/{id}")
    public Result<Void> updateOrder(@PathVariable Long id, @RequestBody TicketOrder order) {
        TicketOrder existing = orderMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("订单不存在");
        }
        order.setId(id);
        orderMapper.updateById(order);
        return Result.success("订单更新成功", null);
    }

    @Operation(summary = "删除订单")
    @OperLog(module = "订单管理", action = "删除订单", description = "删除了一个订单")
    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        TicketOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 如果订单状态是"已支付"/"修改待审核"/"已入园"/"已出园"，需要回退库存
        if (order.getStatus() == 1 || order.getStatus() == 4 || order.getStatus() == 5 || order.getStatus() == 6) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
            for (OrderItem item : items) {
                ticketTypeMapper.decreaseSoldCount(item.getTicketTypeId(), item.getQuantity());
            }
        }

        // 删除关联的入园记录（含抓拍照片文件）
        List<EntryLog> entryLogs = entryLogMapper.selectList(
                new LambdaQueryWrapper<EntryLog>().eq(EntryLog::getOrderId, id));
        for (EntryLog log : entryLogs) {
            deleteImageFile(log.getCaptureImagePath());
        }
        entryLogMapper.delete(new LambdaQueryWrapper<EntryLog>().eq(EntryLog::getOrderId, id));

        // 删除关联的人脸数据（含百度端和本地图片）
        List<FaceData> faces = faceDataMapper.selectList(
                new LambdaQueryWrapper<FaceData>().eq(FaceData::getOrderId, id));
        for (FaceData face : faces) {
            cleanFaceData(face);
        }
        faceDataMapper.delete(new LambdaQueryWrapper<FaceData>().eq(FaceData::getOrderId, id));

        // 先删除订单项，再删除订单
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        orderMapper.deleteById(id);
        return Result.success("订单删除成功", null);
    }

    @Operation(summary = "批量删除订单")
    @OperLog(module = "订单管理", action = "批量删除订单", description = "批量删除了订单")
    @DeleteMapping("/orders/batch")
    public Result<String> deleteOrders(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的订单");
        }
        for (Long id : ids) {
            TicketOrder order = orderMapper.selectById(id);
            if (order != null) {
                if (order.getStatus() == 1 || order.getStatus() == 4 || order.getStatus() == 5 || order.getStatus() == 6) {
                    List<OrderItem> items = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
                    for (OrderItem item : items) {
                        ticketTypeMapper.decreaseSoldCount(item.getTicketTypeId(), item.getQuantity());
                    }
                }

                // 删除关联的入园记录（含抓拍照片文件）
                List<EntryLog> entryLogs = entryLogMapper.selectList(
                        new LambdaQueryWrapper<EntryLog>().eq(EntryLog::getOrderId, id));
                for (EntryLog log : entryLogs) {
                    deleteImageFile(log.getCaptureImagePath());
                }
                entryLogMapper.delete(new LambdaQueryWrapper<EntryLog>().eq(EntryLog::getOrderId, id));

                // 删除关联的人脸数据（含百度端和本地图片）
                List<FaceData> faces = faceDataMapper.selectList(
                        new LambdaQueryWrapper<FaceData>().eq(FaceData::getOrderId, id));
                for (FaceData face : faces) {
                    cleanFaceData(face);
                }
                faceDataMapper.delete(new LambdaQueryWrapper<FaceData>().eq(FaceData::getOrderId, id));

                orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
            }
        }
        orderMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 个订单");
    }

    @Operation(summary = "审核订单修改申请")
    @OperLog(module = "订单管理", action = "审核订单修改", description = "审核了订单修改申请")
    @PostMapping("/orders/{id}/audit-modify")
    public Result<Void> auditOrderModify(@PathVariable Long id, @RequestParam boolean approved) {
        orderService.auditModify(id, approved);
        return Result.success(approved ? "审核通过" : "已拒绝修改申请", null);
    }

    // ==================== 退款审核 ====================

    @Operation(summary = "获取所有退款申请")
    @GetMapping("/refunds")
    public Result<PageVO<RefundVO>> getAllRefunds(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Page<RefundVO> result = refundService.getAllRefunds(page, size, status);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "审核退款")
    @OperLog(module = "退款审核", action = "审核退款", description = "处理了退款申请")
    @PostMapping("/refunds/audit")
    public Result<Void> auditRefund(@Valid @RequestBody RefundAuditDTO dto) {
        Long auditorId = securityUtil.getCurrentUserId();
        refundService.auditRefund(auditorId, dto);
        return Result.success("审核完成", null);
    }

    @Operation(summary = "删除退款记录")
    @OperLog(module = "退款审核", action = "删除退款记录", description = "删除了退款记录")
    @DeleteMapping("/refunds/{id}")
    public Result<Void> deleteRefund(@PathVariable Long id) {
        refundService.deleteRefund(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除退款记录")
    @OperLog(module = "退款审核", action = "批量删除退款记录", description = "批量删除了退款记录")
    @DeleteMapping("/refunds/batch")
    public Result<String> deleteRefunds(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的退款记录");
        }
        for (Long id : ids) {
            refundService.deleteRefund(id);
        }
        return Result.success((String) null, "成功删除 " + ids.size() + " 条退款记录");
    }

    // ==================== 入园记录 ====================

    @Operation(summary = "获取入园记录")
    @GetMapping("/entry-logs")
    public Result<PageVO<Map<String, Object>>> getEntryLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<EntryLog> result = entryLogMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<EntryLog>().orderByDesc(EntryLog::getEntryTime));

        // 批量查询关联的订单信息和票种信息
        List<Long> orderIds = result.getRecords().stream()
                .map(EntryLog::getOrderId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, TicketOrder> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(TicketOrder::getId, o -> o));
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

        List<Map<String, Object>> voList = result.getRecords().stream().map(log -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", log.getId());
            vo.put("orderItemId", log.getOrderItemId());
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

        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), voList));
    }

    @Operation(summary = "批量删除入园记录")
    @OperLog(module = "入园记录", action = "批量删除", description = "批量删除了入园记录")
    @DeleteMapping("/entry-logs/batch")
    public Result<String> deleteEntryLogs(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的记录");
        }
        // 先查询记录，删除关联的抓拍照片文件
        List<EntryLog> logs = entryLogMapper.selectBatchIds(ids);
        for (EntryLog log : logs) {
            deleteImageFile(log.getCaptureImagePath());
        }
        entryLogMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 条记录");
    }

    @Operation(summary = "删除入园记录")
    @OperLog(module = "入园记录", action = "删除记录", description = "删除了入园记录")
    @DeleteMapping("/entry-logs/{id}")
    public Result<Void> deleteEntryLog(@PathVariable Long id) {
        EntryLog log = entryLogMapper.selectById(id);
        if (log != null) {
            deleteImageFile(log.getCaptureImagePath());
        }
        entryLogMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    // ==================== 人脸库管理 ====================

    @Operation(summary = "获取人脸数据")
    @GetMapping("/face-data")
    public Result<PageVO<Map<String, Object>>> getFaceData(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<FaceData> result = faceDataMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<FaceData>().orderByDesc(FaceData::getCreateTime));

        List<Map<String, Object>> voList = result.getRecords().stream().map(face -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", face.getId());
            vo.put("userId", face.getUserId());
            // 直接使用 face_data 表中该票使用者的真实姓名和手机号
            vo.put("realName", face.getRealName());
            vo.put("phone", face.getPhone());
            vo.put("faceImagePath", face.getFaceImagePath());
            vo.put("baiduFaceToken", face.getBaiduFaceToken());
            vo.put("baiduGroupId", face.getBaiduGroupId());
            vo.put("qualityScore", face.getQualityScore());
            vo.put("expireTime", face.getExpireTime());
            vo.put("status", face.getStatus());
            vo.put("createTime", face.getCreateTime());
            // 查询订单号
            if (face.getOrderId() != null) {
                TicketOrder order = orderMapper.selectById(face.getOrderId());
                vo.put("orderNo", order != null ? order.getOrderNo() : null);
                vo.put("isGroup", order != null ? order.getIsGroup() : null);
            }
            // 查询用户名
            if (face.getUserId() != null) {
                SysUser user = sysUserMapper.selectById(face.getUserId());
                vo.put("username", user != null ? user.getUsername() : null);
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), voList));
    }

    @Operation(summary = "批量删除人脸数据")
    @OperLog(module = "人脸库管理", action = "批量删除", description = "批量删除了人脸数据")
    @DeleteMapping("/face-data/batch")
    public Result<String> deleteFaceDatas(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的记录");
        }
        List<FaceData> faces = faceDataMapper.selectBatchIds(ids);
        for (FaceData face : faces) {
            cleanFaceData(face);
        }
        faceDataMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 条记录");
    }

    @Operation(summary = "删除人脸数据")
    @OperLog(module = "人脸库管理", action = "删除记录", description = "删除了人脸数据")
    @DeleteMapping("/face-data/{id}")
    public Result<Void> deleteFaceData(@PathVariable Long id) {
        FaceData face = faceDataMapper.selectById(id);
        if (face != null) {
            cleanFaceData(face);
        }
        faceDataMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "清理百度端旧人脸数据（按分组/订单）")
    @PostMapping("/face-data/cleanup")
    public Result<String> cleanupFaceData(@RequestParam(required = false) String groupId,
                                          @RequestParam(required = false) Long orderId) {
        if (StrUtil.isBlank(groupId) && orderId == null) {
            throw new BusinessException("请提供 groupId 或 orderId 至少一个参数");
        }

        List<FaceData> facesToClean;

        if (orderId != null) {
            // 按订单清理
            facesToClean = faceDataMapper.selectList(
                    new LambdaQueryWrapper<FaceData>().eq(FaceData::getOrderId, orderId));
        } else {
            // 按分组清理
            facesToClean = faceDataMapper.selectList(
                    new LambdaQueryWrapper<FaceData>().eq(FaceData::getBaiduGroupId, groupId));
        }

        int baiduCleaned = 0;
        int dbCleaned = 0;

        for (FaceData face : facesToClean) {
            // 从百度端删除
            try {
                cleanFaceData(face);
                baiduCleaned++;
            } catch (Exception e) {
                log.warn("清理百度人脸失败: faceDataId={}", face.getId(), e);
            }
            // 从数据库删除
            faceDataMapper.deleteById(face.getId());
            dbCleaned++;
        }

        // 如果指定了 groupId，尝试删除整个百度分组
        if (StrUtil.isNotBlank(groupId)) {
            try {
                baiduFaceApiClient.deleteGroup(groupId);
                log.info("百度分组删除成功: groupId={}", groupId);
            } catch (Exception e) {
                log.warn("百度分组删除失败: groupId={}", groupId, e);
            }
        }

        log.info("人脸数据清理完成: baiduCleaned={}, dbCleaned={}, groupId={}, orderId={}",
                baiduCleaned, dbCleaned, groupId, orderId);

        return Result.success(String.format("清理完成: 百度端 %d 条, 数据库 %d 条", baiduCleaned, dbCleaned));
    }

    // ==================== 私有辅助方法 ====================

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

    /**
     * 清理人脸数据：从百度删除、删除本地图片、更新游客状态
     */
    private void cleanFaceData(FaceData face) {
        // 从百度人脸库删除
        try {
            if (StrUtil.isNotBlank(face.getBaiduFaceToken())
                    && StrUtil.isNotBlank(face.getBaiduGroupId())) {
                baiduFaceApiClient.deleteFace(face.getBaiduFaceToken(), face.getBaiduGroupId());
            }
        } catch (Exception e) {
            log.warn("百度人脸删除失败: faceToken={}", face.getBaiduFaceToken(), e);
        }

        // 删除本地图片
        deleteImageFile(face.getFaceImagePath());

        // 更新游客人脸状态
        try {
            Tourist tourist = touristMapper.selectOne(
                    new LambdaQueryWrapper<Tourist>().eq(Tourist::getUserId, face.getUserId()));
            if (tourist != null) {
                tourist.setFaceStatus(0);
                tourist.setBaiduFaceToken(null);
                touristMapper.updateById(tourist);
            }
        } catch (Exception e) {
            log.warn("更新游客人脸状态失败: userId={}", face.getUserId(), e);
        }
    }

    // ==================== 用户管理 ====================

    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    public Result<PageVO<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<SysUser> result = sysUserMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreateTime));

        List<Map<String, Object>> voList = result.getRecords().stream().map(u -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", u.getId());
            vo.put("username", u.getUsername());
            vo.put("realName", u.getRealName());
            vo.put("phone", u.getPhone());
            vo.put("email", u.getEmail());
            vo.put("status", u.getStatus());
            vo.put("createTime", u.getCreateTime());
            // 查询用户角色
            List<SysRole> roles = sysRoleMapper.findRolesByUserId(u.getId());
            vo.put("roles", roles);
            return vo;
        }).collect(Collectors.toList());

        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), voList));
    }

    @Operation(summary = "获取角色列表")
    @GetMapping("/roles")
    public Result<List<SysRole>> getRoles() {
        Long userId = securityUtil.getCurrentUserId();
        // 获取当前用户的角色，判断显示哪些角色
        List<SysRole> allRoles = sysRoleMapper.selectList(null);
        List<SysRole> currentUserRoles = sysRoleMapper.findRolesByUserId(userId);

        // 系统管理员(roleLevel=1)可以看所有角色，景区管理员(roleLevel=2)只能分配检票员
        boolean isAdmin = currentUserRoles.stream().anyMatch(r -> r.getRoleLevel() == 1);
        if (isAdmin) {
            return Result.success(allRoles);
        }
        // 景区管理员只能看到检票员(level=3)和游客(level=4)角色
        List<SysRole> filtered = allRoles.stream()
                .filter(r -> r.getRoleLevel() >= 3)
                .collect(Collectors.toList());
        return Result.success(filtered);
    }

    @Operation(summary = "创建用户")
    @OperLog(module = "用户管理", action = "创建用户", description = "新增了一个用户")
    @PostMapping("/users")
    public Result<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) throw new BusinessException("用户名已存在");

        // 权限校验：景区管理员只能分配检票员和游客角色
        Long currentUserId = securityUtil.getCurrentUserId();
        List<SysRole> currentUserRoles = sysRoleMapper.findRolesByUserId(currentUserId);
        boolean isAdmin = currentUserRoles.stream().anyMatch(r -> r.getRoleLevel() == 1);
        boolean isManager = currentUserRoles.stream().anyMatch(r -> r.getRoleLevel() == 2);

        if (!isAdmin && isManager && dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            List<SysRole> assignedRoles = sysRoleMapper.selectBatchIds(dto.getRoleIds());
            boolean hasHighRole = assignedRoles.stream()
                    .anyMatch(r -> r.getRoleLevel() != null && r.getRoleLevel() <= 2);
            if (hasHighRole) {
                throw new BusinessException("景区管理员只能分配检票员和游客角色");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        sysUserMapper.insert(user);

        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }
        return Result.success("用户创建成功", null);
    }

    @Operation(summary = "编辑用户信息")
    @OperLog(module = "用户管理", action = "编辑用户", description = "编辑了用户信息")
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if (body.containsKey("username") && !body.get("username").equals(user.getUsername())) {
            // 检查用户名是否被占用
            Long count = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, body.get("username"))
            );
            if (count > 0) throw new BusinessException("该用户名已被使用");
            user.setUsername((String) body.get("username"));
        }
        if (body.containsKey("realName")) user.setRealName((String) body.get("realName"));
        if (body.containsKey("phone")) user.setPhone((String) body.get("phone"));
        if (body.containsKey("email")) user.setEmail((String) body.get("email"));
        // 支持修改密码
        Object password = body.get("password");
        if (password != null && password instanceof String pw && !pw.isBlank()) {
            user.setPassword(passwordEncoder.encode(pw));
        }
        sysUserMapper.updateById(user);
        return Result.success("用户信息更新成功", null);
    }

    @Operation(summary = "批量删除用户")
    @OperLog(module = "用户管理", action = "批量删除", description = "批量删除了用户")
    @DeleteMapping("/users/batch")
    public Result<String> deleteUsers(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的用户");
        }
        // 删除用户角色关联
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, ids));
        // 删除用户
        sysUserMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 个用户");
    }

    @Operation(summary = "更新用户角色")
    @OperLog(module = "用户管理", action = "分配角色", description = "为用户分配了角色")
    @PutMapping("/users/{id}/roles")
    public Result<Void> updateUserRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");

        Long currentUserId = securityUtil.getCurrentUserId();
        List<SysRole> currentUserRoles = sysRoleMapper.findRolesByUserId(currentUserId);
        boolean isAdmin = currentUserRoles.stream().anyMatch(r -> r.getRoleLevel() == 1);
        boolean isManager = currentUserRoles.stream().anyMatch(r -> r.getRoleLevel() == 2);

        if (!isAdmin && !isManager) {
            throw new BusinessException("无权分配角色");
        }

        if (!isAdmin && isManager) {
            // 景区管理员只能分配检票员(level=3)和游客(level=4)
            if (roleIds != null && !roleIds.isEmpty()) {
                List<SysRole> allRoles = sysRoleMapper.selectBatchIds(roleIds);
                boolean hasHighRole = allRoles.stream()
                        .anyMatch(r -> r.getRoleLevel() != null && r.getRoleLevel() <= 2);
                if (hasHighRole) {
                    throw new BusinessException("景区管理员只能分配检票员和游客角色");
                }
            }
        }

        // ===== 至少保留一个超级管理员 =====
        // 获取超级管理员角色ID (role_level = 1)
        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleLevel, 1));

        if (adminRole != null) {
            // 检查目标用户当前是否拥有超级管理员角色
            List<SysRole> targetUserExistingRoles = sysRoleMapper.findRolesByUserId(id);
            boolean targetUserHasAdmin = targetUserExistingRoles.stream()
                    .anyMatch(r -> r.getRoleLevel() == 1);

            // 检查新角色列表是否包含超级管理员角色
            boolean newRolesHasAdmin = roleIds != null && roleIds.contains(adminRole.getId());

            if (targetUserHasAdmin && !newRolesHasAdmin) {
                // 即将移除该用户的超级管理员角色，检查是否还有其他超级管理员
                Long adminUserCount = sysUserRoleMapper.selectCount(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, adminRole.getId()));
                if (adminUserCount <= 1) {
                    throw new BusinessException("系统至少需要保留一个超级管理员，无法移除");
                }
            }
        }

        // 删除旧角色，插入新角色
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }
        return Result.success("角色分配成功", null);
    }

    @Operation(summary = "更新用户状态")
    @OperLog(module = "用户管理", action = "修改状态", description = "修改了用户状态")
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) throw new BusinessException("状态值不能为空");
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        sysUserMapper.updateById(user);
        return Result.success("状态更新成功", null);
    }

    // ==================== 系统配置 ====================

    @Operation(summary = "获取系统配置列表")
    @GetMapping("/configs")
    public Result<PageVO<SysConfig>> getConfigs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String configGroup) {
        Page<SysConfig> result = sysConfigService.getConfigs(page, size, configGroup);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "更新系统配置")
    @OperLog(module = "系统配置", action = "更新配置", description = "更新了系统配置参数")
    @PutMapping("/configs")
    public Result<Void> updateConfig(@Valid @RequestBody ConfigUpdateDTO dto) {
        sysConfigService.updateConfig(dto);
        return Result.success("配置更新成功", null);
    }

    // ==================== 景点管理 ====================

    @Operation(summary = "获取景点列表")
    @GetMapping("/spots")
    public Result<List<ScenicSpot>> getSpots() {
        return Result.success(scenicSpotMapper.selectList(
                new LambdaQueryWrapper<ScenicSpot>().orderByAsc(ScenicSpot::getSortOrder)));
    }

    @Operation(summary = "新增/更新景点")
    @OperLog(module = "景点管理", action = "保存景点", description = "保存了景点信息")
    @PostMapping("/spots")
    public Result<Void> saveSpot(@RequestBody ScenicSpot spot) {
        if (spot.getId() != null) {
            // 更新：如果图片发生变化，删除旧图片文件
            ScenicSpot old = scenicSpotMapper.selectById(spot.getId());
            if (old != null && StrUtil.isNotBlank(old.getImageUrl())
                    && !old.getImageUrl().equals(spot.getImageUrl())) {
                deleteImageFile(old.getImageUrl());
            }
            scenicSpotMapper.updateById(spot);
        } else {
            spot.setScenicId(1L);
            scenicSpotMapper.insert(spot);
        }
        return Result.success("保存成功", null);
    }

    @Operation(summary = "删除景点")
    @OperLog(module = "景点管理", action = "删除景点", description = "删除了一个景点")
    @DeleteMapping("/spots/{id}")
    public Result<Void> deleteSpot(@PathVariable Long id) {
        ScenicSpot spot = scenicSpotMapper.selectById(id);
        if (spot != null) {
            deleteImageFile(spot.getImageUrl());
        }
        scenicSpotMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除景点")
    @OperLog(module = "景点管理", action = "批量删除景点", description = "批量删除了景点")
    @DeleteMapping("/spots/batch")
    public Result<String> deleteSpots(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的景点");
        }
        List<ScenicSpot> spots = scenicSpotMapper.selectBatchIds(ids);
        for (ScenicSpot spot : spots) {
            deleteImageFile(spot.getImageUrl());
        }
        scenicSpotMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 个景点");
    }

    @Operation(summary = "批量更新景点排序")
    @OperLog(module = "景点管理", action = "排序调整", description = "调整了景点排序")
    @PutMapping("/spots/sort")
    public Result<Void> updateSpotSort(@RequestBody List<ScenicSpot> spots) {
        for (ScenicSpot spot : spots) {
            ScenicSpot existing = scenicSpotMapper.selectById(spot.getId());
            if (existing != null) {
                existing.setSortOrder(spot.getSortOrder());
                scenicSpotMapper.updateById(existing);
            }
        }
        return Result.success("排序更新成功", null);
    }

    // ==================== 文件上传 ====================

    @Operation(summary = "上传图片")
    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "common") String category,
            @RequestParam(required = false) String name) {
        if (file.isEmpty()) throw new BusinessException("上传文件不能为空");

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只允许上传图片文件");
        }

        // 生成分类子目录 + 文件名前缀
        String subDir;
        String prefix;
        switch (category) {
            case "spot"       -> { subDir = "spots";       prefix = "spots"; }
            case "facility"   -> { subDir = "facilities";  prefix = "facility"; }
            case "face"       -> { subDir = "face";        prefix = "face"; }
            case "group"      -> { subDir = "group-face";  prefix = "group"; }
            case "avatar"     -> { subDir = "avatars";     prefix = "avatar"; }
            case "banner"     -> { subDir = "banners";     prefix = "banner"; }
            case "background" -> { subDir = "backgrounds"; prefix = "bg"; }
            default           -> { subDir = "common";      prefix = "common"; }
        }

        try {
            // 解析为绝对路径，防止Tomcat工作目录不一致导致路径错误
            Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path uploadDir = basePath.resolve(subDir);
            Files.createDirectories(uploadDir);

            // 获取扩展名
            String originalName = file.getOriginalFilename();
            String ext = ".jpg";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            }

            // 生成可读文件名: 优先使用 name 参数，否则回退到短ID
            String fileName;
            if (StrUtil.isNotBlank(name)) {
                // 清理非法文件名字符
                String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_");
                fileName = prefix + "_" + safeName + ext;
            } else {
                fileName = prefix + "_" + IdUtil.fastSimpleUUID().substring(0, 8) + ext;
            }
            Path targetPath = uploadDir.resolve(fileName);

            // 写入磁盘
            file.transferTo(targetPath.toFile());

            // 返回访问URL
            String url = "/uploads/" + subDir + "/" + fileName;
            return Result.success("上传成功", Map.of("url", url));
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    // ==================== 设施管理 ====================

    @Operation(summary = "获取设施列表")
    @GetMapping("/facilities")
    public Result<List<ScenicFacility>> getFacilities() {
        return Result.success(scenicFacilityMapper.selectList(null));
    }

    @Operation(summary = "新增/更新设施")
    @OperLog(module = "设施管理", action = "保存设施", description = "保存了设施信息")
    @PostMapping("/facilities")
    public Result<Void> saveFacility(@RequestBody ScenicFacility facility) {
        if (facility.getId() != null) {
            // 更新：如果图片发生变化，删除旧图片文件
            ScenicFacility old = scenicFacilityMapper.selectById(facility.getId());
            if (old != null && StrUtil.isNotBlank(old.getImageUrl())
                    && !old.getImageUrl().equals(facility.getImageUrl())) {
                deleteImageFile(old.getImageUrl());
            }
            scenicFacilityMapper.updateById(facility);
        } else {
            facility.setScenicId(1L);
            scenicFacilityMapper.insert(facility);
        }
        return Result.success("保存成功", null);
    }

    @Operation(summary = "删除设施")
    @OperLog(module = "设施管理", action = "删除设施", description = "删除了一个设施")
    @DeleteMapping("/facilities/{id}")
    public Result<Void> deleteFacility(@PathVariable Long id) {
        ScenicFacility facility = scenicFacilityMapper.selectById(id);
        if (facility != null) {
            deleteImageFile(facility.getImageUrl());
        }
        scenicFacilityMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除设施")
    @OperLog(module = "设施管理", action = "批量删除设施", description = "批量删除了设施")
    @DeleteMapping("/facilities/batch")
    public Result<String> deleteFacilities(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的设施");
        }
        List<ScenicFacility> facilities = scenicFacilityMapper.selectBatchIds(ids);
        for (ScenicFacility facility : facilities) {
            deleteImageFile(facility.getImageUrl());
        }
        scenicFacilityMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 个设施");
    }

    // ==================== AI对话记录 ====================

    @Operation(summary = "获取AI对话记录")
    @GetMapping("/ai-conversations")
    public Result<PageVO<AiConversationVO>> getAiConversations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<AiConversation> wrapper =
                new LambdaQueryWrapper<AiConversation>().orderByDesc(AiConversation::getCreateTime);

        // 支持按日期范围筛选
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(AiConversation::getCreateTime, java.time.LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(AiConversation::getCreateTime, java.time.LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }

        Page<AiConversation> result = aiConversationMapper.selectPage(new Page<>(page, size), wrapper);
        List<AiConversation> records = result.getRecords();

        // 批量查询用户名
        Map<Long, String> userMap = Collections.emptyMap();
        if (!records.isEmpty()) {
            List<Long> userIds = records.stream().map(AiConversation::getUserId).distinct().collect(Collectors.toList());
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> StrUtil.isNotBlank(u.getRealName()) ? u.getRealName() : u.getUsername()));
        }

        Map<Long, String> finalUserMap = userMap;
        List<AiConversationVO> vos = records.stream().map(c -> {
            AiConversationVO vo = new AiConversationVO();
            vo.setId(c.getId());
            vo.setUserId(c.getUserId());
            vo.setSessionId(c.getSessionId());
            vo.setQuestion(c.getQuestion());
            vo.setAnswer(c.getAnswer());
            vo.setIntent(c.getIntent());
            vo.setTokensUsed(c.getTokensUsed());
            vo.setFeedback(c.getFeedback());
            vo.setCreateTime(c.getCreateTime());
            vo.setUsername(finalUserMap.getOrDefault(c.getUserId(), "未知用户"));
            return vo;
        }).collect(Collectors.toList());

        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), vos));
    }

    @Operation(summary = "批量删除AI对话记录")
    @OperLog(module = "AI对话", action = "批量删除", description = "批量删除了AI对话记录")
    @DeleteMapping("/ai-conversations/batch")
    public Result<String> deleteAiConversations(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的记录");
        }
        aiConversationMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 条记录");
    }

    @Operation(summary = "删除AI对话记录")
    @OperLog(module = "AI对话", action = "删除记录", description = "删除了AI对话记录")
    @DeleteMapping("/ai-conversations/{id}")
    public Result<Void> deleteAiConversation(@PathVariable Long id) {
        AiConversation conv = aiConversationMapper.selectById(id);
        if (conv == null) {
            throw new BusinessException("对话记录不存在");
        }
        aiConversationMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    // ==================== 操作日志 ====================

    @Operation(summary = "获取操作日志")
    @GetMapping("/oper-logs")
    public Result<PageVO<SysOperLog>> getOperLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<SysOperLog> wrapper =
                new LambdaQueryWrapper<SysOperLog>().orderByDesc(SysOperLog::getCreateTime);

        // 支持按日期范围筛选
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(SysOperLog::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(SysOperLog::getCreateTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }

        Page<SysOperLog> result = sysOperLogMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "批量删除操作日志")
    @OperLog(module = "操作日志", action = "批量删除", description = "清理了操作日志")
    @DeleteMapping("/oper-logs/batch")
    public Result<String> deleteOperLogs(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的记录");
        }
        sysOperLogMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 条记录");
    }

    private final ScenicMapper scenicMapper;
    private final AnnouncementMapper announcementMapper;

    // ==================== 公告管理 ====================

    @Operation(summary = "获取公告列表")
    @GetMapping("/announcements")
    public Result<PageVO<Announcement>> getAnnouncements(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Announcement> result = announcementMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getIsTop)
                        .orderByDesc(Announcement::getCreateTime));
        return Result.success(new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "新增公告")
    @OperLog(module = "公告管理", action = "新增公告", description = "新增了一条公告")
    @PostMapping("/announcements")
    public Result<Void> createAnnouncement(@Valid @RequestBody AnnouncementDTO dto) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setType(dto.getType() != null ? dto.getType() : 1);
        announcement.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : 0);
        announcement.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        announcementMapper.insert(announcement);
        return Result.success("公告发布成功", null);
    }

    @Operation(summary = "更新公告")
    @OperLog(module = "公告管理", action = "编辑公告", description = "编辑了公告")
    @PutMapping("/announcements/{id}")
    public Result<Void> updateAnnouncement(@PathVariable Long id, @Valid @RequestBody AnnouncementDTO dto) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) throw new BusinessException("公告不存在");
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        if (dto.getType() != null) announcement.setType(dto.getType());
        if (dto.getIsTop() != null) announcement.setIsTop(dto.getIsTop());
        if (dto.getStatus() != null) announcement.setStatus(dto.getStatus());
        announcementMapper.updateById(announcement);
        return Result.success("公告更新成功", null);
    }

    @Operation(summary = "删除公告")
    @OperLog(module = "公告管理", action = "删除公告", description = "删除了公告")
    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        if (announcementMapper.selectById(id) == null) throw new BusinessException("公告不存在");
        announcementMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除公告")
    @OperLog(module = "公告管理", action = "批量删除", description = "批量删除了公告")
    @DeleteMapping("/announcements/batch")
    public Result<String> deleteAnnouncements(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException("请选择要删除的公告");
        announcementMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 条公告");
    }

    // ==================== 客流拥挤度热力图 ====================

    @Operation(summary = "获取客流拥挤度数据")
    @GetMapping("/crowd-heatmap")
    public Result<CrowdHeatmapVO> getCrowdHeatmap() {
        return Result.success(dashboardService.getCrowdHeatmap());
    }

    // ==================== 数字纪念票 ====================

    @Operation(summary = "获取订单数字纪念票数据")
    @GetMapping("/orders/{orderNo}/souvenir-ticket")
    public Result<SouvenirTicketVO> getSouvenirTicket(@PathVariable String orderNo) {
        Long userId = securityUtil.getCurrentUserId();
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo));
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException("无权查看此订单");

        // 查询订单项
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        List<SouvenirTicketVO.TicketItem> ticketItems = new ArrayList<>();
        for (OrderItem item : items) {
            TicketType tt = ticketTypeMapper.selectById(item.getTicketTypeId());
            ticketItems.add(SouvenirTicketVO.TicketItem.builder()
                    .ticketName(tt != null ? tt.getName() : "未知票种")
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build());
        }

        // 查询入园记录
        EntryLog entryLog = entryLogMapper.selectOne(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, order.getId())
                        .eq(EntryLog::getUserId, userId)
                        .orderByAsc(EntryLog::getEntryTime)
                        .last("LIMIT 1"));
        LocalDateTime entryTime = null;
        String entryPhotoUrl = null;
        String gateName = null;
        if (entryLog != null) {
            entryTime = entryLog.getEntryTime();
            entryPhotoUrl = entryLog.getCaptureImagePath();
            if (StrUtil.isNotBlank(entryLog.getGateNo())) {
                gateName = entryLog.getGateNo();
            }
        }

        // 获取景区景点图片（用于纪念票装饰）
        List<ScenicSpot> spots = scenicSpotMapper.selectList(
                new LambdaQueryWrapper<ScenicSpot>().eq(ScenicSpot::getStatus, 1)
                        .isNotNull(ScenicSpot::getImageUrl)
                        .orderByAsc(ScenicSpot::getSortOrder)
                        .last("LIMIT 4"));
        List<String> spotImages = spots.stream()
                .map(ScenicSpot::getImageUrl)
                .collect(Collectors.toList());

        String statusDesc = switch (order.getStatus()) {
            case 1 -> "已支付";
            case 5 -> "已入园";
            case 6 -> "已出园";
            default -> "已完成";
        };

        return Result.success(SouvenirTicketVO.builder()
                .orderNo(order.getOrderNo())
                .scenicName(scenicProperties.getName())
                .visitDate(order.getVisitDate())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .statusDesc(statusDesc)
                .payTime(order.getPayTime())
                .entryTime(entryTime)
                .gateName(gateName)
                .entryPhotoUrl(entryPhotoUrl)
                .tickets(ticketItems)
                .spotImages(spotImages)
                .build());
    }

    // ==================== 景区信息 ====================

    @Operation(summary = "获取景区信息")
    @GetMapping("/scenic")
    public Result<Scenic> getScenic() {
        List<Scenic> scenics = scenicMapper.selectList(null);
        return Result.success(scenics.isEmpty() ? null : scenics.get(0));
    }

    @Operation(summary = "更新景区信息")
    @OperLog(module = "系统配置", action = "更新景区信息", description = "更新了景区基本信息")
    @PutMapping("/scenic")
    public Result<Void> updateScenic(@RequestBody Scenic scenic) {
        if (scenic.getId() != null) {
            // 处理可能存在的 base64 图片数据（前端可能直接传 data URI）
            scenic.setLogoUrl(processBase64Image(scenic.getLogoUrl(), "logo", null));
            scenic.setHomeBgImage(processBase64Image(scenic.getHomeBgImage(), "backgrounds", "home"));
            scenic.setTicketsBgImage(processBase64Image(scenic.getTicketsBgImage(), "backgrounds", "tickets"));
            scenic.setAiBgImage(processBase64Image(scenic.getAiBgImage(), "backgrounds", "ai"));
            scenic.setOrdersBgImage(processBase64Image(scenic.getOrdersBgImage(), "backgrounds", "orders"));
            scenic.setProfileBgImage(processBase64Image(scenic.getProfileBgImage(), "backgrounds", "profile"));
            scenic.setLoginBgImage(processBase64Image(scenic.getLoginBgImage(), "backgrounds", "login"));
            scenic.setRegisterBgImage(processBase64Image(scenic.getRegisterBgImage(), "backgrounds", "register"));

            // 先查询旧数据，清理不再使用的图片文件
            Scenic old = scenicMapper.selectById(scenic.getId());
            if (old != null) {
                // 检查背景图字段变化，删除旧图片
                checkAndDeleteBgImage(old.getHomeBgImage(), scenic.getHomeBgImage());
                checkAndDeleteBgImage(old.getTicketsBgImage(), scenic.getTicketsBgImage());
                checkAndDeleteBgImage(old.getAiBgImage(), scenic.getAiBgImage());
                checkAndDeleteBgImage(old.getOrdersBgImage(), scenic.getOrdersBgImage());
                checkAndDeleteBgImage(old.getProfileBgImage(), scenic.getProfileBgImage());
                checkAndDeleteBgImage(old.getLoginBgImage(), scenic.getLoginBgImage());
                checkAndDeleteBgImage(old.getRegisterBgImage(), scenic.getRegisterBgImage());
                // 检查Logo变化，删除旧Logo文件
                checkAndDeleteBgImage(old.getLogoUrl(), scenic.getLogoUrl());
                // 检查轮播图变化，删除被移除的旧图片
                checkAndDeleteBannerImages(old.getBannerImages(), scenic.getBannerImages());
            }
            scenicMapper.updateById(scenic);
            // 同步更新内存中的 ScenicProperties
            if (scenic.getName() != null) scenicProperties.setName(scenic.getName());
            if (scenic.getAddress() != null) scenicProperties.setAddress(scenic.getAddress());
            if (scenic.getDescription() != null) scenicProperties.setDescription(scenic.getDescription());
            if (scenic.getOpenTime() != null) scenicProperties.setOpenTime(scenic.getOpenTime().toString());
            if (scenic.getCloseTime() != null) scenicProperties.setCloseTime(scenic.getCloseTime().toString());
            if (scenic.getMaxCapacity() != null) scenicProperties.setMaxCapacity(scenic.getMaxCapacity());
            if (scenic.getLogoUrl() != null) scenicProperties.setLogoUrl(scenic.getLogoUrl());
            if (scenic.getBannerImages() != null) scenicProperties.setBannerImages(scenic.getBannerImages());
            if (scenic.getHomeBgImage() != null) scenicProperties.setHomeBgImage(scenic.getHomeBgImage());
            if (scenic.getTicketsBgImage() != null) scenicProperties.setTicketsBgImage(scenic.getTicketsBgImage());
            if (scenic.getAiBgImage() != null) scenicProperties.setAiBgImage(scenic.getAiBgImage());
            if (scenic.getOrdersBgImage() != null) scenicProperties.setOrdersBgImage(scenic.getOrdersBgImage());
            if (scenic.getProfileBgImage() != null) scenicProperties.setProfileBgImage(scenic.getProfileBgImage());
            if (scenic.getLoginBgImage() != null) scenicProperties.setLoginBgImage(scenic.getLoginBgImage());
            if (scenic.getRegisterBgImage() != null) scenicProperties.setRegisterBgImage(scenic.getRegisterBgImage());
            if (scenic.getPrimaryColor() != null) scenicProperties.setPrimaryColor(scenic.getPrimaryColor());
            if (scenic.getStatus() != null) scenicProperties.setStatus(scenic.getStatus());
        }
        return Result.success("更新成功", null);
    }

    /**
     * 背景图/Logo 变化时，删除旧的本地图片文件
     * newUrl == null 表示该字段未在本次请求中传送（部分更新），跳过清理
     */
    private void checkAndDeleteBgImage(String oldUrl, String newUrl) {
        if (newUrl == null) return; // 未传字段，不做清理
        if (StrUtil.isNotBlank(oldUrl) && !oldUrl.equals(newUrl)) {
            deleteImageFile(oldUrl);
        }
    }

    /**
     * 处理可能存在的 base64 图片数据，将其保存为文件并返回路径
     * 如果不是 base64 数据，原样返回
     * @param imageValue 图片数据（可能是 base64 或普通 URL）
     * @param category 分类目录（如 "logo"、"backgrounds"）
     * @param nameSuffix 文件名后缀（如 logo 传 null，背景图传 "home"/"login" 等）
     */
    private String processBase64Image(String imageValue, String category, String nameSuffix) {
        if (StrUtil.isBlank(imageValue) || !imageValue.startsWith("data:image/")) {
            return imageValue;
        }
        try {
            // 解析 data URI: "data:image/png;base64,iVBORw0KG..."
            String[] parts = imageValue.split(",", 2);
            if (parts.length < 2) return imageValue;
            byte[] imageBytes = Base64.getDecoder().decode(parts[1]);

            // 根据 MIME 类型确定扩展名
            String ext = ".png";
            String header = parts[0].toLowerCase();
            if (header.contains("image/jpeg") || header.contains("image/jpg")) ext = ".jpg";
            else if (header.contains("image/gif")) ext = ".gif";
            else if (header.contains("image/webp")) ext = ".webp";

            // 生成可读文件名: logo/logo.png 或 backgrounds/bg_{type}.png
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(category);
            Files.createDirectories(uploadDir);
            String fileName;
            if ("logo".equals(category)) {
                fileName = "logo" + ext;
            } else if (StrUtil.isNotBlank(nameSuffix)) {
                fileName = "bg_" + nameSuffix + ext;
            } else {
                fileName = "bg_" + IdUtil.fastSimpleUUID().substring(0, 8) + ext;
            }
            Path targetPath = uploadDir.resolve(fileName);
            Files.write(targetPath, imageBytes);

            String url = "/uploads/" + category + "/" + fileName;
            log.info("base64图片已转换为文件: {}", url);
            return url;
        } catch (Exception e) {
            log.error("处理base64图片失败(category={}): {}", category, e.getMessage());
            throw new BusinessException("图片处理失败，请通过上传接口上传图片");
        }
    }

    /**
     * 轮播图更新时，删除被移除的旧轮播图文件
     * newJson == null 表示该字段未在本次请求中传送（部分更新），跳过清理
     */
    private void checkAndDeleteBannerImages(String oldJson, String newJson) {
        if (newJson == null) return; // 未传字段，不做清理
        if (StrUtil.isBlank(oldJson)) return;
        try {
            List<String> oldList = JSONUtil.toList(oldJson, String.class);
            List<String> newList = StrUtil.isNotBlank(newJson)
                    ? JSONUtil.toList(newJson, String.class)
                    : Collections.emptyList();
            for (String url : oldList) {
                if (!newList.contains(url)) {
                    deleteImageFile(url);
                }
            }
        } catch (Exception e) {
            log.warn("解析轮播图JSON失败，跳过旧图片清理", e);
        }
    }

    // ==================== Excel样式辅助方法 ====================

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setFontName("微软雅黑");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setFontName("微软雅黑");
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setFontName("微软雅黑");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createMoneyStyle(XSSFWorkbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private void addOverviewRow(Sheet sheet, CellStyle headerStyle, CellStyle dataStyle,
                                int rowIdx, String col0, String col1, String col2) {
        Row row = sheet.createRow(rowIdx);
        Cell c0 = row.createCell(0);
        c0.setCellValue(col0);
        c0.setCellStyle("指标".equals(col0) ? headerStyle : dataStyle);
        Cell c1 = row.createCell(1);
        c1.setCellValue(col1);
        c1.setCellStyle(dataStyle);
        Cell c2 = row.createCell(2);
        c2.setCellValue(col2);
        c2.setCellStyle(dataStyle);
    }

    // 辅助方法
    private String getOrderStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已退款";
            case 4 -> "修改待审核";
            case 5 -> "已入园";
            case 6 -> "已出园";
            default -> "未知";
        };
    }
}
