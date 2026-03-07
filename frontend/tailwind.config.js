/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      // 可以在这里添加自定义主题配置
    },
  },
  plugins: [],
  // 避免与 Element Plus 冲突
  corePlugins: {
    preflight: false,
  },
}
