// theme/dynamic-theme.ts
function getCSSVar(name: string): string {
    const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
    // 若为空，则使用默认值或抛错提示更好排查
    if (!value) {
        console.warn(`CSS variable ${name} is not defined or empty`)
    }
    return value
}

export function createDynamicNaiveTheme() {
    return {
        common: {
            primaryColor: getCSSVar('--primary'),
            primaryColorHover: getCSSVar('--primary-hover'),
            primaryColorPressed: getCSSVar('--primary'),
            primaryColorSuppl: getCSSVar('--primary'),

            successColor: getCSSVar('--success'),
            warningColor: getCSSVar('--warning'),
            errorColor: getCSSVar('--error'),
            infoColor: getCSSVar('--info'),

            borderColor: getCSSVar('--border-main'),
            borderColorHover: getCSSVar('--border-hover'),

            textColorBase: getCSSVar('--text-main'),
            textColor1: getCSSVar('--text-main'),
            textColor2: getCSSVar('--text-sub'),
            textColor3: getCSSVar('--text-muted'),
            textColorDisabled: getCSSVar('--border-sub'),

            placeholderColor: getCSSVar('--text-muted'),
            placeholderColorDisabled: getCSSVar('--border-sub'),

            bodyColor: getCSSVar('--bg-main'),
            cardColor: getCSSVar('--bg-sub'),
            modalColor: getCSSVar('--bg-muted'),
            popoverColor: getCSSVar('--bg-main'),
            tableColor: getCSSVar('--bg-main'),

            dividerColor: getCSSVar('--border-sub'),
            borderRadius: getCSSVar('--radius-md'),
            fontFamily: getCSSVar('--font-family'),
        }
    }
}
