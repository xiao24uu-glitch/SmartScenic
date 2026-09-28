package com.scenic.controller;

import com.scenic.common.Result;
import com.scenic.entity.Scenic;
import com.scenic.mapper.ScenicMapper;
import com.scenic.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "配置获取")
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ScenicMapper scenicMapper;
    private final SysConfigService sysConfigService;

    @Operation(summary = "获取前端所需配置（景区名称等）")
    @GetMapping
    public Result<Map<String, Object>> getConfig() {
        List<Scenic> scenics = scenicMapper.selectList(null);
        Scenic scenic = scenics.isEmpty() ? null : scenics.get(0);
        Map<String, Object> config = new HashMap<>();
        if (scenic != null) {
            config.put("scenicName", scenic.getName());
            config.put("scenicAddress", scenic.getAddress());
            config.put("scenicDescription", scenic.getDescription());
            config.put("scenicOpenTime", scenic.getOpenTime() != null ? scenic.getOpenTime().toString() : null);
            config.put("scenicCloseTime", scenic.getCloseTime() != null ? scenic.getCloseTime().toString() : null);
            config.put("scenicMaxCapacity", scenic.getMaxCapacity());
            config.put("scenicStatus", scenic.getStatus());
            config.put("scenicLogoUrl", scenic.getLogoUrl());
        }
        // 预约天数配置（散客/团体各一档）
        String normalDays = sysConfigService.getConfigValue("ticket.booking_days_normal");
        String groupDays = sysConfigService.getConfigValue("ticket.booking_days_group");
        config.put("bookingDaysNormal", normalDays != null ? Integer.valueOf(normalDays) : 7);
        config.put("bookingDaysGroup", groupDays != null ? Integer.valueOf(groupDays) : 14);
        return Result.success(config);
    }
}
