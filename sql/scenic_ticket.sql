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
-- Records of ai_conversation
-- ----------------------------
INSERT INTO `ai_conversation` VALUES (77, 1, '211e853d00b34ee88ee180f9371d1d3e', '我想买票', '好的！请问您计划哪天来游玩呢？', 'buy_ticket', 15, 0, '2026-06-29 19:30:10');
INSERT INTO `ai_conversation` VALUES (78, 1, '211e853d00b34ee88ee180f9371d1d3e', '今天', '好的！今天是个好日子～我们有以下票种可供选择：\n\n- **成人票**：¥40.00（18-59周岁）\n- **学生票**：¥20.00（全日制在校学生，凭学生证）\n- **老人票**：¥20.00（60周岁及以上，凭身份证）\n- **儿童票**：¥10.00（6-17周岁，须成人陪同）\n- **团体票**：¥35.00（10人及以上团体）\n\n请问您需要哪种票呢？', 'consult', 182, 0, '2026-06-29 19:30:15');
INSERT INTO `ai_conversation` VALUES (79, 1, '211e853d00b34ee88ee180f9371d1d3e', '我要买成人票1张', '好的，为您汇总一下购物车信息：\n\n📋 **购物车确认**\n- 游览日期：今天\n- 票种及数量：成人票 × 1张，小计 ¥40.00\n- **合计：¥40.00**\n\n请确认以上信息是否正确？', 'buy_ticket', 96, 0, '2026-06-29 19:30:22');
INSERT INTO `ai_conversation` VALUES (80, 1, 'travelogue_SC20260629193026E65173', '生成游记 - 订单SC20260629193026E65173', '# 🌟《橘子洲头，我的40元“穿越”之旅》🌟\n\n📝 **游览概况：2026年6月29日，我花了40元（一张成人票），在晚上8点15分32秒“闪现”橘子洲！** 🎒\n\n当夜幕降临，我踩着月光走进橘子洲，心里默念：“独立寒秋？不，我是‘独立夏夜’！”🌙 毕竟，6月底的长沙，连风都是热的，但橘子洲的风，却带着湘江的清凉和历史的厚重——这波“穿越”不亏！\n\n---\n\n## 🗺️ **景点体验：从“指点江山”到“放飞自我”**\n\n### 🌟 **毛泽东青年艺术雕塑：32米高的“凡尔赛”**\n一进景区，我就被那尊高32米、长83米的雕塑震撼到了！**“这哪是雕塑，分明是‘凡尔赛’本赛！”** 毛主席的飘逸长发、深邃目光，仿佛在说：“问苍茫大地，谁主沉浮？”我默默掏出手机，拍了张“同款pose”，结果被朋友吐槽：“你这是在模仿‘思考者’吗？”🤣\n\n### 🌟 **问天台：我也有“豪情壮志”**\n站在问天台上，俯瞰湘江两岸，我忽然有种“指点江山”的冲动。但当我准备吟诗一首时，旁边的游客突然喊：“帅哥，借过一下，我拍个抖音！”**好吧，我的“豪情壮志”瞬间被“社死”现实打败。** 😂\n\n### 🌟 **百米高喷：音乐喷泉的“夜店风”**\n晚上的百米音乐喷泉，简直是个“大型蹦迪现场”！水柱随音乐节奏摇摆，灯光闪烁，我差点以为自己是来参加“水上电音节”的。**“这票价，值了！”** 我一边拍视频，一边跟着节奏扭动，结果被旁边的大爷投来“关爱智障”的眼神。😅\n\n### 🌟 **橘洲沙滩公园：长沙人的“马尔代夫”**\n听说这里有长沙唯一的户外沙滩，我立刻冲过去！结果发现，沙滩上全是人，**“这哪是‘马尔代夫’，分明是‘长沙的饺子馆’！”** 🥟 但不得不说，吹着江风、踩着沙子，确实有种“逃离城市”的错觉。\n\n---\n\n## 🍜 **美食推荐：别问，问就是“茶颜悦色”**\n在橘子洲，我忍住了吃“臭豆腐”的冲动（因为排队太长），但必须推荐**茶颜悦色**！**“幽兰拿铁”配“湘江夜景”，简直是“神仙组合”！** 我一边喝奶茶，一边对着湘江发呆，心想：**“这才是生活啊！”** 当然，如果你不怕排队，可以试试景区里的“长沙大香肠”，那味道，**“绝了！”** 🌭\n\n---\n\n## 💡 **实用贴士：避免“社死”指南**\n1. **时间选择**：晚上8点入园，完美避开白天暴晒，还能看夜景！🌙\n2. **交通**：地铁2号线直达，千万别开车！**“停车难，难于上青天！”** 🚗\n3. **装备**：带好防蚊喷雾，不然你会成为“蚊子自助餐”！🦟\n4. **拍照**：雕塑和喷泉是必拍点，但记得避开人群——**“否则你的照片里全是‘路人甲’。”** 📸\n\n---\n\n## ✨ **结语：40元的“快乐暴击”**\n走出橘子洲时，我回头看了一眼夜景，心里默默感叹：**“40元，买不了吃亏，买不了上当，但能买来一个‘穿越’到1925年的夜晚。”** 🌟\n\n如果你也想体验“独立寒秋”的豪情，或者单纯想吹吹湘江的晚风，**橘子洲，值得！** 🎒\n\n**“湘江北去，我往南走，下次再来！”** 👋', 'travelogue', 1317, 0, '2026-06-29 20:42:04');
INSERT INTO `ai_conversation` VALUES (81, 1, 'travelogue_SC20260629193026E65173', '生成游记 - 订单SC20260629193026E65173', '# 🌟 橘子洲夜游记：湘江之畔的红色诗篇 🌟\n\n📝 **题记**：2026年6月29日，傍晚8点15分，我踏入了这片毛泽东笔下“独立寒秋”的土地。虽然已是夏夜，但湘江的风依然带着凉意，仿佛在诉说着百年前那位青年在此“问苍茫大地”的豪情。\n\n---\n\n## 🎒 游览概况\n\n傍晚时分，我怀揣着对历史与自然的好奇，购得一张成人票（¥40），从橘子洲大桥入口入园。夜幕初降，华灯初上，江风拂面，游人如织。我沿着洲上的林荫道缓步前行，脚下是百年沉淀的泥土，耳畔是湘江的涛声，仿佛每一步都在与历史对话。\n\n---\n\n## 🗺️ 景点体验\n\n### 毛泽东青年艺术雕塑 🏛️\n\n走过橘洲公园的绿荫，首先映入眼帘的是那座高达32米的巨型雕塑。灯光下，青年毛泽东的目光深邃而坚定，长发飘逸，仿佛正俯瞰着这片他曾经“问苍茫大地”的土地。我仰望着他，心中涌起一股莫名的感动——32米寓意他32岁来长沙，83米寓意享年83岁，41米寓意执政41年，每一个数字都饱含着历史的重量。\n\n### 问天台 🌌\n\n沿着湘江边走，我来到问天台。这里曾是青年毛泽东读书和思考的地方。站在台上，湘江两岸的灯火璀璨，岳麓山在夜色中若隐若现。我闭上眼睛，仿佛能听到百年前那位青年在此吟诵“问苍茫大地，谁主沉浮”的声音。那一刻，时间仿佛凝固了。\n\n### 橘洲公园 🌿\n\n穿过问天台，我漫步在橘洲公园的林间小径。虽然已是盛夏，但园内依然绿意盎然，橘树成林。我坐在一处观景亭里，看着远处湘江上的游船缓缓驶过，耳边传来江水的轻吟。这里没有城市的喧嚣，只有自然的宁静与历史的回响。\n\n### 百米高喷 💦\n\n走到公园中部，忽然听到音乐响起，接着一道水柱冲天而起，高达100米的喷泉在灯光下变幻着色彩。水花在夜风中飘散，带来丝丝凉意。游客们纷纷驻足，孩子们在喷泉边嬉戏，笑声与音乐声交织在一起，为这片古老的土地增添了生机。\n\n### 《沁园春·长沙》诗词碑 📜\n\n最后，我来到诗词碑前。巨型石碑上镌刻着毛泽东手书的《沁园春·长沙》全文。“独立寒秋，湘江北去，橘子洲头……”我轻声诵读着这些诗句，每一个字都仿佛带着历史的温度。站在碑前，我仿佛看到了那个意气风发的青年，他的诗句穿越时空，依然激励着后人。\n\n---\n\n## ✨ 美食推荐\n\n游览结束后，我在洲头附近的小摊买了一份长沙臭豆腐（¥10）和一杯冰凉的绿豆沙（¥8）。臭豆腐外酥里嫩，配上辣椒酱，让人欲罢不能。绿豆沙清甜解暑，正好驱散夏夜的燥热。强烈推荐！🌟\n\n---\n\n## 🎒 实用贴士\n\n- **最佳游览时间**：傍晚至夜晚（18:00-21:00），既能欣赏日落，又能感受夜景的璀璨。\n- **交通**：可乘坐地铁2号线至“橘子洲站”下车，或从橘洲大桥步行入口进入。\n- **门票**：成人票¥40，学生票半价，建议提前在官方平台购票。\n- **穿着**：建议穿舒适的平底鞋，因为景区较大，步行较多。\n- **注意事项**：夜晚游览时注意防蚊虫，携带驱蚊液。\n\n---\n\n## 🌟 结语\n\n当我走出橘子洲，回头望去，湘江两岸的灯火与洲上的星光交相辉映。这片土地见证了历史的沧桑，也承载着无数人的梦想与情怀。毛泽东曾在此“问苍茫大地”，而今天，我在这里找到了属于自己的答案——历史从未远去，它就在我们脚下，在每一寸土地、每一句诗行中。✨\n\n**橘子洲，不虚此行。** 🌟', 'travelogue', 1403, 0, '2026-06-29 20:56:35');
INSERT INTO `ai_conversation` VALUES (82, 1, 'travelogue_SC20260629193026E65173', '生成游记 - 订单SC20260629193026E65173', '# 🌟 湘江夜话：橘子洲的时空漫游 📝\n\n## 一、初见·暮色中的画卷 🎒\n\n2026年6月29日20:15，我踏着夏夜的凉风，走进橘子洲景区。湘江在暮色中泛着粼粼波光，仿佛一条流动的丝带，将洲头与两岸的灯火串连。🎫 一张成人票，¥40，换来的是一场穿越时空的浪漫之旅。\n\n## 二、洲头·诗意的起点 ✨\n\n沿着蜿蜒的步道向南，我来到橘子洲头。夜色中，“指点江山”石碑静静矗立，仿佛在诉说着百年前那个青年诗人的豪情。站在洲头，极目远眺，湘江北去，岳麓山在灯火中若隐若现。那一刻，我仿佛听见了《沁园春·长沙》的吟诵声：“独立寒秋，湘江北去，橘子洲头……” 🗺️\n\n## 三、雕塑·永恒的凝视 🌟\n\n毛泽东青年艺术雕塑在夜色中更显庄重。32米的高度，寓意着他32岁来到长沙的青春岁月；83米的长度，仿佛在诉说着一个世纪的沧桑。雕像面向东南，飘逸的长发在夜风中微微颤动，深邃的目光望向远方。我站在雕像前，心中涌起“问苍茫大地，谁主沉浮”的豪情。📷\n\n## 四、美食·舌尖上的长沙 🍜\n\n游览间隙，我在洲上的小吃街驻足。一碗热腾腾的臭豆腐，外酥里嫩，配上特制的辣椒酱，让人欲罢不能。再来一份糖油粑粑，甜而不腻，仿佛把整个长沙的甜蜜都尝遍了。🌶️\n\n## 五、实用贴士·夜游攻略 🎒\n\n- **最佳时间**：傍晚6点后入园，既能欣赏日落，又能感受夜景\n- **交通方式**：地铁2号线直达，出站即是景区入口\n- **穿搭建议**：夏季夜晚凉爽，建议带一件薄外套\n- **必带物品**：驱蚊水、充电宝、相机（夜景超美）\n- **游览时长**：建议3-4小时，悠闲漫步最惬意\n\n## 六、结语·星城之恋 ✨\n\n离开时，回望橘子洲，灯火璀璨，湘江两岸的繁华尽收眼底。这座“山水洲城”，在夜色中绽放着它独有的魅力。橘子洲不仅是一处风景，更是一段历史，一份情怀。🌟\n\n我相信，每个来到橘子洲的人，都会在心中留下一首属于自己的诗。而我，带着这份诗意，继续我的旅途。📝', 'travelogue', 834, 0, '2026-06-29 21:04:42');
INSERT INTO `ai_conversation` VALUES (83, 1, 'travelogue_SC20260629193026E65173', '生成游记 - 订单SC20260629193026E65173', '# 湘水悠悠，橘子洲头 🌟\n\n📝 **游览概况**\n\n2026年6月29日，傍晚时分，我怀揣着一颗向往诗意的心，踏上了橘子洲这片传奇之地。购票入园时间定格在20:15，暮色渐浓，华灯初上。🎒门票40元，换来一场与湘江、与历史的深情对话。\n\n---\n\n## 漫步橘子洲，触摸历史的温度 🗺️\n\n夜色中的橘子洲，像一条绿色的巨轮，静静停泊在湘江的怀抱里。我沿着潇湘大道风光带缓步前行，江风拂面，两岸灯火璀璨，长沙的天际线在夜色中熠熠生辉。\n\n**毛泽东青年艺术雕塑**是这里的灵魂所在。当它出现在视野里时，我不由屏住了呼吸。32米高的雕像，在夜色灯光下更显庄重深邃。飘逸的长发，深邃的目光，仿佛正凝视着湘江北去，凝视着这片他深爱的土地。那一刻，我仿佛听见了那句穿越时空的叩问——“问苍茫大地，谁主沉浮？”✨\n\n继续南行，来到**问天台**。相传青年毛泽东常在此读书思考，如今我站在这方寸之地，看湘江奔流，岳麓山在远处若隐若现。夜色中的江水泛着粼粼波光，仿佛在诉说着百年前那个热血青年的理想与豪情。我闭上眼睛，感受江风拂过脸庞，心中涌起莫名的感动。🌟\n\n## 江畔寻味，舌尖上的长沙 🍜\n\n从问天台折返，我在橘洲公园的步道上偶遇一家小吃摊。老板热情推荐了长沙臭豆腐——黑褐色的豆腐块在油锅里翻滚，捞出后淋上特制酱汁，外酥里嫩，一口咬下，汁水在口腔中爆开，辣而不燥，香而不腻。再配上一杯冰凉的绿豆汤，暑气顿消，好不惬意！\n\n## 实用贴士 📌\n\n- 🕐 **最佳游览时间**：傍晚至夜晚，可同时欣赏夕阳与夜景\n- 👟 **穿着建议**：舒适的平底鞋，园区面积大，需步行较长时间\n- 🌂 **必备物品**：防晒霜、驱蚊水、充电宝（夜景拍照耗电快）\n- 🚇 **交通方式**：地铁2号线橘子洲站直达，出站即达景区入口\n- 💡 **小提醒**：夜间游览注意安全，结伴而行更佳\n\n---\n\n## 结语 🎯\n\n离开橘子洲时，已是深夜。回头望去，洲上的灯火如繁星点点，湘江水依旧静静流淌。四十元的门票，换来一场穿越百年的心灵之旅，值得。\n\n想起《沁园春·长沙》中的名句：“恰同学少年，风华正茂；书生意气，挥斥方遒。”站在橘子洲头，我仿佛也成了那个意气风发的少年，在这片红色的土地上，感受着历史的厚重与青春的热烈。\n\n橘子洲，不只是一座江心洲，更是一首流动的诗，一幅会呼吸的画。它用千年的江水，洗去游人心中的尘埃；用百年的历史，点燃我们心中不灭的梦想。🌟\n\n**愿每一个来到橘子洲的人，都能在这里找到属于自己的“谁主沉浮”的答案。** ✨', 'travelogue', 1072, 0, '2026-06-29 21:08:03');
INSERT INTO `ai_conversation` VALUES (84, 1, 'travelogue_SC20260629193026E65173', '生成游记 - 订单SC20260629193026E65173', '# 🌟 橘洲夜话：湘江畔的诗与远方\n\n📝 **游览概况**  \n2026年6月29日，暮色初临，我携一张成人票踏入橘子洲景区。入园时间定格在20:15，夏夜的微风裹着湘江水汽拂面而来。门票仅¥40，却换来一场穿越时空的对话——与伟人、与历史、与这座城的灵魂。🎒\n\n---\n\n## ✨ 夜色中的诗意启程\n\n踏过橘洲大桥，湘江两岸华灯初上，如繁星落入人间。桥上的步行通道是俯瞰全景的绝佳位置：江水如墨，洲上绿荫掩映着点点灯火，远处岳麓山轮廓依稀可辨。我深吸一口气，仿佛能嗅到百年前那首《沁园春·长沙》的墨香。\n\n---\n\n## 🌟 景点体验：每一步都是历史回响\n\n**毛泽东青年艺术雕塑**  \n夜幕下的雕塑更显庄严。32米的身躯、83米的长发、41米的宽厚，每一寸都诉说着“问苍茫大地，谁主沉浮”的豪情。雕像面向东南，仿佛青年毛泽东正凝望湘江，思考着民族的未来。我站在雕像前，久久不愿离去，直到夜色将它的轮廓勾勒成一幅剪影。\n\n**问天台与诗词碑**  \n漫步至洲头，问天台静卧江畔。相传青年毛泽东常在此读书思考，而《沁园春·长沙》的不朽诗句便诞生于此。我抚摸着石碑上那行“独立寒秋，湘江北去”，指尖仿佛触到了历史的脉搏。不远处，诗词碑上镌刻着毛泽东手书全文，笔力遒劲，与夜色中的江水相映成趣。\n\n**百米高喷与橘洲公园**  \n行至洲中，百米音乐喷泉正随《浏阳河》的旋律起舞。水柱冲上100米高空，在灯光下幻化成七彩瀑布，引得游人阵阵惊叹。橘洲公园内，橘林在月光下泛着微光，偶有夜鸟惊起，划破宁静。我在长椅上小憩，看江上渔火点点，听风过橘林沙沙作响。\n\n---\n\n## 🍜 美食推荐：舌尖上的湘江夜\n\n洲头的“橘洲驿”茶室值得一试。点一杯安化黑茶，配几块长沙臭豆腐和糖油粑粑，茶香与辣味交织，是夏夜最熨帖的搭配。若想尝鲜，不妨试试“沁园春”主题餐厅的剁椒鱼头，辣中带鲜，仿佛能品出湘江的豪迈。\n\n---\n\n## 🗺️ 实用贴士\n\n- **时间选择**：建议傍晚6点后入园，避开日晒，又可见到日落与夜景交替的震撼。\n- **交通建议**：地铁2号线直达橘子洲站，出站即达；自驾需注意停车位紧张。\n- **游览路线**：从北端沙滩公园向南，依次经过拱极楼、江神庙、毛泽东雕塑、问天台，全程约3公里，步行需2-3小时。\n- **注意事项**：夏季蚊虫较多，建议携带驱蚊液；景区内可租借观光车（¥20/人）代步。\n\n---\n\n## 💫 结语\n\n离开时已近午夜，回望橘子洲，它如一叶扁舟浮在湘江之上，承载着历史的厚重与青春的浪漫。那张¥40的门票，换来的不仅是一段旅程，更是一堂生动的历史课——关于理想、关于豪情、关于一座城与一个人的不朽对话。\n\n若你问我来长沙最值得去的地方，我会说：“去橘子洲吧，在夜色中，与伟人共饮一杯湘江水。”✨', 'travelogue', 1170, 0, '2026-06-29 21:26:44');

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
-- Records of announcement
-- ----------------------------
INSERT INTO `announcement` VALUES (1, '景区夏季开放时间调整通知', '自7月1日起，景区开放时间调整为 06:00 - 19:00，请各位游客合理安排游览时间。', 1, 0, 1, '2026-06-29 17:19:26', '2026-06-29 17:19:26');
INSERT INTO `announcement` VALUES (2, '关于团体票预订须知', '团体票需提前3天预订，最少10人起订。预订成功后请及时上传成员名单。如有疑问请联系客服。', 1, 0, 1, '2026-06-29 17:19:26', '2026-06-29 17:19:26');
INSERT INTO `announcement` VALUES (3, '暑期亲子活动即将开始', '7月15日至8月31日，景区将举办\"亲子探索季\"主题活动，届时将有丰富的互动体验项目，敬请期待！', 3, 0, 1, '2026-06-29 17:19:26', '2026-06-29 17:19:26');
INSERT INTO `announcement` VALUES (4, '该吃饭了', '你饿了吗 反正我饿了', 2, 1, 1, '2026-06-29 18:52:10', '2026-06-29 18:52:10');

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
-- Records of coupon
-- ----------------------------
INSERT INTO `coupon` VALUES (2, '满100减20', 1, 100.00, 20.00, NULL, 10, 0, 0, 1, 10, 1, '2026-06-18 07:32:49', '2026-06-18 07:32:49', -1);
INSERT INTO `coupon` VALUES (5, '立减10元', 1, 0.00, 10.00, NULL, 10, 0, 0, 1, 10, 1, '2026-06-18 07:40:23', '2026-06-27 20:42:12', -1);

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
-- Records of entry_log
-- ----------------------------
INSERT INTO `entry_log` VALUES (8, 1, 1029, 1, 1, 28, '/uploads/face/capture_20260629201532.jpg', 91.3641, '2026-06-29 20:15:32', '2026-06-29 20:41:16', 'A01', 1, NULL, '2026-06-29 20:15:32');

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
-- Records of face_data
-- ----------------------------
INSERT INTO `face_data` VALUES (28, 1, 1, 1029, '00000000000000000000000000000001', 'scenic_20260629', '/uploads/face/face_test_20260629.jpg', 92.47, '2026-06-30 23:59:59', 1, '2026-06-29 19:35:06', '测试游客', '13800000001', 'tourist_1_1029_demo01');
INSERT INTO `face_data` VALUES (37, 1, 1, 1033, '00000000000000000000000000000001', 'scenic_20260629', '/uploads/group-face/20260629_demo/成员1_13800010001.jpg', 92.47, '2026-06-30 23:59:59', 1, '2026-06-29 21:55:47', '张三', '13800010001', 'tourist_1_1033_demo02');
INSERT INTO `face_data` VALUES (38, 1, 1, 1033, '00000000000000000000000000000002', 'scenic_20260629', '/uploads/group-face/20260629_demo/成员2_13800010002.jpg', 86.94, '2026-06-30 23:59:59', 1, '2026-06-29 21:55:49', '李四', '13800010002', 'tourist_1_1033_demo03');
INSERT INTO `face_data` VALUES (39, 1, 1, 1033, '00000000000000000000000000000003', 'scenic_20260629', '/uploads/group-face/20260629_demo/成员3_13800010003.jpg', 92.12, '2026-06-30 23:59:59', 1, '2026-06-29 21:55:50', '王五', '13800010003', 'tourist_1_1033_demo04');

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
-- Records of gate
-- ----------------------------
INSERT INTO `gate` VALUES (4, 'A01', '南门（主）', 1, '2026-06-24 15:12:30');
INSERT INTO `gate` VALUES (5, 'A02', '北门', 1, '2026-06-24 15:12:30');

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
-- Records of group_member
-- ----------------------------
INSERT INTO `group_member` VALUES (33, 5, '张三', '110101********1234', '13800010001', NULL, '/uploads/group-face/20260629_demo/成员1_13800010001.jpg', 1, 0, '2026-06-29 21:55:19', NULL);
INSERT INTO `group_member` VALUES (34, 5, '李四', '110101********2345', '13800010002', NULL, '/uploads/group-face/20260629_demo/成员2_13800010002.jpg', 1, 0, '2026-06-29 21:55:19', NULL);
INSERT INTO `group_member` VALUES (35, 5, '王五', '110101********1456', '13800010003', NULL, '/uploads/group-face/20260629_demo/成员3_13800010003.jpg', 1, 0, '2026-06-29 21:55:19', NULL);
INSERT INTO `group_member` VALUES (36, 5, '赵六', '110101********1567', '13800010004', NULL, '/uploads/group-face/20260629_demo/成员4_13800010004.jpg', 2, 0, '2026-06-29 21:55:19', '人脸注册失败: face is fuzzy');

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
-- Records of group_order
-- ----------------------------
INSERT INTO `group_order` VALUES (5, 1, '测试团体', '张三', '13800000001', 1, '2026-06-29', 4, 140.00, NULL, 3, 1, '2026-06-29 21:55:38', NULL, '2026-06-29 21:55:19', '2026-06-29 21:55:19');

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
-- Records of order_item
-- ----------------------------
INSERT INTO `order_item` VALUES (1, 1029, 6, 1, 40.00, '2026-06-29 19:30:27');
INSERT INTO `order_item` VALUES (5, 1033, 11, 4, 35.00, '2026-06-29 21:55:46');

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
-- Records of refund
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
-- Records of scenic
-- ----------------------------
INSERT INTO `scenic` VALUES (1, '橘子洲景区', '湖南省长沙市岳麓区橘子洲头2号', '橘子洲位于湖南省长沙市湘江中心，是湘江下游面积最大的沙洲，被誉为\"中国第一洲\"。橘子洲西望岳麓山，东临长沙城，四面环水，绵延十多里，形状是一个长岛。\n\n洲上最著名的景点是毛泽东青年艺术雕塑，高32米，长83米，宽41米，展现了一代伟人\"指点江山、激扬文字\"的豪迈气概。洲头建有问天台、橘洲公园，洲尾有沙滩公园。春季橘花飘香，秋季橘果累累，四季景色各异，是长沙最具代表性的城市名片。\n\n景区内的毛泽东《沁园春·长沙》诗词碑、\"指点江山\"石刻等人文景观，与湘江两岸的都市天际线交相辉映，构成一幅\"山水洲城\"的壮美画卷。', '/uploads/logo/logo.png', '07:00:00', '22:00:00', 80000, 1, '2026-06-24 15:12:30', '2026-06-29 22:40:09', '[\"/uploads/banners/banner_第一张.jpg\", \"/uploads/banners/banner_第二张.png\", \"/uploads/banners/banner_第三张.jpg\", \"/uploads/banners/banner_第四张.jpg\", \"/uploads/banners/banner_第五张.png\"]', '/uploads/backgrounds/bg_home.png', '/uploads/backgrounds/bg_tickets.jpg', '/uploads/backgrounds/bg_ai.png', '/uploads/backgrounds/bg_orders.png', '/uploads/backgrounds/bg_profile.png', '#113056', '#1a73e8', '/uploads/backgrounds/bg_login.png', '/uploads/backgrounds/bg_register.png');

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
-- Records of scenic_facility
-- ----------------------------
INSERT INTO `scenic_facility` VALUES (11, 1, '地铁站出口卫生间', 1, '地铁2号线橘子洲站2号出口旁，景区入口第一个卫生间，人流量大', '/uploads/facilities/facility_地铁站出口卫生间.jpg', 112.954500, 28.191500, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (12, 1, '橘洲公园南卫生间', 1, '橘洲公园南部，靠近百米高喷，环境整洁', '/uploads/facilities/facility_橘洲公园南卫生间.jpg', 112.955600, 28.194500, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (13, 1, '橘洲公园北卫生间', 1, '橘洲公园北部，靠近橘洲大桥下方', '/uploads/facilities/facility_橘洲公园北卫生间.jpg', 112.956300, 28.197200, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (14, 1, '毛泽东雕塑广场卫生间', 1, '毛泽东青年雕塑东侧广场旁', '/uploads/facilities/facility_毛泽东雕塑广场卫生间.jpg', 112.955200, 28.193800, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (15, 1, '沙滩公园卫生间', 1, '沙滩公园入口处西侧，配有母婴室', '/uploads/facilities/facility_沙滩公园卫生间.jpg', 112.957900, 28.201000, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (16, 1, '橘子洲头卫生间', 1, '橘子洲头广场停车场旁', '/uploads/facilities/facility_橘子洲头卫生间.jpg', 112.953600, 28.188900, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (17, 1, '橘洲茶社', 2, '橘洲公园中心位置，提供长沙特色茶饮和简餐，可一边品茶一边赏湘江美景', '/uploads/facilities/facility_橘洲茶社.jpg', 112.955700, 28.195800, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (18, 1, '湘江渔馆', 2, '靠近橘洲大桥，以湘菜和河鲜为主，环境优雅，可容纳200人同时用餐', '/uploads/facilities/facility_湘江渔馆.jpg', 112.956100, 28.197500, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (19, 1, '毛泽东雕塑广场小吃街', 2, '毛泽东雕塑北侧，汇集长沙臭豆腐、糖油粑粑、口味虾等地道小吃', '/uploads/facilities/facility_毛泽东雕塑广场小吃街.jpg', 112.955300, 28.194000, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (20, 1, '沙滩公园烧烤区', 2, '沙滩公园内设有户外烧烤区域，提供自助烧烤服务', '/uploads/facilities/facility_沙滩公园烧烤区.jpg', 112.958100, 28.200800, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (21, 1, '橘洲咖啡厅', 2, '位于地铁站出口附近，提供现磨咖啡、甜品和简餐', '/uploads/facilities/facility_橘洲咖啡厅.jpg', 112.954800, 28.192000, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (22, 1, '南门停车场', 3, '景区南入口，地铁站旁，可容纳800辆车，含新能源充电桩50个', '/uploads/facilities/facility_南门停车场.jpg', 112.954300, 28.191800, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (23, 1, '北门停车场', 3, '景区北入口，靠近沙滩公园，可容纳600辆车，适合自驾游客', '/uploads/facilities/facility_北门停车场.jpg', 112.957500, 28.199500, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (24, 1, '景区医务室', 4, '橘洲公园中部，紧邻游客服务中心，配备急救设备和常用药品', '/uploads/facilities/facility_景区医务室.jpg', 112.955500, 28.195500, '2026-06-24 15:12:30');
INSERT INTO `scenic_facility` VALUES (25, 1, '游客服务中心', 5, '地铁站出口北行100米，提供咨询、导览、寄存、轮椅租赁等服务', '/uploads/facilities/facility_游客服务中心.jpg', 112.954600, 28.192200, '2026-06-24 15:12:30');

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
-- Records of scenic_spot
-- ----------------------------
INSERT INTO `scenic_spot` VALUES (11, 1, '橘子洲头', '橘子洲最南端，毛泽东《沁园春·长沙》\"独立寒秋，湘江北去，橘子洲头\"即指此地。洲头广场矗立着\"指点江山\"石碑，是游客必到的打卡地。极目远眺，湘江北去，岳麓山尽收眼底。', '/uploads/spots/spots_橘子洲头.png', 112.953750, 28.189280, 1, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (12, 1, '毛泽东青年艺术雕塑', '橘子洲最具标志性的景观。雕塑高32米（寓意毛主席32岁来长沙），长83米（寓意享年83岁），宽41米（寓意执政41年）。雕像面向东南，飘逸的长发、深邃的目光，生动再现了青年毛泽东\"问苍茫大地，谁主沉浮\"的豪情壮志。', '/uploads/spots/spots_毛泽东青年艺术雕塑.png', 112.955100, 28.193500, 2, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (13, 1, '问天台', '位于橘子洲头南端，相传为毛泽东青年时期常来读书和思考之处。在这里，青年毛泽东放眼湘江，\"问苍茫大地，谁主沉浮\"，《沁园春·长沙》的不朽诗篇即诞生于此。站在问天台上，可俯瞰湘江两岸风光。', '/uploads/spots/spots_问天台.png', 112.953400, 28.188600, 3, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (14, 1, '百米高喷', '位于橘洲公园中部的百米音乐喷泉，喷水高度达100米。喷泉随音乐节奏变换造型，夜晚配合灯光效果更是美轮美奂，是游客休憩观赏的好去处。', '/uploads/spots/spots_百米高喷.png', 112.956200, 28.194800, 4, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (15, 1, '橘洲公园', '橘子洲的核心游览区，占地约14公顷。园内绿树成荫，橘林成片，四季常青。春季橘花盛开香气四溢，秋季金橘挂满枝头硕果累累。园内设有休闲步道、观景亭台，是市民和游客休闲散步的绝佳场所。', '/uploads/spots/spots_橘洲公园.png', 112.955800, 28.196200, 5, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (16, 1, '江神庙', '又称水陆寺，始建于六朝时期，是长沙最古老的寺庙之一。庙内供奉江神，古时过往船只都要在此祈福。建筑古朴典雅，院内古木参天，香火延续千年不绝。', '/uploads/spots/spots_江神庙.png', 112.955000, 28.194200, 6, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (17, 1, '拱极楼', '位于江神庙旁，始建于明代，是橘子洲上著名的古建筑。楼高约20米，登楼可眺望湘江美景。历史上文人墨客常在此吟诗作赋，\"拱极\"意为拱卫北极星，象征着对家国的忠诚。', '/uploads/spots/spots_拱极楼.png', 112.955300, 28.194400, 7, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (18, 1, '橘洲沙滩公园', '位于橘子洲北端，占地约20万平方米。拥有长沙市区内唯一的户外沙滩，设有沙滩排球、足球场、儿童游乐区等设施。每年夏季举办各类音乐节和文化活动，是年轻人最爱的聚集地。', '/uploads/spots/spots_橘洲沙滩公园.png', 112.957800, 28.200500, 8, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (19, 1, '《沁园春·长沙》诗词碑', '镌刻着毛泽东手书《沁园春·长沙》全文的巨型石碑。\"独立寒秋，湘江北去，橘子洲头。看万山红遍，层林尽染…\"大气磅礴的诗句与雄浑的书法相得益彰，令无数游客驻足诵读。', '/uploads/spots/spots_《沁园春·长沙》诗词碑.png', 112.954800, 28.192500, 9, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (20, 1, '橘洲大桥景观区', '橘洲大桥横跨橘子洲中部，连接湘江东西两岸。桥上的步行通道是俯瞰橘子洲全景的最佳位置之一。从桥上可看到湘江碧波、洲上绿荫和两岸高楼交相辉映的壮美画面。', '/uploads/spots/spots_橘洲大桥景观区.png', 112.956500, 28.197800, 10, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (21, 1, '潇湘大道风光带', '沿橘子洲东岸修建的景观步道，全长约3公里。沿途绿树成荫，设有多个观景平台和休息区。漫步其间，可欣赏湘江东岸长沙城市天际线的现代风貌，感受\"山水洲城\"的独特魅力。', '/uploads/spots/spots_潇湘大道风光带.png', 112.956000, 28.195500, 11, 1, '2026-06-24 15:12:30');
INSERT INTO `scenic_spot` VALUES (22, 1, '梅园', '位于橘洲公园西北角，种植有上百株各色梅花。每年冬末春初梅花盛开时节，暗香浮动，是长沙市民赏梅胜地。园内还种植了桃花、樱花等，四季花开不断。', '/uploads/spots/spots_梅园.png', 112.955200, 28.196800, 12, 1, '2026-06-24 15:12:30');

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
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, 'scenic.name', '橘子洲景区', 'scenic', '景区名称（全局引用）', 0, '2026-06-09 00:29:42', '2026-06-24 15:12:30');
INSERT INTO `sys_config` VALUES (2, 'scenic.address', '湖南省长沙市岳麓区橘子洲头2号', 'scenic', '景区详细地址', 0, '2026-06-09 00:29:42', '2026-06-24 15:12:30');
INSERT INTO `sys_config` VALUES (3, 'scenic.description', '橘子洲位于湖南省长沙市湘江中心，被誉为\"中国第一洲\"，是长沙最具代表性的城市名片。洲上有毛泽东青年艺术雕塑、问天台、橘洲公园等著名景点，是国家5A级旅游景区，免门票对外开放。', 'scenic', '景区文字介绍', 0, '2026-06-09 00:29:42', '2026-06-24 15:12:30');
INSERT INTO `sys_config` VALUES (4, 'scenic.logo_url', '/uploads/logo/orange_isle_logo.png', 'scenic', '首页/标签页Logo', 0, '2026-06-09 00:29:42', '2026-06-24 15:12:30');
INSERT INTO `sys_config` VALUES (5, 'deepseek.api-key', '', 'deepseek', 'DeepSeek大模型密钥', 1, '2026-06-09 00:29:42', '2026-06-17 15:23:09');
INSERT INTO `sys_config` VALUES (6, 'deepseek.base-url', 'https://api.deepseek.com/chat/completions', 'deepseek', 'API基础地址', 0, '2026-06-09 00:29:42', '2026-06-09 00:34:46');
INSERT INTO `sys_config` VALUES (7, 'deepseek.model', 'deepseek-chat', 'deepseek', '使用的模型版本', 0, '2026-06-09 00:29:42', '2026-06-09 00:34:59');
INSERT INTO `sys_config` VALUES (8, 'deepseek.max-tokens', '2048', 'deepseek', '最大输出长度', 0, '2026-06-09 00:29:42', NULL);
INSERT INTO `sys_config` VALUES (9, 'deepseek.temperature', '0.7', 'deepseek', '随机性参数', 0, '2026-06-09 00:29:42', NULL);
INSERT INTO `sys_config` VALUES (10, 'baidu.face.api-key', '', 'baidu', '百度智能云API Key', 1, '2026-06-09 00:29:42', '2026-06-24 14:41:36');
INSERT INTO `sys_config` VALUES (11, 'baidu.face.secret-key', '', 'baidu', '百度智能云Secret Key', 1, '2026-06-09 00:29:42', '2026-06-24 14:41:46');
INSERT INTO `sys_config` VALUES (12, 'baidu.face.app-id', '', 'baidu', '百度智能云App ID', 1, '2026-06-09 00:29:42', '2026-06-24 14:41:57');
INSERT INTO `sys_config` VALUES (13, 'baidu.face.threshold', '0.80', 'baidu', '1:N比对相似度阈值', 0, '2026-06-09 00:29:42', NULL);
INSERT INTO `sys_config` VALUES (14, 'ticket.booking_days_normal', '7', 'ticket', '散客最大可预约天数（从今天起算）', 0, '2026-06-27 22:37:27', '2026-06-27 22:42:50');
INSERT INTO `sys_config` VALUES (15, 'ticket.booking_days_group', '14', 'ticket', '团体票最大可预约天数（从今天起算）', 0, '2026-06-27 22:37:27', NULL);

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
-- Records of sys_oper_log
-- ----------------------------
INSERT INTO `sys_oper_log` VALUES (119, 1, 'admin', '操作日志', '批量删除', '清理了操作日志', 'DELETE', '/api/v1/admin/oper-logs/batch', '[118]', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-06-27 23:03:19');
INSERT INTO `sys_oper_log` VALUES (120, 1, 'admin', '公告管理', '新增公告', '新增了一条公告', 'POST', '/api/v1/admin/announcements', 'AnnouncementDTO(title=该吃饭了, content=你饿了吗 反正我饿了, type=2, isTop=1, status=1)', '0:0:0:0:0:0:0:1', 1, NULL, 57, '2026-06-29 18:52:10');
INSERT INTO `sys_oper_log` VALUES (121, 1, 'admin', '公告管理', '编辑公告', '编辑了公告', 'PUT', '/api/v1/admin/announcements/1', '1, AnnouncementDTO(title=景区夏季开放时间调整通知, content=自7月1日起，景区开放时间调整为 06:00 - 19:00，请各位游客合理安排游览时间。, type=1, isTop=0, status=1)', '0:0:0:0:0:0:0:1', 1, NULL, 15, '2026-06-29 18:52:18');
INSERT INTO `sys_oper_log` VALUES (122, 1, 'admin', '票务管理', '更新票种', '修改了票种信息', 'PUT', '/api/v1/admin/ticket-types/11', '11, TicketTypeDTO(name=团体票, price=35, totalStock=15000, dailyStock=3000, description=适用于4人及以上团体，需统一入园, isGroup=1, minGroupSize=4, maxBookingDays=null, status=1)', '0:0:0:0:0:0:0:1', 1, NULL, 23, '2026-06-29 19:40:30');

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
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'ADMIN', '系统最高权限，可切换任意角色', 1, 1, '2026-06-09 00:29:42');
INSERT INTO `sys_role` VALUES (2, '景区管理员', 'MANAGER', '票务、订单、团体票管理，可切换检票员角色', 2, 1, '2026-06-09 00:29:42');
INSERT INTO `sys_role` VALUES (3, '检票员', 'CHECKER', '入园检票、人脸核验、人工通道', 3, 1, '2026-06-09 00:29:42');
INSERT INTO `sys_role` VALUES (4, '游客', 'TOURIST', '普通游客，购票、入园', 4, 1, '2026-06-09 00:29:42');

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
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$4WRlwLzV3uPYhllXj.jc.uOFx9a2HXI.Vr.E1uTn1bqkJs2VW0ZCO', '超级管理员', '13800000000', 'admin@scenic.com', NULL, 1, 1, '2026-06-09 00:29:42', '2026-06-29 22:40:09', 0);
INSERT INTO `sys_user` VALUES (2, 'manager', '$2a$10$4WRlwLzV3uPYhllXj.jc.uOFx9a2HXI.Vr.E1uTn1bqkJs2VW0ZCO', '景区管理员', '13800000002', 'manager@scenic.com', NULL, NULL, 1, '2026-06-09 00:29:42', NULL, 0);
INSERT INTO `sys_user` VALUES (3, 'checker', '$2a$10$4WRlwLzV3uPYhllXj.jc.uOFx9a2HXI.Vr.E1uTn1bqkJs2VW0ZCO', '检票员', '13800000003', 'checker@scenic.com', NULL, NULL, 1, '2026-06-09 00:29:42', NULL, 0);
INSERT INTO `sys_user` VALUES (4, 'tester', '$2a$10$4WRlwLzV3uPYhllXj.jc.uOFx9a2HXI.Vr.E1uTn1bqkJs2VW0ZCO', '测试游客', '13800000001', 'tester@scenic.com', NULL, NULL, 1, '2026-06-09 00:29:42', NULL, 0);

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
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (7, 4, 4);
INSERT INTO `sys_user_role` VALUES (8, 1, 1);
INSERT INTO `sys_user_role` VALUES (9, 1, 2);
INSERT INTO `sys_user_role` VALUES (10, 1, 3);
INSERT INTO `sys_user_role` VALUES (11, 1, 4);
INSERT INTO `sys_user_role` VALUES (12, 2, 2);
INSERT INTO `sys_user_role` VALUES (13, 2, 3);
INSERT INTO `sys_user_role` VALUES (14, 2, 4);
INSERT INTO `sys_user_role` VALUES (15, 3, 3);
INSERT INTO `sys_user_role` VALUES (16, 3, 4);

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
-- Records of ticket_order
-- ----------------------------
INSERT INTO `ticket_order` VALUES (1029, 'SC20260629193026E65173', 1, 1, '2026-06-29', NULL, 40.00, 40.00, 0, '2026-06-29 19:34:16', 'MOCK_820ddf86d3aa43af82e4d846b6d2d6d7', 0, NULL, 6, '2026-06-29 19:30:27', '2026-06-29 20:41:16', NULL, 0.00);
INSERT INTO `ticket_order` VALUES (1033, 'SCG2026062921554545F0A6', 1, 1, '2026-06-29', NULL, 140.00, 140.00, 0, '2026-06-29 21:55:46', 'MOCK_GROUP_e252fe4140d14d4fbd129ff74b679906', 1, 5, 1, '2026-06-29 21:55:46', '2026-06-29 21:55:46', NULL, 0.00);

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
-- Records of ticket_type
-- ----------------------------
INSERT INTO `ticket_type` VALUES (6, 1, '成人票', 40.00, 60000, 15000, 1, '适用于18-59周岁成人', 0, NULL, 10, 1, '2026-06-24 15:12:30', '2026-06-29 19:34:15');
INSERT INTO `ticket_type` VALUES (7, 1, '学生票', 20.00, 30000, 8000, 0, '全日制在校学生，凭有效学生证入场', 0, NULL, 7, 1, '2026-06-24 15:12:30', '2026-06-27 21:43:17');
INSERT INTO `ticket_type` VALUES (8, 1, '老人票', 20.00, 20000, 5000, 0, '适用于60周岁及以上老人，凭身份证入场', 0, NULL, 7, 1, '2026-06-24 15:12:30', '2026-06-27 19:50:37');
INSERT INTO `ticket_type` VALUES (9, 1, '儿童票', 10.00, 20000, 5000, 0, '适用于6-17周岁未成年人，须由成人陪同', 0, NULL, 7, 1, '2026-06-24 15:12:30', '2026-06-27 19:50:37');
INSERT INTO `ticket_type` VALUES (11, 1, '团体票', 35.00, 15000, 3000, 4, '适用于4人及以上团体，需统一入园', 1, 4, 14, 1, '2026-06-24 15:12:30', '2026-06-29 21:55:45');

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
-- Records of tourist
-- ----------------------------
INSERT INTO `tourist` VALUES (1, 1, '测试游客', NULL, '13800000001', 1, '00000000000000000000000000000001', '2026-06-24 17:04:13', '2026-06-24 17:04:13');

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
-- Records of user_coupon
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
