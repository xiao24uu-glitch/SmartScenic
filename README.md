# 景区门票销售及入场系统（SmartScenic）

> 基于 SpringBoot 3.2 + Vue 3 + MyBatis-Plus + MySQL + Redis/Caffeine + 百度人脸识别 + DeepSeek 大模型的全栈景区票务系统。

## 目录

- [一、项目解决什么问题](#一项目解决什么问题)
- [二、主要功能](#二主要功能)
- [三、安装方法](#三安装方法)
- [四、使用方法](#四使用方法)
- [五、输入输出示例](#五输入输出示例)
- [附录 A：技术栈](#附录-a技术栈)
- [附录 B：项目结构](#附录-b项目结构)
- [附录 C：数据库表（22 张）](#附录-c数据库表22-张)
- [附录 D：API 接口概览](#附录-dapi-接口概览)
- [附录 E：架构与流程图](#附录-e架构与流程图)

## 一、项目解决什么问题

传统景区票房在「售票—检票—统计」链路上普遍存在以下痛点：

- **售票分散**：纸质票与多个第三方渠道各卖各的，票种、价格、总库存与每日限量难以统一管理。
- **检票低效**：依赖人工核对身份证，高峰期排队严重，且存在一票多用、冒用他人票的风险。
- **团体票难处理**：旅行社/学校/企业的团体名单靠 Excel 线下提交，人工录入慢、易出错、无法追踪录入进度。
- **售后无留痕**：退票、改期、优惠券等流程不上线，缺少审核环节与操作审计。
- **数据不透明**：管理者看不到实时客流、销售趋势与财务报表；购票咨询还要靠人工客服重复回答。

本项目把上述环节收敛到一个系统里：**在线选票下单 → 模拟支付 → 人脸录入 → 刷脸入园 → 客流/财务统计**，并用 AI 智能助手承担一部分购票咨询与景区导览工作。

一句话概括：**让景区「售票—支付—检票—统计」全流程线上化，用人脸识别替代人工验票，用 AI 替代部分人工客服。**
## 二、主要功能

### 1. 游客端（Vue 3 前台）

- **景区首页**：轮播图、公告、景区介绍、票种预览，景点与设施展示（含 Leaflet 地图点位）。
- **门票选购**：选择游览日期（散客默认最多提前 7 天，可配置）、票种筛选、库存展示、多票种购物车。
- **优惠券**：领取满减券/立减券，下单时选择使用或取消使用。
- **模拟支付**：支付成功/失败结果回传，订单状态自动流转。
- **订单管理**：订单列表（分页 + 状态筛选）、订单详情、取消订单、申请退款、申请修改游览日期（需审核）。
- **人脸录入**：摄像头实时拍照或本地上传，经百度人脸质量检测后入库；支持按订单逐人录入。
- **团体票**：下载 Excel 模板 → 填写名单 → 上传导入 → 审核 → 支付 → 逐人录入人脸（进度可查）。
- **AI 智能助手**：基于 DeepSeek 的 SSE 流式对话（智能咨询 / 辅助购票 / 景区导览），支持生成 AI 游记并导出 Word。
- **个人中心**：资料与头像修改、修改密码、内部通道人脸查看/删除。

### 2. 检票端

- **人脸核验检票**：调用百度 1:N 搜索比对，相似度阈值可配置（默认 `0.80`）。
- **人工验票入场 / 人工出园**：人脸不可用时的人工兜底通道。
- **今日入园记录、今日有效订单、检票员仪表盘**。

### 3. 管理端（ADMIN / MANAGER 角色）

- **系统仪表盘**：核心指标卡片 + ECharts 多维度图表（销售趋势、票种占比、时段分布等）。
- **票务管理**：票种 CRUD、价格设置、总库存 / 每日库存管理、状态启停。
- **订单管理**：查询（分页 + 筛选）、编辑、删除（回退库存）、改期申请审核。
- **团体票管理**：导入、审核（通过/拒绝）、支付确认、成员增删。
- **退款审核**：退款申请列表、审核（通过/拒绝）、删除记录。
- **入园记录 / 人脸库管理**：查询、批量删除、过期人脸定时任务自动清理。
- **客流监控**：实时入园/在园人数、销售趋势图表。
- **财务报表**：多 Sheet Excel 导出（财务概览 / 7 日趋势 / 票种占比 / 时段分布）。
- **优惠券管理**：模板创建（满减券/立减券）、启停、批量操作、清空领取量。
- **内容与资源运营**：公告管理、景点管理（拖拽排序 + 经纬度）、设施管理（5 类）、闸机管理。
- **用户与权限**：用户 CRUD、四级角色分配、状态启用/禁用。
- **系统配置**：景区信息、DeepSeek/百度密钥、预约天数等动态配置，保存后热生效。
- **AI 对话记录、操作日志**（AOP 自动记录）。

### 4. 技术要点

- **RBAC 四级角色**（ADMIN / MANAGER / CHECKER / TOURIST），多角色用户登录后可一键切换身份并同步刷新 JWT。
- **统一响应体 `Result`** + 全局异常处理 + 参数校验（`@Valid`）。
- **配置热更新**：数据库 `sys_config` → Redis/Caffeine 缓存 → 内存属性，修改完成后无需重启即时生效。
- **缓存可降级**：开发环境用 Caffeine 本地缓存替代 Redis（`application-dev.yml` 已排除 Redis 自动装配）。
- **人脸识别**：百度智能云人脸 V3（检测 / 注册 / 1:N 搜索 / 1:1 比对），Access Token 带缓存。
## 三、安装方法

### 1. 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 7.x（可选，开发环境可不装） |

### 2. 初始化数据库

```bash
mysql -u root -p < sql/scenic_ticket.sql
```

脚本会创建 `scenic_ticket` 库、22 张业务表，并写入初始数据（橘子洲景区信息、4 个角色与 4 个测试账号、5 个票种、2 张优惠券、4 条公告、2 个闸机、景点与设施数据等）。

> `uploads/` 目录（演示图片与用户上传文件）未纳入版本库：克隆后请自行把景区 / 设施 / 背景 / 头像等图片放入对应子目录，否则前端图片位置会显示为空白，数据库中的图片路径字段无需修改。

### 3. 修改数据库连接

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scenic_ticket?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 你的数据库密码
```

默认激活的 profile 是 `dev`（见 `application.yml` 中的 `spring.profiles.active`）。

### 4. 配置 JWT 密钥（可选）

JWT 签名密钥不再硬编码在代码或配置文件里，改为通过环境变量注入（长度至少 32 字节）：

```powershell
# Windows PowerShell（当前会话有效）
$env:JWT_SECRET = "换成你自己的 32 字节以上随机字符串"
```

```bash
# Linux / macOS
export JWT_SECRET="换成你自己的 32 字节以上随机字符串"
```

未设置时后端会生成一次性随机密钥并打印告警：本地调试可以正常使用，但**每次重启都需要重新登录**。生产环境务必显式配置。

生成随机密钥（任选其一）：

```bash
openssl rand -base64 48
```

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

### 5. 配置密钥（AI 与百度人脸）

推荐做法：**先启动系统，登录管理端 →「系统配置」页面填写**。配置保存在数据库 `sys_config` 表中，重启不丢失，保存即热生效。

| 配置项 | 说明 |
|--------|------|
| `deepseek.api-key` | DeepSeek 大模型密钥（AI 助手使用） |
| `deepseek.base-url` | 默认 `https://api.deepseek.com/chat/completions` |
| `deepseek.model` | 默认 `deepseek-chat` |
| `deepseek.max-tokens` / `deepseek.temperature` | 默认 `2048` / `0.7` |
| `baidu.face.app-id` / `baidu.face.api-key` / `baidu.face.secret-key` | 百度智能云人脸识别 V3 凭证 |
| `baidu.face.threshold` | 1:N 比对相似度阈值，默认 `0.80` |
| `ticket.booking_days_normal` | 散客最大可预约天数，默认 `7` |
| `ticket.booking_days_group` | 团体票最大可预约天数，默认 `14` |
| `scenic.name` / `scenic.address` / `scenic.logo_url` / ... | 景区名称、地址、Logo 等，全局引用 |

> 未配置 DeepSeek / 百度密钥时，AI 助手与人脸相关接口会返回「配置未设置」的错误提示，其余功能不受影响。

### 6. 启动后端

```bash
# 项目根目录
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/scenic-ticket-system-1.0.0.jar
```

也可以在 IntelliJ IDEA 中直接运行 `src/main/java/com/scenic/ScenicApplication.java`。

- 后端端口：`8080`
- 接口文档（Knife4j）：http://localhost:8080/doc.html

### 7. 启动前端

```bash
cd scenic-web
npm install
npm run dev
```

`vite.config.js` 已把 `/api` 与 `/uploads` 代理到 `http://localhost:8080`，前端默认监听 `5173` 端口。

### 8. 访问地址

| 端 | 地址 |
|----|------|
| 游客端 | http://localhost:5173 |
| 管理端 | http://localhost:5173/admin |
| 接口文档 | http://localhost:8080/doc.html |

### 默认账号

| 用户名 | 密码 | 角色 | 可切换角色 |
|--------|------|------|------------|
| admin | admin123 | 超级管理员 | ADMIN / MANAGER / CHECKER |
| manager | admin123 | 景区管理员 | MANAGER / CHECKER |
| checker | admin123 | 检票员 | 仅 CHECKER |
| tester | admin123 | 游客 | 仅 TOURIST |

### 常见问题

- **`npm install` 缓慢**：切换国内 npm 镜像后重试。
- **人脸接口报「百度人脸配置未设置」**：在系统配置中补全 `baidu.face.*` 三项。
- **AI 对话报错**：检查 `deepseek.api-key` 是否有效，以及服务器能否访问 `api.deepseek.com`。
- **本机未装 Redis**：保持默认即可，`application-dev.yml` 已排除 Redis 自动配置，自动使用 Caffeine 本地缓存。
- **重启后登录失效 / 控制台提示「未配置 jwt.secret」**：设置环境变量 `JWT_SECRET`（至少 32 字节）后重启后端即可。

## 四、使用方法

### 1. 游客购票入园流程

1. 打开 http://localhost:5173 ，注册或登录（新注册用户自动分配「游客」角色）。
2. 「门票选购」选择游览日期与票种数量，可选优惠券，提交订单（状态「待支付」）。
3. 在支付页确认支付（模拟支付，开关 `pay.mock-enabled: true`），订单变为「已支付」。
4. 进入「人脸录入」，为该订单每张票录入使用人的人脸（拍照或上传，可填姓名/手机号）。
5. 到景区闸机刷脸，检票员在检票端核验通过后，订单变为「已入园」。
6. 可在「我的订单」查看详情、取消订单、申请退款或申请改期；也可在「AI 助手」咨询购票与游玩问题。

### 2. 团体票流程

1. 游客端「团体票购买」下载 Excel 模板。
2. 按模板填写：序号 / 姓名 / 身份证号 / 手机号 / 人脸照片（可在 Excel 中直接粘贴图片）。
3. 上传文件并填写团体名称、联系人、联系电话、游览日期，提交后生成团体订单。
4. 管理员在后台「团体票管理」审核（通过 / 拒绝）并确认支付。
5. 成员人脸录入进度可在页面查看（对应接口 `GET /api/v1/group/face-register-progress/{groupOrderId}`）。

### 3. 管理端使用

- 使用 `admin` / `manager` 登录后访问 http://localhost:5173/admin ，左侧菜单按角色动态显示。
- 使用 `checker` 登录后默认跳转 `/admin/checker-dashboard`。
- 建议首次使用顺序：**系统配置**（景区信息 + 密钥）→ **票种管理** → **景点/设施/闸机** → **公告** → **优惠券** → 正式售票。

### 4. 角色切换

超级管理员 / 景区管理员登录后可在个人中心切换当前操作身份，后端重新签发 JWT（`POST /api/v1/auth/switch-role`），前端菜单与接口权限随之变化。

### 5. 配置热更新

后台修改的任何 `sys_config` 配置（景区名称、密钥、预约天数等）保存后即时生效，无需重启；同时写入 Redis/Caffeine 缓存，应用重启后自动从数据库回载。
## 五、输入输出示例

### 统一响应格式

所有非 SSE 接口统一返回 `Result` 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1782755152634
}
```

- `code = 200`：成功；`401`：未登录/令牌失效；`403`：无权限；其他为业务错误，具体原因见 `message`。
- 除认证接口、公共接口、`/uploads/**` 与接口文档外，其余接口都需要请求头 `Authorization: Bearer <token>`。

### 示例 1：登录

请求：

```
POST /api/v1/auth/login
Content-Type: application/json

{ "username": "tester", "password": "admin123" }
```

响应（节选）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 4,
    "username": "tester",
    "realName": "测试游客",
    "roleName": "游客",
    "roleCode": "TOURIST",
    "roleLevel": 4,
    "availableRoles": [
      { "roleId": 4, "roleName": "游客", "roleCode": "TOURIST", "roleLevel": 4 }
    ],
    "currentRoleId": 4
  },
  "timestamp": 1782755152634
}
```

### 示例 2：查询在售票种（免登录）

```
GET /api/v1/public/ticket-types
```

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "id": 6, "name": "成人票", "price": 40.00, "dailyStock": 15000, "soldCount": 10, "description": "适用于18-59周岁成人", "status": 1 }
  ],
  "timestamp": 1782755152634
}
```
### 示例 3：创建订单

请求：

```
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "visitDate": "2026-10-01",
  "items": [
    { "ticketTypeId": 6, "quantity": 2 },
    { "ticketTypeId": 7, "quantity": 1 }
  ],
  "userCouponId": null
}
```

响应（节选）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderNo": "SC20260928153000A1B2C3",
    "visitDate": "2026-10-01",
    "totalAmount": 100.00,
    "payAmount": 100.00,
    "discountAmount": 0,
    "status": 0,
    "statusText": "待支付",
    "totalTickets": 3,
    "faceCount": 0,
    "items": [
      { "ticketTypeName": "成人票", "quantity": 2, "unitPrice": 40.00, "subtotal": 80.00 },
      { "ticketTypeName": "学生票", "quantity": 1, "unitPrice": 20.00, "subtotal": 20.00 }
    ]
  },
  "timestamp": 1782755152634
}
```

> 订单状态：`0 待支付 / 1 已支付 / 2 已取消 / 3 已退款 / 4 修改待审核 / 5 已入园 / 6 已出园`。

### 示例 4：模拟支付

```
POST /api/v1/orders/pay
Authorization: Bearer <token>
Content-Type: application/json

{ "orderNo": "SC20260928153000A1B2C3", "success": true }
```

```json
{
  "code": 200,
  "message": "success",
  "data": { "orderNo": "SC20260928153000A1B2C3", "status": 1, "statusText": "已支付", "payType": 1, "payTime": "2026-09-28 15:32:10" },
  "timestamp": 1782755152634
}
```

### 示例 5：人脸录入

```
POST /api/v1/face/register
Authorization: Bearer <token>
Content-Type: application/json

{
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "orderNo": "SC20260928153000A1B2C3",
  "realName": "张三",
  "phone": "13800000000"
}
```

```json
{ "code": 200, "message": "人脸录入成功", "data": null, "timestamp": 1782755152634 }
```

### 示例 6：刷脸检票（百度 1:N 搜索）

```
POST /api/v1/face/search
Authorization: Bearer <token>
Content-Type: application/json

{ "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRg...", "gateNo": "A01" }
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "score": 92.6,
    "message": "欢迎入园",
    "userName": "张三",
    "action": "ENTRY",
    "entryTime": "2026-09-28 15:40:02",
    "gateNo": "A01",
    "captureImagePath": "/uploads/face/capture_1782755152634.jpg"
  },
  "timestamp": 1782755152634
}
```

> `action` 取值 `ENTRY`（入园）或 `EXIT`（出园）。相似度低于 `baidu.face.threshold`（默认 `0.80`）或人脸未注册时，`success = false`，`message` 给出原因。
### 示例 7：AI 助手流式对话（SSE）

```
POST /api/v1/ai/chat
Authorization: Bearer <token>
Content-Type: application/json

{ "sessionId": null, "message": "成人票多少钱？开放时间是什么时候？" }
```

响应 `Content-Type: text/event-stream`，事件名分为 `message` / `done` / `error`，逐字返回：

```
event: message
data: {"delta":"成人票","type":"text"}

event: message
data: {"delta":"40元一张，","type":"text"}

event: message
data: {"delta":"景区开放时间为 07:00-22:00。","type":"text"}

event: done
data: {"delta":"[DONE]","type":"done","sessionId":"a1b2c3d4","conversationId":128,"intent":"consult"}
```

> 多轮对话把上一轮返回的 `sessionId` 回传即可保持上下文；`intent` 为 `consult` / `buy_ticket` / `guide`。AI 回复会保存到 `ai_conversation` 表，可在后台「AI 对话记录」查看。另有 `POST /api/v1/ai/travelogue`（游记生成，SSE）与 `POST /api/v1/ai/travelogue/download`（游记导出 Word）。

### 示例 8：团体名单 Excel 导入

```
POST /api/v1/group/import
Authorization: Bearer <token>
Content-Type: multipart/form-data

file=<团体名单.xlsx>
groupName=长沙一中春游团
contactName=李老师
contactPhone=13900000000
visitDate=2026-10-05
```

Excel 模板列（模板可通过 `GET /api/v1/group/template` 下载）：

| 序号 | 姓名 | 身份证号 | 手机号 | 人脸照片（粘贴或插入图片路径） |
|------|------|----------|--------|-------------------------------|
| 1 | 张三 | 4301**********1234 | 13800000000 | （图片） |
| 2 | 李四 | 4301**********5678 | 13800000001 | （图片） |

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 12, "groupName": "长沙一中春游团", "contactName": "李老师", "visitDate": "2026-10-05", "totalCount": 2, "totalAmount": 70.00, "status": 0, "statusText": "待审核" },
  "timestamp": 1782755152634
}
```

### 示例 9：分页查询我的订单

```
GET /api/v1/orders/list?page=1&size=10&status=1
Authorization: Bearer <token>
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "page": 1,
    "size": 10,
    "records": [
      { "orderNo": "SC20260928153000A1B2C3", "visitDate": "2026-10-01", "payAmount": 100.00, "status": 1, "statusText": "已支付", "totalTickets": 3, "faceCount": 1, "entryCount": 0 }
    ]
  },
  "timestamp": 1782755152634
}
```

### 错误响应示例

```json
{ "code": 403, "message": "无权访问该资源", "data": null, "timestamp": 1782755152634 }
{ "code": 500, "message": "库存不足", "data": null, "timestamp": 1782755152634 }
```

### 前端页面级输入输出

| 页面 | 输入 | 输出 |
|------|------|------|
| 门票选购 | 游览日期、票种数量、优惠券 | 生成「待支付」订单 |
| 支付页 | 支付成功 / 失败 | 订单状态变为「已支付」/ 保持「待支付」 |
| 人脸录入 | 摄像头拍照或本地图片 | 提示「人脸录入成功」，订单人脸进度 +1 |
| 检票端 | 现场人脸图片 | 匹配到订单则放行并写入入园记录，否则提示原因 |
| 报表导出 | 日期范围 | 下载多 Sheet Excel 财务报表 |
| AI 助手 | 自然语言问题 | 流式逐字回答，可生成并下载 AI 游记（Word） |
## 附录 A：技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | SpringBoot | 3.2.0 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis（生产）/ Caffeine（本地） | Spring Data Redis / Caffeine |
| 安全框架 | Spring Security + JWT | jjwt 0.12.3 |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| 工具库 | Hutool | 5.8.24 |
| Excel 处理 | EasyExcel / Apache POI | 3.3.3 / 5.2.5 |
| JSON 处理 | FastJSON2 | 2.0.43 |
| 前端框架 | Vue 3 + Vite | 3.4+ / 5.2 |
| UI 组件 | Element Plus | 2.6+ |
| 状态管理 | Pinia | 2.1+ |
| 路由 | Vue Router | 4.3+ |
| 图表 | ECharts / vue-echarts | 5.5+ |
| 地图 | Leaflet | 1.9+ |
| 拖拽排序 | SortableJS | 1.15+ |
| AI 大模型 | DeepSeek API | deepseek-chat |
| 人脸识别 | 百度智能云人脸识别 V3 | — |

## 附录 B：项目结构

```
SmartScenic/
├── sql/
│   └── scenic_ticket.sql               # 建表（22 张）+ 初始化数据
├── mermaid_images/                     # 架构图 / ER 图 / 业务流程图（.mmd）
├── src/main/java/com/scenic/           # 后端源码
│   ├── config/                         # 配置类（11 个）
│   ├── common/                         # Result / 全局异常 / AOP 操作日志
│   ├── security/                       # JWT 认证（Filter + Util + SecurityUtil）
│   ├── entity/                         # 实体类（22 张表）
│   ├── dto/ vo/                        # 数据传输对象 / 视图对象
│   ├── mapper/                         # MyBatis-Plus 映射（22 个）
│   ├── service/ + service/impl/        # 业务接口与实现（9 个）
│   ├── controller/                     # REST 控制器（11 个）
│   ├── client/                         # 外部 API 客户端（百度人脸）
│   ├── task/                           # 定时任务（人脸数据清理）
│   └── ScenicApplication.java          # 启动类
├── src/main/resources/
│   ├── application.yml                 # 主配置（端口 / 上传 / JWT / Knife4j）
│   └── application-dev.yml             # 开发环境配置（数据源 / MyBatis-Plus）
├── scenic-web/                         # 前端 Vue 3 模块
│   ├── src/api/                        # Axios 请求封装
│   ├── src/stores/                     # Pinia 状态管理（用户 / 景区）
│   ├── src/router/                     # 路由配置（游客端 / 管理端）
│   ├── src/layouts/                    # 布局（游客端 / 管理端）
│   ├── src/views/                      # 页面组件
│   ├── package.json
│   └── vite.config.js
├── uploads/                            # 上传目录（头像/人脸/背景/轮播/景点/设施/Logo）
└── pom.xml
```
## 附录 C：数据库表（22 张）

| 分类 | 表名 | 说明 |
|------|------|------|
| 系统管理 | `sys_user`, `sys_role`, `sys_user_role`, `sys_oper_log`, `sys_config` | 用户 / 角色 / 权限 / 日志 / 配置 |
| 景区管理 | `scenic`, `scenic_spot`, `scenic_facility` | 景区 / 景点 / 设施 |
| 票务订单 | `ticket_type`, `ticket_order`, `order_item`, `refund`, `gate` | 票种 / 订单 / 明细 / 退款 / 闸机 |
| 团体票 | `group_order`, `group_member` | 团体订单 / 成员 |
| 人脸入园 | `tourist`, `face_data`, `entry_log` | 游客 / 人脸 / 入园记录 |
| 营销工具 | `coupon`, `user_coupon` | 优惠券模板 / 用户领取 |
| 内容运营 | `announcement` | 景区公告 |
| AI 智能 | `ai_conversation` | AI 对话记录 |

## 附录 D：API 接口概览

> 统一前缀 `/api/v1`。除标注「免登录」外均需 `Authorization: Bearer <token>`。

### 认证 `/auth`（免登录）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 用户登录（返回 JWT + 角色列表） |
| POST | `/auth/register` | 用户注册（自动分配游客角色） |
| POST | `/auth/refresh` | 刷新 Token |
| POST | `/auth/switch-role` | 角色切换（生成新 JWT） |
| GET | `/auth/profile` | 获取个人信息 |
| PUT | `/auth/profile` | 更新个人信息 |
| PUT | `/auth/password` | 修改密码 |
| POST | `/auth/avatar/upload-temp` | 临时上传头像 |
| POST | `/auth/avatar/upload` | 保存头像 |

### 公共 `/public`、`/config`（免登录）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/public/scenic-info` | 景区信息 |
| GET | `/public/ticket-types` | 在售票种 |
| GET | `/public/spots` | 景点列表 |
| GET | `/public/facilities` | 设施列表 |
| GET | `/public/announcements` | 启用中的公告 |
| GET | `/public/ai-stats` | AI 使用统计 |
| GET | `/public/crowd-heatmap` | 客流热力图数据 |
| GET | `/config` | 前端所需配置（景区信息 + 预约天数） |

### 订单 `/orders`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/orders` | 创建订单 |
| GET | `/orders/{orderNo}` | 订单详情 |
| GET | `/orders/list` | 订单列表（分页 + 状态筛选） |
| POST | `/orders/pay` | 模拟支付 |
| PUT | `/orders/{orderNo}/cancel` | 取消订单 |
| DELETE | `/orders/{orderNo}` | 删除订单 |
| PUT | `/orders/{orderNo}/modify` | 申请修改游览日期（需审核） |
| PUT | `/orders/{orderNo}/coupon` | 订单使用优惠券 |
| DELETE | `/orders/{orderNo}/coupon` | 移除订单优惠券 |
| POST | `/orders/refund` | 申请退款 |
| GET | `/orders/refund/list` | 我的退款列表 |

### 人脸 `/face`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/face/register` | 人脸录入（票务人脸 / 内部通道） |
| POST | `/face/search` | 人脸搜索比对（刷脸入园） |
| POST | `/face/clean` | 清理过期人脸数据 |
| GET | `/face/my-face` | 查询内部通道人脸 |
| DELETE | `/face/my-face` | 删除内部通道人脸 |
| GET | `/face/order-faces` | 订单关联人脸列表 |
| POST | `/face/compare` | 两张图片人脸对比 |
| POST | `/face/re-register-member` | 重新录入团体成员人脸 |
### 团体票 `/group`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/group/template` | 下载 Excel 模板 |
| POST | `/group/import` | 上传团体名单导入 |
| GET | `/group/orders` | 团体订单列表（管理端） |
| GET | `/group/my-orders` | 我的团体订单 |
| GET | `/group/orders/{id}` | 团体订单详情 |
| POST | `/group/audit` | 审核团体订单 |
| POST | `/group/pay/{groupOrderId}` | 团体订单支付确认 |
| PUT | `/group/orders/{id}` | 修改团体登记信息 |
| DELETE | `/group/orders/{id}` | 删除团体订单 |
| DELETE | `/group/orders/batch` | 批量删除团体订单 |
| DELETE | `/group/members/{id}` | 删除团体成员 |
| GET | `/group/face-register-progress/{groupOrderId}` | 团体人脸录入进度 |

### AI 助手 `/ai`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/chat` | AI 对话（SSE 流式） |
| POST | `/ai/travelogue` | AI 游记生成（SSE 流式） |
| POST | `/ai/travelogue/download` | 游记导出 Word |
| POST | `/ai/feedback` | 提交 AI 回答评价 |

### 优惠券 `/coupons`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/coupons` | 管理员创建优惠券 |
| GET | `/coupons` | 管理员分页查询 |
| PUT | `/coupons/{id}` | 管理员更新优惠券 |
| PUT | `/coupons/{id}/toggle` | 启用 / 停用 |
| DELETE | `/coupons/{id}` | 删除优惠券 |
| DELETE | `/coupons/batch` | 批量删除 |
| PUT | `/coupons/batch/toggle` | 批量启停 |
| PUT | `/coupons/{id}/clear-received` | 清空领取量 |
| POST | `/coupons/{couponId}/receive` | 用户领取优惠券 |
| GET | `/coupons/available-templates` | 用户可领取的优惠券模板 |
| GET | `/coupons/available` | 用户下单可用优惠券 |
| GET | `/coupons/my` | 我的优惠券 |
| DELETE | `/coupons/user/{userCouponId}` | 删除已领取的优惠券 |

### 检票员 `/checker`（ADMIN / MANAGER / CHECKER）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/checker/verify-face` | 人脸核验检票 |
| POST | `/checker/manual-entry` | 人工验票入场 |
| POST | `/checker/manual-exit` | 人工出园 |
| GET | `/checker/entries` | 今日入园记录 |
| DELETE | `/checker/entries/batch` | 批量删除入园记录 |
| GET | `/checker/valid-orders` | 今日有效订单 |
| GET | `/checker/dashboard` | 检票员仪表盘 |
### 后台管理 `/admin`（ADMIN / MANAGER）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/dashboard` | 仪表盘数据 |
| GET | `/admin/crowd-heatmap` | 客流热力图 |
| GET/POST/PUT/DELETE | `/admin/ticket-types` | 票种 CRUD（支持批量删除） |
| GET/PUT/DELETE | `/admin/orders` | 订单管理（支持批量删除） |
| POST | `/admin/orders/{id}/audit-modify` | 审核订单改期申请 |
| GET | `/admin/orders/{orderNo}/souvenir-ticket` | 生成纪念票 |
| GET/POST/DELETE | `/admin/refunds` | 退款审核（支持批量删除） |
| GET/DELETE | `/admin/entry-logs` | 入园记录管理 |
| GET/DELETE | `/admin/face-data` | 人脸库管理（含批量删除、手动清理） |
| GET/POST/PUT/DELETE | `/admin/users` | 用户管理（含批量删除） |
| GET | `/admin/roles` | 角色列表 |
| PUT | `/admin/users/{id}/roles` | 角色分配 |
| PUT | `/admin/users/{id}/status` | 用户状态启停 |
| GET/PUT | `/admin/configs` | 系统配置（动态热更新） |
| GET/PUT | `/admin/scenic` | 景区信息 |
| GET/POST/DELETE | `/admin/spots` | 景点管理 |
| PUT | `/admin/spots/sort` | 景点拖拽排序 |
| GET/POST/DELETE | `/admin/facilities` | 设施管理 |
| GET/POST/DELETE | `/admin/gates` | 闸机管理 |
| GET/POST/PUT/DELETE | `/admin/announcements` | 公告管理（含批量删除） |
| GET/DELETE | `/admin/ai-conversations` | AI 对话记录 |
| GET | `/admin/oper-logs` | 操作日志 |
| GET | `/admin/reports/export` | 财务报表导出 |
| POST | `/admin/upload/image` | 图片上传 |

### 闸机与静态资源
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/gates` | 启用闸机列表（登录后可用） |
| GET | `/uploads/**` | 上传文件访问（免登录） |

## 附录 E：架构与流程图

`mermaid_images/` 目录提供了可直接渲染的 Mermaid 源文件：

| 文件 | 内容 |
|------|------|
| `01_架构分层示意图.mmd` | 系统分层架构（前端 / 后端 / AI 服务 / 存储） |
| `02_数据库ER图.mmd` | 数据库 ER 关系 |
| `03_散客购票入园流程.mmd` | 散客购票到入园的完整流程 |
| `04_团体票批量导入流程.mmd` | 团体票 Excel 批量导入流程 |
| `05_AI智能助手工作流程.mmd` | AI 助手 SSE 流式对话流程 |

在支持 Mermaid 的编辑器（Typora / VS Code 插件 / GitHub Markdown）中打开即可查看图形。
