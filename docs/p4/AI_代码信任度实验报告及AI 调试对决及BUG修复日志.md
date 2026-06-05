# AI 代码信任度实验报告 (含任务 6,7,8)

## 一、实验基本信息

| 项目 | 内容 |
|---|---|
| 项目名称 | Campus Mutual Aid Platform / 校园互助服务平台 |
| 仓库地址 | https://github.com/Team-OffTrack/Campus_Mutual_Aid_Platform |
| 实验主题 | AI 代码信任度实验 |
| 实验重点 | 前后端集成、模块间流程联调、AI 辅助调试可信度评估 |
| 实验对象 | Spring Boot 后端、Vue/Vite 前端、MariaDB 数据库、Redis/Valkey、GitHub Actions CI |
| 实验时间 | 2026 年 6 月 |
| 实验环境 | Arch Linux 主机、MariaDB、Redis/Valkey、JDK、Maven、Node.js、npm |

---

## 二、项目背景

Campus Mutual Aid Platform 是一个面向校园场景的互助服务平台，功能包括用户认证、互助需求发布与接单、聊天、评价、通知、信用分等模块。

项目采用前后端分离架构：

| 层次 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2.5 |
| 数据库 | MySQL / MariaDB |
| ORM / Mapper | MyBatis-Plus |
| 数据库迁移 | Flyway |
| 认证机制 | Spring Security + JWT |
| 前端 | Vue 3 + Vite |
| 状态管理 | Pinia |
| HTTP 请求 | Axios |
| UI 框架 | Vant |
| CI/CD | GitHub Actions |

仓库中后端控制器包含：

- `UserController`
- `AdminController`
- `DemandController`
- `EvaluationController`
- `NotificationController`
- `ChatController`

前端 API 模块包含：

- `user.js`
- `admin.js`
- `demand.js`
- `evaluation.js`
- `notification.js`
- `chat.js`
- `client.js`

这说明项目已经具备较完整的前后端模块拆分基础，也适合进行集成测试与 AI 辅助调试可信度实验。

---

## 三、实验目标

本实验的目标不是单纯验证 AI 能否生成代码，而是评估 AI 在真实项目集成、调试和修复建议中的可信度。

具体目标包括：

1. 检查前后端接口字段是否一致。
2. 检查认证状态是否能从前端正确传递到后端。
3. 检查订单、评价、通知、聊天等跨模块流程是否连贯。
4. 检查异常提示是否清晰，能否帮助开发者快速定位问题。
5. 检查数据库状态是否符合业务预期。
6. 比较纯人工调试和 AI 辅助调试在定位速度、定位准确度、修复方案质量上的差异。
7. 总结 AI 在本项目中的可相信范围和不可盲信风险。

---

## 四、实验一：前后端与模块集成检查

### 4.1 实验任务要求

本实验重点检查以下内容：

- 接口字段是否一致
- 认证状态是否正确传递
- 订单、评价、通知等跨模块流程是否连贯
- 异常提示是否清晰
- 数据库状态是否符合业务预期

---

### 4.2 Prompt 记录

#### Prompt 1：前后端接口字段一致性检查

```text
请根据 Campus Mutual Aid Platform 仓库，检查前端 API 模块和后端 Controller / DTO / Entity 是否存在字段不一致问题。重点检查登录、注册、需求发布、接单、完成、评价、通知等流程。
```

#### AI 直出结果

AI 给出的初步检查思路如下：

```text
建议按模块建立接口字段对照表。前端以 src/api/*.js 为入口，后端以 controller、dto、entity 为入口。
重点关注：
1. 登录接口返回 token、user 信息是否与前端 auth store 读取字段一致。
2. 需求发布接口的 title、description、reward、category、images 等字段是否一致。
3. 评价接口的 rating、comment、targetUserId、demandId 是否与后端接收对象一致。
4. 通知接口的 read / unread 状态字段是否与前端展示逻辑一致。
5. 接单、完成、取消等接口需要检查 demand status 的枚举值是否一致。
```

#### 人工检查问题

