package com.scenic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scenic.client.BaiduFaceApiClient;
import com.scenic.common.Result;
import com.scenic.dto.FaceEntryDTO;
import com.scenic.dto.FaceRegisterDTO;
import com.scenic.entity.FaceData;
import com.scenic.entity.TicketOrder;
import com.scenic.mapper.FaceDataMapper;
import com.scenic.mapper.TicketOrderMapper;
import com.scenic.security.SecurityUtil;
import com.scenic.service.FaceService;
import com.scenic.vo.FaceEntryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "人脸识别")
@RestController
@RequestMapping("/api/v1/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceService faceService;
    private final SecurityUtil securityUtil;
    private final FaceDataMapper faceDataMapper;
    private final TicketOrderMapper orderMapper;

    @Operation(summary = "人脸注册（录入）")
    @PostMapping("/register")
    public Result<Void> registerFace(@Valid @RequestBody FaceRegisterDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        faceService.registerFace(userId, dto);
        return Result.success("人脸录入成功", null);
    }

    @Operation(summary = "人脸搜索比对（检票入园）")
    @PostMapping("/search")
    public Result<FaceEntryVO> faceSearch(@Valid @RequestBody FaceEntryDTO dto) {
        return Result.success(faceService.faceEntry(dto));
    }

    @Operation(summary = "清理过期人脸数据")
    @PostMapping("/clean")
    public Result<Void> cleanExpiredFaces() {
        faceService.cleanExpiredFaces();
        return Result.success("清理完成", null);
    }

    @Operation(summary = "查询我的内部通道人脸数据")
    @GetMapping("/my-face")
    public Result<FaceData> getMyFace() {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(faceService.getMyFace(userId));
    }

    @Operation(summary = "删除我的内部通道人脸数据")
    @DeleteMapping("/my-face")
    public Result<Void> deleteMyFace() {
        Long userId = securityUtil.getCurrentUserId();
        faceService.deleteMyFace(userId);
        return Result.success("人脸数据已删除", null);
    }

    @Operation(summary = "查询订单关联的人脸列表")
    @GetMapping("/order-faces")
    public Result<List<FaceData>> getOrderFaces(@RequestParam String orderNo) {
        Long userId = securityUtil.getCurrentUserId();
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getOrderNo, orderNo)
                        .eq(TicketOrder::getUserId, userId)
        );
        if (order == null) {
            return Result.error("订单不存在");
        }
        List<FaceData> list = faceDataMapper.selectList(
                new LambdaQueryWrapper<FaceData>()
                        .eq(FaceData::getOrderId, order.getId())
                        .eq(FaceData::getStatus, 1)
                        .orderByDesc(FaceData::getId)
        );
        return Result.success(list);
    }

    @Operation(summary = "人脸对比 — 判断两张图片是否为同一个人")
    @PostMapping("/compare")
    public Result<BaiduFaceApiClient.FaceMatchResult> compareFaces(@RequestBody Map<String, String> body) {
        String image1 = body.get("image1");
        String image2 = body.get("image2");
        if (image1 == null || image1.isBlank() || image2 == null || image2.isBlank()) {
            return Result.error("两张图片数据不能为空");
        }
        return Result.success(faceService.compareFaces(image1, image2));
    }

    @Operation(summary = "团体成员人脸重新录入 — 针对注册失败成员单独重传照片")
    @PostMapping("/re-register-member")
    public Result<Void> reRegisterMemberFace(@RequestBody Map<String, String> body) {
        String memberIdStr = body.get("memberId");
        String imageBase64 = body.get("imageBase64");
        if (memberIdStr == null || memberIdStr.isBlank() || imageBase64 == null || imageBase64.isBlank()) {
            return Result.error("参数不完整：memberId 和 imageBase64 不能为空");
        }
        try {
            faceService.reRegisterGroupMemberFace(Long.valueOf(memberIdStr), imageBase64);
            return Result.success("人脸重新录入成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
