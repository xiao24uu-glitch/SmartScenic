package com.scenic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scenic.common.Result;
import com.scenic.config.ScenicProperties;
import com.scenic.entity.*;
import com.scenic.mapper.*;
import com.scenic.service.DashboardService;
import com.scenic.vo.AiStatsVO;
import com.scenic.vo.CrowdHeatmapVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "公共接口")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final ScenicProperties scenicProperties;
    private final ScenicMapper scenicMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final ScenicSpotMapper scenicSpotMapper;
    private final ScenicFacilityMapper scenicFacilityMapper;
    private final AiConversationMapper aiConversationMapper;
    private final AnnouncementMapper announcementMapper;
    private final DashboardService dashboardService;

    @Operation(summary = "获取景区基本信息")
    @GetMapping("/scenic-info")
    public Result<Scenic> getScenicInfo() {
        List<Scenic> scenics = scenicMapper.selectList(null);
        return Result.success(scenics.isEmpty() ? null : scenics.get(0));
    }

    @Operation(summary = "获取在售票种")
    @GetMapping("/ticket-types")
    public Result<List<TicketType>> getTicketTypes() {
        return Result.success(ticketTypeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TicketType>()
                        .eq(TicketType::getStatus, 1)));
    }

    @Operation(summary = "获取景点列表")
    @GetMapping("/spots")
    public Result<List<ScenicSpot>> getSpots() {
        return Result.success(scenicSpotMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScenicSpot>()
                        .eq(ScenicSpot::getStatus, 1)
                        .orderByAsc(ScenicSpot::getSortOrder)));
    }

    @Operation(summary = "获取设施列表")
    @GetMapping("/facilities")
    public Result<List<ScenicFacility>> getFacilities() {
        return Result.success(scenicFacilityMapper.selectList(null));
    }

    @Operation(summary = "获取AI服务统计")
    @GetMapping("/ai-stats")
    public Result<AiStatsVO> getAiStats() {
        LocalDate today = LocalDate.now();
        long todayCount = aiConversationMapper.selectCount(
                new LambdaQueryWrapper<AiConversation>()
                        .ge(AiConversation::getCreateTime, today.atStartOfDay())
                        .lt(AiConversation::getCreateTime, today.plusDays(1).atStartOfDay()));
        long totalCount = aiConversationMapper.selectCount(null);
        AiStatsVO vo = new AiStatsVO();
        vo.setTodayCount(todayCount);
        vo.setTotalCount(totalCount);
        return Result.success(vo);
    }

    @Operation(summary = "获取公告列表（公开）")
    @GetMapping("/announcements")
    public Result<List<Announcement>> getPublicAnnouncements() {
        return Result.success(announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, 1)
                        .orderByDesc(Announcement::getIsTop)
                        .orderByDesc(Announcement::getCreateTime)));
    }

    @Operation(summary = "获取客流拥挤度数据（公开）")
    @GetMapping("/crowd-heatmap")
    public Result<CrowdHeatmapVO> getCrowdHeatmap() {
        return Result.success(dashboardService.getCrowdHeatmap());
    }
}
