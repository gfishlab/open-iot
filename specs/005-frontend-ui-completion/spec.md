# Feature Specification: IoT 平台前端 UI 补全

**Feature Branch**: `005-frontend-ui-completion`
**Created**: 2026-03-18
**Status**: Draft
**Input**: User description: "完善前后端联调，包括页面调试点击和页面元素检测"

## 背景

004 分支已完成后端核心 API（OTA 固件管理、OTA 升级任务、设备影子、设备轨迹、历史数据查询等），但前端 UI 尚未配套实现。本规范专注于补全这些功能的前端页面，确保前后端联调完整，同时优化现有页面的玻璃拟态设计一致性。

**现有完成度**：
- ✅ 后端 OTA 固件/任务 API 已完成 (004)
- ✅ 后端设备影子 API 已完成 (004)
- ✅ 后端设备轨迹 API 已完成 (004)
- ✅ 后端历史数据 API 已完成 (004)
- ❌ 前端 OTA 管理页面缺失
- ❌ 前端设备影子 UI 缺失
- ❌ 前端设备轨迹 UI 缺失
- ❌ 前端部分页面玻璃拟态样式不一致

**本次补全目标**：
1. 创建 OTA 固件管理页面
2. 创建 OTA 升级任务页面
3. 在设备详情页添加设备影子标签页
4. 在设备详情页添加设备轨迹标签页
5. 修复现有页面的玻璃拟态样式一致性
6. 完善路由菜单和导航

## User Scenarios & Testing

### User Story 1 - OTA 固件管理 (Priority: P1)

作为设备管理员，我需要通过 Web 界面上传设备固件包，管理固件版本，支持版本查看、文件下载和删除操作。

**Why this priority**: 后端 API 已就绪，配套 UI 是管理员使用 OTA 功能的入口。

**Acceptance Scenarios**:

1. **Given** 管理员进入 OTA 固件管理页面，**When** 点击"上传固件"按钮，**Then** 弹出上传对话框，支持选择产品、填写版本号、上传固件文件
2. **Given** 管理员选择固件文件（.bin/.hex/.elf），**When** 文件上传完成，**Then** 列表显示新固件记录，包含文件大小、版本号、上传时间
3. **Given** 管理员点击固件列表的"下载"按钮，**When** 浏览器开始下载固件文件，**Then** 文件名格式为 `{固件名}_{版本号}.bin`
4. **Given** 管理员点击固件列表的"删除"按钮，**When** 确认删除操作，**Then** 固件记录从列表移除

---

### User Story 2 - OTA 升级任务管理 (Priority: P1)

作为设备管理员，我需要创建 OTA 升级任务，选择目标设备和固件版本，跟踪升级进度，处理升级失败设备。

**Why this priority**: 后端 API 已就绪，配套 UI 是执行 OTA 升级的操作界面。

**Acceptance Scenarios**:

1. **Given** 管理员进入 OTA 升级任务页面，**When** 点击"创建升级任务"，**Then** 弹出创建对话框，支持选择产品、固件版本、升级范围
2. **Given** 管理员选择"指定设备"范围，**When** 选择具体设备后提交，**Then** 任务创建成功，列表显示新任务
3. **Given** 升级任务执行中，**When** 管理员点击"设备状态"按钮，**Then** 弹出对话框显示每台设备的升级状态（待升级/下载中/安装中/成功/失败）
4. **Given** 任务状态为"待执行"，**When** 管理员点击"开始"按钮，**Then** 任务状态变更为"执行中"，设备开始接收升级通知

---

### User Story 3 - 设备影子管理 (Priority: P1)

作为应用开发者，我需要在设备详情页查看设备影子数据（reported/desired/delta），并能修改期望状态。

**Why this priority**: 设备影子是 004 后端实现的核心功能，前端 UI 是查看和操作的入口。

**Acceptance Scenarios**:

1. **Given** 用户进入设备详情页，**When** 点击"设备影子"标签页，**Then** 显示三列数据：Reported（设备上报）、Desired（期望状态）、Delta（差异）
2. **Given** 设备影子有数据，**When** 查看三列内容，**Then** JSON 格式高亮显示，版本号和更新时间可见
3. **Given** 用户点击"修改"按钮（Desired 区域），**When** 弹出修改对话框，**Then** 可编辑期望状态的 JSON，需输入当前版本号（乐观锁）
4. **Given** 用户提交期望状态修改，**When** 版本号匹配，**Then** 修改成功，Delta 区域自动计算差异

---

### User Story 4 - 设备轨迹管理 (Priority: P2)

作为设备管理员，我需要在设备详情页查看设备历史轨迹，在地图上显示移动路径，查看统计数据（距离、速度等）。

**Why this priority**: 后端 API 已就绪，轨迹可视化是定位设备的核心功能。

**Acceptance Scenarios**:

1. **Given** 用户进入设备详情页，**When** 点击"设备轨迹"标签页，**Then** 显示时间范围选择器和地图组件
2. **Given** 用户选择时间范围并查询，**When** 后端返回轨迹点数据，**Then** 地图上绘制移动路径，标记起点和终点
3. **Given** 轨迹数据加载完成，**When** 查看统计卡片，**Then** 显示总距离、平均速度、最大速度、轨迹点数量

---

### User Story 5 - 玻璃拟态样式一致性 (Priority: P2)

作为最终用户，我需要所有页面保持统一的视觉风格（玻璃拟态设计），避免页面间样式不一致。

**Why this priority**: 004 开发过程中部分页面未完全应用玻璃拟态样式，需要统一修复。

**Acceptance Scenarios**:

1. **Given** 用户浏览任意列表页面，**When** 查看操作按钮，**Then** 所有按钮都应用 `glass-button` 类，悬停有发光效果
2. **Given** 用户浏览任意标签页面，**When** 查看标签元素，**Then** 所有标签都应用 `glass-tag` 类
3. **Given** 用户浏览列表页面，**When** 滚动到底部，**Then** 分页器应用 `glass-pagination` 类
4. **Given** 用户查看卡片组件，**When** 悬停在卡片上，**Then** 卡片背景加深、边框发光、阴影增强

---

### User Story 6 - 路由菜单和导航完善 (Priority: P2)

作为最终用户，我需要通过侧边栏导航访问所有功能页面，包括新增的 OTA 管理、设备轨迹等功能。

**Why this priority**: 新增页面需要在导航菜单中有入口，否则用户无法发现。

**Acceptance Scenarios**:

1. **Given** 用户登录系统，**When** 查看侧边栏菜单，**Then** 显示"OTA 固件"和"OTA 任务"菜单项
2. **Given** 用户点击"OTA 固件"菜单，**When** 菜单被激活，**Then** 路由跳转到 `/ota/firmware` 页面
3. **Given** 用户在设备详情页，**When** 查看标签页，**Then** 包含"实时数据"、"数据历史"、"服务调用"、"事件记录"、"设备影子"、"设备轨迹"六个标签

## Requirements

### Functional Requirements

**P1 - 高优先级**

- **FR-001**: 系统 MUST 提供OTA 固件管理页面 (`/ota/firmware`)，支持固件上传、列表查看、文件下载、删除操作
- **FR-002**: 系统 MUST 提供OTA 升级任务页面 (`/ota/tasks`)，支持任务创建、状态查看、设备状态查看、任务控制（开始/暂停/取消）
- **FR-003**: 系统 MUST 在设备详情页添加"设备影子"标签页，显示 reported/desired/delta 三层数据
- **FR-004**: 系统 MUST 支持通过 UI 修改设备期望状态（desired），使用版本号进行并发控制

**P2 - 中优先级**

- **FR-005**: 系统 MUST 在设备详情页添加"设备轨迹"标签页，支持时间范围查询和地图显示
- **FR-006**: 系统 MUST 在侧边栏导航添加"OTA 固件"和"OTA 任务"菜单项
- **FR-007**: 所有列表页面的操作按钮 MUST 应用 `glass-button` 类
- **FR-008**: 所有标签元素 MUST 应用 `glass-tag` 类
- **FR-009**: 所有分页器 MUST 应用 `glass-pagination` 类