人工检查后认为 AI 的方向基本正确，但存在以下问题：

| 问题 | 说明 |
|---|---|
| 只给出检查方向，未能直接证明字段完全一致 | AI 没有直接运行项目，也没有逐文件执行静态比对 |
| 对部分字段存在推测 | 如 `images`、`targetUserId` 等字段需要结合具体 DTO 和迁移脚本确认 |
| 没有区分 Entity 字段和 DTO 字段 | Entity 字段不一定直接暴露给前端 |
| 对异常返回结构检查不足 | 需要进一步确认 `ApiResult`、全局异常处理和前端 Axios 拦截器是否匹配 |

#### 人工修正后的检查结论

人工将检查范围拆成以下几组：

| 模块 | 检查入口 | 检查重点 |
|---|---|---|
| 用户认证 | `user.js`、`UserController`、JWT Filter、auth store | token 是否保存、请求头是否自动携带 |
| 需求/订单 | `demand.js`、`DemandController`、`Demand`、迁移脚本 | status 枚举、接单人字段、创建人字段、图片字段 |
| 评价 | `evaluation.js`、`EvaluationController`、`Evaluation`、`UserAccount` | 评价后是否更新信用分 |
| 通知 | `notification.js`、`NotificationController`、`Notification` | 通知创建、读取、未读状态 |
| 聊天 | `chat.js`、`ChatController`、`Conversation`、`Message` | 会话创建、消息归属、图片字段 |
| 管理员 | `admin.js`、`AdminController`、角色字段 | 管理员权限校验、封禁状态 |

---

### 4.3 Prompt 2：认证状态传递检查

```text
请检查前端登录成功后，JWT 是否能在后续请求中自动传递到后端，并说明可能出现的问题。
```

#### AI 直出结果

```text
应重点检查：
1. 登录接口是否返回 token。
2. 前端是否将 token 存入 Pinia 或 localStorage。
3. Axios 请求拦截器是否在 Authorization 请求头中添加 Bearer token。
4. 后端 JwtAuthenticationFilter 是否从请求头读取 token。
5. Spring Security 是否放行登录和注册接口，并保护需要登录的接口。
```

#### 人工检查问题

AI 的结论方向正确，但仍然偏“模板化”。人工进一步检查后补充了以下具体风险：

| 风险 | 影响 |
|---|---|
| 前端访问后端时如果使用了错误协议，如 `http` 访问 `https` 后端 | 请求直接失败，表现为网络错误 |
| 跨主机访问时前端代理仍指向 `localhost` | 另一台主机访问前端时，API 请求会打到客户端自己的 localhost |
| token 失效后前端未清理登录状态 | 页面可能显示已登录，但接口实际 401 |
| 后端 CORS 配置不完整 | 浏览器跨域请求失败，但 curl 可能正常 |

#### 修复前后简要对比

| 项目 | 修复前 | 修复后 |
|---|---|---|
| API 地址 | 可能固定为 `localhost` | 开发环境走 Vite 代理，跨主机时改为服务器 IP 或通过反向代理 |
| HTTPS | 后端启用 HTTPS，但前端/浏览器可能未信任证书 | 明确使用 `https://服务器IP:8080`，必要时重新生成包含服务器 IP 的 SAN 证书 |
| token 传递 | 只检查登录流程 | 同时检查 Axios 拦截器、后端 JWT Filter 和失效处理 |
| 错误表现 | “连不上” | 区分网络不可达、端口不可达、证书错误、401 未认证 |

---

### 4.4 Prompt 3：跨模块流程连贯性检查

```text
请检查需求发布、接单、完成、评价、通知、信用分更新之间是否构成完整业务闭环。
```

#### AI 直出结果

```text
建议将业务流程拆成：
1. 用户 A 发布需求。
2. 用户 B 接单。
3. 用户 B 完成任务。
4. 用户 A 确认完成。
5. 双方进行评价。
6. 系统生成通知。
7. 用户信用分根据评价均分和完成率更新。
```

#### 人工检查问题

