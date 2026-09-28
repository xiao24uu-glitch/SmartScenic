package com.scenic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.dto.ConfigUpdateDTO;
import com.scenic.entity.SysConfig;

public interface SysConfigService {
    Page<SysConfig> getConfigs(Integer page, Integer size, String configGroup);
    void updateConfig(ConfigUpdateDTO dto);
    String getConfigValue(String configKey);
    void refreshConfigCache();
}
