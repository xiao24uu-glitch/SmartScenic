package com.scenic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.common.exception.BusinessException;
import com.scenic.config.ScenicProperties;
import com.scenic.config.DeepSeekProperties;
import com.scenic.config.BaiduFaceProperties;
import com.scenic.config.LocalCacheStore;
import com.scenic.dto.ConfigUpdateDTO;
import com.scenic.entity.Scenic;
import com.scenic.entity.SysConfig;
import com.scenic.mapper.ScenicMapper;
import com.scenic.mapper.SysConfigMapper;
import com.scenic.service.SysConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final ScenicMapper scenicMapper;
    private final ScenicProperties scenicProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final BaiduFaceProperties baiduFaceProperties;
    private final LocalCacheStore localCacheStore;

    private static final String CONFIG_CACHE_PREFIX = "sys:config:";

    /**
     * 应用启动时从数据库加载配置，覆盖 application.yml 中的默认值。
     * 这样即使重启，数据库中的修改也不会丢失。
     */
    @PostConstruct
    public void initFromDatabase() {
        log.info("从数据库加载景区配置...");
        try {
            List<SysConfig> scenicConfigs = sysConfigMapper.selectList(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigGroup, "scenic"));
            for (SysConfig config : scenicConfigs) {
                String key = config.getConfigKey();
                String value = config.getConfigValue();
                if (value == null || value.isBlank()) continue;

                switch (key) {
                    case "scenic.name" -> {
                        scenicProperties.setName(value);
                        log.info("  ✓ 景区名称: {}", value);
                    }
                    case "scenic.address" -> {
                        scenicProperties.setAddress(value);
                        log.info("  ✓ 景区地址: {}", value);
                    }
                    case "scenic.description" -> {
                        scenicProperties.setDescription(value);
                        log.info("  ✓ 景区介绍已加载");
                    }
                    case "scenic.open_time" -> {
                        scenicProperties.setOpenTime(value);
                        log.info("  ✓ 景区开放时间: {}", value);
                    }
                    case "scenic.close_time" -> {
                        scenicProperties.setCloseTime(value);
                        log.info("  ✓ 景区关闭时间: {}", value);
                    }
                    case "scenic.max_capacity" -> {
                        scenicProperties.setMaxCapacity(Integer.valueOf(value));
                        log.info("  ✓ 最大承载量: {}", value);
                    }
                    case "scenic.logo_url" -> {
                        scenicProperties.setLogoUrl(value);
                        log.info("  ✓ 景区Logo已加载");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从数据库加载景区配置失败，将使用 application.yml 默认值: {}", e.getMessage());
        }

        // 从 scenic 表加载景区信息（作为 sys_config 的补充/兜底）
        try {
            List<Scenic> scenics = scenicMapper.selectList(null);
            if (!scenics.isEmpty()) {
                Scenic scenic = scenics.get(0);
                if (scenicProperties.getName() == null && scenic.getName() != null) {
                    scenicProperties.setName(scenic.getName());
                }
                if (scenicProperties.getAddress() == null && scenic.getAddress() != null) {
                    scenicProperties.setAddress(scenic.getAddress());
                }
                if (scenicProperties.getDescription() == null && scenic.getDescription() != null) {
                    scenicProperties.setDescription(scenic.getDescription());
                }
                if (scenicProperties.getLogoUrl() == null && scenic.getLogoUrl() != null) {
                    scenicProperties.setLogoUrl(scenic.getLogoUrl());
                }
                if (scenicProperties.getOpenTime() == null && scenic.getOpenTime() != null) {
                    scenicProperties.setOpenTime(scenic.getOpenTime().toString());
                }
                if (scenicProperties.getCloseTime() == null && scenic.getCloseTime() != null) {
                    scenicProperties.setCloseTime(scenic.getCloseTime().toString());
                }
                if (scenicProperties.getMaxCapacity() == null && scenic.getMaxCapacity() != null) {
                    scenicProperties.setMaxCapacity(scenic.getMaxCapacity());
                }
                if (scenicProperties.getStatus() == null && scenic.getStatus() != null) {
                    scenicProperties.setStatus(scenic.getStatus());
                }
                log.info("从 scenic 表补充加载景区信息完成: {}, 开放 {}, 关闭 {}, 承载 {}",
                        scenic.getName(), scenic.getOpenTime(), scenic.getCloseTime(), scenic.getMaxCapacity());
            }
        } catch (Exception e) {
            log.warn("从 scenic 表加载景区信息失败: {}", e.getMessage());
        }

        // 同样加载 DeepSeek / 百度配置（如果数据库中有的话）
        try {
            List<SysConfig> allConfigs = sysConfigMapper.selectList(null);
            for (SysConfig config : allConfigs) {
                String key = config.getConfigKey();
                String value = config.getConfigValue();
                if (value == null || value.isBlank()) continue;

                switch (key) {
                    case "deepseek.api-key" -> deepSeekProperties.setApiKey(value);
                    case "deepseek.base-url" -> deepSeekProperties.setBaseUrl(value);
                    case "deepseek.model" -> deepSeekProperties.setModel(value);
                    case "deepseek.max-tokens" -> deepSeekProperties.setMaxTokens(Integer.valueOf(value));
                    case "deepseek.temperature" -> deepSeekProperties.setTemperature(Double.valueOf(value));
                    case "baidu.face.app-id" -> baiduFaceProperties.setAppId(value);
                    case "baidu.face.api-key" -> baiduFaceProperties.setApiKey(value);
                    case "baidu.face.secret-key" -> baiduFaceProperties.setSecretKey(value);
                    case "baidu.face.threshold" -> baiduFaceProperties.setThreshold(Double.valueOf(value));
                }
            }
            log.info("系统配置初始化完成");
        } catch (Exception e) {
            log.warn("加载其他系统配置时出错: {}", e.getMessage());
        }
    }

    @Override
    public Page<SysConfig> getConfigs(Integer page, Integer size, String configGroup) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigGroup)
                .orderByAsc(SysConfig::getId);
        if (configGroup != null && !configGroup.isBlank()) {
            wrapper.eq(SysConfig::getConfigGroup, configGroup);
        }
        // 景区基本信息（scenic.name / address / description / logo_url）
        // 在页面上方有独立表单维护，不在列表中重复展示
        wrapper.notIn(SysConfig::getConfigKey,
                List.of("scenic.name", "scenic.address", "scenic.description", "scenic.open_time", "scenic.logo_url"));

        Page<SysConfig> configPage = sysConfigMapper.selectPage(new Page<>(page, size), wrapper);

        // 脱敏密钥类配置
        configPage.getRecords().forEach(config -> {
            if (config.getIsEncrypted() != null && config.getIsEncrypted() == 1
                    && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
                config.setConfigValue("******" + config.getConfigValue().substring(
                        Math.max(0, config.getConfigValue().length() - 4)));
            }
        });

        return configPage;
    }

    @Override
    @Transactional
    public void updateConfig(ConfigUpdateDTO dto) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, dto.getConfigKey())
        );
        if (config == null) {
            throw new BusinessException("配置项不存在: " + dto.getConfigKey());
        }

        config.setConfigValue(dto.getConfigValue());
        sysConfigMapper.updateById(config);

        // 刷新本地缓存
        String cacheKey = CONFIG_CACHE_PREFIX + dto.getConfigKey();
        localCacheStore.set(cacheKey, dto.getConfigValue());

        // 同步更新对应 Properties 内存中的值
        switch (dto.getConfigKey()) {
            case "scenic.name" -> {
                scenicProperties.setName(dto.getConfigValue());
                log.info("景区名称已更新为: {}", dto.getConfigValue());
            }
            case "scenic.address" -> {
                scenicProperties.setAddress(dto.getConfigValue());
                log.info("景区地址已更新为: {}", dto.getConfigValue());
            }
            case "scenic.description" -> {
                scenicProperties.setDescription(dto.getConfigValue());
                log.info("景区介绍已更新");
            }
            case "scenic.open_time" -> {
                scenicProperties.setOpenTime(dto.getConfigValue());
                log.info("景区开放时间已更新");
            }
            case "scenic.close_time" -> {
                scenicProperties.setCloseTime(dto.getConfigValue());
                log.info("景区关闭时间已更新");
            }
            case "scenic.max_capacity" -> {
                scenicProperties.setMaxCapacity(Integer.valueOf(dto.getConfigValue()));
                log.info("最大承载量已更新");
            }
            case "scenic.logo_url" -> {
                scenicProperties.setLogoUrl(dto.getConfigValue());
                log.info("景区Logo已更新");
            }
            case "deepseek.api-key" -> deepSeekProperties.setApiKey(dto.getConfigValue());
            case "deepseek.base-url" -> deepSeekProperties.setBaseUrl(dto.getConfigValue());
            case "deepseek.model" -> deepSeekProperties.setModel(dto.getConfigValue());
            case "deepseek.max-tokens" -> deepSeekProperties.setMaxTokens(Integer.valueOf(dto.getConfigValue()));
            case "deepseek.temperature" -> deepSeekProperties.setTemperature(Double.valueOf(dto.getConfigValue()));
            case "baidu.face.app-id" -> baiduFaceProperties.setAppId(dto.getConfigValue());
            case "baidu.face.api-key" -> baiduFaceProperties.setApiKey(dto.getConfigValue());
            case "baidu.face.secret-key" -> baiduFaceProperties.setSecretKey(dto.getConfigValue());
            case "baidu.face.threshold" -> baiduFaceProperties.setThreshold(Double.valueOf(dto.getConfigValue()));
            default -> {}
        }

        log.info("配置更新成功: {} = {}", dto.getConfigKey(), dto.getConfigValue());
    }

    @Override
    public String getConfigValue(String configKey) {
        // 先从本地缓存读取
        String cacheKey = CONFIG_CACHE_PREFIX + configKey;
        Object cachedValue = localCacheStore.get(cacheKey);
        if (cachedValue != null) {
            return cachedValue.toString();
        }

        // 从数据库读取
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configKey)
        );
        if (config == null) {
            return null;
        }

        // 写入本地缓存
        localCacheStore.set(cacheKey, config.getConfigValue());
        return config.getConfigValue();
    }

    @Override
    public void refreshConfigCache() {
        localCacheStore.deleteByPrefix(CONFIG_CACHE_PREFIX);
        log.info("配置缓存已刷新");
    }
}
