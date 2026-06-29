#!/usr/bin/env bash
# ============================================================================
# prepare_for_demo.sh — 演示数据填充脚本
# ============================================================================
# 通过后端 API 创建用户、需求、签到、评价等数据，使网站看起来像已经运营中。
#
# 用法:
#   1. 清空数据库:
#      sudo mariadb -u root -p -e "DROP DATABASE campus_help; CREATE DATABASE campus_help DEFAULT CHARACTER SET utf8mb4;"
#   2. 启动后端:
#      cd backend && mvn spring-boot:run
#   3. 运行本脚本:
#      bash prepare_for_demo.sh
#   4. 启动前端:
#      cd frontend && npm run dev
#
# 要求: curl, python3 (标准库即可，无需额外安装)
# ============================================================================

set -euo pipefail

API_BASE="https://localhost:8080/api/v1"
CURL="curl -sk"                  # -k 跳过自签名证书验证，-s 安静模式
PASS="123456"                    # 所有用户统一密码
TODAY=$(date +%Y-%m-%d)

# 存储每个用户的 token: TOKENS[user_key]=token
declare -A TOKENS
declare -A USER_IDS
declare -A DEMAND_IDS

# ---------------------------------------------------------------------------
# 输出辅助
# ---------------------------------------------------------------------------
green() { echo -e "\033[32m  ✓\033[0m $*"; }
warn()  { echo -e "\033[33m  ⚠\033[0m $*"; }
fail()  { echo -e "\033[31m  ✗\033[0m $*"; }
info()  { echo -e "\033[36m  ➤\033[0m $*"; }
step()  { echo -e "\n\033[1;34m═══ $* ═══\033[0m"; }

# 通用 API 调用: api METHOD URL [TOKEN] [BODY_JSON]
api() {
  local method="$1" url="$2" token="${3:-}" body="${4:-}"
  local headers=(-H "Content-Type: application/json")
  if [[ -n "$token" ]]; then
    headers+=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$body" ]]; then
    $CURL -X "$method" "${headers[@]}" -d "$body" "$url"
  else
    $CURL -X "$method" "${headers[@]}" "$url"
  fi
}

# 从 JSON 提取字段 (使用 python3 标准库)
json_field() {
  python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$1',''))" 2>/dev/null
}

json_data_field() {
  python3 -c "import sys,json; d=json.load(sys.stdin); data=d.get('data',{}); print(data.get('$1','') if isinstance(data,dict) else '')" 2>/dev/null
}

# ---------------------------------------------------------------------------
# Step 1: 注册用户
# ---------------------------------------------------------------------------
step "1. 注册用户 (共 11 人)"

register_user() {
  local key="$1" sid="$2" name="$3"
  local resp
  resp=$(api POST "$API_BASE/user/register" "" "{\"studentId\":\"$sid\",\"password\":\"$PASS\",\"name\":\"$name\"}")
  local code
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    green "注册 $name ($sid) — 成功 (含 100 积分注册奖励)"
  elif echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); exit(0 if '已注册' in d.get('msg','') else 1)" 2>/dev/null; then
    warn "注册 $name ($sid) — 跳过（已存在）"
  else
    local msg
    msg=$(echo "$resp" | json_field msg)
    fail "注册 $name ($sid) — 失败: $msg"
  fi
}

register_user "zs"  "2024001001" "张三"
register_user "ls"  "2024001002" "李四"
register_user "ww"  "2024001003" "王五"
register_user "zl"  "2024001004" "赵六"
register_user "sq"  "2024001005" "孙七"
register_user "zb"  "2024001006" "周八"
register_user "wj"  "2024001007" "吴九"
register_user "zs2" "2024001008" "郑十"
register_user "lxm" "2024001009" "林小明"
register_user "cxh" "2024001010" "陈小红"
register_user "hxg" "2024001011" "黄小刚"

# ---------------------------------------------------------------------------
# Step 2: 登录所有用户，获取 token
# ---------------------------------------------------------------------------
step "2. 登录获取 JWT Token"

login_user() {
  local key="$1" sid="$2"
  local resp
  resp=$(api POST "$API_BASE/user/login" "" "{\"studentId\":\"$sid\",\"password\":\"$PASS\"}")
  local token uid
  token=$(echo "$resp" | json_data_field token)
  uid=$(echo "$resp" | json_data_field userId)
  if [[ -n "$token" ]]; then
    TOKENS[$key]="$token"
    USER_IDS[$key]="$uid"
    green "登录 $sid — userId=$uid"
  else
    fail "登录 $sid 失败: $(echo "$resp" | json_field msg)"
  fi
}

