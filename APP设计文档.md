# 鹦鹉日记 · APP 设计文档 v1.3（MVP）

> v1.3 变更（用户反馈驱动）：①英雄卡数据改为 **最新体重 / 陪伴时间 / 记一笔总数**；②日历压缩为**周条，可展开月视图/年热力图**（记录条数热力）；③新增**记开销与花费统计**（调研养宠记）；④**周期照护提醒强化**：预设驱虫/化毛等周期 +「记一次自动顺延」（调研肥崽/maketimes）。
> v1.2 内容（白色主题/夜间模式/英雄卡/回忆中心/行迹地图/M3E 动效）全部保留。上游：[技术方案定案.md](技术方案定案.md)。原型：[原型图.html](原型图.html)。

---

## 1. 设计总则

**目标用户**：手养鹦鹉家庭（1-3 只），每天与鸟互动 10 分钟以上。

**三条铁律**：①3 秒录入（≤2 次点击）②数据永续 ③提醒克制。**视觉**：白色底、草木绿主色、应用内三档外观切换、M3 Expressive 组件语言 + 有目的的动效（§9）。

---

## 2. 信息架构与导航

```
┌─────────────────────────────────────────────┐
│  ①今天        ②回忆        (FAB)    ③鸟   ④我的 │
│ 英雄卡+周条   日记/照片/地图  ┌────┐  档案   设置 │
│                             │记一笔│             │
│                             └────┘              │
└─────────────────────────────────────────────┘
```

FAB「记一笔」→ 抽屉**五选一**：记体重 / 记喂食 / 拍照·视频 / 写日记 / **记开销**。

---

## 3. 核心用户流程

1. 首次启动：建第一只鸟（30 秒）→ 询问自动备份 → 进今天页
2. **每日称重**：FAB → 记体重 → 默认上次值 → ±0.5g → 保存（弹性反馈）
3. **随手拍**：FAB → 拍照·视频 → 相机 →（可选一句话）→ 保存 → 进回忆·照片与行迹地图
4. **看曲线**：鸟详情 → 曲线叠加品种健康区间 → 异常趋势温和提示
5. **翻日历（v1.3）**：今天页默认显示**本周一行**（热力格）→ 点「展开」切月视图 → 再切**年热力图**（GitHub 贡献图样式）→ 点任意日期，下方展开当天记录；空白日可补记
6. **周期照护（v1.3）**：提醒卡到期 → 点**「记一次」→ 完成本次并自动顺延下一周期**（如体内驱虫 +90 天），全程 1 秒
7. **记开销（v1.3）**：FAB → 记开销 → 金额+分类（粮食/玩具/用品/医疗/洗护/其他）→ 保存；「我的 → 花费统计」看周/月/年合计与分类构成

---

## 4. 屏幕明细（MVP 共 12 屏）

| # | 屏幕 | 目的 | 关键元素 |
|---|---|---|---|
| S1 | 今天 | 一眼掌握 | **英雄卡**（大图+名字+自动年龄+头像堆叠换鸟；数据行：**最新体重 / 陪伴 N 天 / 记一笔 N 条**）→ **周条日历**（7 格热力，可展开月/年视图，点日期看当天记录、空白日补记）→ 到期周期提醒卡（**记一次**按钮） |
| S2 | 记体重 | 3 秒录入 | 大等宽数字+±步进；多鸟 chips |
| S3 | 记喂食 | 喂食流水 | 食物选择器（最爱优先）；数量/备注 |
| S4 | 拍照·视频 | 最低门槛记录 | 调相机 → 可加一句话 → 存为媒体日记（自动带 EXIF 位置） |
| S5 | 写日记 | 图文记录 | 文本+多图视频+标签 |
| S6 | 回忆 | 内容中心 | 三段：日记 / 照片 / **地图（行迹：EXIF GPS 聚合地点钉，本地隐私）** |
| S7 | 鸟列表 | 档案入口 | 卡片列表+添加 |
| S8 | 鸟详情 | 单鸟主页 | 概览（曲线+品种区间+饮食卡）/ 照片墙 / 记录流 |
| S9 | 食物库 | 饮食偏好管理 | 36 种内置+自定义；🟢🟡🔴；偏好四档 |
| S10 | 记开销 | 花费流水 | 金额+分类（粮食/玩具/用品/医疗/洗护/其他）+可选关联鸟 |
| S11 | 花费统计 | 养宠账本 | **周/月/年切换**：合计金额 + 分类构成条形 + 趋势；预算管理二期 |
| S12 | 备份与导出 | 数据安全 | 立即备份/自动/恢复/CSV/就诊报告 |
| S13 | 我的/设置 | 杂项 | 外观切换、提醒管理、食物库、花费统计入口、关于 |

