# Campus Mutual Aid Platform CI/CD 配置与运行记录

## 1. 文档说明

本文档根据 GitHub Actions 页面中显示的 CI/CD 运行记录整理，用于说明 `Campus_Mutual_Aid_Platform` 项目的持续集成执行情况。

记录来源为仓库 Actions 页面中的运行列表，主要包含以下信息：

- Workflow 名称
- 运行编号
- 触发提交
- 提交说明
- 触发分支
- 触发时间
- 执行耗时

> 注意：当前记录文本中没有明确给出每次运行的成功或失败状态，因此本文只整理“运行记录”和“耗时”，不额外判断运行结果是否通过。

---

## 2. CI/CD 工作流概览

该项目目前包含两类 GitHub Actions 工作流：

| Workflow | 作用范围 | 主要用途 |
|---|---|---|
| Backend CI | 后端项目 | 后端代码检查、测试、构建 |
| Frontend CI | 前端项目 | 前端依赖安装、Lint、测试、构建 |

从运行记录看，目前共出现：

| Workflow | 运行次数 |
|---|---:|
| Backend CI | 4 |
| Frontend CI | 4 |
| 总计 | 8 |

---

## 3. CI/CD 运行记录

| 序号 | Workflow | 编号 | Commit | 提交信息 | 分支 | 触发人 | 时间 | 耗时 |
|---:|---|---:|---|---|---|---|---|---|
| 1 | Backend CI | #4 | `d5da12e` | `feat: implement credit score formula with weighted rating and complet…` | `main` | `snnbyyds` | 2026-06-03 18:43 GMT+8 | 1m 6s |
| 2 | Backend CI | #3 | `effeefa` | `[REVERTME] ci: single-job pipelines, no cache, NJU mirrors, debug output` | `main` | `snnbyyds` | 2026-06-03 18:29 GMT+8 | 1m 25s |
| 3 | Frontend CI | #4 | `f8201b3` | `fix: remove npm cache and use npm install since package-lock is gitig…` | `main` | `snnbyyds` | 2026-05-29 09:02 GMT+8 | 36s |
| 4 | Frontend CI | #3 | `d4091fe` | `fix: remove npm cache and use npm install since package-lock is gitig…` | `main` | `snnbyyds` | 2026-05-29 09:01 GMT+8 | 35s |
| 5 | Backend CI | #2 | `d7ce3a1` | `ci: add GitHub Actions CI/CD pipeline with linting and static analysis` | `main` | `snnbyyds` | 2026-05-29 08:57 GMT+8 | 1m 23s |
| 6 | Frontend CI | #2 | `d7ce3a1` | `ci: add GitHub Actions CI/CD pipeline with linting and static analysis` | `main` | `snnbyyds` | 2026-05-29 08:57 GMT+8 | 14s |
| 7 | Frontend CI | #1 | `2ba2641` | `ci: add GitHub Actions CI/CD pipeline with linting and static analysis` | `main` | `snnbyyds` | 2026-05-29 08:57 GMT+8 | 12s |
| 8 | Backend CI | #1 | `2ba2641` | `ci: add GitHub Actions CI/CD pipeline with linting and static analysis` | `main` | `snnbyyds` | 2026-05-29 08:57 GMT+8 | 1m 28s |

---

## 4. 时间线分析

### 2026-05-29：首次加入 CI/CD

在 2026-05-29 08:57 GMT+8，仓库开始出现 CI/CD 相关运行记录。提交信息为：

```text
ci: add GitHub Actions CI/CD pipeline with linting and static analysis
```

该提交同时触发了 Backend CI 和 Frontend CI，说明项目在这一阶段开始引入 GitHub Actions 自动化检查流程。

### 2026-05-29：前端 CI 配置调整

随后在 09:01 和 09:02，Frontend CI 又连续运行了两次，对应提交信息为：

```text
fix: remove npm cache and use npm install since package-lock is gitig…
```

这说明前端 CI 在依赖安装或缓存策略上进行了修复，改为使用 `npm install`，并移除了 npm cache 相关配置。

### 2026-06-03：后端 CI 配置调整与功能提交

2026-06-03 18:29，Backend CI #3 对应提交：

```text
[REVERTME] ci: single-job pipelines, no cache, NJU mirrors, debug output
```

从提交信息看，这次可能是为了调试 CI 环境而临时修改流水线配置，例如：

- 单 Job 运行
- 禁用缓存
- 使用 NJU 镜像源
- 增加 debug 输出

2026-06-03 18:43，Backend CI #4 对应提交：

```text
feat: implement credit score formula with weighted rating and complet…
```

这说明后端 CI 在新增“信用分计算公式”相关功能后再次被触发。

---

## 5. 执行耗时统计

### Backend CI 耗时

| 编号 | 耗时 |
|---:|---:|
| #1 | 1m 28s |
| #2 | 1m 23s |
| #3 | 1m 25s |
| #4 | 1m 6s |

Backend CI 平均耗时约为：

```text
1m 20.5s
```

### Frontend CI 耗时

| 编号 | 耗时 |
|---:|---:|
| #1 | 12s |
| #2 | 14s |
| #3 | 35s |
| #4 | 36s |

Frontend CI 平均耗时约为：

```text
24.25s
```

---

## 6. 结论

根据当前记录，该项目已经配置了 GitHub Actions CI/CD，并分别针对后端和前端建立了独立的 CI 工作流。

整体情况如下：

1. CI/CD 首次集中出现在 2026-05-29。
2. 后端和前端均有独立的 CI 运行记录。
3. 后端 CI 运行时间通常在 1 分钟以上。
4. 前端 CI 运行时间较短，多数在 1 分钟以内。
5. 后续提交显示开发者曾对 CI 配置进行过修复和调试。
6. 最近一次后端 CI 与信用分功能实现相关，说明 CI 已经用于功能提交后的自动检查。

---

## 7. 后续建议

建议在课程设计或项目报告中这样描述该项目的 CI/CD：

```text
本项目使用 GitHub Actions 实现持续集成，分别为前端和后端配置独立的 CI 工作流。后端 CI 主要负责代码规范检查、测试与构建，前端 CI 主要负责依赖安装、代码检查、测试与构建。每次向 main 分支提交代码后，系统会自动触发对应流水线，从而保证项目在多人协作开发过程中的基本可构建性和代码质量。
```

如果需要进一步完善 CI/CD，可以考虑：

- 在 README 中添加 CI 状态徽章
- 增加构建产物上传
- 增加 Docker 镜像构建
- 增加自动部署阶段
- 区分开发环境、测试环境和生产环境
- 在 Pull Request 中强制要求 CI 通过后才能合并
