export function useDebounce<T extends (...args: any[]) => any>(
    func: T,
    wait: number
): (...args: Parameters<T>) => void {
    let timeout: ReturnType<typeof setTimeout>
    return (...args: Parameters<T>) => {
        clearTimeout(timeout)
        timeout = setTimeout(() => func(...args), wait)
    }
}

export function useQuickDebounce<T extends (...args: any[]) => any>(
    func: T,
    wait: number = 50
): (...args: Parameters<T>) => void {
    let timeout: ReturnType<typeof setTimeout>
    let lastCallTime = 0

    return (...args: Parameters<T>) => {
        const now = Date.now()
        const timeSinceLastCall = now - lastCallTime

        if (timeSinceLastCall >= wait) {
            lastCallTime = now
            func(...args)
        } else {
            clearTimeout(timeout)
            timeout = setTimeout(() => {
                lastCallTime = Date.now()
                func(...args)
            }, wait - timeSinceLastCall)
        }
    }
}