人工检查认为 AI 的流程描述基本符合项目业务，但存在两个不足：

1. AI 没有明确区分“发布者”和“接单者”的数据库字段。
2. AI 没有检查每个状态转换是否都有权限限制。

因此人工补充了状态流转检查表：

| 阶段 | 操作人 | 关键数据库状态 | 需要验证 |
|---|---|---|---|
| 发布需求 | 发布者 | demand 创建，状态为待接单 | 创建人是否为当前登录用户 |
| 接单 | 接单者 | demand 记录绑定 acceptor | 不能接自己的单；不能重复接单 |
| 完成 | 接单者或发布者 | demand 状态变化 | 状态转换是否合法 |
| 评价 | 交易双方 | evaluation 新增 | 是否防止重复评价 |
| 通知 | 系统 | notification 新增 | 通知对象是否正确 |
| 信用分 | 系统 | user_account 更新 | 评分和完成率是否按公式更新 |

---

### 4.5 Prompt 4：数据库状态检查

```text
请根据 Spring Boot 启动日志判断数据库迁移是否成功，以及默认管理员是否创建成功。
```

#### AI 直出结果

```text
如果日志中出现 HikariPool Start completed，说明数据库连接池启动成功。
如果日志中出现 Successfully applied migrations，说明 Flyway 迁移成功。
如果日志中出现 Default admin user created: admin / admin123，说明默认管理员创建成功。
```

#### 人工检查结果

实际运行日志中出现：

```text
HikariPool-1 - Start completed.
Successfully applied 8 migrations to schema `campus_help`, now at version v8.
Tomcat started on port 8080 (https).
Default admin user created: admin / admin123.
```

因此可以判断：

| 检查项 | 结果 |
|---|---|
| MariaDB 连接 | 成功 |
| Flyway 迁移 | 成功 |
| 表结构初始化 | 成功 |
| 默认管理员创建 | 成功 |
| 后端服务启动 | 成功 |
| 监听端口 | 8080 / HTTPS |

---

## 五、“AI 调试对决”

### 5.1 实验说明

团队在集成过程中选取 3 个有代表性的 Bug，对比纯人工调试和 AI 辅助调试的差异：

1. MariaDB root 登录权限导致后端启动失败。
2. HTTPS 证书 alias 已存在，导致证书重新生成失败。
3. 另一台主机能 ping 通服务器，但无法访问项目服务。

每个 Bug 均从以下维度记录：

| 步骤 | 纯人工调试 | AI 辅助调试 |
|------|----------|-----------|
| 1. 问题描述 | 团队成员自行分析报错信息 | 将报错信息、复现步骤和相关代码提供给 AI |
| 2. 定位耗时 | 记录从开始到定位根因的时间 | 记录 AI 给出定位建议的时间 |
| 3. 定位准确度 | 记录人工判断过程 | 判断 AI 的定位是否正确 |
| 4. 修复方案 | 人工设计修复方案 | 记录 AI 建议的修复方案 |
| 5. 方案质量 | 记录人工方案的优缺点 | 判断 AI 方案是否引入新问题 |
| 6. 最终修复 | 记录最终采用的方案和原因 | 说明是否采纳 AI 建议 |

---

## 5.2 Bug #1：MariaDB root 权限导致后端启动失败

### 问题描述

后端执行：

```bash
mvn spring-boot:run
```

启动失败，日志核心错误为：

```text
java.sql.SQLException: Access denied for user 'root'@'localhost'
SQL State  : 28000
Error Code : 1698
Message    : Access denied for user 'root'@'localhost'
```

同时可以看到 Flyway 无法获得数据库连接：

```text
Error creating bean with name 'flywayInitializer'
Unable to obtain connection from database: Access denied for user 'root'@'localhost'
```

### Prompt

```text
Spring Boot 项目启动失败，HikariCP 和 Flyway 报错：
Access denied for user 'root'@'localhost'
但我可以通过 sudo mariadb -u root 进入数据库。
请判断原因并给出 Arch / MariaDB 下的修复方式。
```

### AI 直出结果

