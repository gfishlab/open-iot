<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
/* CSS Reset */
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 根元素 */
:root {
  --color-bg-primary: #0f172a;
  --color-bg-secondary: #1e293b;
  --color-bg-tertiary: #334155;
  --color-text-primary: #f1f5f9;
  --color-text-secondary: #94a3b8;
  --color-text-muted: #64748b;
  --color-border: rgba(255, 255, 255, 0.1);
  --color-accent: #6366f1;
  --color-accent-light: #a5b4fc;
  --color-success: #34d399;
  --color-warning: #fbbf24;
  --color-danger: #f87171;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.15);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.2);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.3);
}

/* HTML & Body */
html, body {
  height: 100%;
  width: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  background: var(--color-bg-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 链接 */
a {
  color: var(--color-accent-light);
  text-decoration: none;
  transition: color 0.2s ease;
}

a:hover {
  color: var(--color-accent);
}

/* 滚动条 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* Element Plus 主题覆盖 - 深色主题 */
.el-card {
  --el-card-bg-color: var(--color-bg-secondary);
  --el-card-border-color: var(--color-border);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.el-card__header {
  border-color: var(--color-border);
  color: var(--color-text-primary);
}

.el-table {
  --el-table-bg-color: transparent;
  --el-table-header-bg-color: var(--color-bg-tertiary);
  --el-table-tr-color: var(--color-text-primary);
  --el-table-border-color: var(--color-border);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.05);
  background: transparent;
}

.el-table th.el-table__cell {
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
  font-weight: 600;
}

.el-table td.el-table__cell {
  color: var(--color-text-primary);
}

.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background: rgba(255, 255, 255, 0.02);
}

.el-button--primary {
  --el-button-bg-color: var(--color-accent);
  --el-button-border-color: var(--color-accent);
  --el-button-hover-bg-color: #4f46e5;
  --el-button-hover-border-color: #4f46e5;
}

.el-button--default {
  --el-button-bg-color: var(--color-bg-tertiary);
  --el-button-border-color: var(--color-border);
  --el-button-text-color: var(--color-text-primary);
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.1);
}

.el-input__wrapper {
  --el-input-bg-color: var(--color-bg-tertiary);
  --el-input-border-color: var(--color-border);
  --el-input-text-color: var(--color-text-primary);
  --el-input-placeholder-color: var(--color-text-muted);
}

.el-input__inner {
  background: var(--color-bg-tertiary);
  border-color: var(--color-border);
  color: var(--color-text-primary);
}

.el-input__inner:focus {
  border-color: var(--color-accent);
}

.el-select-dropdown {
  background: var(--color-bg-secondary);
  border-color: var(--color-border);
}

.el-select-dropdown__item {
  color: var(--color-text-primary);
}

.el-select-dropdown__item.hover {
  background: rgba(255, 255, 255, 0.05);
}

.el-tag {
  border-radius: 6px;
}

.el-tag--success {
  --el-tag-bg-color: rgba(52, 211, 153, 0.15);
  --el-tag-border-color: transparent;
  --el-tag-text-color: #34d399;
}

.el-tag--warning{
  --el-tag-bg-color: rgba(251, 191, 36, 0.15);
  --el-tag-border-color: transparent;
  --el-tag-text-color: #fbbf24;
}

.el-tag--danger{
  --el-tag-bg-color: rgba(248, 113, 113, 0.15);
  --el-tag-border-color: transparent;
  --el-tag-text-color: #f87171;
}

.el-tag--info{
  --el-tag-bg-color: rgba(99, 102, 241, 0.15);
  --el-tag-border-color: transparent;
  --el-tag-text-color: #a5b4fc;
}

.el-dialog {
  --el-dialog-bg-color: var(--color-bg-secondary);
  --el-dialog-border-radius: var(--radius-lg);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
}

.el-dialog__header {
  color: var(--color-text-primary);
}

.el-dialog__title {
  color: var(--color-text-primary);
}

.el-form-item__label {
  color: var(--color-text-secondary);
}

.el-pagination {
  --el-pagination-bg-color: var(--color-bg-tertiary);
  --el-pagination-text-color: var(--color-text-secondary);
  --el-pagination-button-bg-color: var(--color-bg-tertiary);
  --el-pagination-button-color: var(--color-text-primary);
  --el-pagination-hover-color: var(--color-accent);
}

.el-timeline-item__tail {
  border-left-color: var(--color-border);
}

.el-timeline-item__node {
  background: var(--color-bg-tertiary);
}

.el-empty__description {
  color: var(--color-text-muted);
}

/* 工具提示 */
.el-tooltip__popper {
  background: var(--color-bg-secondary) !important;
  border: 1px solid var(--color-border);
}

/* 消息提示 */
.el-message {
  --el-message-bg-color: var(--color-bg-secondary);
  --el-message-border-color: var(--color-border);
}

/* 加载遮罩 */
.el-loading-mask {
  background: rgba(15, 23, 42, 0.8);
}

.el-loading-spinner .path {
  stroke: var(--color-accent);
}

.el-loading-spinner .el-loading-text {
  color: var(--color-text-secondary);
}
</style>
