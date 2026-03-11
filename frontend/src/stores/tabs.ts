import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

/**
 * 标签页数据结构
 */
export interface TabItem {
  path: string           // 路由路径
  title: string          // 标签页标题
  name: string           // 路由名称
  query?: object         // 查询参数
  closable: boolean      // 是否可关闭
  affix?: boolean        // 是否固定(不可关闭)
  needRefresh?: number   // 刷新时间戳(内部使用)
}

/**
 * 标签页管理 Store
 * 负责管理多标签页的打开、关闭、刷新等操作
 */
export const useTabsStore = defineStore('tabs', () => {
  // 标签页列表
  const tabList = ref<TabItem[]>([])

  // 当前激活的标签页路径
  const activeTab = ref<string>('')

  // 缓存的组件名称列表(用于 keep-alive)
  const cachedViews = computed(() => {
    return tabList.value
      .filter(tab => tab.name)
      .map(tab => tab.name)
  })

  /**
   * 初始化标签页
   * 从 localStorage 恢复当前激活的标签页路径
   */
  function initTabs() {
    const savedActiveTab = localStorage.getItem('activeTab')
    if (savedActiveTab) {
      activeTab.value = savedActiveTab
    }
  }

  /**
   * 添加标签页
   * @param route 路由对象
   */
  function addTab(route: RouteLocationNormalized) {
    const { path, name, meta, query } = route

    // 忽略没有名称的路由
    if (!name) return

    // 检查标签页是否已存在
    const isExists = tabList.value.some(tab => tab.path === path)
    if (isExists) {
      // 已存在则激活该标签页
      activeTab.value = path
      return
    }

    // 添加新标签页
    tabList.value.push({
      path,
      name: name as string,
      title: (meta.title as string) || (name as string),
      query: query ? { ...query } : undefined,
      closable: meta.closable !== false, // 默认可关闭
      affix: meta.affix === true        // 默认不固定
    })

    // 激活新标签页
    activeTab.value = path
  }

  /**
   * 关闭指定标签页
   * @param path 标签页路径
   * @returns 关闭后需要跳转的路径(如果关闭的是当前激活标签)
   */
  function closeTab(path: string): string | null {
    // 不能关闭固定标签页
    const tab = tabList.value.find(t => t.path === path)
    if (tab?.affix) return null

    const index = tabList.value.findIndex(t => t.path === path)
    if (index === -1) return null

    tabList.value.splice(index, 1)

    // 如果关闭的是当前激活的标签页,需要切换到其他标签页
    if (activeTab.value === path) {
      // 优先切换到后一个标签页,没有则切换到前一个
      const nextTab = tabList.value[index] || tabList.value[index - 1]
      if (nextTab) {
        activeTab.value = nextTab.path
        localStorage.setItem('activeTab', nextTab.path)
        return nextTab.path
      }
      // 没有其他标签页,返回 null(由调用方决定跳转)
      activeTab.value = ''
      localStorage.removeItem('activeTab')
      return null
    }

    return null
  }

  /**
   * 关闭其他标签页(保留指定标签页和所有固定标签页)
   * @param path 保留的标签页路径
   */
  function closeOtherTabs(path: string) {
    tabList.value = tabList.value.filter(tab => tab.path === path || tab.affix)
    activeTab.value = path
  }

  /**
   * 关闭所有标签页(仅保留固定标签页)
   * @returns 需要跳转的路径(如果有固定标签页则跳转到第一个固定标签页)
   */
  function closeAllTabs(): string | null {
    const affixTabs = tabList.value.filter(tab => tab.affix)
    tabList.value = affixTabs

    if (affixTabs.length > 0) {
      activeTab.value = affixTabs[0].path
      return affixTabs[0].path
    }

    activeTab.value = ''
    return null
  }

  /**
   * 刷新指定标签页
   * 通过从 cachedViews 中移除来实现刷新效果
   * @param path 标签页路径
   */
  function refreshTab(path: string) {
    // 注意:这里不直接修改 cachedViews,因为它是一个 computed
    // 实际刷新逻辑在组件中通过重新渲染实现
    // 这里可以触发一个事件或状态变更,让组件监听并重新渲染
    const tab = tabList.value.find(t => t.path === path)
    if (tab) {
      // 标记该标签页需要刷新(组件监听此属性实现刷新)
      tab.needRefresh = Date.now()
    }
  }

  /**
   * 设置当前激活的标签页
   * @param path 标签页路径
   */
  function setActiveTab(path: string) {
    activeTab.value = path
    // 持久化到 localStorage
    localStorage.setItem('activeTab', path)
  }

  /**
   * 清空所有标签页(退出登录时调用)
   */
  function clearTabs() {
    tabList.value = []
    activeTab.value = ''
    localStorage.removeItem('activeTab')
  }

  return {
    tabList,
    activeTab,
    cachedViews,
    initTabs,
    addTab,
    closeTab,
    closeOtherTabs,
    closeAllTabs,
    refreshTab,
    setActiveTab,
    clearTabs
  }
})
