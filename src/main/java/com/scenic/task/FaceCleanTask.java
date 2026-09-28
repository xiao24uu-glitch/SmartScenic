package com.scenic.task;

import com.scenic.service.FaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FaceCleanTask {

    private final FaceService faceService;

    /**
     * 每天凌晨2点清理过期人脸数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredFaces() {
        log.info("开始执行过期人脸数据清理任务");
        try {
            faceService.cleanExpiredFaces();
            log.info("过期人脸数据清理任务完成");
        } catch (Exception e) {
            log.error("过期人脸数据清理任务异常", e);
        }
    }
}
