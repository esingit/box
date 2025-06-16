module.exports = {
    content: ['./src/**/*.{vue,js,ts,jsx,tsx}'],
    theme: {
        extend: {
            colors: {
                success: 'var(--success)',
                error: 'var(--error)',
                warning: 'var(--warning)',
                info: 'var(--info)',

                'success-light': 'var(--success-light)',
                'error-light': 'var(--error-light)',
                'warning-light': 'var(--warning-light)',
                'info-light': 'var(--info-light)',

                'bg-sub': 'var(--bg-sub)',
                'border-sub': 'var(--border-sub)',

                'text-muted': 'var(--text-muted)',
                'text-main': 'var(--text-main)',
            },
            spacing: {
                sm: 'var(--space-sm)',
                md: 'var(--space-md)',
            },
            borderRadius: {
                lg: 'var(--radius-lg)',
            },
            fontWeight: {
                medium: 'var(--font-medium)',
            },
            fontFamily: {
                sans: ['var(--font-family)'],
            },
        },
    },
    plugins: [],
}
