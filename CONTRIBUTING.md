# 贡献指南 (Contributing Guide)

---

## Commit Message 规范

本项目采用 **Conventional Commits** 格式，便于生成 changelog 和语义化版本管理。

### 格式

```
<type>(<scope>): <subject>

[body]

[footer]
```

### Type（必填）

| Type | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `refactor` | 重构（不改变功能） |
| `style` | 格式调整（空格、缩进、分号等，不影响逻辑） |
| `docs` | 文档变更 |
| `test` | 添加或修改测试 |
| `chore` | 构建、依赖、工具链等杂务 |
| `perf` | 性能优化 |

### Scope（可选）

用模块名或文件名标识改动范围，例如：`auth`, `user`, `admin`, `db`, `frontend`, `backend`。

### Subject（必填）

- 中文或英文均可，团队统一用一种即可
- 使用祈使语气："添加" 而非 "添加了"
- 首字母小写（英文时），不超过 50 字符
- 结尾不加句号

### Body（可选）

- 解释 **为什么** 做这个改动，而非描述怎么做
- 每行不超过 72 字符

### Footer（可选）

- 引用 issue：`Closes #123` 或 `Refs #456`
- `BREAKING CHANGE:` 标记不兼容变更

---

### 示例

**英文**：
```
feat(user): add student-id registration with JWT login

Registration creates user + privacy_profile + user_account in one
transaction. Login returns a stateless JWT token carrying userId and
role claims, avoiding server-side session storage.

Closes #1
```

**中文**：
```
feat(user): 实现学号注册与 JWT 登录

注册在事务中创建 user、privacy_profile、user_account 三张表记录。
登录返回无状态 JWT，载荷中携带 userId 和 role，免去服务端 session。

Closes #1
```

---

### 提交前检查清单

- [ ] 代码编译通过：`mvn compile && npm run build`
- [ ] 测试全部通过：`mvn test`
- [ ] 未包含敏感信息（密码、密钥、Token）

---

## 分支策略

| 分支 | 用途 |
|------|------|
| `main` | 稳定版本，可部署 |
| `dev` | 开发集成分支 |
| `feature/<name>` | 功能分支，从 `dev` 拉出，合并回 `dev` |
| `fix/<name>` | 修复分支 |

---

## 代码风格

- **Java**：遵循阿里巴巴 Java 开发手册
- **Vue**：Composition API + `<script setup>`，组件名 PascalCase
- **SQL**：表名小写下划线，字段名小写下划线，字符集 utf8mb4
- 注释使用英文
- 页面 UI 文本使用中文
