# 期末答辩 PPTX 生成 — 上下文提示词

> 下一轮直接按此 prompt 重写 `gen_final_ppt.py` 并生成 PPTX。

## 目标

为软工期末答辩生成 16:9 PPTX（`docs/final_demo/期末答辩.pptx`），由 **沈诺** 主讲。

## 团队分工（README.md 表格，顺序不可变）

| 角色 | 成员 | 职责 |
|------|------|------|
| 需求负责人 | 侯乔岳 | 功能规划、需求文档 |
| 架构负责人 | 杨佳兴 | 系统设计、数据库 Schema、API 设计 |
| 开发负责人和测试负责人 | 沈诺、胡皓轩 | 前后端开发、代码实现、测试用例、质量保证 |

## 核心原则（最重要！）

1. **PPT 不是讲稿，字越少越好、越大越好。** 理论解释留给口述，PPT 只放关键词/图表/截图。删掉所有"话术"批注、理论段落、教学式解释（如"层间接口定义=合同"、"状态图要点"等）
2. **四个 Section 用分隔页区分**：需求 → 架构 → 开发（功能+HCI）→ 测试
3. **功能演示每页只放一张桌面端截图**（铺满 70%+ 页面），截图底部只留一行 HCI 关键词标签。绝不展示移动端截图
4. **表格分到独立页面**，字体 >= 14pt，不然看不清
5. **PlantUML 图尽量铺满整页**（scale 1.5+，fontSize 14+）
6. **颜色对比度要高**：暗底 `#1A1A2E`，卡片 `#252540`，强调紫 `#7C3AED` / 青 `#06B6D4`，文字 `#FFFFFF` / `#B0B0CC` / `#777788`
7. **不要的**：用例图（删掉 `use_case.puml`，不放入 PPT）、移动端截图、大段理论解释、话术批注、SOLID 检查、QA/SCM 章节

## 幻灯片结构（~22 页）

### Section 1 — 需求（5 页）
| # | 页 | 内容 |
|---|-----|------|
| 1 | 封面 | 校园互助服务平台 · 期末答辩 · 软工 II + 人机交互 · 团队不误正业 · 侯乔岳/杨佳兴/沈诺/胡皓轩 · 2026.06 |
| 2 | 团队介绍 | 上方表格（README 三列），下方四张彩色卡片（每人名字+3-5 条职责要点。侯=绿/杨=橙/沈=青/胡=紫） |
| 3 | 项目概述 | 5 个关键数字（15表/57API/18路由/243测试/9徽章）+ 6 种需求类型卡片 + 技术栈两行 |
| 4 | 敏捷开发 | 4 条敏捷价值观 + P0→P4 五阶段时间线方块 + Sprint 执行策略要点 |
| 5 | 需求分析 | 需求三层次（三卡片：业务需求/用户需求/系统需求）+ 6 种需求类型列表 + 功能性/非功能性需求 |

### Section 2 — 架构（3 页）
| # | 页 | 内容 |
|---|-----|------|
| 6 | 体系结构设计 | 左侧 PlantUML `architecture.png`（分层风格，占 60% 页面），右侧 4 个 ADR 卡片（模块化单体>微服务 / JWT>Session / 单表继承 / 悲观锁）。**不要理论解释文字** |
| 7 | 包结构 | PlantUML `packages.png` 铺满整页。底部最多一行文字「10 Controller · 14 Service · 17 Mapper」 |
| 8 | 数据库设计 | **大字关键词**：15 张表 · Flyway V1-V14 · 单表 discriminator（type+JSON）· 积分不可变账本（仅追加）· UNIQUE 防重 · 悲观锁 SELECT FOR UPDATE · 15 表详细清单 |

### Section 3 — 开发：功能 + HCI（7 页）
| # | 页 | 内容 |
|---|-----|------|
| 9 | API 规范 + 类图 | 左 40%：API 关键词（57 端点 · 10 Controller · 统一 ApiResult · 全局异常处理）。右 60%：PlantUML `class_diagram.png`（放大看清） |
| 10 | 状态图 | PlantUML `state_diagram.png` 铺满整页。底部一行「Demand：OPEN → IN_PROGRESS → COMPLETED / CANCELLED」 |
| 11 | 顺序图 | PlantUML `sequence.png` 铺满整页。底部一行「SSD：发布跑腿需求 — 展示层→逻辑层→数据层」 |
| 12 | 登录与首页 | 全页截图 `login.png` 或 `home.png`。底部一行 HCI 标签：`EEC 闭环  ·  Nielsen 第 1 条「系统状态的可见度」  ·  Shneiderman 第 3 条「提供信息丰富的反馈」` |
| 13 | 需求广场 | 全页截图 `demand_list_desktop.png`。底部一行 HCI 标签：`响应式 768px  ·  Fitts 定律 FAB  ·  Nielsen 第 6 条「依赖识别而非记忆」  ·  彩色类型 chip` |
| 14 | 发布需求 | 全页截图 `demand_publish.png`。底部一行 HCI 标签：`Shneiderman 第 5 条「预防并处理错误」  ·  第 4 条「设计说明对话框以生成结束信息」  ·  spring 弹簧动画  ·  三重反馈` |
| 15 | 需求详情 | 全页截图 `demand_detail.png`。底部一行 HCI 标签：`Shneiderman 第 7 条「支持内部控制点」  ·  第 5 条防错确认  ·  第 6 条「让操作容易撤销」  ·  Nielsen 第 1 条状态颜色` |
| 16 | 聊天 & 徽章 | 左右并排 `chat.png` + `badges.png`。底部一行 HCI 标签：`格式塔-图底原则  ·  WebSocket 实时  ·  Peak-End Rule  ·  游戏化激励` |
| 17 | 管理后台 | 全页截图 `admin_dashboard.png`。底部一行 HCI 标签：`Nielsen 第 8 条「审美感和最小化设计」  ·  数据格式化 1.2k/1.2w  ·  红色角标强调  ·  信息架构` |