```text
问题不是 Maven，也不是 Spring Boot 编译失败，而是数据库账号认证方式不匹配。
MariaDB 中 root 用户通常使用 unix_socket 认证。
sudo mariadb -u root 能进入，是因为系统 sudo 身份被 MariaDB 信任；
但 Spring Boot 是普通 Java 进程，只能通过 JDBC 用户名和密码连接，所以会被拒绝。

推荐做法：
1. 不要让项目使用 root。
2. 创建 campus_help 专用用户。
3. 授权 campus_help 数据库。
4. 修改 application-dev.yml 中的 datasource username/password。
```

AI 建议命令：

```sql
CREATE DATABASE IF NOT EXISTS campus_help
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'campus_help'@'localhost'
  IDENTIFIED BY 'campus_help';

CREATE USER IF NOT EXISTS 'campus_help'@'127.0.0.1'
  IDENTIFIED BY 'campus_help';

GRANT ALL PRIVILEGES ON campus_help.* TO 'campus_help'@'localhost';
GRANT ALL PRIVILEGES ON campus_help.* TO 'campus_help'@'127.0.0.1';

FLUSH PRIVILEGES;
```

配置修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/campus_help?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: campus_help
    password: campus_help
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 人工检查问题

| 检查项 | 人工判断 |
|---|---|
| 是否准确识别根因 | 是 |
| 是否只修表象 | 否，定位到了 MariaDB root socket 认证机制 |
| 是否会引入新问题 | 基本不会；专用用户比 root 更安全 |
| 是否需要人工补充 | 需要确认数据库名、配置文件是否实际改对 |

### 修复前后简要对比

| 对比项 | 修复前 | 修复后 |
|---|---|---|
| 数据库用户 | `root` | `campus_help` |
| 连接方式 | root 密码连接失败 | 项目专用用户密码连接成功 |
| Flyway | 无法获得连接 | 成功执行 8 个迁移 |
| 后端启动 | BUILD FAILURE | Tomcat 成功启动 |
| 安全性 | 使用 root，不推荐 | 最小权限用户，更合理 |

### 最终结果

最终采用 AI 建议方案。后端成功启动，日志显示：

```text
HikariPool-1 - Start completed.
Successfully applied 8 migrations to schema `campus_help`, now at version v8.
Tomcat started on port 8080 (https).
Default admin user created: admin / admin123.
```

---

## 5.3 Bug #2：HTTPS 证书 alias 已存在

### 问题描述

执行后端证书生成命令：

```bash
keytool -genkeypair -alias campus-help -keyalg RSA -keysize 2048   -storetype PKCS12 -keystore keystore.p12 -validity 3650   -storepass changeit -keypass changeit -dname "CN=campus-help"   -ext "SAN=DNS:localhost,IP:127.0.0.1"
```

出现错误：

```text
keytool error: java.lang.Exception: Key pair not generated, alias <campus-help> already exists
```

### Prompt

```text
执行 keytool 生成 keystore.p12 时提示：
alias <campus-help> already exists
这是什么情况？是否会影响 Spring Boot 启动？应该如何处理？
```

### AI 直出结果

```text
该错误说明 keystore.p12 中已经存在 campus-help 这个别名。
如果后端已经能启动 HTTPS，则不需要重复生成。
如果确实需要重新生成，可以：
1. 删除旧 alias；
2. 或删除整个 keystore.p12 文件；
3. 然后重新执行 keytool 命令。
如果需要另一台主机通过服务器 IP 访问，应将服务器 IP 加入 SAN。
```

AI 建议命令：

```bash
keytool -list -keystore keystore.p12 -storepass changeit
keytool -delete -alias campus-help -keystore keystore.p12 -storepass changeit
```

或者：

```bash
rm -f keystore.p12
```

如果跨主机访问，重新生成：

```bash
keytool -genkeypair -alias campus-help -keyalg RSA -keysize 2048   -storetype PKCS12 -keystore keystore.p12 -validity 3650   -storepass changeit -keypass changeit -dname "CN=campus-help"   -ext "SAN=DNS:localhost,IP:127.0.0.1,IP:服务器局域网IP"
```

