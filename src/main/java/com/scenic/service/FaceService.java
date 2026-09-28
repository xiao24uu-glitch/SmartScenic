package com.scenic.service;

import com.scenic.client.BaiduFaceApiClient;
import com.scenic.dto.FaceEntryDTO;
import com.scenic.dto.FaceRegisterDTO;
import com.scenic.entity.FaceData;
import com.scenic.vo.FaceEntryVO;
import com.scenic.vo.GroupFaceProgressVO;

public interface FaceService {
    void registerFace(Long userId, FaceRegisterDTO dto);
    FaceEntryVO faceEntry(FaceEntryDTO dto);
    void cleanExpiredFaces();
    FaceData getMyFace(Long userId);
    void deleteMyFace(Long userId);
    /** 人脸对比 — 判断两张图片是否为同一个人 */
    BaiduFaceApiClient.FaceMatchResult compareFaces(String imageBase641, String imageBase642);
    /** 出园操作 — 记录出园时间，若全部出园则更新订单状态为已出园 */
    void exitPark(String orderNo);
    /** 团体订单支付后批量注册人脸 — 将团体成员的人脸注册到百度库并写入 face_data 表 */
    void registerGroupFaces(Long groupOrderId, Long ticketOrderId);
    /** 异步批量注册团体人脸（支付后调用，不阻塞HTTP响应） */
    void registerGroupFacesAsync(Long groupOrderId, Long ticketOrderId);
    /** 查询团体人脸批量注册进度 */
    GroupFaceProgressVO getGroupFaceProgress(Long groupOrderId);
    /** 清理团体订单关联的人脸数据（百度端+本地文件+数据库） */
    void cleanGroupOrderFaces(Long ticketOrderId);
    /** 为单个团体成员重新录入人脸（适用于首次注册失败或需更换照片的场景） */
    void reRegisterGroupMemberFace(Long memberId, String imageBase64);
}
