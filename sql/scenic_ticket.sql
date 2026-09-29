/*
 Navicat Premium Data Transfer

 Source Server         : mysqlll
 Source Server Type    : MySQL
 Source Server Version : 80022
 Source Host           : localhost:3306
 Source Schema         : scenic_ticket

 Target Server Type    : MySQL
 Target Server Version : 80022
 File Encoding         : 65001

 Date: 29/06/2026 23:10:50
*/

CREATE DATABASE IF NOT EXISTS `scenic_ticket`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `scenic_ticket`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_conversation
-- ----------------------------
DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE `ai_conversation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户问题',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI回答',
  `intent` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'other' COMMENT '意图分类 consult/buy_ticket/guide/other',
  `tokens_used` int(0) NULL DEFAULT 0 COMMENT '消耗的tokens数',
  `feedback` tinyint(0) NULL DEFAULT 0 COMMENT '0-未评价 1-有用 2-无用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_session_id`(`session_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 77 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI对话记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for announcement
-- ----------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '公告内容',
  `type` tinyint(0) NULL DEFAULT 1 COMMENT '公告类型 1-一般公告 2-紧急通知 3-活动公告',
  `is_top` tinyint(0) NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '状态 0-隐藏 1-显示',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for coupon
-- ----------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '优惠券名称',
  `type` tinyint(0) NOT NULL COMMENT '类型：1-满减券，2-折扣券',
  `threshold` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛(满多少元可用)',
  `discount_value` decimal(10, 2) NOT NULL COMMENT '优惠值(满减券为金额，折扣券为折扣率如0.85)',
  `max_discount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最大优惠金额(折扣券专用)',
  `total_count` int(0) NOT NULL DEFAULT 0 COMMENT '发行总量',
  `received_count` int(0) NOT NULL DEFAULT 0 COMMENT '已领取数量',
  `used_count` int(0) NOT NULL DEFAULT 0 COMMENT '已使用数量',
  `per_user_limit` int(0) NULL DEFAULT 1 COMMENT '每人限领数量',
  `valid_days` int(0) NOT NULL DEFAULT 30 COMMENT '有效期天数(从领取日起算)',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `max_order_count` int(0) NULL DEFAULT -1 COMMENT '新用户订单上限，-1不限，N表示用户最多N笔已支付订单时可领',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '优惠券模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for entry_log
-- ----------------------------
DROP TABLE IF EXISTS `entry_log`;
CREATE TABLE `entry_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_item_id` bigint(0) NOT NULL COMMENT '订单详情ID',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `tourist_id` bigint(0) NOT NULL COMMENT '游客ID',
  `face_data_id` bigint(0) NULL DEFAULT NULL COMMENT '人脸数据ID',
  `capture_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '抓拍图片路径',
  `compare_score` decimal(6, 4) NULL DEFAULT NULL COMMENT '人脸比对相似度(0-100)',
  `entry_time` datetime(0) NOT NULL COMMENT '入园时间',
  `exit_time` datetime(0) NULL DEFAULT NULL COMMENT '出园时间',
  `gate_no` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'A01' COMMENT '闸机/通道编号',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-失败 1-成功',
  `fail_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_face_entry`(`order_id`, `face_data_id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_entry_time`(`entry_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '入园记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for face_data
-- ----------------------------
DROP TABLE IF EXISTS `face_data`;
CREATE TABLE `face_data`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tourist_id` bigint(0) NOT NULL COMMENT '游客ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `order_id` bigint(0) NULL DEFAULT NULL COMMENT '关联订单ID（票务人脸绑定；内部通道人脸为NULL）',
  `baidu_face_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '百度人脸唯一标识',
  `baidu_group_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '百度人脸库分组ID',
  `face_image_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本地存储的人脸图片路径',
  `quality_score` decimal(5, 2) NULL DEFAULT NULL COMMENT '百度检测返回的质量分',
  `expire_time` datetime(0) NULL DEFAULT NULL COMMENT '过期时间（游览日期+7天）',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-无效 1-有效',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `baidu_user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '百度人脸库中的唯一user_id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tourist_id`(`tourist_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_baidu_face_token`(`baidu_face_token`) USING BTREE,
  INDEX `idx_baidu_group_id`(`baidu_group_id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人脸数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for gate
-- ----------------------------
DROP TABLE IF EXISTS `gate`;
CREATE TABLE `gate`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gate_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '闸机编号，如 A01',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '闸机名称/位置描述',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_gate_no`(`gate_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '闸机表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for group_member
-- ----------------------------
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_order_id` bigint(0) NOT NULL COMMENT '团体订单ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证号（加密存储）',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '关联系统用户ID（注册后回填）',
  `face_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人脸照片路径',
  `face_status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '人脸录入状态 0-未录入 1-已录入',
  `entry_status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '入园状态 0-未入园 1-已入园',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人脸注册失败原因',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_group_order_id`(`group_order_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '团体成员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for group_order
-- ----------------------------
DROP TABLE IF EXISTS `group_order`;
CREATE TABLE `group_order`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '提交用户ID',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '团体名称',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人电话',
  `scenic_id` bigint(0) NOT NULL COMMENT '景区ID',
  `visit_date` date NOT NULL COMMENT '游览日期',
  `total_count` int(0) NOT NULL COMMENT '团体总人数',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '总金额',
  `import_file_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上传的Excel模板文件路径',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝 3-已支付 4-修改待审核',
  `audit_user_id` bigint(0) NULL DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime(0) NULL DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注（拒绝理由）',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scenic_id`(`scenic_id`) USING BTREE,
  INDEX `idx_visit_date`(`visit_date`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '团体订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for order_item
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `ticket_type_id` bigint(0) NOT NULL COMMENT '票种ID',
  `quantity` int(0) NOT NULL COMMENT '数量',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '单价',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单详情表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for refund
-- ----------------------------
DROP TABLE IF EXISTS `refund`;
CREATE TABLE `refund`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `refund_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退款编号',
  `refund_amount` decimal(10, 2) NOT NULL COMMENT '退款金额',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退款原因',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝 3-已退款',
  `audit_user_id` bigint(0) NULL DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime(0) NULL DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_refund_no`(`refund_no`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '退款记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for scenic
-- ----------------------------
DROP TABLE IF EXISTS `scenic`;
CREATE TABLE `scenic`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '景区名称（全局引用）',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '景区地址',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '景区介绍',
  `logo_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'Logo图片路径',
  `open_time` time(0) NULL DEFAULT '08:00:00' COMMENT '开放时间',
  `close_time` time(0) NULL DEFAULT '17:00:00' COMMENT '关闭时间',
  `max_capacity` int(0) NULL DEFAULT 50000 COMMENT '最大日承载量',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-关闭 1-运营',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `banner_images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '首页轮播图(JSON数组)',
  `home_bg_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '首页背景图',
  `tickets_bg_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '购票页背景',
  `ai_bg_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI助手页背景',
  `orders_bg_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '订单页背景',
  `profile_bg_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '个人中心背景',
  `primary_color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '#1a73e8' COMMENT '主题主色',
  `header_color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '#1a73e8' COMMENT '顶部导航颜色',
  `login_bg_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '登录页背景',
  `register_bg_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '注册页背景',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '景区信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for scenic_facility
-- ----------------------------
DROP TABLE IF EXISTS `scenic_facility`;
CREATE TABLE `scenic_facility`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` bigint(0) NOT NULL COMMENT '景区ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设施名称',
  `type` tinyint(0) NOT NULL COMMENT '类型 1-卫生间 2-餐饮 3-停车场 4-医疗 5-游客中心',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '位置描述',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设施图片路径',
  `longitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '纬度',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scenic_id`(`scenic_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '景区设施表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for scenic_spot
-- ----------------------------
DROP TABLE IF EXISTS `scenic_spot`;
CREATE TABLE `scenic_spot`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` bigint(0) NOT NULL COMMENT '景区ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '景点名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '景点介绍',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '景点图片',
  `longitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '纬度',
  `sort_order` int(0) NULL DEFAULT 0 COMMENT '排序序号',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-隐藏 1-显示',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scenic_id`(`scenic_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '景区景点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置值（支持Base64图片存储）',
  `config_group` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置分组',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置说明',
  `is_encrypted` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否加密存储 0-否 1-是',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作用户名',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作模块',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作类型',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作描述',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方式',
  `request_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求参数',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '0-失败 1-成功',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `cost_time` bigint(0) NULL DEFAULT NULL COMMENT '耗时(ms)',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 120 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `role_level` int(0) NOT NULL DEFAULT 99 COMMENT '角色等级(1-最高 2-管理员 3-检票员 4-游客)',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像路径',
  `current_role_id` bigint(0) NULL DEFAULT NULL COMMENT '当前切换的角色ID(为空则使用最高角色)',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` tinyint(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `role_id` bigint(0) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for ticket_order
-- ----------------------------
DROP TABLE IF EXISTS `ticket_order`;
CREATE TABLE `ticket_order`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint(0) NOT NULL COMMENT '下单用户ID',
  `scenic_id` bigint(0) NOT NULL COMMENT '景区ID',
  `visit_date` date NOT NULL COMMENT '游览日期',
  `pending_visit_date` date NULL DEFAULT NULL COMMENT '待审核的游览日期',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单总金额',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '实付金额',
  `pay_type` tinyint(0) NULL DEFAULT NULL COMMENT '支付方式 0-模拟 1-微信 2-支付宝',
  `pay_time` datetime(0) NULL DEFAULT NULL COMMENT '支付时间',
  `pay_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模拟支付流水号',
  `is_group` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否团体订单 0-否 1-是',
  `group_order_id` bigint(0) NULL DEFAULT NULL COMMENT '关联团体订单ID',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消 3-已退款 4-修改待审核 5-已入园 6-已出园',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `coupon_id` bigint(0) NULL DEFAULT NULL COMMENT '使用的优惠券ID',
  `discount_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '优惠金额',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_visit_date`(`visit_date`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1029 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for ticket_type
-- ----------------------------
DROP TABLE IF EXISTS `ticket_type`;
CREATE TABLE `ticket_type`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` bigint(0) NOT NULL COMMENT '景区ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '票种名称',
  `price` decimal(10, 2) NOT NULL COMMENT '售价（元）',
  `total_stock` int(0) NOT NULL DEFAULT 0 COMMENT '总库存',
  `daily_stock` int(0) NOT NULL DEFAULT 0 COMMENT '每日库存上限',
  `sold_count` int(0) NOT NULL DEFAULT 0 COMMENT '已售数量',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '票种说明/使用条件',
  `is_group` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否团体票 0-否 1-是',
  `min_group_size` int(0) NULL DEFAULT NULL COMMENT '团体票最少人数',
  `max_booking_days` int(0) NOT NULL DEFAULT 7 COMMENT '最大可预约天数',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '0-停售 1-在售',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scenic_id`(`scenic_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '票种表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for tourist
-- ----------------------------
DROP TABLE IF EXISTS `tourist`;
CREATE TABLE `tourist`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(0) NOT NULL COMMENT '关联系统用户ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号（AES加密存储）',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `face_status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '人脸录入状态 0-未录入 1-已录入',
  `baidu_face_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '百度人脸库中的face_token',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '游客表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------

-- ----------------------------
-- Table structure for user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `coupon_id` bigint(0) NOT NULL COMMENT '优惠券ID',
  `coupon_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '券名称(冗余)',
  `type` tinyint(0) NULL DEFAULT NULL COMMENT '券类型(冗余)',
  `threshold` decimal(10, 2) NULL DEFAULT NULL COMMENT '门槛(冗余)',
  `discount_value` decimal(10, 2) NULL DEFAULT NULL COMMENT '优惠值(冗余)',
  `max_discount` decimal(10, 2) NULL DEFAULT NULL COMMENT '最大优惠(冗余)',
  `order_id` bigint(0) NULL DEFAULT NULL COMMENT '使用的订单ID',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '0-未使用 1-已使用 2-已过期',
  `valid_from` date NULL DEFAULT NULL COMMENT '有效期开始',
  `valid_until` date NULL DEFAULT NULL COMMENT '有效期结束',
  `receive_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '领取时间',
  `use_time` datetime(0) NULL DEFAULT NULL COMMENT '使用时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_coupon_id`(`coupon_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户优惠券表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Initial system data (no demo/business data)
-- ----------------------------

-- Roles required by RBAC.
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `role_level`, `status`, `create_time`) VALUES
(1, '超级管理员', 'ADMIN', '系统最高权限，可切换任意角色', 1, 1, NOW()),
(2, '景区管理员', 'MANAGER', '票务、订单、团体票管理，可切换检票员角色', 2, 1, NOW()),
(3, '检票员', 'CHECKER', '入园检票、人脸核验、人工通道', 3, 1, NOW()),
(4, '游客', 'TOURIST', '普通游客，购票、入园', 4, 1, NOW());

-- Default administrator account. Password: admin123
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `avatar_url`, `current_role_id`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'admin', '$2a$10$4WRlwLzV3uPYhllXj.jc.uOFx9a2HXI.Vr.E1uTn1bqkJs2VW0ZCO', '超级管理员', NULL, NULL, NULL, 1, 1, NOW(), NULL, 0);

-- The administrator can switch among all roles.
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1),
(2, 1, 2),
(3, 1, 3),
(4, 1, 4);

-- A blank scenic record so the administrator can edit it on the configuration page.
INSERT INTO `scenic` (`id`, `name`, `status`) VALUES (1, '', 1);

-- Configuration keys required by the application. Leave secret values empty for the user to fill in.
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_group`, `description`, `is_encrypted`) VALUES
('deepseek.api-key', '', 'deepseek', 'DeepSeek大模型密钥', 1),
('deepseek.base-url', 'https://api.deepseek.com/chat/completions', 'deepseek', 'API基础地址', 0),
('deepseek.model', 'deepseek-chat', 'deepseek', '使用的模型版本', 0),
('deepseek.max-tokens', '2048', 'deepseek', '最大输出长度', 0),
('deepseek.temperature', '0.7', 'deepseek', '随机性参数', 0),
('baidu.face.api-key', '', 'baidu', '百度智能云API Key', 1),
('baidu.face.secret-key', '', 'baidu', '百度智能云Secret Key', 1),
('baidu.face.app-id', '', 'baidu', '百度智能云App ID', 1),
('baidu.face.threshold', '0.80', 'baidu', '1:N比对相似度阈值', 0),
('ticket.booking_days_normal', '7', 'ticket', '散客最大可预约天数（从今天起算）', 0),
('ticket.booking_days_group', '14', 'ticket', '团体票最大可预约天数（从今天起算）', 0);

SET FOREIGN_KEY_CHECKS = 1;