### 人工检查问题

| 检查项 | 人工判断 |
|---|---|
| 是否准确识别根因 | 是 |
| 是否说明影响范围 | 是，若服务已能 HTTPS 启动则无需重建 |
| 是否考虑跨主机访问 | 是，指出原证书只包含 localhost 和 127.0.0.1 |
| 是否存在风险 | 删除整个 keystore 会影响后端 HTTPS，需确保能重新生成 |

### 修复前后简要对比

| 对比项 | 修复前 | 修复后 |
|---|---|---|
| 证书状态 | alias 已存在，重复生成失败 | 复用旧证书或删除后重建 |
| 本机访问 | 可用 | 可用 |
| 跨主机访问 | 证书可能不匹配服务器 IP | SAN 中加入服务器 IP |
| 错误理解 | 误以为 keytool 或 Java 出错 | 明确是 alias 重复 |

### 最终结果

若后端已成功启动，则不重复生成证书。若需要跨主机访问，则重新生成包含服务器局域网 IP 的证书。

---

## 5.4 Bug #3：另一台主机能 ping 通，但无法访问服务

### 问题描述

另一台主机可以 ping 通服务器 IP，但无法访问项目。

典型表现：

```text
ping 服务器IP 可以通
浏览器访问 https://服务器IP:8080 失败
或访问 http://服务器IP:5173 失败
```

### Prompt

```text
另一台主机能 ping 到服务器信号，为什么连不上 Spring Boot / Vite 项目？
后端日志显示 Tomcat started on port 8080 (https)。
请分析可能原因并给出排查步骤。
```

### AI 直出结果

```text
ping 通只能说明 ICMP 网络可达，不代表 TCP 端口可达。
项目访问需要检查：
1. 后端 8080 是否监听 0.0.0.0，而不是 127.0.0.1。
2. 前端 Vite 是否使用 --host 0.0.0.0 启动。
3. 防火墙是否放行 8080 和 5173。
4. 访问协议是否正确，后端是 https 不是 http。
5. 前端代理是否仍然指向 localhost。
```

AI 建议命令：

```bash
ss -lntp | grep -E '8080|5173'
```

如果 Vite 需要对外访问：

```bash
npm run dev -- --host 0.0.0.0
```

后端可在配置中显式设置：

```yaml
server:
  address: 0.0.0.0
  port: 8080
```

测试端口：

```bash
curl -k https://服务器IP:8080
nc -vz 服务器IP 8080
```

### 人工检查问题

| 检查项 | 人工判断 |
|---|---|
| 是否准确识别 ping 和 TCP 访问的区别 | 是 |
| 是否覆盖后端和前端 | 是 |
| 是否考虑 HTTPS | 是 |
| 是否考虑防火墙 | 是 |
| 是否仍需人工验证 | 是，需要在服务器上实际执行 `ss` 和在客户端执行 `curl` |

### 修复前后简要对比

| 对比项 | 修复前 | 修复后 |
|---|---|---|
| 网络判断 | 只根据 ping 判断 | 区分 ICMP、TCP、HTTP/HTTPS |
| 后端访问 | 可能只监听本机或使用错误协议 | 使用 `https://服务器IP:8080` |
| 前端访问 | Vite 默认可能只对本机开放 | 使用 `npm run dev -- --host 0.0.0.0` |
| 防火墙 | 未明确检查 | 检查并放行 8080/5173 |
| 证书 | SAN 只有 localhost | 如需跨主机，加入服务器 IP |

### 最终结果

最终采用 AI 的排查路径。该 Bug 说明 AI 对“网络分层排错”表现较好，能够避免将 ping 通误判为服务必然可访问。

---

## 5.5 实验记录表