---

## 5. 档案字段（v1.1 定稿）

身份：名字*/品种*/花色/脚环号/来源；时间：出壳日/到家日（**陪伴天数=到家日起算，未填则用出壳日，都没有则隐藏该格**）；生理：性别+判定方式；个性：性格标签；其他：头像/备注/强调色/归档。仅名字+品种必填。

## 6. 饮食偏好子系统（v1.1 定稿）

内置 36 种常见食物（清单见附录）+ 用户自定义，不做百科全书。Food（🟢🟡🔴 安全三档，🔴默认隐藏可搜索即教育）/ PetFoodPref（❤️👍😐🚫）/ FeedLog.foodId 自动统计最近 30 天构成比。

## 7. 照片与行迹地图（★特色功能 · 已核对 StoryTeller 真实界面与官方设计博客）

照片来源=日记挂图+拍照入口；照片墙按月分组。**行迹地图=本 App 的招牌功能**。设计 DNA 经三重核证：①[真实界面截图](https://allaboutwindowsphone.com/flow/item/20458_Lumia_Storyteller_improvements.php) ②[设计师 Annina Koskinen 案例页](https://anninakoskinen.com/project/nokia-storyteller)（15 张设计稿全览）③[官方设计博客 "it's emotional"](https://blogs.windows.com/devices/2013/12/10/nokia-storyteller-emotional/)。

**设计哲学（官方原话）**："把照片重新变回事件（turn pictures back into events）"；**位置 = 情感元数据（Emotional metadata）**——"你在巴黎的一天可以变成更深层的故事：告诉人们餐厅在哪、背景的教堂叫什么。讲故事时，你是在重踏这座城市的足迹。"

**真实界面 DNA（截图核证）**：
1. **巨型小写文字 Tab**（timeline places / stories featured 大字即页签）
2. **照片马赛克 + 左侧月份竖轨**（JULY/AUGUST 圆点轨道）+ **故事名压在照片上**（"BEACH — 18 IMAGES"）
3. **地点视图 = 纸色地图 + 黑色圆形类别钉**（酒店/餐厅/景点图标）+ **圆形照片聚合** + 黑色计数气泡
4. **故事 = 一次出行**：地图带路线 + 洋红标签（"HEADING TO SPAIN"）+ "DAY 1" 行程徽章
5. **时光滑轨**（底部圆形按钮）：拖动看照片按时间在地图上逐个亮起
6. **隐藏重复照片**（连拍自动保留最好一张）

**我们的转译（绿色系 + 鸟的场景）**：故事压字="人民公园的夏天 · 12 张"；地点大字列表绿/蓝/琥珀交替；行程徽章="第 1 天 · 家"；禁吃食物默认隐藏搜索可见；禁吃快捷选择不出现。

结构：
1. **「故事」是一级组织**（治"地点一多就乱"）：同日同地自动聚合成故事卡；全部地点走二级检索页（搜索/排序/分组：大本营/常去/今年/更早）
2. **缩放即聚合**：缩小时小地点合并为区域钉
3. **时光滑轨**：拖动看行迹生长

隐私：位置数据仅存本地，地图仅展示不联网打点。

## 8. 数据库 Schema v1.3（Room 定稿）

```kotlin
// 全表：id UUID 主键 + createdAt/updatedAt
Pet(id, name, speciesId, morph?, legBand?, source?, gender, genderMethod?,
    birthDate?, gotchaDate?, avatarPath?, personalityTags?: JSON, accentIndex, notes?, isArchived)
WeightLog(id, petId, grams, weighedAt, note?)
FeedLog(id, petId, foodId?, type, brand?, amountText?, fedAt, note?)
DiaryEntry(id, petId, text, happenedAt)
DiaryMedia(id, entryId, path, type, sort, lat?, lng?, takenAt?, placeId?)  // placeId: 手动归入地点
Place(id, name, lat?, lng?, coverMediaId?, firstVisitAt?, note?)           // v1.3 新增：地点档案
HealthRecord(id, petId, type, title, detail?, happenedAt)
Reminder(id, petId?, kind: WEIGH/CAGE_CLEAN/BATH/NAIL/DEWORM_IN/DEWORM_OUT/HAIRBALL/CUSTOM,
         title, intervalDays, nextAt, enabled)      // v1.3：预设驱虫/化毛周期
ExpenseLog(id, petId?, category: FOOD/TOY/SUPPLY/MEDICAL/GROOMING/OTHER,
           amount: Double, spentAt: Long, note?)     // v1.3 新增：养宠记账
Food(id, name, category, safety, isBuiltin)
PetFoodPref(id, petId, foodId, level, note?, updatedAt)
AppSetting(key, value)   // appearance 等
```

索引要点：WeightLog(petId,weighedAt)、FeedLog(petId,fedAt)、DiaryEntry(petId,happenedAt)、ExpenseLog(spentAt)、DiaryMedia(lat,lng)。v2 预留：EggLog、MoltLog、HandFeedLog、VetContact、**Budget(预算)、Stock(囤货)**。

---

## 9. 视觉与动效规范（2026 业界调研定稿）

色彩：浅色=白底 #FFFFFF/表面 #F6F7F8/墨字 #1B1C1E/**主色草木绿 #3D9A63**；深色=#111316/#1C1F23/#E7E9EA/#7FD3A2。主色管交互，每鸟强调色（6 套）只管"这是谁"。
动效（M3 Expressive "有目的的动效"）：FAB 抽屉 spring 滑入(250ms)、保存回弹(≤400ms)、日历选中缩放(200ms)、段切换淡入位移(180ms)、深色切换交叉淡入(250ms)；尊重系统减弱动态；无装饰循环动画。

**设计禁令（2026-10-03 增补，用户定）**：禁止"左侧彩色装饰条"、渐变横幅按钮等 AI 生成味元素。设计语言对齐 Nokia StoryTeller：浅纸色底、内容即界面（少盒子少框线）、彩色只给内容不给装饰、排版用字号层级与留白表达。回忆区=淡纸绿底(#F2F6F2)的 StoryTeller 纸面，引言用纯排版（去框线）。
**「回忆」区 = StoryTeller 真实结构复刻（2026-10-02 已核对真实界面截图后修正）**：此前"暗色影院画布"是基于 Metro 常识的误判，已推翻。真实 StoryTeller 的设计 DNA（来自[设计师 Annina Koskinen 案例页](https://anninakoskinen.com/project/nokia-storyteller)与[真机截图](https://allaboutwindowsphone.com/flow/item/20458_Lumia_Storyteller_improvements.php)）：①**巨型文字 Tab**（timeline map favourites 直接用大号文字做页签，当前项着墨色、其余浅灰）；②**纸色地图 + 洋红/蓝双色的圆形计数气泡**（缩放时分裂/合并）；③**地点列表=大号彩色文字**（地名+张数，无卡片框）；④照片马赛克+故事压字。我们以白色主题+绿色系复刻这套结构。
排版遵循 Metro 四支柱（大字排版/动效/内容大于装饰/诚实，源自 Windows Phone 设计语言）；故事卡横向滑动浏览（WP Panorama 手势致敬 Nokia StoryTeller）。
依据：[m3.material.io](https://m3.material.io)（2026 UX Design Award）、[M3E in Compose 指南](https://medium.com/@androidlab/material-3-expressive-ui-in-compose-the-ultimate-guide-51c703e8e88a)、[Orizon 2026 趋势](https://www.orizon.co/blog/10-ui-ux-trends-that-will-shape-2026)、[Tubik 2026](https://tubikstudio.com/blog/ui-design-trends-2026)、[Figma Pawsome Companion](https://www.figma.com/community/file/1360166837210685517/pawsome-companion-20-ui-screens-designs-for-pet-caring-applications)。

---

## 10. 案例功能取舍表（v1.1 + v1.3 增补）

| 案例功能 | 来源 | 决策 |
|---|---|---|
| 按品种健康体重区间 | Bird Tracker | ✅ M3 |
| 饮食偏好+营养 | BirdTricks | ✅ M4（强化为三合一） |
| 每鸟照片墙 | PetFetch | ✅ M5 |
| 芯片号→脚环号 | PetNoter | ✅ M2 |
| 自动年龄 | NestSync | ✅ M2 |
| **养宠记账（分类+周月年统计）** | **养宠记** | ✅ **v1.3 采纳（S10/S11，M6）** |
| **倒计时周期提醒（驱虫/洗澡/生日）** | **肥崽** | ✅ **v1.3 采纳强化（预设周期+记一次顺延，M6）** |
| **极简快记+自动顺延** | **maketimes** | ✅ **v1.3 采纳（周期提醒交互范式）** |
| 预算管理 | 养宠记 | ⏳ 二期 |
| 囤货管理（库存/临期） | 宠物·好好养 | ⏳ 二期 |
| 品种照护指南 | Bird Tracker | ⏳ 二期 |
| 粪便/换羽 | 美鹦鹉 | ⏳ 二期（标签已预留） |
| 热量计算 / 多人共享 / 文档库 | 各家 | ⏳ 远期 |
| 疫苗表 / 社区 | 各家 | ❌ 不做 |

---

## 11. 实施里程碑（v1.3 · 含完成状态与路线 A 调序）

| 里程碑 | 内容 | 验收 | 状态 |
|---|---|---|---|
| M1 | 数据层：Room 全表（含 ExpenseLog/EXIF/Place）+食物库导入+仓库 | 单测通过 | ✅ 完成（e2e6dd1） |
| M2 | 档案+自动年龄+外观切换 | 建 3 只鸟、深浅切换 | ✅ 完成（148b641） |
| M3 | **视觉骨架**：今天页（英雄卡+周条热力+3 秒记体重）+ 回忆影院骨架 + 四 Tab + 我的页 | 模拟器全流程 | ✅ 完成（5d717fe） |
| M3.5 | 体重曲线 Canvas（品种区间色带+异常趋势提示） | 曲线随录入实时更新 | ⏭ 下一步 |
| M4 | 喂食+食物库+饮食统计 | 偏好→记录→统计正确 | |
| M5 | 日记+拍照+照片墙+行迹地图（高德） | 拍照带 GPS 上地图 | |
| M6 | 周期提醒（预设+记一次顺延）+记开销+花费统计 | 驱虫记一次自动顺延 90 天 | |
| M7 | 备份恢复+设置+整体视觉打磨（图标/间距/动效对齐原型） | **备份往返测试通过** | |
| **M8** | **虚拟宠物屋（Rive 路线，见 §13）** | 房间+idle 动画+3 个记录联动 | 预研定案 |

> 路线 A 说明：应用户要求改为"视觉骨架先行"——M3 已把首页/回忆骨架立起，后续里程碑在此骨架上填功能与打磨。

---

## 12. 待确认（不阻塞）

养几只什么品种 / 有无育雏需求 / 正式名。

---

## 13. 虚拟宠物屋（M8）· 技术预研与定案（2026-10-02）

**概念**：每只鸟一间 2D 虚拟房间，鸟在其中"生活"，**行为由真实记录驱动**——今日已称重→开心动画；3 天未记录→蔫蔫的；记喂食→进食反应；到期提醒→头顶气泡"该称重啦"；性格标签（已存库）影响 idle 行为（话痨=冒音符，高冷=偶尔背对你）。

**技术定案：Rive 路线**（备选 Lottie；手写 Canvas 方案降级为无美术资源时的兜底）

调研结论（2026-10-02 联网核证）：开源 Tamagotchi 类 Compose demo（[Totigotchi](https://github.com/caroltaus/Totigotchi)、[Tamagochi-OSG](https://github.com/Tech-aficionado/Tamagochi---Open-Source-Game)、[pimagotchi](https://github.com/Studio-Sunnyfield/pimagotchi) 等）普遍简陋——**问题在个人项目的美术与打磨，不在 Compose**。商业级 2D 角色的标准做法是"美术工具做角色+状态机，App 只播放与喂数据"：

- **Rive**：角色+状态机在设计稿内完成（待机/开心/饿/睡 + 点击触发），App 端 [rive-android 运行时](https://rive.app/docs/runtimes/android/state-machines)加载 .riv 文件；Compose 集成有[现成示例](https://github.com/srcemre/interactive-login-screen-with-rive-and-jetpack-compose)；生产背书：[Slice 银行工程博客](https://engineering.slice.bank.in/bringing-interfaces-to-life-with-rive-real-time-animations-at-slice-cd80d2742176)；官方可交互示例见 [rive.app/examples](https://rive.app/examples)
- **Lottie**：[Duolingo 同款路线](https://blog.duolingo.com/streak-milestone-design-animation)（Duo 由真实学习记录驱动哭/笑/庆祝），[LottieFiles](https://lottiefiles.com) 现成角色生态最大，门槛更低但交互弱于 Rive

**关键市场佐证**：Duolingo 与 Finch（自我照顾宠物鸟 App）= "虚拟宠物由真实行为驱动"的商业级成功，与本项目概念 100% 同构，且我们的数据（称重/喂食/陪伴）比打卡更具体。

**范围锁死（第一版）**：静态房间 + 鸟 idle 动画 + 3 个记录联动（称重/喂食/提醒气泡）；装饰与换装二期。
**入口**：首页英雄卡或鸟详情进入全屏房间（不加第 5 个底部 Tab）。
**成本结构**：代码少量（数天）；主要成本=角色美术——先用 Rive Marketplace / LottieFiles 现成角色做技术验证，满意再投资定制。
**架构影响**：零改动——Compose 骨架（页面/导航/数据）全部保留，仅"宠物屋画布"换为 RiveView/Lottie 组件 + 记录数据作状态机输入。

## 附录：内置食物清单（36 种）

谷物种子(10)：黄谷子、燕麦米、稗子、加纳利子、荞麦、糙米饭、葵花子🟡、麻子🟡、南瓜子🟡、红花子🟡 ·
滋养丸(2)：通用、其他品牌→自定义 ·
蔬果(12)：青菜、油菜、胡萝卜、玉米、西兰花、南瓜、甜椒、黄瓜、苹果(去核)、梨(去核)、葡萄🟡、菠菜🟡 ·
零食补给(4)：墨鱼骨、煮蛋黄、面包虫🟡、坚果碎🟡 ·
禁吃警示(8)：牛油果、巧克力、洋葱大蒜、咖啡浓茶、酒精、果核、含盐调味食物、生豆类