login_user "zs"  "2024001001"
login_user "ls"  "2024001002"
login_user "ww"  "2024001003"
login_user "zl"  "2024001004"
login_user "sq"  "2024001005"
login_user "zb"  "2024001006"
login_user "wj"  "2024001007"
login_user "zs2" "2024001008"
login_user "lxm" "2024001009"
login_user "cxh" "2024001010"
login_user "hxg" "2024001011"

# ---------------------------------------------------------------------------
# Step 3: 每日签到（部分用户签，部分不签 — 让首页有「已签」「未签」两种状态）
# ---------------------------------------------------------------------------
step "3. 每日签到（7 人签到，4 人不签）"

do_checkin() {
  local key="$1" name="$2"
  local resp
  resp=$(api POST "$API_BASE/points/checkin" "${TOKENS[$key]}")
  local code pts streak
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    pts=$(echo "$resp" | json_data_field pointsAwarded)
    streak=$(echo "$resp" | json_data_field streak)
    green "$name 签到成功 +${pts} 积分 (连续 ${streak} 天)"
  elif [[ "$code" == "409" ]]; then
    warn "$name 今日已签到 — 跳过"
  else
    fail "$name 签到失败: $(echo "$resp" | json_field msg)"
  fi
}

# 已签到的用户
do_checkin "zs"  "张三"
do_checkin "ls"  "李四"
do_checkin "sq"  "孙七"
do_checkin "zb"  "周八"
do_checkin "wj"  "吴九"
do_checkin "cxh" "陈小红"
do_checkin "hxg" "黄小刚"

info "王五、赵六、郑十、林小明 未签到（首页可看到签到按钮）"

# ---------------------------------------------------------------------------
# Step 4: 发布需求
# ---------------------------------------------------------------------------
step "4. 发布需求 (共约 28 条，覆盖全部 6 种类型)"

publish_demand() {
  local key="$1" label="$2" body="$3"
  local resp
  resp=$(api POST "$API_BASE/demands" "${TOKENS[$key]}" "$body")
  local code did
  code=$(echo "$resp" | json_field code)
  did=$(echo "$resp" | json_data_field demandId)
  if [[ "$code" == "200" ]]; then
    green "$label — demandId=$did"
    echo "$did"
  else
    fail "$label — 失败: $(echo "$resp" | json_field msg)"
    echo ""
  fi
}

# --- 跑腿代取 (errand) ---
info "跑腿代取…"
D_errand_1=$(publish_demand "zs"  "代取韵达快递 [张三→李四 已完成]" \
  '{"type":"errand","title":"代取韵达快递","description":"韵达快递在2号驿站，快递单号YT123456789，比较大件最好带个小推车。取件码在短信里。","location":"2号菜鸟驿站","rewardType":"point","rewardAmount":20,"attributes":{"pickup_location":"东区2号菜鸟驿站"},"deadline":"2028-06-12T18:00:00"}')
D_errand_2=$(publish_demand "ww"  "南门代取外卖 [王五→张三 进行中]" \
  '{"type":"errand","title":"南门代取外卖","description":"外卖快到了，我下午有公司不方便出校门。美团外卖，在保卫处旁边的外卖架。","location":"南门保卫处旁外卖架","rewardType":"point","rewardAmount":15,"attributes":{"pickup_location":"南门保卫处旁外卖架"},"deadline":"2028-06-11T13:00:00"}')
D_errand_3=$(publish_demand "ls"  "图书馆代还书 [李四 开放]" \
  '{"type":"errand","title":"图书馆代还书","description":"有两本书到期了，《算法导论》和《计算机网络》，在三楼西区。帮忙去图书馆还一下就行！","location":"校图书馆三楼西区","rewardType":"point","rewardAmount":10,"attributes":{"pickup_location":"校图书馆"},"deadline":"2028-06-13T10:00:00"}')
D_errand_4=$(publish_demand "zl"  "代买早餐包子豆浆 [赵六 开放]" \
  '{"type":"errand","title":"代买早餐包子豆浆","description":"明天早上帮我带两个肉包一杯豆浆，二号食堂一楼包子铺，宿舍送到北区 5 号楼。","location":"二号食堂","rewardType":"point","rewardAmount":8,"attributes":{"pickup_location":"二号食堂一楼"},"deadline":"2028-06-12T08:00:00"}')