| Bug # | Bug 描述 | 纯人工定位耗时 | AI 辅助定位耗时 | AI 定位是否准确 | AI 修复方案是否可用 | 最终方案来源 |
|-------|---------|-------------|-------------|-------------|-----------------|-----------|
| 1 | MariaDB root 用户无法通过 JDBC 登录，导致 Flyway 初始化失败 | 约 20 分钟 | 约 2 分钟 | 准确 | 可用 | AI 建议 + 人工确认 |
| 2 | keytool 生成证书失败，提示 alias 已存在 | 约 8 分钟 | 约 1 分钟 | 准确 | 可用 | AI 建议 |
| 3 | 客户端能 ping 通服务器，但无法访问 8080/5173 服务 | 约 25 分钟 | 约 3 分钟 | 基本准确 | 可用，但需人工执行端口验证 | AI 排查路径 + 人工验证 |

---

## 六、AI 表现分析

### 6.1 AI 在哪类 Bug 上表现较好？

AI 在以下类型的问题上表现较好：

1. **日志中有明确错误信息的问题**

   例如：

   ```text
   Access denied for user 'root'@'localhost'
   ```

   这种错误信息具有较强指向性，AI 能快速联想到 MariaDB root socket 认证和 JDBC 密码认证之间的差异。

2. **工具命令错误**

   例如：

   ```text
   alias <campus-help> already exists
   ```

   该错误语义清晰，AI 能直接给出 `keytool -list`、`keytool -delete`、删除 keystore 后重建等处理方案。

3. **网络分层排查**

   对于“能 ping 通但服务连不上”的问题，AI 能把 ICMP、TCP 端口、HTTP/HTTPS 协议、防火墙、监听地址、Vite host 等因素分层拆开。

4. **集成检查清单生成**

   AI 能较快生成前后端字段、认证、订单流转、通知、评价、数据库状态等检查清单，适合用作人工 review 的起点。

---

### 6.2 AI 在哪类 Bug 上表现较差？

AI 在以下问题上表现相对较弱：

1. **需要逐文件确认字段完全一致的问题**

   AI 可以给出检查方法，但不能替代人工逐项核对 DTO、Entity、Controller、前端 API 参数和页面使用字段。

2. **业务状态机问题**

   例如“接单后是否允许取消”“评价是否允许重复提交”“通知是否只发给目标用户”等，需要结合具体业务规则和测试数据判断，AI 容易只给出通用流程。

3. **隐含上下文不足的问题**

   如果没有提供完整日志、配置文件、运行命令和复现步骤，AI 可能会给出多个可能原因，但不能直接锁定根因。

4. **运行环境差异问题**

   Arch Linux、MariaDB 版本、Java 版本、Redis/Valkey 替代关系、HTTPS 自签证书等环境细节会影响最终结论。AI 需要明确上下文才能给出准确建议。

---

### 6.3 AI 是否出现只修表象、不找根因的情况？

本次实验中，AI 在 3 个主要 Bug 上没有明显只修表象的问题：

| Bug | 是否只修表象 | 说明 |
|---|---|---|
| MariaDB root 登录失败 | 否 | AI 定位到 socket 认证与 JDBC 密码认证差异 |
| keytool alias 已存在 | 否 | AI 指出 alias 重复，并说明复用/删除/重建三种处理方式 |
| ping 通但无法访问服务 | 否 | AI 从网络分层角度拆解，不仅停留在“防火墙”一个原因 |

但在接口字段一致性检查中，AI 的回答偏泛化，没有直接完成逐字段核对。因此该类问题仍然需要人工验证。

---

### 6.4 人工提供哪些上下文后，AI 的调试建议明显变好？

本次实验发现，AI 对上下文高度敏感。提供以下信息后，AI 的建议明显更准确：

| 上下文 | 作用 |
|---|---|
| 完整错误日志 | 帮助 AI 定位真正根因，而不是停留在 Maven BUILD FAILURE |
| 运行命令 | 区分是构建失败、启动失败还是运行时失败 |
| 操作系统 | Arch Linux 下 MariaDB、Redis/Valkey、systemd 命令与其他发行版不同 |
| 配置文件片段 | 判断 datasource、server、SSL、Vite proxy 是否正确 |
| 复现步骤 | 帮助判断问题发生在数据库初始化、后端启动、前端启动还是跨主机访问 |
| 项目仓库地址 | 帮助 AI 对照项目结构、技术栈和模块划分 |
| 成功日志 | 帮助判断问题是否已经修复，例如 HikariPool、Flyway、Tomcat、默认管理员创建日志 |

