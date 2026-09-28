package com.scenic.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.common.exception.BusinessException;
import com.scenic.dto.GroupAuditDTO;
import com.scenic.dto.GroupImportDTO;
import com.scenic.entity.*;
import com.scenic.mapper.*;
import com.scenic.service.GroupOrderService;
import com.scenic.security.SecurityUtil;
import com.scenic.service.FaceService;
import com.scenic.vo.GroupMemberVO;
import com.scenic.vo.GroupOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupOrderServiceImpl implements GroupOrderService {

    private final GroupOrderMapper groupOrderMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final TicketOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ScenicMapper scenicMapper;
    private final FaceService faceService;

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Override
    public byte[] downloadTemplate() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            List<GroupMemberTemplate> data = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                GroupMemberTemplate row = new GroupMemberTemplate();
                row.setSeq(String.valueOf(i));
                data.add(row);
            }

            WriteCellStyle headStyle = new WriteCellStyle();
            headStyle.setBorderBottom(BorderStyle.THIN);
            headStyle.setBorderLeft(BorderStyle.THIN);
            headStyle.setBorderRight(BorderStyle.THIN);
            headStyle.setBorderTop(BorderStyle.THIN);
            headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
            headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headStyle.setWrapped(true);
            WriteFont headFont = new WriteFont();
            headFont.setFontName("微软雅黑");
            headFont.setFontHeightInPoints((short) 10);
            headFont.setBold(true);
            headStyle.setWriteFont(headFont);

            WriteCellStyle contentStyle = new WriteCellStyle();
            contentStyle.setBorderBottom(BorderStyle.THIN);
            contentStyle.setBorderLeft(BorderStyle.THIN);
            contentStyle.setBorderRight(BorderStyle.THIN);
            contentStyle.setBorderTop(BorderStyle.THIN);
            contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
            contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            contentStyle.setWrapped(true);
            WriteFont contentFont = new WriteFont();
            contentFont.setFontName("微软雅黑");
            contentFont.setFontHeightInPoints((short) 9);
            contentStyle.setWriteFont(contentFont);

            HorizontalCellStyleStrategy styleStrategy =
                    new HorizontalCellStyleStrategy(headStyle, contentStyle);

            EasyExcel.write(bos, GroupMemberTemplate.class)
                    .registerWriteHandler(styleStrategy)
                    .registerWriteHandler(new TemplateWidthHandler())
                    .sheet("团体成员名单")
                    .doWrite(data);

        // 获取团体票最少人数
        int minGroupSize = 10;
        TicketType groupTicket = ticketTypeMapper.selectOne(
                new LambdaQueryWrapper<TicketType>().eq(TicketType::getIsGroup, 1)
        );
        if (groupTicket != null && groupTicket.getMinGroupSize() != null) {
            minGroupSize = groupTicket.getMinGroupSize();
        }
            return addTipRow(bos.toByteArray(), minGroupSize);
        } catch (IOException e) {
            throw new BusinessException("模板生成失败: " + e.getMessage());
        }
    }

    private byte[] addTipRow(byte[] raw, int minGroupSize) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(raw));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);

            int tipRowIndex = 22;
            Row tipRow = sheet.createRow(tipRowIndex);
            tipRow.setHeightInPoints(28);

            CellStyle tipStyle = workbook.createCellStyle();
            tipStyle.setBorderBottom(BorderStyle.THIN);
            tipStyle.setBorderLeft(BorderStyle.THIN);
            tipStyle.setBorderRight(BorderStyle.THIN);
            tipStyle.setBorderTop(BorderStyle.THIN);
            tipStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            tipStyle.setWrapText(true);
            org.apache.poi.ss.usermodel.Font tipFont = workbook.createFont();
            tipFont.setFontName("微软雅黑");
            tipFont.setFontHeightInPoints((short) 9);
            tipFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            tipStyle.setFont(tipFont);

            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(tipRowIndex, tipRowIndex, 0, 4));
            Cell tipCell = tipRow.createCell(0);
            tipCell.setCellValue("提示：团体名单至少" + minGroupSize + "人，如需增加行数请手动插入新行，序号列建议保持连续；人脸照片列可在Excel中直接粘贴图片。");
            tipCell.setCellStyle(tipStyle);

            for (int c = 1; c <= 4; c++) {
                Cell cell = tipRow.createCell(c);
                cell.setCellStyle(tipStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static class TemplateWidthHandler implements CellWriteHandler {
        @Override
        public void afterCellCreate(WriteSheetHolder writeSheetHolder,
                                    WriteTableHolder writeTableHolder,
                                    org.apache.poi.ss.usermodel.Cell cell,
                                    Head head,
                                    Integer relativeRowIndex,
                                    Boolean isHead) {
            if (!Boolean.TRUE.equals(isHead)) return;
            Sheet sheet = writeSheetHolder.getSheet();
            int colIndex = cell.getColumnIndex();
            switch (colIndex) {
                case 0 -> sheet.setColumnWidth(colIndex, 8 * 256);
                case 1 -> sheet.setColumnWidth(colIndex, 14 * 256);
                case 2 -> sheet.setColumnWidth(colIndex, 24 * 256);
                case 3 -> sheet.setColumnWidth(colIndex, 16 * 256);
                case 4 -> sheet.setColumnWidth(colIndex, 30 * 256);
            }
        }
    }

    @Override
    @Transactional
    public GroupOrderVO importGroup(MultipartFile file, GroupImportDTO dto) {
        // 1. 用EasyExcel读取文本数据
        List<GroupMemberTemplate> memberList = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), GroupMemberTemplate.class,
                    new ReadListener<GroupMemberTemplate>() {
                        @Override
                        public void invoke(GroupMemberTemplate data, AnalysisContext context) { memberList.add(data); }
                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {}
                    }).sheet().doRead();
        } catch (IOException e) {
            throw new BusinessException("Excel文件解析失败: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new BusinessException("Excel数据格式错误，请确认使用正确的模板格式: " + e.getMessage());
        }

        if (memberList.isEmpty()) throw new BusinessException("Excel文件中没有有效数据");

        // 过滤全空行
        List<GroupMemberTemplate> validList = memberList.stream()
                .filter(m -> !isAllBlank(m.getRealName(), m.getIdCard(), m.getPhone()))
                .collect(Collectors.toList());
        if (validList.isEmpty()) throw new BusinessException("Excel文件中没有有效数据");

        // 先获取团体票票种，拿到最小人数限制
        TicketType groupTicket = ticketTypeMapper.selectOne(
                new LambdaQueryWrapper<TicketType>().eq(TicketType::getIsGroup, 1)
        );
        if (groupTicket == null) throw new BusinessException("团体票票种未配置");
        int minSize = groupTicket.getMinGroupSize() != null ? groupTicket.getMinGroupSize() : 10;
        if (validList.size() < minSize) throw new BusinessException("团体名单至少需要" + minSize + "人，当前仅有" + validList.size() + "人");

        // 校验必填字段
        for (int i = 0; i < validList.size(); i++) {
            GroupMemberTemplate member = validList.get(i);
            if (member.getRealName() == null || member.getRealName().isBlank())
                throw new BusinessException("姓名不能为空（第" + member.getSeq() + "行序号）");
            if (member.getIdCard() == null || member.getIdCard().isBlank())
                throw new BusinessException("身份证号不能为空（第" + member.getSeq() + "行序号）");
            if (member.getPhone() == null || member.getPhone().isBlank())
                throw new BusinessException("手机号不能为空（第" + member.getSeq() + "行序号）");
        }

        List<Scenic> scenics = scenicMapper.selectList(null);
        if (scenics.isEmpty()) throw new BusinessException("景区信息未配置");
        Scenic scenic = scenics.get(0);
        if (scenic.getStatus() != null && scenic.getStatus() == 0)
            throw new BusinessException("景区暂停运营，暂不支持创建团体订单");

        // 创建团体订单
        GroupOrder groupOrder = new GroupOrder();
        groupOrder.setUserId(dto.getUserId());
        groupOrder.setGroupName(dto.getGroupName());
        groupOrder.setContactName(dto.getContactName());
        groupOrder.setContactPhone(dto.getContactPhone());
        groupOrder.setScenicId(scenic.getId());
        groupOrder.setVisitDate(dto.getVisitDate());
        groupOrder.setTotalCount(validList.size());
        groupOrder.setTotalAmount(groupTicket.getPrice().multiply(BigDecimal.valueOf(validList.size())));
        groupOrder.setStatus(0);
        groupOrderMapper.insert(groupOrder);

        // 2. 用POI提取Excel中的图片，按日期+团队名分类存储
        String dateStr = dto.getVisitDate().toString().replace("-", "");
        String safeGroupName = dto.getGroupName().replaceAll("[\\\\/:*?\"<>|]", "_");
        String imageDirRelative = "/group-face/" + dateStr + "_" + safeGroupName + "/";
        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path imageDirPath = basePath.resolve("group-face").resolve(dateStr + "_" + safeGroupName);
        byte[][] extractedImages;
        try {
            extractedImages = extractImagesFromExcel(file.getBytes(), 4); // 第5列(index=4)是人脸照片列
        } catch (IOException e) {
            log.warn("提取Excel图片失败，将跳过图片处理: {}", e.getMessage());
            extractedImages = new byte[validList.size()][];
        }

        // 3. 创建团体成员（含照片路径）
        for (int i = 0; i < validList.size(); i++) {
            GroupMemberTemplate template = validList.get(i);
            GroupMember member = new GroupMember();
            member.setGroupOrderId(groupOrder.getId());
            member.setRealName(template.getRealName());
            member.setIdCard(template.getIdCard());
            member.setPhone(template.getPhone());
            member.setFaceStatus(0);
            member.setEntryStatus(0);

            // 如果提取到了图片，保存到磁盘并记录路径
            if (i < extractedImages.length && extractedImages[i] != null && extractedImages[i].length > 0) {
                try {
                    String safeName = sanitizeFileName(template.getRealName());
                    String safePhone = template.getPhone() != null ? template.getPhone().replaceAll("[^0-9]", "") : "000";
                    String fileName = safeName + "_" + safePhone + ".jpg";
                    Files.createDirectories(imageDirPath);
                    Path filePath = imageDirPath.resolve(fileName);
                    Files.write(filePath, extractedImages[i]);
                    member.setFaceImagePath("/uploads" + imageDirRelative + fileName);
                    member.setFaceStatus(0); // 照片已保存，待支付后注册
                    log.info("保存团体成员人脸照片: {}", filePath);
                } catch (IOException e) {
                    log.warn("保存成员[{}]的人脸照片失败: {}", template.getRealName(), e.getMessage());
                }
            }
            groupMemberMapper.insert(member);
        }

        log.info("团体订单导入成功: {} - {}人", dto.getGroupName(), validList.size());
        return buildGroupOrderVO(groupOrder);
    }

    /**
     * 从Excel的指定列中提取图片（支持插入图片和WPS粘贴图片DISPIMG）
     */
    private byte[][] extractImagesFromExcel(byte[] excelBytes, int colIndex) throws IOException {
        List<byte[]> images = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            java.util.Map<Integer, byte[]> rowImageMap = new java.util.LinkedHashMap<>();

            // 方式1：从 drawing patriarch 中提取形状（适用于"插入图片"方式）
            if (sheet.getDrawingPatriarch() != null) {
                XSSFDrawing drawing = (XSSFDrawing) sheet.getDrawingPatriarch();
                for (Shape shape : drawing.getShapes()) {
                    if (shape instanceof Picture picture) {
                        ClientAnchor anchor = picture.getClientAnchor();
                        if (anchor != null && anchor.getCol1() == colIndex) {
                            int rowIdx = anchor.getRow1();
                            byte[] imgData = picture.getPictureData().getData();
                            rowImageMap.putIfAbsent(rowIdx, imgData);
                            log.info("提取人脸照片(shapes): 行={}, 大小={}字节", rowIdx, imgData.length);
                        }
                    }
                }
            } else {
                log.info("Sheet 没有 Drawing，尝试备用方案提取图片");
            }

            // 方式2：备用方案 - 获取 workbook 所有图片，按行匹配（适用于WPS的DISPIMG粘贴方式）
            if (rowImageMap.isEmpty()) {
                List<? extends org.apache.poi.xssf.usermodel.XSSFPictureData> allPics = workbook.getAllPictures();
                log.info("备用方案：workbook 中共有 {} 张图片", allPics.size());

                int dataStartRow = 1;
                int lastRowNum = sheet.getLastRowNum();
                int picIdx = 0;
                for (int r = dataStartRow; r <= lastRowNum && picIdx < allPics.size(); r++) {
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        Cell cell = row.getCell(colIndex);
                        if (cell != null) {
                            String cellStr = cell.toString().trim();
                            // DISPIMG 公式或以 "DISPIMG" 开头的单元格说明该行有人脸照片
                            if (!cellStr.isEmpty()) {
                                byte[] imgData = allPics.get(picIdx).getData();
                                rowImageMap.put(r, imgData);
                                log.info("备用方案提取照片: 行={}, 大小={}字节", r, imgData.length);
                                picIdx++;
                            }
                        }
                    }
                }
            }

            if (rowImageMap.isEmpty()) {
                log.warn("未从Excel中提取到任何图片，请确认人脸照片是通过「插入图片」或「粘贴」方式嵌入Excel单元格的");
            }

            // 按数据行的顺序排列（表头在第0行）
            int dataStartRow = 1;
            int lastRowNum = sheet.getLastRowNum();
            for (int r = dataStartRow; r <= lastRowNum; r++) {
                byte[] img = rowImageMap.get(r);
                images.add(img != null ? img : new byte[0]);
            }
        }
        return images.toArray(new byte[0][]);
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 删除团体订单目录下的所有照片
     */
    private void deleteGroupFaceImages(Long groupOrderId) {
        try {
            List<GroupMember> members = groupMemberMapper.selectList(
                    new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupOrderId, groupOrderId)
            );
            for (GroupMember m : members) {
                if (m.getFaceImagePath() != null && !m.getFaceImagePath().isBlank()) {
                    try {
                        Path absBasePath = Paths.get(uploadPath).toAbsolutePath().normalize();
                        Path path = absBasePath.resolve(m.getFaceImagePath().replace("/uploads/", ""));
                        if (Files.exists(path)) {
                            Files.delete(path);
                            log.info("已删除成员照片: {}", path);
                        }
                    } catch (IOException e) {
                        log.warn("删除照片失败 {}: {}", m.getFaceImagePath(), e.getMessage());
                    }
                }
            }
            // 尝试删除整个团体文件夹
            if (!members.isEmpty() && members.get(0).getFaceImagePath() != null) {
                String dirPath = members.get(0).getFaceImagePath().substring(0, members.get(0).getFaceImagePath().lastIndexOf('/'));
                Path absBasePath = Paths.get(uploadPath).toAbsolutePath().normalize();
                Path dir = absBasePath.resolve(dirPath.replace("/uploads/", ""));
                if (Files.exists(dir)) {
                    try { Files.deleteIfExists(dir); } catch (IOException ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("清理团体照片时出错: {}", e.getMessage());
        }
    }

    @Override
    public Page<GroupOrderVO> getGroupOrders(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<GroupOrder>()
                .orderByDesc(GroupOrder::getCreateTime);
        if (status != null) wrapper.eq(GroupOrder::getStatus, status);

        Page<GroupOrder> orderPage = groupOrderMapper.selectPage(new Page<>(page, size), wrapper);
        Page<GroupOrderVO> result = new Page<>(page, size, orderPage.getTotal());

        List<GroupOrderVO> voList = orderPage.getRecords().stream()
                .map(this::buildGroupOrderVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    @Override
    public Page<GroupOrderVO> getUserGroupOrders(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<GroupOrder>()
                .eq(GroupOrder::getUserId, userId)
                .orderByDesc(GroupOrder::getCreateTime);
        if (status != null) wrapper.eq(GroupOrder::getStatus, status);

        Page<GroupOrder> orderPage = groupOrderMapper.selectPage(new Page<>(page, size), wrapper);
        Page<GroupOrderVO> result = new Page<>(page, size, orderPage.getTotal());

        List<GroupOrderVO> voList = orderPage.getRecords().stream()
                .map(this::buildGroupOrderVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    @Override
    public GroupOrderVO getGroupOrderDetail(Long id) {
        GroupOrder groupOrder = groupOrderMapper.selectById(id);
        if (groupOrder == null) throw new BusinessException("团体订单不存在");
        return buildGroupOrderVO(groupOrder);
    }

    @Override
    @Transactional
    public void auditGroupOrder(Long auditorId, GroupAuditDTO dto) {
        GroupOrder groupOrder = groupOrderMapper.selectById(dto.getGroupOrderId());
        if (groupOrder == null) throw new BusinessException("团体订单不存在");
        if (groupOrder.getStatus() != 0 && groupOrder.getStatus() != 4) {
            throw new BusinessException("该订单已处理");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            groupOrder.setStatus(1); // 已通过
        } else {
            groupOrder.setStatus(2); // 已拒绝
            groupOrder.setAuditRemark(dto.getRemark());
        }
        groupOrder.setAuditUserId(auditorId);
        groupOrder.setAuditTime(LocalDateTime.now());
        groupOrderMapper.updateById(groupOrder);

        log.info("团体订单审核: {} -> {}，备注: {}", dto.getGroupOrderId(), dto.getApproved() ? "通过" : "拒绝", dto.getRemark());
    }

    @Override
    @Transactional
    public void confirmPay(Long groupOrderId, Long userId) {
        GroupOrder groupOrder = groupOrderMapper.selectById(groupOrderId);
        if (groupOrder == null) throw new BusinessException("团体订单不存在");
        if (groupOrder.getStatus() != 1) throw new BusinessException("只有已通过的团体订单才能支付");
        // 订单拥有者或管理员可支付
        if (userId != null && !userId.equals(groupOrder.getUserId())
                && !SecurityUtil.isAdminOrManager()) {
            throw new BusinessException("您无权支付该订单");
        }

        groupOrder.setStatus(3); // 已支付
        groupOrderMapper.updateById(groupOrder);

        String orderNo = "SCG" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") +
                IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();

        TicketOrder order = new TicketOrder();
        order.setOrderNo(orderNo);
        if (groupOrder.getUserId() == null) {
            throw new BusinessException("团体订单缺少用户信息，无法生成支付订单");
        }
        order.setUserId(groupOrder.getUserId());
        order.setScenicId(groupOrder.getScenicId());
        order.setVisitDate(groupOrder.getVisitDate());
        order.setTotalAmount(groupOrder.getTotalAmount());
        order.setPayAmount(groupOrder.getTotalAmount());
        order.setPayType(0);
        order.setPayTime(LocalDateTime.now());
        order.setPayTradeNo("MOCK_GROUP_" + IdUtil.fastSimpleUUID());
        order.setIsGroup(1);
        order.setGroupOrderId(groupOrder.getId());
        order.setStatus(1);
        orderMapper.insert(order);

        TicketType groupTicket = ticketTypeMapper.selectOne(
                new LambdaQueryWrapper<TicketType>().eq(TicketType::getIsGroup, 1)
        );
        if (groupTicket != null) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setTicketTypeId(groupTicket.getId());
            orderItem.setQuantity(groupOrder.getTotalCount());
            orderItem.setUnitPrice(groupTicket.getPrice());
            orderItemMapper.insert(orderItem);

            // 原子更新团体票已售数量
            ticketTypeMapper.increaseSoldCount(groupTicket.getId(), groupOrder.getTotalCount());
        }

        // 异步批量注册团体成员人脸（不阻塞支付响应，前端轮询进度）
        faceService.registerGroupFacesAsync(groupOrder.getId(), order.getId());

        log.info("团体订单支付确认: {}", groupOrderId);
    }

    @Override
    @Transactional
    public GroupOrderVO updateGroupOrder(Long id, Long userId, String groupName, String contactName, String contactPhone, String visitDate) {
        GroupOrder groupOrder = groupOrderMapper.selectById(id);
        if (groupOrder == null) throw new BusinessException("团体订单不存在");
        // 权限校验：只有创建者或管理员能修改
        if (userId != null && !userId.equals(groupOrder.getUserId())) {
            throw new BusinessException("无权修改该订单");
        }
        if (groupOrder.getStatus() == 3) {
            throw new BusinessException("已支付的订单不能修改");
        }
        groupOrder.setGroupName(groupName);
        groupOrder.setContactName(contactName);
        groupOrder.setContactPhone(contactPhone);
        groupOrder.setVisitDate(LocalDate.parse(visitDate));
        // 修改后状态变为"修改待审核"
        if (groupOrder.getStatus() == 1) {
            groupOrder.setStatus(4); // 修改待审核
        }
        groupOrderMapper.updateById(groupOrder);
        log.info("团体订单信息已更新(待重新审核): {}", id);
        return buildGroupOrderVO(groupOrder);
    }

    @Override
    @Transactional
    public void deleteGroupOrder(Long id) {
        GroupOrder groupOrder = groupOrderMapper.selectById(id);
        if (groupOrder == null) throw new BusinessException("团体订单不存在");

        List<GroupMember> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupOrderId, id)
        );
        boolean hasEntered = members.stream().anyMatch(m -> m.getEntryStatus() == 1);
        if (hasEntered) throw new BusinessException("已有成员入园的团体订单不能删除");

        // 已支付订单：清理关联的 TicketOrder、FaceData、EntryLog
        if (groupOrder.getStatus() == 3) {
            TicketOrder linkedOrder = orderMapper.selectOne(
                    new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getGroupOrderId, id));
            if (linkedOrder != null) {
                // 还原已售数量
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, linkedOrder.getId()));
                for (OrderItem item : items) {
                    ticketTypeMapper.decreaseSoldCount(item.getTicketTypeId(), item.getQuantity());
                }
                // 清理人脸数据和百度端人脸
                try {
                    faceService.cleanGroupOrderFaces(linkedOrder.getId());
                } catch (Exception e) {
                    log.warn("清理团体订单关联人脸失败: orderId={}", linkedOrder.getId(), e);
                }
                orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, linkedOrder.getId()));
                orderMapper.deleteById(linkedOrder.getId());
            }
        }

        // 删除照片文件
        deleteGroupFaceImages(id);

        groupMemberMapper.delete(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupOrderId, id));
        groupOrderMapper.deleteById(id);
        log.info("团体订单已删除: {}", id);
    }

    @Override
    @Transactional
    public void deleteGroupMember(Long memberId) {
        GroupMember member = groupMemberMapper.selectById(memberId);
        if (member == null) throw new BusinessException("成员不存在");
        if (member.getEntryStatus() == 1) throw new BusinessException("已入园的成员不能删除");

        // 删除该成员的照片文件
        if (member.getFaceImagePath() != null && !member.getFaceImagePath().isBlank()) {
            try {
                Path absBasePath = Paths.get(uploadPath).toAbsolutePath().normalize();
                Path path = absBasePath.resolve(member.getFaceImagePath().replace("/uploads/", ""));
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("已删除成员照片: {}", path);
                }
            } catch (IOException e) {
                log.warn("删除成员照片失败: {}", e.getMessage());
            }
        }

        groupMemberMapper.deleteById(memberId);

        // 更新团体订单的人数和金额
        GroupOrder groupOrder = groupOrderMapper.selectById(member.getGroupOrderId());
        if (groupOrder != null) {
            long remainingCount = groupMemberMapper.selectCount(
                    new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupOrderId, member.getGroupOrderId())
            );
            TicketType groupTicket = ticketTypeMapper.selectOne(
                    new LambdaQueryWrapper<TicketType>().eq(TicketType::getIsGroup, 1)
            );
            groupOrder.setTotalCount((int) remainingCount);
            if (groupTicket != null) {
                groupOrder.setTotalAmount(groupTicket.getPrice().multiply(BigDecimal.valueOf(remainingCount)));
            }
            groupOrderMapper.updateById(groupOrder);
        }
        log.info("团体成员已删除: {}", memberId);
    }

    private GroupOrderVO buildGroupOrderVO(GroupOrder groupOrder) {
        GroupOrderVO vo = new GroupOrderVO();
        vo.setId(groupOrder.getId());
        vo.setUserId(groupOrder.getUserId());
        vo.setGroupName(groupOrder.getGroupName());
        vo.setContactName(groupOrder.getContactName());
        vo.setContactPhone(groupOrder.getContactPhone());
        vo.setScenicId(groupOrder.getScenicId());
        vo.setVisitDate(groupOrder.getVisitDate());
        vo.setTotalCount(groupOrder.getTotalCount());
        vo.setTotalAmount(groupOrder.getTotalAmount());
        vo.setImportFileUrl(groupOrder.getImportFileUrl());
        vo.setStatus(groupOrder.getStatus());
        vo.setStatusText(getStatusText(groupOrder.getStatus()));
        vo.setCreateTime(groupOrder.getCreateTime());

        List<GroupMember> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupOrderId, groupOrder.getId())
        );
        long enteredCount = members.stream().filter(m -> m.getEntryStatus() == 1).count();
        vo.setEnteredCount((int) enteredCount);

        List<GroupMemberVO> memberVOs = members.stream().map(m -> {
            GroupMemberVO mvo = new GroupMemberVO();
            mvo.setId(m.getId());
            mvo.setRealName(m.getRealName());
            mvo.setIdCard(m.getIdCard());
            mvo.setPhone(m.getPhone());
            mvo.setUserId(m.getUserId());
            mvo.setFaceImagePath(resolveFaceImagePath(m));
            mvo.setFaceStatus(m.getFaceStatus());
            mvo.setFailReason(m.getFailReason());
            mvo.setEntryStatus(m.getEntryStatus());
            return mvo;
        }).collect(Collectors.toList());
        vo.setMembers(memberVOs);

        return vo;
    }

    /**
     * 校验并修正成员人脸照片路径（兼容旧数据中UUID命名与实际手机号命名不一致的问题）
     */
    private String resolveFaceImagePath(GroupMember member) {
        String storedPath = member.getFaceImagePath();
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        // 1. 存储的路径对应文件存在，直接返回
        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path storedFile = basePath.resolve(storedPath.replace("/uploads/", ""));
        if (Files.exists(storedFile)) {
            return storedPath;
        }

        // 2. 文件不存在，尝试按「姓名_手机号」命名规范重建路径
        String dirPart = storedPath.substring(0, storedPath.lastIndexOf('/'));
        String safeName = sanitizeFileName(member.getRealName());
        String safePhone = member.getPhone() != null ? member.getPhone().replaceAll("[^0-9]", "") : "000";
        String expectedFileName = safeName + "_" + safePhone + ".jpg";
        String correctedPath = dirPart + "/" + expectedFileName;

        Path correctedFile = basePath.resolve(correctedPath.replace("/uploads/", ""));
        if (Files.exists(correctedFile)) {
            log.info("修正成员照片路径: {} -> {}", storedPath, correctedPath);
            // 同步回写数据库，下次直接命中
            groupMemberMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<GroupMember>()
                            .eq(GroupMember::getId, member.getId())
                            .set(GroupMember::getFaceImagePath, correctedPath));
            return correctedPath;
        }

        // 3. 都找不到，返回原始路径（前端会显示裂图）
        return storedPath;
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private boolean isAllBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return false;
        }
        return true;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            case 3 -> "已支付";
            case 4 -> "修改待审核";
            default -> "未知";
        };
    }

    @lombok.Data
    public static class GroupMemberTemplate {
        @com.alibaba.excel.annotation.ExcelProperty(index = 0, value = "序号")
        private String seq;
        @com.alibaba.excel.annotation.ExcelProperty(index = 1, value = "姓名")
        private String realName;
        @com.alibaba.excel.annotation.ExcelProperty(index = 2, value = "身份证号")
        private String idCard;
        @com.alibaba.excel.annotation.ExcelProperty(index = 3, value = "手机号")
        private String phone;
        @com.alibaba.excel.annotation.ExcelProperty(index = 4, value = "人脸照片（粘贴或插入图片路径）")
        private String facePhotoUrl;
    }
}