D_errand_5=$(publish_demand "sq"  "代打印明天考试资料 [孙七 已取消]" \
  '{"type":"errand","title":"代打印明天考试资料","description":"打印明天的模电考试资料，大概10页，黑白双面。图书馆打印室就行。","location":"图书馆打印室","rewardType":"point","rewardAmount":12,"attributes":{"pickup_location":"图书馆打印室"},"deadline":"2028-06-11T18:00:00"}')
D_errand_6=$(publish_demand "cxh" "代取京东快递 [陈小红 开放]" \
  '{"type":"errand","title":"代取京东快递","description":"京东快递到了，在一号驿站。是个小包裹，很轻。取件码 JD987654。","location":"一号驿站","rewardType":"point","rewardAmount":25,"attributes":{"pickup_location":"一号驿站"},"deadline":"2028-06-15T12:00:00"}')

# --- 二手交易 (trade) ---
info "二手交易…"
D_trade_1=$(publish_demand "zs"  "出售二手《数据结构》教材 [张三→王五 已完成]" \
  '{"type":"trade","title":"出售二手《数据结构》教材","description":"严蔚敏版本，九成新，几乎没有笔记。期末考试复习必备！附赠往年期末真题。","location":"北区宿舍 7 号楼","rewardType":"point","rewardAmount":30,"deadline":"2028-06-20T12:00:00"}')
D_trade_2=$(publish_demand "ls"  "出售九成新台灯 [李四 开放]" \
  '{"type":"trade","title":"出售九成新台灯","description":"LED护眼台灯，三档调光，USB充电。原价89，毕业带不走。","location":"南区宿舍 3 号楼","rewardType":"point","rewardAmount":25,"deadline":"2028-06-25T12:00:00"}')
D_trade_3=$(publish_demand "zb"  "出售闲置机械键盘 [周八 开放]" \
  '{"type":"trade","title":"出售闲置机械键盘","description":"IKBC C87 红轴，用了半年，换了新的所以出。手感很好，送一套 PBT 键帽。","location":"北区宿舍 2 号楼","rewardType":"point","rewardAmount":80,"deadline":"2028-06-30T12:00:00"}')
D_trade_4=$(publish_demand "sq"  "出售二手自行车 [孙七→张三 进行中]" \
  '{"type":"trade","title":"出售二手自行车","description":"捷安特 ATX 660，七成新，变速正常，前后轮胎刚换新的。有车锁。","location":"北区车棚","rewardType":"point","rewardAmount":150,"deadline":"2028-06-28T12:00:00"}')
D_trade_5=$(publish_demand "ww"  "出售《算法导论》二手 [王五 已取消]" \
  '{"type":"trade","title":"出售《算法导论》二手","description":"CLRS 原版影印，计算机专业圣经。书角有轻微折痕，但不影响阅读。","location":"南区宿舍 5 号楼","rewardType":"point","rewardAmount":35,"deadline":"2028-06-22T12:00:00"}')
D_trade_6=$(publish_demand "zs2" "二手显示器 24寸 [郑十 开放]" \
  '{"type":"trade","title":"二手显示器 24寸","description":"Dell U2415 24寸 1080P IPS屏，色彩很棒，做设计/写代码都很合适。箱说全。","location":"南区宿舍 1 号楼","rewardType":"point","rewardAmount":200,"deadline":"2028-06-30T12:00:00"}')

# --- 组队匹配 (team) ---
info "组队匹配…"
D_team_1=$(publish_demand "zb"  "组队参加蓝桥杯校赛 [周八 开放]" \
  '{"type":"team","title":"组队参加蓝桥杯校赛","description":"蓝桥杯校赛选拔，需要一个三人小队。我擅长算法，再找两个队友。最好有 C++/Java 基础。","location":"信息学院实验室","rewardType":"donation","rewardAmount":0,"attributes":{"team_size":4,"team_type":"competition"},"deadline":"2028-06-25T12:00:00"}')
