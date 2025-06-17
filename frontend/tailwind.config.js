import defaultTheme from 'tailwindcss/defaultTheme'

export default {
    darkMode: 'class',
    content: [
        './index.html',
        './src/**/*.{vue,js,ts,jsx,tsx}',
    ],
    theme: {
        extend: {
            colors: {
                bg: 'var(--bg-main)',                     // 背景主色
                'bg-sub': 'var(--bg-sub)',                // 背景次级色，补充变量建议
                text: 'var(--text-main)',                  // 文字主色
                'text-muted': 'var(--text-muted)',         // 文字次色
                primary: 'var(--color-primary, #10A37F)',  // 主色，带回退
                secondary: 'var(--color-secondary, #2563EB)',
                border: 'var(--border-main, #2D2F37)',    // 边框主色
                error: 'var(--color-error, #EF4444)',     // 错误色
                success: 'var(--color-success, #22C55E)', // 成功色
                warning: 'var(--color-warning, #F59E0B)', // 警告色（建议添加）
                info: 'var(--color-info, #3B82F6)',       // 信息色（建议添加）

                // 你用到的轻色调，比如成功淡色等，可按需补充：
                'success-light': 'var(--success-light)',
                'error-light': 'var(--error-light)',
                'warning-light': 'var(--warning-light)',
                'info-light': 'var(--info-light)',
            },
            fontFamily: {
                sans: ['var(--font-family)', ...defaultTheme.fontFamily.sans],
            },
            borderRadius: {
                xl: '1rem',
                '2xl': '1.5rem',
                full: '9999px',
            },
            boxShadow: {
                card: '0 4px 12px rgba(0, 0, 0, 0.15)',
                soft: '0 1px 3px rgba(0, 0, 0, 0.08)',
                modal: 'var(--shadow-modal)',  // 直接对应你的阴影变量
                focus: 'var(--shadow-focus)',  // 焦点阴影
            },
            spacing: {
                layout: '1.5rem',
                sm: '0.5rem',    // 建议补全 spacing 变量
                md: '1rem',
                lg: '1.5rem',
                xl: '2rem',
            },
            maxWidth: {
                content: '1280px',
                '100rem': '100rem',
            },
            transitionProperty: {
                height: 'height',
            },
            keyframes: {
                fadeIn: {
                    '0%': { opacity: '0' },
                    '100%': { opacity: '1' },
                },
            },
            animation: {
                fadeIn: 'fadeIn 0.3s ease-in-out',
            },
        },
    },
    plugins: [],
}
