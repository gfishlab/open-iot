# 娡块更新范围

## MainLayout.vue (主布局)
**Files:**
- modify: `frontend/src/layouts/MainLayout.vue`

**步骤:**
1. 添加侧边栏玻璃效果类 `.glass-sidebar`
2. 添加 Header 玻璃效果类 `.glass-header`
3. 添加菜单玻璃效果类 `.glass-menu-item`
4. 更新过渡效果:添加 `transition-colors duration-200`

**技术栈:** Vue 3 + Tailwind CSS

**测试:** 视觉检查 - 悬停效果是否正确
- **遵循规范:** 代码注释, 空格适中

---

## 设备监控页面

**Files:**
- modify: `frontend/src/views/monitor/DeviceMonitor.vue`

**步骤:**
1. 添加统计卡片玻璃效果类 `.glass-stat-card`
2. 添加状态指示器发光效果
3. 添加脉冲动画类 `.pulse`
4. 更新过渡效果:添加 `transition-colors duration-200`

**技术栈:** Vue 3 + Tailwind CSS

**测试:** 视觉检查 - 悬停效果是否正确
- **遵循规范:** 代码注释, 空格适中

- **性能:** 动画流畅,无卡顿

---
## 产品列表页面
**Files:**
- modify: `frontend/src/views/product/ProductList.vue`

**步骤:**
1. 添加表格容器玻璃效果类 `.glass-table`
2. 添加操作按钮玻璃效果 + 发光
3. 添加状态标签渐变效果
4. 添加分页器玻璃背景
5. 更新过渡效果:添加 `transition-colors duration-200`

**技术栈:** Vue 3 + Element Plus + Tailwind CSS

**测试:** 视觉检查 - 悬停效果是否正确
- **遵循规范:** 代码注释, 空格适中
- **兼容性:** 确保 Element Plus 组件正常工作

- **性能:** 不添加大量动画,避免性能问题

---
## 设备列表页面
**Files:**
- modify: `frontend/src/views/device/DeviceList.vue`

**步骤:**
1. 添加表格容器玻璃效果类 `.glass-table`
2. 添加状态标签发光效果
3. 添加操作按钮玻璃效果
4. 添加分页器玻璃背景
5. 更新过渡效果:添加 `transition-colors duration-200`

**技术栈:** Vue 3 + Element Plus + Tailwind CSS

**测试:** 视觉检查 - 悬停效果是否正确
- **遵循规范:** 代码注释, 空格适中
- **兼容性:** 确保 Element Plus 组件正常工作
- **性能:** 不添加大量动画,避免性能问题

---
## 告警列表页面
**Files:**
- modify: `frontend/src/views/alert/AlertList.vue`

**步骤:**
1. 添加表格容器玻璃效果类 `.glass-table`
2. 添加告警级别颜色区分
3. 添加操作按钮发光效果
4. 添加分页器玻璃背景
5. 更新过渡效果:添加 `transition-colors duration-200`

**技术栈:** Vue 3 + Element Plus + Tailwind CSS
**测试:** 视觉检查 - 悬停效果是否正确
- **遵循规范:** 代码注释, 空格适中
- **兼容性:** 确保 Element Plus 组件正常工作
- **性能:** 不添加大量动画,避免性能问题

---
## 登录页面
**Files:**
- modify: `frontend/src/views/auth/Login.vue`

**步骤:**
1. 添加登录卡片玻璃效果类 `.glass-login-card`
2. 添加输入框玻璃效果 + 发光
3. 添加登录按钮玻璃效果 + 渐变
4. 添加背景渐变动画
5. 更新过渡效果:添加 `transition-colors duration-200`

**技术栈:** Vue 3 + Element Plus + Tailwind CSS

**测试:** 视觉检查 - 悬停效果是否正确
- **遵循规范:** 代码注释, 空格适中
- **性能:** 緻加背景渐变动画可能影响性能,需测试加载速度

- **兼容性:** 确保 Element Plus 组件正常工作

---
## 共享样式文件
**Files:**
- create: `frontend/src/styles/glassmorphism.css`

**步骤:**
1. 定义玻璃效果变量
2. 定义玻璃卡片类
3. 定义玻璃按钮类
4. 定义玻璃输入框类
5. 定义悬停效果类
6. 定义动画工具类
7. 定义渐变工具类

8. 定义脉冲动画

**技术栈:** CSS

**测试:** 视觉检查 - 各个类是否正确应用
- **遵循规范:** 代码注释详细
- **兼容性:** 緻加 !important 标记避免覆盖问题
- **性能:** 测试动画性能

- **浏览器兼容:** 测试多种浏览器
- **可访问性:** 测试键盘导航和屏幕阅读器
- **响应式:** 测试不同屏幕尺寸
- **暗黑模式:** 在暗黑模式下测试视觉效果