D_team_2=$(publish_demand "zs"  "周末打篮球 4缺1 [张三 开放]" \
  '{"type":"team","title":"周末打篮球 4缺1","description":"周末下午在体育馆篮球场组队打球，现在有3个人，再来1个就能凑一波。水平不限～","location":"体育馆篮球场","rewardType":"donation","rewardAmount":0,"attributes":{"team_size":4,"team_type":"club"},"deadline":"2028-06-14T14:00:00"}')
D_team_3=$(publish_demand "cxh" "图书馆组队自习 [陈小红 进行中]" \
  '{"type":"team","title":"图书馆组队自习","description":"期末季，找个学习搭子一起泡图书馆互相监督。每天 8:00-22:00，可以轮流占座。","location":"校图书馆","rewardType":"donation","rewardAmount":0,"attributes":{"team_size":4,"team_type":"course_project"},"deadline":"2028-06-20T12:00:00"}')
D_team_4=$(publish_demand "wj"  "组队参加大创项目 [吴九 开放]" \
  '{"type":"team","title":"组队参加大创项目","description":"大创项目申报，课题是「校园互助小程序优化」，需要2-3个队友。欢迎有前端/后端经验的同学。","location":"创新楼 301","rewardType":"donation","rewardAmount":0,"attributes":{"team_size":3,"team_type":"competition"},"deadline":"2028-06-30T12:00:00"}')
D_team_5=$(publish_demand "lxm" "求羽毛球搭子 [林小明 开放]" \
  '{"type":"team","title":"求羽毛球搭子","description":"平时晚上或周末打羽毛球，我水平中等（学过一年多）。希望在体育馆打。","location":"体育馆羽毛球馆","rewardType":"donation","rewardAmount":0,"attributes":{"team_size":2,"team_type":"club"},"deadline":"2028-06-18T12:00:00"}')

# --- 失物招领 (lost_found) ---
info "失物招领…"
D_lf_1=$(publish_demand "zl"  "失物：校园卡（学号 2023...） [赵六 开放]" \
  '{"type":"lost_found","title":"失物：校园卡（学号 20230112...）","description":"今天上午在二教到食堂的路上丢了校园卡，卡号记不太清了，是2023开头的。有捡到的同学麻烦联系我！","location":"二教→食堂沿线","rewardType":"donation","rewardAmount":0,"attributes":{"lf_type":"LOST"},"deadline":"2028-06-18T12:00:00"}')
D_lf_2=$(publish_demand "zs"  "捡到一把钥匙（二教 301） [张三 开放]" \
  '{"type":"lost_found","title":"捡到一把钥匙（二教 301）","description":"在二教 301 教室捡到一把钥匙，上面有一个卡通钥匙扣。失主请联系我认领。","location":"二教 301","rewardType":"donation","rewardAmount":0,"attributes":{"lf_type":"FOUND"},"deadline":"2028-06-20T12:00:00"}')
D_lf_3=$(publish_demand "sq"  "丢失黑色钱包 [孙七 已取消]" \
  '{"type":"lost_found","title":"丢失黑色钱包","description":"昨天在一食堂一楼吃饭时遗失黑色钱包，里面有身份证和银行卡。已经挂失了。","location":"一号食堂一楼","rewardType":"donation","rewardAmount":0,"attributes":{"lf_type":"LOST"},"deadline":"2028-06-15T12:00:00"}')
D_lf_4=$(publish_demand "ls"  "捡到 U 盘（图书馆三楼） [李四 开放]" \
  '{"type":"lost_found","title":"捡到 U 盘（图书馆三楼）","description":"在图书馆三楼自习室捡到一个 U 盘，金士顿 32G 蓝色。失主可以通过描述文件内容来认领。","location":"图书馆三楼自习室","rewardType":"donation","rewardAmount":0,"attributes":{"lf_type":"FOUND"},"deadline":"2028-06-25T12:00:00"}')

# --- 学习互助 (study) ---
info "学习互助…"
D_study_1=$(publish_demand "lxm" "求辅导高等数学 [林小明 开放]" \
  '{"type":"study","title":"求辅导高等数学","description":"高数下册，多重积分和曲线曲面积分不太懂。希望找一个数学好的同学辅导 2-3 次，每次 1 小时。","location":"图书馆或自习室","rewardType":"point","rewardAmount":50,"deadline":"2028-06-25T12:00:00"}')
