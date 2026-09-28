package com.scenic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scenic.common.Result;
import com.scenic.common.exception.BusinessException;
import com.scenic.entity.Gate;
import com.scenic.mapper.GateMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "闸机管理")
@RestController
@RequiredArgsConstructor
public class GateController {

    private final GateMapper gateMapper;

    @Operation(summary = "获取启用的闸机列表（下拉选择用）")
    @GetMapping("/api/v1/gates")
    public Result<List<Gate>> listEnabledGates() {
        List<Gate> list = gateMapper.selectList(
                new LambdaQueryWrapper<Gate>()
                        .eq(Gate::getStatus, 1)
                        .orderByAsc(Gate::getGateNo));
        return Result.success(list);
    }

    @Operation(summary = "获取所有闸机")
    @GetMapping("/api/v1/admin/gates")
    public Result<List<Gate>> listAllGates() {
        List<Gate> list = gateMapper.selectList(
                new LambdaQueryWrapper<Gate>()
                        .orderByAsc(Gate::getGateNo));
        return Result.success(list);
    }

    @Operation(summary = "新增/更新闸机")
    @PostMapping("/api/v1/admin/gates")
    public Result<Void> saveGate(@RequestBody Gate gate) {
        if (gate.getGateNo() == null || gate.getGateNo().isBlank()) {
            throw new BusinessException("闸机编号不能为空");
        }
        // 检查编号唯一性
        LambdaQueryWrapper<Gate> wrapper = new LambdaQueryWrapper<Gate>()
                .eq(Gate::getGateNo, gate.getGateNo());
        if (gate.getId() != null) {
            wrapper.ne(Gate::getId, gate.getId());
        }
        Long count = gateMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("闸机编号已存在: " + gate.getGateNo());
        }

        if (gate.getId() != null) {
            gateMapper.updateById(gate);
        } else {
            if (gate.getStatus() == null) {
                gate.setStatus(1);
            }
            gateMapper.insert(gate);
        }
        return Result.success("保存成功", null);
    }

    @Operation(summary = "删除闸机")
    @DeleteMapping("/api/v1/admin/gates/{id}")
    public Result<Void> deleteGate(@PathVariable Long id) {
        gateMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "批量删除闸机")
    @DeleteMapping("/api/v1/admin/gates/batch")
    public Result<String> deleteGates(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的闸机");
        }
        gateMapper.deleteBatchIds(ids);
        return Result.success((String) null, "成功删除 " + ids.size() + " 个闸机");
    }
}