---

## 七、AI 直出代码/方案的可信度评价

### 7.1 可信度分级

| 任务类型 | AI 可信度 | 是否可直接采用 |
|---|---:|---|
| 根据明确报错解释原因 | 高 | 通常可以采用，但需人工确认环境 |
| 给出 Linux / Maven / MariaDB 命令 | 较高 | 可采用，但执行前需检查路径和用户名 |
| 生成排查清单 | 高 | 适合作为 checklist |
| 修改安全认证逻辑 | 中 | 必须人工 review |
| 修改跨模块业务状态流转 | 中低 | 必须结合业务规则和测试用例验证 |
| 生成数据库迁移脚本 | 中 | 必须检查字段类型、约束和历史迁移 |
| 生成前后端完整功能代码 | 中 | 不宜直接信任，需要测试和代码审查 |

---

### 7.2 AI 直出结果适合承担的角色

本实验中，AI 更适合承担以下角色：

1. **日志解释器**

   帮助从长日志中提取关键错误。

2. **排查路径生成器**

   快速列出可能原因和验证命令。

3. **配置助手**

   帮助生成 MariaDB 用户、Spring datasource、keytool 证书、Vite host 等配置命令。

4. **代码审查辅助工具**

   生成检查清单，但最终结论仍由人工确认。

5. **报告整理工具**

   将调试过程、运行记录、修复前后对比整理成可提交文档。

---

### 7.3 AI 不适合完全替代的部分

AI 不应完全替代以下工作：

1. 最终代码合并决策。
2. 数据库迁移脚本审核。
3. 权限控制逻辑审核。
4. 支付、信用分、评价等核心业务规则确认。
5. 涉及生产环境安全配置的判断。
6. CI/CD 失败原因的最终归因。
7. 前后端接口逐字段一致性的最终确认。

---

## 八、修复前后总览

| 项目 | 修复前 | 修复后 |
|---|---|---|
| 后端启动 | 数据库认证失败，BUILD FAILURE | Spring Boot 成功启动 |
| 数据库连接 | root 用户 JDBC 登录失败 | campus_help 专用用户连接成功 |
| Flyway 迁移 | 无法获得连接 | 成功应用 8 个迁移 |
| 默认管理员 | 未创建 | 创建 `admin / admin123` |
| HTTPS 证书 | 重复生成时报 alias 已存在 | 复用或重建证书 |
| 跨主机访问 | ping 通但服务不可访问 | 按端口、监听、防火墙、协议排查 |
| 前端访问 | 可能仅本机可访问 | 使用 `--host 0.0.0.0` 支持局域网访问 |
| AI 使用方式 | 直接询问笼统问题 | 提供日志、命令、配置后再询问 |

---

## 九、实验结论

通过本次 AI 代码信任度实验可以得出以下结论：

1. AI 对明确日志错误具有较高定位效率，尤其适合处理数据库连接、证书生成、端口访问等工程配置问题。
2. AI 在生成排查步骤和命令方面效率高，能够显著缩短初始定位时间。
3. AI 的建议不能直接等同于最终结论，尤其是涉及业务状态流转、权限边界和数据库状态一致性时，仍必须人工复核。
4. 在前后端集成中，AI 可以快速建立检查框架，但接口字段一致性、认证状态传递、跨模块流程闭环仍需要结合真实代码和运行结果验证。
5. AI 最适合作为“调试助理”和“审查清单生成器”，不适合作为唯一决策者。
6. 当人工提供完整上下文时，AI 的定位准确率明显提升；当上下文不足时，AI 容易给出泛化建议。
7. 本项目最终采用“AI 建议 + 人工验证 + 实际运行日志确认”的模式，能够在保证可信度的前提下提高集成效率。

---