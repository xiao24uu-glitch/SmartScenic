package com.scenic.service;

import com.scenic.vo.CrowdHeatmapVO;
import com.scenic.vo.DashboardVO;

public interface DashboardService {
    DashboardVO getDashboard();

    CrowdHeatmapVO getCrowdHeatmap();
}