### UI/UX Requirements

- **UI-001**: 新增页面 MUST 遵循玻璃拟态设计风格（深色背景、半透明卡片、模糊效果、悬停发光）
- **UI-002**: 列表页面 MUST 采用弹性布局填满容器高度，表格自动扩展（参考 `DeviceList.vue` 的 flex 布局）
- **UI-003**: 表单对话框 MUST 使用统一的头部样式和底部按钮布局
- **UI-004**: 加载状态、空状态、错误状态 MUST 有友好的视觉反馈

## Key Pages

### 新增页面

| 页面路径 | 组件文件 | 功能描述 |
|---------|---------|---------|
| `/ota/firmware` | `ota/FirmwareList.vue` | OTA 固件列表、上传、下载、删除 |
| `/ota/tasks` | `ota/TaskList.vue` | OTA 升级任务列表、创建、状态查看 |

### 修改页面

| 页面路径 | 组件文件 | 修改内容 |
|---------|---------|---------|
| `/devices/:id` | `device/DeviceDetail.vue` | 添加"设备影子"和"设备轨迹"标签页 |
| `/` | `layouts/MainLayout.vue` | 添加 OTA 相关菜单项 |

## Success Criteria

### Measurable Outcomes

- **SC-001**: OTA 固件页面可成功上传固件，文件大小正确显示，下载功能正常
- **SC-002**: OTA 任务页面可创建升级任务，设备状态对话框正确显示统计数据
- **SC-003**: 设备影子标签页正确显示三层数据，期望状态修改成功后 Delta 自动更新
- **SC-004**: 侧边栏菜单包含所有新增功能的入口，路由跳转正常
- **SC-005**: 所有列表页面的按钮样式统一应用玻璃拟态效果
- **SC-006**: 设备详情页的六个标签页都能正常切换和加载数据

## Technical Notes

### 前端技术栈

- Vue 3 + TypeScript
- Element Plus UI 框架
- 玻璃拟态样式 (`glassmorphism.css`)
- ECharts 图表库（轨迹可视化）
- Axios HTTP 客户端

### 后端 API 映射

| 功能 | 后端 API | 前端页面 |
|------|---------|---------|
| 固件上传 | `POST /api/v1/ota/firmware` | `FirmwareList.vue` 上传对话框 |
| 固件列表 | `GET /api/v1/ota/firmware` | `FirmwareList.vue` 表格 |
| 固件下载 | `GET /api/v1/ota/firmware/{id}/download` | `FirmwareList.vue` 下载按钮 |
| 创建任务 | `POST /api/v1/ota/tasks` | `TaskList.vue` 创建对话框 |
| 任务列表 | `GET /api/v1/ota/tasks` | `TaskList.vue` 表格 |
| 设备状态 | `GET /api/v1/ota/tasks/{id}/devices` | `TaskList.vue` 设备状态对话框 |
| 设备影子 | `GET /api/v1/devices/{id}/shadow` | `DeviceDetail.vue` 影子标签页 |
| 更新期望 | `PUT /api/v1/devices/{id}/shadow/desired` | `DeviceDetail.vue` 修改对话框 |
| 设备轨迹 | `GET /api/v1/devices/{id}/trajectory/query` | `DeviceDetail.vue` 轨迹标签页 |

## Assumptions

1. 后端 API 在 004 分支已完整实现且可用
2. 设备轨迹地图组件使用轻量级方案（如 Leaflet 或简单的 SVG 绘制），暂不集成重型地图 SDK
3. 固件文件上传限制为 50MB，使用 `multipart/form-data` 格式
4. 设备影子修改需要乐观锁，前端需要处理 409 冲突错误
5. 所有新增页面遵循玻璃拟态设计规范，样式复用现有 CSS 类

## Dependencies

- 依赖 `004-iot-platform-completion` 分支的后端 API
- 依赖 `frontend/src/styles/glassmorphism.css` 玻璃拟态样式
- 依赖 `frontend/src/api` 模块添加 OTA 相关 API 定义