D_study_2=$(publish_demand "cxh" "辅导 C 语言编程 [陈小红→张三 进行中]" \
  '{"type":"study","title":"辅导 C 语言编程","description":"大一下 C 语言，指针和链表部分需要辅导。最好有项目经验的学长学姐。","location":"信息学院机房","rewardType":"point","rewardAmount":40,"deadline":"2028-06-20T12:00:00"}')
D_study_3=$(publish_demand "hxg" "求线性代数期末辅导 [黄小刚 开放]" \
  '{"type":"study","title":"求线性代数期末辅导","description":"线代期末冲刺！特征值特征向量、二次型这块比较薄弱。求学霸救命！","location":"图书馆","rewardType":"point","rewardAmount":60,"deadline":"2028-06-22T12:00:00"}')
D_study_4=$(publish_demand "zl"  "求辅导英语四级 [赵六→陈小红 已完成]" \
  '{"type":"study","title":"求辅导英语四级","description":"四级考试快到了，求英语好的同学辅导，重点在阅读和写作。","location":"外语楼","rewardType":"point","rewardAmount":30,"deadline":"2028-06-10T18:00:00"}')

# --- 其他 (other) ---
info "其他…"
D_other_1=$(publish_demand "hxg" "帮忙搬家（北区宿舍→南区） [黄小刚 开放]" \
  '{"type":"other","title":"帮忙搬家（北区宿舍→南区）","description":"从北区 7 号楼搬到南区 3 号楼，东西不算多（几个纸箱+被子+小家电）。需要一个帮手，大约 1-2 小时。","location":"北区7号楼→南区3号楼","rewardType":"point","rewardAmount":100,"deadline":"2028-06-18T12:00:00"}')
D_other_2=$(publish_demand "zs2" "帮忙装系统 win11 [郑十 开放]" \
  '{"type":"other","title":"帮忙装系统 win11","description":"笔记本想重装 win11 系统，需要有 U 盘启动盘。我对装机不太熟，求帮忙。","location":"南区宿舍 1 号楼","rewardType":"point","rewardAmount":20,"deadline":"2028-06-20T12:00:00"}')
D_other_3=$(publish_demand "lxm" "求帮忙修理自行车链条 [林小明→李四 已完成]" \
  '{"type":"other","title":"求帮忙修理自行车链条","description":"自行车链条掉了，自己装不回去，求会修车的同学帮忙。在北区车棚。","location":"北区车棚","rewardType":"point","rewardAmount":15,"deadline":"2028-06-09T18:00:00"}')

# ---------------------------------------------------------------------------
# Step 5: 操作需求（接单/完成/取消）
# ---------------------------------------------------------------------------
step "5. 需求生命周期操作"

# 5a. 接受需求
accept_demand() {
  local key="$1" name="$2" did="$3" label="$4"
  if [[ -z "$did" ]]; then warn "跳过 $label — demandId 为空"; return; fi
  local resp
  resp=$(api PUT "$API_BASE/demands/$did/accept" "${TOKENS[$key]}")
  local code
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    green "$name 接受了 $label"
  else
    fail "$name 接受 $label 失败: $(echo "$resp" | json_field msg)"
  fi
}

# 5b. 完成需求 (publisher 操作)
complete_demand() {
  local key="$1" name="$2" did="$3" label="$4"
  if [[ -z "$did" ]]; then warn "跳过 $label — demandId 为空"; return; fi
  local resp
  resp=$(api PUT "$API_BASE/demands/$did/complete" "${TOKENS[$key]}")
  local code
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    green "$name 完成了 $label"
  else
    fail "$name 完成 $label 失败: $(echo "$resp" | json_field msg)"
  fi
}

# 5c. 取消需求 (publisher 操作)
cancel_demand() {
  local key="$1" name="$2" did="$3" label="$4"
  if [[ -z "$did" ]]; then warn "跳过 $label — demandId 为空"; return; fi
  local resp
  resp=$(api PUT "$API_BASE/demands/$did/cancel" "${TOKENS[$key]}")
  local code
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    green "$name 取消了 $label"
  else
    fail "$name 取消 $label 失败: $(echo "$resp" | json_field msg)"
  fi
}

info "接单操作…"
# COMPLETED 需求: 接单 → 完成
accept_demand "ls"  "李四" "$D_errand_1"  "代取韵达快递 [张三→李四]"
accept_demand "ww"  "王五" "$D_trade_1"   "二手数据结构教材 [张三→王五]"
accept_demand "zs"  "张三" "$D_study_2"   "辅导 C 语言 [陈小红→张三]"
accept_demand "cxh" "陈小红" "$D_study_4" "辅导英语四级 [赵六→陈小红]"
accept_demand "ls"  "李四" "$D_other_3"   "修自行车链条 [林小明→李四]"