### Section 4 — 测试（4 页）
| # | 页 | 内容 |
|---|-----|------|
| 18 | 白盒测试 | 上表：三种覆盖标准（语句⭐/分支⭐⭐/路径⭐⭐⭐，含要求和最少用例数）。下表：本项目实践（Service 135 用例 / Controller 102 用例 / WebSocket 6 用例）。**字体 >= 14pt** |
| 19 | 黑盒测试：等价类 + 边界值 | 左表：等价类（有效/无效/额外无效 + 积分1-100示例）。右表：边界值（min-1/min/min+1/max-1/max/max+1 + 积分1-100示例）。**字体 >= 14pt** |
| 20 | 测试用例模板 | 5 字段模板（TC-ID / 测试对象 / 前置条件 / 输入数据 / 期望输出）。下方 3-5 行本项目实际用例。**字体 >= 14pt** |
| 21 | CI/CD + 总结致谢 | 上半：Backend CI (Maven+H2, ~1m20s) + Frontend CI (npm build, ~24s) + 部署架构一句。下半：三列总结（功能·工程·HCI）+ 243 测试全通过 + 谢谢老师 |

## 文件清单

```
docs/final_demo/
├── prompt.md              # 本文件
├── gen_final_ppt.py       # PPTX 生成脚本（需重写）
├── 期末答辩.pptx           # 输出
├── screenshots/           # 用户手动放截图
│   ├── login.png
│   ├── home.png
│   ├── demand_list_desktop.png
│   ├── demand_publish.png
│   ├── demand_detail.png
│   ├── chat.png
│   ├── badges.png
│   ├── admin_dashboard.png
│   └── admin_reports.png
├── architecture.puml      # 分层架构图 → architecture.png
├── packages.puml          # 逻辑包图 → packages.png
├── class_diagram.puml     # 设计类图 → class_diagram.png
├── state_diagram.puml     # 需求状态图 → state_diagram.png
├── sequence.puml          # 系统顺序图 → sequence.png
└── use_case.puml          # 已废弃，不放入 PPT
```

## 截图自动加载逻辑

`gen_final_ppt.py` 中需要实现 `screenshot(slide, left, top, width, height, name)`：
- `path = os.path.join(BASEDIR, "screenshots", name)`
- 存在 → Pillow 读取尺寸 → 等比缩放嵌入（`width/height` 区域内居中，不变形）
- 不存在 → 灰色虚线矩形 + 文字 `[截图: <name>]`

## PlantUML 图加载逻辑

`plantuml_img(slide, left, top, width, height, filename)`：
- `path = os.path.join(BASEDIR, filename)`
- 存在 → Pillow 读取尺寸 → 等比缩放嵌入
- 不存在 → 红色文字提示缺失

## Python 环境

- `python-pptx` — PPTX 生成
- `Pillow` — 图片尺寸检测
- `/usr/bin/plantuml` — 重新渲染 .puml (已安装，`plantuml -tpng *.puml`)
- 字体：Microsoft YaHei（系统已安装）

## PPTX 辅助函数（建议复用）

```python
def set_bg(slide)          # 暗色背景 #1A1A2E
def tb(slide, l,t,w,h, text, size, color, bold, align)  # 单行文本框
def ml(slide, l,t,w,h, lines, size, color)  # 多行文本框，lines=[(text, bold?, color?), ...]
def card(slide, l,t,w,h, color)  # 圆角矩形
def bar(slide, l,t,w, color)     # 顶部彩色细条
def stitle(slide, text, subtitle) # 标准标题(28pt) + 装饰条
def pn(slide, num)              # 页码
def screenshot(slide, l,t,w,h, name)  # 智能截图/占位
def plantuml_img(slide, l,t,w,h, filename)  # PlantUML 图等比嵌入
def add_table(slide, l,t, col_widths, headers, rows)  # 表格
```

## 注意事项

1. PlantUML 中文正常，`scale` 参数调大（1.5+）让图更清晰
2. 类图 `class_diagram.puml` 内容多，需要大版面——给至少 60% 页面宽度
3. 截图占位是灰色虚线框，用户手动放截图后重跑脚本即自动嵌入
4. `gen_final_ppt.py` 脚本需要能在缺失 PNG 时不报错（用占位替代）
5. ~~`use_case.png`~~ 不放入 PPT
6. 功能演示页底部 HCI 标签用一行小号彩色关键词，不写解释段落
