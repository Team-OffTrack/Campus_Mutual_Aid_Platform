# 校园互助前端 (Campus Help Frontend)

Vue 3 项目，移动端优先的用户界面 + 管理后台。

---

## 技术栈

| 组件 | 选型 |
|------|------|
| 框架 | Vue 3（Composition API + `<script setup>`） |
| 构建 | Vite 5 |
| UI 组件 | Vant 4（移动端 UI） |
| 路由 | Vue Router 4 |
| 状态管理 | Pinia |
| HTTP | Axios（拦截器自动注入 JWT） |
| 实时通信 | WebSocket + STOMP (SockJS) |
| 语言 | 页面中文，代码英文 |

---

## 项目结构

```
src/
├── main.js                  # 入口：挂载 App + Pinia + Router
├── App.vue                  # 根组件
├── router/index.js          # 路由表（18 条） + 导航守卫
├── stores/
│   ├── auth.js              # 认证状态（token、userId、role）
│   └── badgeToast.js        # 徽章弹窗队列（localStorage 持久化）
├── api/
│   ├── client.js            # Axios 实例 + 拦截器
│   ├── user.js              # 用户 API（register/login/profile）
│   ├── demand.js            # 需求 API（CRUD + accept/complete/cancel）
│   ├── points.js            # 积分 API（checkin/status/transactions）
│   ├── chat.js              # 聊天 API（conversations/messages）
│   ├── notification.js      # 通知 API（list/read/unread-count）
│   ├── evaluation.js        # 评价 API（create/update/list）
│   ├── admin.js             # 管理 API（users/demands/reports/dashboard）
│   ├── badge.js             # 徽章 API（list/wear/unwear/easter-egg）
│   ├── report.js            # 举报 API（create/list/resolve）
│   └── favorite.js          # 收藏 API（list/add/remove）
├── views/
│   ├── Login.vue            # 学号密码登录（分栏式）
│   ├── Register.vue         # 注册（含前端校验）
│   ├── Home.vue             # 首页（问候/积分/签到/功能入口网格）
│   ├── Profile.vue          # 个人资料 + 隐私设置 + 密码修改
│   ├── Settings.vue         # 设置页（关于/彩蛋/退出）
│   ├── DemandList.vue       # 需求广场（搜索/类型筛选/排序/分页/收藏）
│   ├── DemandPublish.vue    # 发布需求（6 种类型动态表单）
│   ├── DemandDetail.vue     # 需求详情（状态流/组队审批/评价/管理删除）
│   ├── MyOrders.vue         # 我的订单（三标签页：发布/接取/队伍）
│   ├── PointsHistory.vue    # 积分明细（余额总览 + 分类流水）
│   ├── Notifications.vue    # 通知中心（列表/未读/已读/导航跳转）
│   ├── ChatDetail.vue       # 私信聊天（文本/图片/Emoji/WebSocket 实时）
│   ├── BadgeList.vue        # 成就徽章（9 种展示/进度/佩戴/取下）
│   ├── FavoriteList.vue     # 我的收藏（分页需求列表）
│   └── admin/
│       ├── AdminDashboard.vue  # 管理仪表盘（统计卡片 + 快速入口）
│       ├── UserList.vue        # 用户管理（表格+卡片/搜索/封禁/徽章）
│       ├── DemandList.vue      # 需求管理（筛选/表格+卡片/删除）
│       └── ReportList.vue      # 举报管理（状态栏/处理面板/操作面板）
├── components/
│   ├── NavActions.vue       # 导航栏右侧操作区
│   ├── ImageViewer.vue      # 全屏图片预览
│   ├── EmojiPicker.vue      # Emoji 选择面板
│   ├── BadgeOverlay.vue     # 徽章角标（头像叠加，全局使用）
│   └── BadgeToast.vue       # 徽章获得全屏动效（teleport to body）
├── constants/
│   └── demandTypes.js       # 需求类型共享常量（单一数据源）
└── styles/
    └── main.css             # M3E 设计系统（200+ 行 CSS 自定义属性）
```

---

## 快速启动

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 生成 SSL 证书（仅首次）