# IN_PROGRESS 需求: 只接单不完成
accept_demand "zs"  "张三" "$D_errand_2"  "南门代取外卖 [王五→张三]"
accept_demand "zs"  "张三" "$D_trade_4"   "二手自行车 [孙七→张三]"

info "完成操作…"
complete_demand "zs"  "张三" "$D_errand_1"  "代取韵达快递"
complete_demand "zs"  "张三" "$D_trade_1"   "二手数据结构教材"
complete_demand "cxh" "陈小红" "$D_study_2" "辅导 C 语言"
complete_demand "zl"  "赵六" "$D_study_4"   "辅导英语四级"
complete_demand "lxm" "林小明" "$D_other_3" "修自行车链条"

info "取消操作…"
cancel_demand "sq"  "孙七" "$D_errand_5"  "代打印考试资料"
cancel_demand "ww"  "王五" "$D_trade_5"   "算法导论二手"
cancel_demand "sq"  "孙七" "$D_lf_3"      "丢失黑色钱包（已找到）"

# ---------------------------------------------------------------------------
# Step 6: 组队申请与审批
# ---------------------------------------------------------------------------
step "6. 组队申请与审批"

apply_team() {
  local key="$1" name="$2" did="$3" label="$4"
  if [[ -z "$did" ]]; then warn "跳过 $label — demandId 为空"; return; fi
  local resp
  resp=$(api POST "$API_BASE/demands/$did/team/apply" "${TOKENS[$key]}" "{\"message\":\"我想加入！\"}")
  local code
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    green "$name 申请加入 $label"
  else
    fail "$name 申请加入 $label 失败: $(echo "$resp" | json_field msg)"
  fi
}

approve_team() {
  local leader_key="$1" leader_name="$2" did="$3" applicant_id="$4" label="$5"
  if [[ -z "$did" ]]; then warn "跳过 $label — demandId 为空"; return; fi
  local resp
  resp=$(api PUT "$API_BASE/demands/$did/team/applicants/$applicant_id/approve" "${TOKENS[$leader_key]}")
  local code
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    green "$leader_name 批准了 userId=$applicant_id 加入 $label"
  else
    fail "$leader_name 审批 $label 失败: $(echo "$resp" | json_field msg)"
  fi
}

info "组队申请…"
# 蓝桥杯校赛: 张三、李四、王五 申请加入 (队长: 周八)
apply_team "zs" "张三" "$D_team_1" "蓝桥杯校赛"
apply_team "ls" "李四" "$D_team_1" "蓝桥杯校赛"
apply_team "ww" "王五" "$D_team_1" "蓝桥杯校赛"
# 周末打篮球: 李四、孙七 申请 (队长: 张三)
apply_team "ls" "李四" "$D_team_2" "周末打篮球"
apply_team "sq" "孙七" "$D_team_2" "周末打篮球"
# 图书馆自习: 林小明、赵六 申请 (队长: 陈小红)
apply_team "lxm" "林小明" "$D_team_3" "图书馆组队自习"
apply_team "zl" "赵六" "$D_team_3" "图书馆组队自习"

info "审批通过…"
# 周八审批 张三、李四 通过（王五 pending）
approve_team "zb"  "周八" "$D_team_1" "${USER_IDS[zs]}" "蓝桥杯 → 张三"
approve_team "zb"  "周八" "$D_team_1" "${USER_IDS[ls]}" "蓝桥杯 → 李四"
# 张三审批 李四、孙七 通过
approve_team "zs"  "张三" "$D_team_2" "${USER_IDS[ls]}" "打篮球 → 李四"
approve_team "zs"  "张三" "$D_team_2" "${USER_IDS[sq]}" "打篮球 → 孙七"
# 陈小红审批 林小明 通过（赵六 pending）
approve_team "cxh" "陈小红" "$D_team_3" "${USER_IDS[lxm]}" "图书馆自习 → 林小明"

# ---------------------------------------------------------------------------
# Step 7: 评价
# ---------------------------------------------------------------------------
step "7. 创建评价"

