package com.scenic.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scenic.config.BaiduFaceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 百度人脸识别API客户端
 * 文档：https://cloud.baidu.com/doc/FACE/s/8k37c1rqz
 */
@Slf4j
@Component
public class BaiduFaceApiClient {

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String FACE_ADD_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add";
    private static final String FACE_SEARCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/search";
    private static final String FACE_DELETE_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/face/delete";
    private static final String USER_DELETE_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/delete";
    private static final String GROUP_DELETE_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/group/delete";
    private static final String FACE_MATCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/match";
    private static final String FACE_DETECT_URL = "https://aip.baidubce.com/rest/2.0/face/v3/detect";

    private final BaiduFaceProperties baiduFaceProperties;

    /** Access Token 缓存（token -> 过期时间戳） */
    private volatile String cachedToken;
    private volatile long tokenExpireTime = 0;

    public BaiduFaceApiClient(BaiduFaceProperties baiduFaceProperties) {
        this.baiduFaceProperties = baiduFaceProperties;
    }

    /**
     * 获取百度 Access Token（带缓存，有效期约30天）
     */
    public synchronized String getAccessToken() {
        // 缓存未过期（提前5分钟刷新）
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime - 300_000) {
            return cachedToken;
        }

        String apiKey = baiduFaceProperties.getApiKey();
        String secretKey = baiduFaceProperties.getSecretKey();

        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new RuntimeException("百度人脸配置未设置，请在系统配置页面填写 api-key 和 secret-key");
        }

        try {
            HttpResponse response = HttpRequest.get(TOKEN_URL)
                    .form("grant_type", "client_credentials")
                    .form("client_id", apiKey)
                    .form("client_secret", secretKey)
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSON.parseObject(response.body());
                if (result.containsKey("access_token")) {
                    cachedToken = result.getString("access_token");
                    int expiresIn = result.getIntValue("expires_in", 2592000);
                    tokenExpireTime = System.currentTimeMillis() + expiresIn * 1000L;
                    log.info("百度 Access Token 获取成功，有效期: {} 天", expiresIn / 86400);
                    return cachedToken;
                } else {
                    String errorMsg = result.getString("error_description");
                    if (StrUtil.isBlank(errorMsg)) {
                        errorMsg = result.getString("error");
                    }
                    if (StrUtil.isBlank(errorMsg)) {
                        errorMsg = "未知错误";
                    }
                    log.error("百度 Access Token 获取失败: {}", errorMsg);
                    throw new RuntimeException("百度 Token 获取失败: " + errorMsg);
                }
            } else {
                log.error("百度 Token 接口请求失败: HTTP {}", response.getStatus());
                throw new RuntimeException("百度 Token 接口请求失败: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("获取百度 Access Token 异常", e);
            throw new RuntimeException("百度 Token 获取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 人脸注册 — 将用户人脸添加到百度人脸库
     *
     * @param imageBase64 人脸图片 Base64（不含前缀 data:image/xxx;base64,）
     * @param groupId     人脸库分组ID（如 scenic_20240608）
     * @param userId      用户标识（如 touristId）
     * @return 注册结果 {face_token, quality_score}
     */
    public FaceRegisterResult registerFace(String imageBase64, String groupId, String userId) {
        String token = getAccessToken();

        JSONObject body = new JSONObject();
        body.put("image", imageBase64);
        body.put("image_type", "BASE64");
        body.put("group_id", groupId);
        body.put("user_id", userId);
        body.put("quality_control", "LOW");       // 质量检测级别: NONE/LOW/NORMAL/HIGH
        body.put("liveness_control", "NONE");      // 活体检测: NONE/LOW/NORMAL/HIGH
        body.put("action_type", "APPEND");         // APPEND 追加，REPLACE 替换

        try {
            HttpResponse response = HttpRequest.post(FACE_ADD_URL + "?access_token=" + token)
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(15000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSON.parseObject(response.body());
                int errorCode = result.getIntValue("error_code", -1);

                if (errorCode == 0) {
                    JSONObject data = result.getJSONObject("result");
                    String faceToken = data.getString("face_token");
                    JSONObject quality = data.getJSONObject("quality");
                    Double score = quality != null ? quality.getDouble("score") : null;

                    log.info("百度人脸注册成功: faceToken={}, groupId={}, userId={}, qualityScore={}",
                            faceToken, groupId, userId, score);

                    FaceRegisterResult r = new FaceRegisterResult();
                    r.setFaceToken(faceToken);
                    r.setQualityScore(score);
                    r.setGroupId(groupId);
                    return r;
                } else {
                    String errorMsg = result.getString("error_msg");
                    log.error("百度人脸注册失败: errorCode={}, errorMsg={}", errorCode, errorMsg);
                    throw new RuntimeException("人脸注册失败: " + errorMsg);
                }
            } else {
                log.error("百度人脸注册接口请求失败: HTTP {}", response.getStatus());
                throw new RuntimeException("人脸注册接口请求失败: " + response.getStatus());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("百度人脸注册异常", e);
            throw new RuntimeException("人脸注册失败: " + e.getMessage(), e);
        }
    }

    /**
     * 人脸搜索（1:N）—— 在指定分组中查找匹配的人脸
     *
     * @param imageBase64  待搜索的人脸图片 Base64
     * @param groupIdList  搜索的分组列表，逗号分隔（如 "scenic_20240608"）
     * @return 搜索结果 {face_token, user_id, score, group_id}
     */
    public FaceSearchResult searchFace(String imageBase64, String groupIdList) {
        String token = getAccessToken();
        double threshold = baiduFaceProperties.getThreshold() != null
                ? baiduFaceProperties.getThreshold() : 0.80;

        log.info("百度人脸搜索请求: imageBase64长度={}, groupIdList={}", 
                imageBase64 != null ? imageBase64.length() : 0, groupIdList);

        JSONObject body = new JSONObject();
        body.put("image", imageBase64);
        body.put("image_type", "BASE64");
        body.put("group_id_list", groupIdList);
        body.put("quality_control", "NONE");       // 不限制质量，尽量检测到人脸
        body.put("liveness_control", "NONE");
        body.put("max_user_num", 1);               // 只返回最匹配的1个

        try {
            HttpResponse response = HttpRequest.post(FACE_SEARCH_URL + "?access_token=" + token)
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(15000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSON.parseObject(response.body());
                int errorCode = result.getIntValue("error_code", -1);

                if (errorCode == 0) {
                    JSONObject data = result.getJSONObject("result");
                    log.info("百度人脸搜索成功响应: {}", result.toJSONString());

                    // 解析人脸数据和用户匹配结果
                    // 百度API返回结构: result 直接包含 face_token 和 user_list
                    String faceToken = data != null ? data.getString("face_token") : null;
                    JSONArray userListArr = data != null ? data.getJSONArray("user_list") : null;

                    // 没有 face_token 且没有 user_list → 真正未检测到人脸
                    if (StrUtil.isBlank(faceToken) && (userListArr == null || userListArr.isEmpty())) {
                        log.info("百度人脸搜索：未检测到人脸, data={}", data != null ? data.toJSONString() : "null");
                        FaceSearchResult r = new FaceSearchResult();
                        r.setMatched(false);
                        r.setScore(0);
                        r.setFailedMessage("未检测到人脸");
                        return r;
                    }

                    // user_list 为空 → 检测到了人脸但库中无匹配用户
                    if (userListArr == null || userListArr.isEmpty()) {
                        log.info("百度人脸搜索：user_list 为空, faceToken={}", faceToken);
                        FaceSearchResult r = new FaceSearchResult();
                        r.setFaceToken(faceToken);
                        r.setMatched(false);
                        r.setScore(0);
                        r.setFailedMessage("未找到匹配的人脸");
                        return r;
                    }

                    JSONObject user = userListArr.getJSONObject(0);

                    String userId = user.getString("user_id");
                    double score = user.getDoubleValue("score");
                    String groupId = user.getString("group_id");
                    String userInfo = user.getString("user_info");

                    log.info("百度人脸搜索成功: faceToken={}, userId={}, score={}, groupId={}",
                            faceToken, userId, score, groupId);

                    FaceSearchResult r = new FaceSearchResult();
                    r.setFaceToken(faceToken);
                    r.setUserId(userId);
                    r.setScore(score);
                    r.setGroupId(groupId);
                    r.setUserInfo(userInfo);

                    // 比对阈值判断
                    r.setMatched(score >= threshold);

                    return r;
                } else {
                    String errorMsg = result.getString("error_msg");
                    // 222207 = 未找到匹配用户（库中无人脸），这是正常情况
                    if (errorCode == 222207) {
                        log.info("百度人脸搜索：未找到匹配用户");
                        FaceSearchResult r = new FaceSearchResult();
                        r.setMatched(false);
                        r.setScore(0);
                        r.setFailedMessage("未找到匹配的人脸");
                        return r;
                    }
                    // 222202 = 图片中没有人脸
                    if (errorCode == 222202) {
                        FaceSearchResult r = new FaceSearchResult();
                        r.setMatched(false);
                        r.setScore(0);
                        r.setFailedMessage("未检测到人脸");
                        return r;
                    }
                    log.error("百度人脸搜索失败: errorCode={}, errorMsg={}", errorCode, errorMsg);
                    throw new RuntimeException("人脸搜索失败: " + errorMsg);
                }
            } else {
                log.error("百度人脸搜索接口请求失败: HTTP {}", response.getStatus());
                throw new RuntimeException("人脸搜索接口请求失败: " + response.getStatus());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("百度人脸搜索异常", e);
            throw new RuntimeException("人脸搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除人脸
     *
     * @param faceToken 百度人脸标识
     * @param groupId   所属分组
     */
    public void deleteFace(String faceToken, String groupId) {
        String token = getAccessToken();

        JSONObject body = new JSONObject();
        body.put("face_token", faceToken);
        body.put("group_id", groupId);

        try {
            HttpResponse response = HttpRequest.post(FACE_DELETE_URL + "?access_token=" + token)
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSON.parseObject(response.body());
                int errorCode = result.getIntValue("error_code", -1);
                if (errorCode == 0) {
                    log.info("百度人脸删除成功: faceToken={}, groupId={}", faceToken, groupId);
                } else {
                    log.warn("百度人脸删除返回错误: errorCode={}, errorMsg={}",
                            errorCode, result.getString("error_msg"));
                }
            }
        } catch (Exception e) {
            log.error("百度人脸删除异常", e);
        }
    }

    /**
     * 按 userId 删除指定分组下的所有人脸
     * 用于清理旧格式 userId（如 tourist_123）的残留数据
     *
     * @param userId  百度用户标识
     * @param groupId 所属分组
     */
    public void deleteFaceByUserId(String userId, String groupId) {
        String token = getAccessToken();

        JSONObject body = new JSONObject();
        body.put("user_id", userId);
        body.put("group_id", groupId);

        try {
            HttpResponse response = HttpRequest.post(USER_DELETE_URL + "?access_token=" + token)
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSON.parseObject(response.body());
                int errorCode = result.getIntValue("error_code", -1);
                if (errorCode == 0) {
                    log.info("百度按userId删除成功: userId={}, groupId={}", userId, groupId);
                } else {
                    // 223105 = 用户不存在，这是正常的（旧数据已不存在）
                    if (errorCode == 223105) {
                        log.info("百度按userId删除：用户不存在（已清理或从未注册）: userId={}, groupId={}", userId, groupId);
                    } else {
                        log.warn("百度按userId删除返回错误: errorCode={}, errorMsg={}",
                                errorCode, result.getString("error_msg"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("百度按userId删除异常: userId={}, groupId={}", userId, groupId, e);
        }
    }

    /**
     * 删除指定分组（清空整个分组下所有用户和人脸）
     *
     * @param groupId 分组ID（如 "scenic_20260624"）
     */
    public void deleteGroup(String groupId) {
        String token = getAccessToken();

        JSONObject body = new JSONObject();
        body.put("group_id", groupId);

        try {
            HttpResponse response = HttpRequest.post(GROUP_DELETE_URL + "?access_token=" + token)
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSON.parseObject(response.body());
                int errorCode = result.getIntValue("error_code", -1);
                if (errorCode == 0) {
                    log.info("百度分组删除成功: groupId={}", groupId);
                } else {
                    log.warn("百度分组删除返回错误: errorCode={}, errorMsg={}",
                            errorCode, result.getString("error_msg"));
                }
            }
        } catch (Exception e) {
            log.warn("百度分组删除异常: groupId={}", groupId, e);
        }
    }

    /**
     * 人脸检测 — 获取人脸质量信息（用于注册后获取质量分）
     *
     * @param imageBase64 人脸图片 Base64
     * @return 综合质量分 0-100（基于完整度、清晰度、光照度计算）
     */
    public Double detectFaceQuality(String imageBase64) {
        String token = getAccessToken();

        JSONObject body = new JSONObject();
        body.put("image", imageBase64);
        body.put("image_type", "BASE64");
        body.put("face_field", "quality");

        try {
            HttpResponse response = HttpRequest.post(FACE_DETECT_URL + "?access_token=" + token)
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(15000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSON.parseObject(response.body());
                int errorCode = result.getIntValue("error_code", -1);

                if (errorCode == 0) {
                    JSONObject data = result.getJSONObject("result");
                    JSONArray faceList = data != null ? data.getJSONArray("face_list") : null;
                    if (faceList != null && !faceList.isEmpty()) {
                        JSONObject face = faceList.getJSONObject(0);
                        JSONObject quality = face.getJSONObject("quality");
                        if (quality != null) {
                            double completeness = quality.getDouble("completeness") != null ? quality.getDouble("completeness") : 0;
                            double blur = quality.getDouble("blur") != null ? quality.getDouble("blur") : 1;
                            double illumination = quality.getDouble("illumination") != null ? quality.getDouble("illumination") : 0;
                            // 综合质量分：完整度40% + 清晰度30% + 光照度30%
                            double blurScore = Math.max(0, 1 - blur) * 100;
                            double illuScore = Math.min(illumination / 255.0, 1.0) * 100;
                            double score = completeness * 40 + blurScore * 0.3 + illuScore * 0.3;
                            log.info("百度人脸检测质量: completeness={}, blur={}, illumination={}, score={}",
                                    completeness, blur, illumination, String.format("%.2f", score));
                            return score;
                        }
                    }
                } else {
                    log.warn("百度人脸检测失败: errorCode={}, errorMsg={}",
                            errorCode, result.getString("error_msg"));
                }
            }
        } catch (Exception e) {
            log.error("百度人脸检测异常", e);
        }
        return null;
    }

    /**
     * 人脸对比 — 对比两张图片中的人脸是否为同一个人
     *
     * @param imageBase641 第一张图片 Base64
     * @param imageBase642 第二张图片 Base64
     * @return 对比结果（相似度分数 score: 0~100）
     */
    public FaceMatchResult matchFaces(String imageBase641, String imageBase642) {
        String token = getAccessToken();

        JSONArray images = new JSONArray();
        JSONObject img1 = new JSONObject();
        img1.put("image", imageBase641);
        img1.put("image_type", "BASE64");
        img1.put("face_type", "LIVE");
        img1.put("quality_control", "LOW");
        img1.put("liveness_control", "NONE");
        images.add(img1);

        JSONObject img2 = new JSONObject();
        img2.put("image", imageBase642);
        img2.put("image_type", "BASE64");
        img2.put("face_type", "LIVE");
        img2.put("quality_control", "LOW");
        img2.put("liveness_control", "NONE");
        images.add(img2);

        try {
            HttpResponse response = HttpRequest.post(FACE_MATCH_URL + "?access_token=" + token)
                    .header("Content-Type", "application/json")
                    .body(images.toJSONString())
                    .timeout(15000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSON.parseObject(response.body());
                int errorCode = result.getIntValue("error_code", -1);
                if (errorCode == 0) {
                    JSONObject data = result.getJSONObject("result");
                    FaceMatchResult r = new FaceMatchResult();
                    r.setScore(data.getDoubleValue("score"));
                    r.setSamePerson(r.getScore() >= 80);
                    JSONArray faceList = data.getJSONArray("face_list");
                    if (faceList != null && faceList.size() >= 2) {
                        r.setFace1Token(faceList.getJSONObject(0).getString("face_token"));
                        r.setFace2Token(faceList.getJSONObject(1).getString("face_token"));
                    }
                    log.info("百度人脸对比成功: score={}, samePerson={}", r.getScore(), r.isSamePerson());
                    return r;
                } else {
                    String errorMsg = result.getString("error_msg");
                    log.error("百度人脸对比失败: errorCode={}, errorMsg={}", errorCode, errorMsg);
                    throw new RuntimeException("人脸对比失败: " + errorMsg);
                }
            } else {
                log.error("百度人脸对比接口请求失败: HTTP {}", response.getStatus());
                throw new RuntimeException("人脸对比接口请求失败: " + response.getStatus());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("百度人脸对比异常", e);
            throw new RuntimeException("人脸对比失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新 Access Token 缓存
     */
    public void refreshToken() {
        cachedToken = null;
        tokenExpireTime = 0;
        getAccessToken();
    }

    // ==================== 内部结果类 ====================

    @lombok.Data
    public static class FaceRegisterResult {
        private String faceToken;
        private Double qualityScore;
        private String groupId;
    }

    @lombok.Data
    public static class FaceSearchResult {
        private String faceToken;
        private String userId;
        private double score;
        private String groupId;
        private String userInfo;
        private boolean matched;
        private String failedMessage;
    }

    @lombok.Data
    public static class FaceMatchResult {
        private double score;
        private boolean samePerson;
        private String face1Token;
        private String face2Token;
    }
}
