function getCSSVar(name: string): string {
    const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
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
            borderRadius: getCSSVar('--radius-full'),
            fontFamily: getCSSVar('--font-family'),
        },

        Button: {
            borderRadius: getCSSVar('--radius-full'),
            heightMedium: getCSSVar('--btn-h'),
            paddingMedium: `0 ${getCSSVar('--space-xl')}`,
            fontSizeMedium: getCSSVar('--font-md'),
            textColorPrimary: getCSSVar('--btn-primary-text'),
            colorPrimary: getCSSVar('--btn-primary-bg'),
            colorHoverPrimary: getCSSVar('--btn-primary-hover-bg'),
            borderPrimary: getCSSVar('--btn-primary-border'),
            borderHoverPrimary: getCSSVar('--btn-primary-hover-border'),
        }
    }
}