do_evaluate() {
  local key="$1" name="$2" did="$3" rating="$4" comment="$5" label="$6"
  if [[ -z "$did" ]]; then warn "跳过 $label — demandId 为空"; return; fi
  local resp
  resp=$(api POST "$API_BASE/evaluations" "${TOKENS[$key]}" "{\"demandId\":$did,\"rating\":$rating,\"comment\":\"$comment\"}")
  local code
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    green "$name 评价 $label — $rating 分"
  else
    fail "$name 评价 $label 失败: $(echo "$resp" | json_field msg)"
  fi
}

info "为已完成的需求互评…"
do_evaluate "zs"  "张三" "$D_errand_1" 5 "效率很高，态度很好！" "代取快递 [评价李四]"
do_evaluate "ls"  "李四" "$D_errand_1" 5 "发布者很友善，描述准确" "代取快递 [评价张三]"
do_evaluate "zs"  "张三" "$D_trade_1"  4 "书保护得很好，很满意" "二手教材 [评价王五]"
do_evaluate "ww"  "王五" "$D_trade_1"  5 "交易愉快，还送了真题" "二手教材 [评价张三]"
do_evaluate "cxh" "陈小红" "$D_study_2" 5 "学长讲得很清楚，收获很大" "C语言辅导 [评价张三]"
do_evaluate "zs"  "张三" "$D_study_2" 4 "态度认真，基础不错" "C语言辅导 [评价陈小红]"
do_evaluate "zl"  "赵六" "$D_study_4" 5 "英语很棒，教得很好" "英语四级 [评价陈小红]"
do_evaluate "cxh" "陈小红" "$D_study_4" 4 "学员很努力" "英语四级 [评价赵六]"
do_evaluate "lxm" "林小明" "$D_other_3" 5 "链条秒装好，太强了" "修自行车 [评价李四]"
do_evaluate "ls"  "李四" "$D_other_3" 5 "简单活，顺手帮了" "修自行车 [评价林小明]"

# ---------------------------------------------------------------------------
# Step 8: 设置部分用户的隐私配置（匿名 + 虚拟昵称）
# ---------------------------------------------------------------------------
step "8. 隐私配置"

update_privacy() {
  local key="$1" name="$2" is_anon="$3" mask="$4"
  local resp
  resp=$(api PUT "$API_BASE/user/profile" "${TOKENS[$key]}" "{\"isAnonymous\":$is_anon,\"maskName\":\"$mask\"}")
  local code
  code=$(echo "$resp" | json_field code)
  if [[ "$code" == "200" ]]; then
    green "$name 隐私设置已更新 (匿名=$is_anon, 昵称=$mask)"
  else
    fail "$name 隐私设置失败: $(echo "$resp" | json_field msg)"
  fi
}

update_privacy "ww"  "王五" "true"  "热心市民小王"
update_privacy "sq"  "孙七" "true"  "匿名雷锋"
update_privacy "zs2" "郑十" "false" ""

# ---------------------------------------------------------------------------
# Step 9: 打印摘要
# ---------------------------------------------------------------------------
step "演示数据填充完成"

echo ""
echo "  ┌──────────────────────────────────────────────┐"
echo "  │          📊 演示数据摘要                    │"
echo "  ├──────────────────────────────────────────────┤"
echo "  │  用户:         11 人 (统一密码 123456)      │"
echo "  │  需求:         ~28 条                       │"
echo "  │    ├ errand:     6 (完成1/进行中1/开放3/取消1) │"
echo "  │    ├ trade:      6 (完成1/进行中1/开放3/取消1) │"
echo "  │    ├ team:       5 (进行中1/开放4)           │"
echo "  │    ├ lost_found: 4 (开放3/取消1)             │"
echo "  │    ├ study:      4 (完成1/进行中1/开放2)     │"
echo "  │    └ other:      3 (完成1/开放2)             │"
echo "  │  签到:         7 人已签 / 4 人未签           │"
echo "  │  评价:         10 条 (4个完成的需求互评)     │"
echo "  │  组队:         7 人申请 + 5 人已审批         │"
echo "  │  匿名用户:     2 人                          │"
echo "  └──────────────────────────────────────────────┘"
echo ""
echo "  🚀 现在运行: cd frontend && npm run dev"
echo "  🔑 登录示例: 学号 2024001001  密码 123456"
echo "  👤 管理员:   admin / admin123"
echo ""

# ============================================================================
# 脚本结束
# ============================================================================
