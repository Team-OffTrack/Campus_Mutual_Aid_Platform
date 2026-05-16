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
| 语言 | 页面中文，代码英文 |

---

## 项目结构

```
src/
├── main.js                  # 入口：挂载 App + Pinia + Router
├── App.vue                  # 根组件
├── router/index.js          # 路由表 + 导航守卫
├── stores/auth.js           # 认证状态（token、userId、role）
├── api/
│   ├── client.js            # Axios 实例 + 拦截器
│   ├── user.js              # 用户 API（register/login/profile）
│   └── admin.js             # 管理 API（listUsers/updateStatus）
├── views/
│   ├── Login.vue            # 学号密码登录
│   ├── Register.vue         # 注册（含前端校验）
│   ├── Home.vue             # 首页（功能入口 + 退出）
│   ├── Profile.vue          # 个人资料 + 隐私设置 + 积分
│   └── admin/
│       └── UserList.vue     # 用户列表 + 封禁/解封（仅 ADMIN）
└── styles/main.css          # 全局样式
```

---

## 快速启动

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

浏览器打开 `http://localhost:5173`。

### 3. 构建生产包

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
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

如果需要修改后端地址，改 `target` 即可。

---

## 路由与权限

| 路径 | 页面 | 权限 | 说明 |
|------|------|------|------|
| `/login` | 登录 | 游客 | 已登录用户访问自动跳转首页 |
| `/register` | 注册 | 游客 | 注册成功跳转登录页 |
| `/` | 首页 | 登录用户 | 功能入口 + 退出按钮 |
| `/profile` | 个人资料 | 登录用户 | 修改姓名/头像/匿名设置 |
| `/admin/users` | 用户管理 | ADMIN | 搜索/翻页/封禁/解封 |

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