```bash
openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout key.pem -out cert.pem -days 3650 \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

### 3. 启动开发服务器

```bash
npm run dev
```

浏览器打开 `https://localhost:5173`（自签名证书需点「继续访问」）。

### 4. 构建生产包

```bash
npm run build        # 产物在 dist/
npm run preview      # 本地预览构建结果
```

---

## 配置说明

### `.env.development`（开发环境）

```
VITE_API_BASE=http://localhost:5173/api/v1
```

开发模式下 API 请求走 Vite 代理（配置在 `vite.config.js` 的 `server.proxy`），自动转发到 `http://localhost:8080`，无需后端开启 CORS。

### `.env.production`（生产环境）

```
VITE_API_BASE=/api/v1
```

生产模式下由 Nginx 统一代理前后端，无跨域问题。

### Vite 代理（`vite.config.js`）

```js
server: {
  proxy: {
    '/api': {
      target: 'https://localhost:8080',   // 后端已启用 HTTPS
      changeOrigin: true,
      secure: false                       // 自签名证书跳过验证
    },
    '/uploads': {
      target: 'https://localhost:8080',
      changeOrigin: true,
      secure: false
    }
  }
}
```

如果需要修改后端地址，改 `target` 即可。自签名证书环境下 `secure: false` 必须保留。

---

## 路由与权限

| 路径 | 页面 | 权限 | 说明 |
|------|------|------|------|
| `/login` | 登录 | 游客 | 分栏式登录页，已登录自动跳转 |
| `/register` | 注册 | 游客 | 注册成功跳转登录页 |
| `/` | 首页 | 登录用户 | 问候/积分统计/签到/功能网格/管理入口 |
| `/profile` | 个人资料 | 登录用户 | 修改姓名/头像/匿名设置/密码 |
| `/settings` | 设置 | 登录用户 | 关于信息/彩蛋触发/退出登录 |
| `/demands` | 需求广场 | 登录用户 | 搜索/类型筛选/排序/收藏 |
| `/demands/publish` | 发布需求 | 登录用户 | 6 种类型动态表单 |
| `/demands/:id` | 需求详情 | 登录用户 | 状态流/组队审批/评价/管理删除 |
| `/demands/my/favorites` | 我的收藏 | 登录用户 | 已收藏需求分页列表 |
| `/orders` | 我的订单 | 登录用户 | 三标签：发布/接取/队伍 |
| `/points/history` | 积分明细 | 登录用户 | 余额总览 + 分类流水 |
| `/notifications` | 通知中心 | 登录用户 | 列表/未读/已读/导航跳转 |
| `/chat/:id` | 私信聊天 | 登录用户 | 文本/图片/Emoji/WebSocket |
| `/badges` | 成就徽章 | 登录用户 | 9 种徽章展示/佩戴/取下 |
| `/admin` | 管理仪表盘 | ADMIN | 统计概览 + 快速入口 |
| `/admin/users` | 用户管理 | ADMIN | 搜索/翻页/封禁/解封/徽章 |
| `/admin/demands` | 需求管理 | ADMIN | 筛选/表格+卡片/硬删除 |
| `/admin/reports` | 举报管理 | ADMIN | 状态筛选/处理操作面板 |

**导航守卫**（`router/index.js`）：
- 未登录访问需认证页面 → 跳转 `/login`
- 已登录访问游客页面 → 跳转 `/`
- 非 ADMIN 访问 `/admin/*` → 跳转 `/`

---

## Axios 拦截器

`src/api/client.js` 中配置了两个拦截器：

**请求拦截**：自动从 `localStorage` 读取 token 注入 `Authorization: Bearer <token>` 头。

**响应拦截**：
- 自动解包 `ApiResult`，返回 `data` 字段
- `code !== 200` 时用 Vant Toast 显示错误信息
- `401` 时清除本地状态并跳转登录页

---

## 自定义后端地址

如果后端不在 `localhost:8080`，两个地方需要改：

1. `vite.config.js` — 修改 `proxy.target`
2. `.env.production` — 修改 `VITE_API_BASE`

如果需要更改前端端口，修改 `vite.config.js` 的 `server.port`。
